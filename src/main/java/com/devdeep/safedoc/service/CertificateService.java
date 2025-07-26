package com.devdeep.safedoc.service;

import com.devdeep.safedoc.dto.CertificateDTO;
import com.devdeep.safedoc.dto.CertificateMetadataDTO;
import com.devdeep.safedoc.dto.CertificateRequest;
import com.devdeep.safedoc.dto.PendingCertificate;
import com.devdeep.safedoc.entity.Certificate;
import com.devdeep.safedoc.exception.BlockchainOperationException;
import com.devdeep.safedoc.exception.ResourceNotFoundException;
import com.devdeep.safedoc.repository.CertificateRepository;
import com.devdeep.safedoc.utils.BlockchainUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safedoc.contracts.CertificateRegistry;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;


import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple6;
import org.web3j.crypto.Hash;
import org.springframework.web.multipart.MultipartFile;


import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service


public class CertificateService {
    private final CertificateRepository certificateRepository;
    private final CertificateRegistry contract;
    private final Web3j web3j;
    private final IPFSService ipfsService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, PendingCertificate> kafkaTemplate;

    private static final String CACHE_PREFIX = "certificate::";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final String PENDING_CERTIFICATES_TOPIC = "pending-certificates";
    private static final int MAX_RETRY_ATTEMPTS = 5;

    public CertificateService(CertificateRepository certificateRepository, CertificateRegistry contract, Web3j web3j, IPFSService ipfsService, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper, KafkaTemplate<String, PendingCertificate> kafkaTemplate) {
        this.certificateRepository = certificateRepository;
        this.contract = contract;
        this.web3j = web3j;
        this.ipfsService = ipfsService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    // ========== ISSUANCE ========== //
    @Retryable(value = {BlockchainOperationException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000))
    @CircuitBreaker(name = "blockchainService",
            fallbackMethod = "issueCertificateFallback")
    @Transactional
    public CertificateDTO issueCertificate(CertificateRequest request, MultipartFile document) {
        String certificateId = UUID.randomUUID().toString();
        byte[] blockchainId = Hash.sha3(certificateId.getBytes());

        // 1. Store document in IPFS
        String ipfsHash = ipfsService.storeDocument(document);

        // 2. Create metadata
        CertificateMetadataDTO metadata = createMetadata(request, certificateId, ipfsHash);

        // 3. Save to database
        Certificate certificate = createCertificateEntity(request, certificateId, ipfsHash, metadata);
        certificateRepository.save(certificate);

        // 4. Issue on blockchain
        issueOnBlockchain(certificate, blockchainId, metadata);

        // 5. Cache and return
        CertificateDTO dto = mapToDTO(certificate);
        cacheCertificate(certificate);
        return dto;
    }

    public CertificateDTO issueCertificateFallback(CertificateRequest request,
                                                   MultipartFile document,
                                                   Exception e) {
        // Fallback flow without blockchain
        String certificateId = UUID.randomUUID().toString();
        String ipfsHash = ipfsService.storeDocument(document);

        CertificateMetadataDTO metadata = createMetadata(request, certificateId, ipfsHash);
        Certificate certificate = createCertificateEntity(request, certificateId, ipfsHash, metadata);
        certificateRepository.save(certificate);

        queueCertificateForRetry(certificate);

        CertificateDTO dto = mapToDTO(certificate);
        dto.setStatus(Certificate.CertificateStatus.QUEUED);
        return dto;
    }

    // ========== VERIFICATION ========== //
    @Cacheable(value = "certificates", key = "#certificateId")
    public CertificateDTO verifyCertificate(String certificateId) {
        // 1. Check cache
        CertificateDTO cached = getCachedCertificate(certificateId);
        if (cached != null) return cached;

        // 2. Fetch from database
        Certificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found"));

        // 3. Verify on blockchain if available
        CertificateDTO dto = mapToDTO(certificate);
        if (certificate.getBlockchainTransactionHash() != null) {
            verifyOnBlockchain(certificateId, certificate, dto);
        }

        // 4. Update cache
        cacheCertificate(certificate);
        return dto;
    }

    // ========== QUEUE PROCESSING ========== //
    @KafkaListener(topics = PENDING_CERTIFICATES_TOPIC)
    @Transactional
    public void processPendingCertificate(@Payload PendingCertificate pendingCert) {
        certificateRepository.findByCertificateId(pendingCert.getCertificateId())
                .ifPresent(cert -> {
                    if (cert.getStatus().name().equals(Certificate.CertificateStatus.ISSUED.name())) return;

                    if (pendingCert.getAttemptCount() >= MAX_RETRY_ATTEMPTS) {
                        cert.setStatus(Certificate.CertificateStatus.FAILED);
                        certificateRepository.save(cert);
                        //log.warn("Max retries reached for certificate {}", cert.getCertificateId());
                        return;
                    }

                    try {
                        byte[] blockchainId = Hash.sha3(cert.getCertificateId().getBytes());
                        CertificateMetadataDTO metadata = objectMapper.readValue(
                                cert.getMetadataJson(), CertificateMetadataDTO.class);

                        TransactionReceipt receipt = contract.issueCertificate(
                                blockchainId,
                                metadata.getRecipientName(),
                                metadata.getCourseName(),
                                metadata.getIssuingOrganization(),
                                metadata.getIpfsDocumentHash()
                        ).send();

                        updateCertificateOnSuccess(cert, receipt);
                    } catch (Exception e) {
                        handleRetryFailure(cert, pendingCert, e);
                    }
                });
    }

    // ========== HELPER METHODS ========== //
    private CertificateMetadataDTO createMetadata(CertificateRequest request,
                                                  String certificateId,
                                                  String ipfsHash) {
        CertificateMetadataDTO metadata = new CertificateMetadataDTO();
        metadata.setCertificateId(certificateId);
        metadata.setRecipientName(request.getRecipientName());
        metadata.setRecipientEmail(request.getRecipientEmail());
        metadata.setCourseName(request.getCourseName());
        metadata.setIssuingOrganization(request.getIssuingOrganization());
        metadata.setIpfsDocumentHash(ipfsHash);
        metadata.setIssueDate(Instant.now());
        return metadata;
    }

    private Certificate createCertificateEntity(CertificateRequest request,
                                                String certificateId,
                                                String ipfsHash,
                                                CertificateMetadataDTO metadata) {
        Certificate certificate = new Certificate();
        certificate.setCertificateId(certificateId);
        certificate.setRecipientName(request.getRecipientName());
        certificate.setRecipientEmail(request.getRecipientEmail());
        certificate.setCourseName(request.getCourseName());
        certificate.setIssuingOrganization(request.getIssuingOrganization());
        certificate.setIpfsHash(ipfsHash);
        certificate.setStatus(Certificate.CertificateStatus.PENDING);
        certificate.setIssuedAt(Instant.now());

        try {
            certificate.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }

        return certificate;
    }

    private void issueOnBlockchain(Certificate certificate,
                                   byte[] blockchainId,
                                   CertificateMetadataDTO metadata) {
        try {
            TransactionReceipt receipt = contract.issueCertificate(
                    blockchainId,
                    metadata.getRecipientName(),
                    metadata.getCourseName(),
                    metadata.getIssuingOrganization(),
                    metadata.getIpfsDocumentHash()
            ).send();

            certificate.setBlockchainTransactionHash(receipt.getTransactionHash());
            certificate.setStatus(Certificate.CertificateStatus.ISSUED);
            certificateRepository.save(certificate);
        } catch (Exception e) {
            //log.warn("Blockchain issuance failed", e);
            queueCertificateForRetry(certificate);
            throw new BlockchainOperationException("Blockchain unavailable");
        }
    }

    private void verifyOnBlockchain(String certificateId,
                                    Certificate certificate,
                                    CertificateDTO dto) {
        try {
            Tuple6<String, String, String, BigInteger, String, Boolean> result =
                    contract.verifyCertificate(Hash.sha3(certificateId.getBytes())).send();

            dto.setBlockchainVerified(result.component6());
            if (result.component6()) {
                CertificateMetadataDTO metadata = objectMapper.readValue(
                        certificate.getMetadataJson(), CertificateMetadataDTO.class);

                dto.setMetadataMatch(
                        metadata.getRecipientName().equals(result.component1()) &&
                                metadata.getCourseName().equals(result.component2()) &&
                                metadata.getIssuingOrganization().equals(result.component3()) &&
                                metadata.getIpfsDocumentHash().equals(result.component5())
                );
            }
        } catch (Exception e) {
            //log.warn("Blockchain verification failed", e);
            dto.setBlockchainVerified(false);
        }
    }

    private void queueCertificateForRetry(Certificate certificate) {
        try {
            PendingCertificate pendingCert = new PendingCertificate(
                    certificate.getCertificateId(),
                    certificate.getRetryCount() != null ? certificate.getRetryCount() : 0
            );

            kafkaTemplate.send(
                    PENDING_CERTIFICATES_TOPIC,
                    certificate.getCertificateId(),
                    pendingCert
            );

            certificate.setQueuedAt(Date.from(Instant.now()));
            certificateRepository.save(certificate);
        } catch (Exception e) {
            //log.error("Failed to queue certificate", e);
        }
    }

    private void updateCertificateOnSuccess(Certificate cert, TransactionReceipt receipt) {
        cert.setBlockchainTransactionHash(receipt.getTransactionHash());
        cert.setStatus(Certificate.CertificateStatus.ISSUED);
        cert.setRetryCount(cert.getRetryCount() != null ? cert.getRetryCount() + 1 : 1);
        certificateRepository.save(cert);
        cacheCertificate(cert);
    }

    private void handleRetryFailure(Certificate cert,
                                    PendingCertificate pendingCert,
                                    Exception e) {
        //log.warn("Retry failed for certificate {}", cert.getCertificateId(), e);
        cert.setRetryCount(pendingCert.getAttemptCount() + 1);
        certificateRepository.save(cert);

        if (pendingCert.getAttemptCount() < MAX_RETRY_ATTEMPTS) {
            queueCertificateWithBackoff(cert);
        }
    }

    private void queueCertificateWithBackoff(Certificate certificate) {
        try {
            long delayMinutes = (long) Math.pow(2, certificate.getRetryCount());

            kafkaTemplate.send(
                    MessageBuilder.withPayload(
                                    new PendingCertificate(
                                            certificate.getCertificateId(),
                                            certificate.getRetryCount()
                                    ))
                            .setHeader(KafkaHeaders.TOPIC, PENDING_CERTIFICATES_TOPIC)
                            .setHeader(KafkaHeaders.KEY, certificate.getCertificateId())
                            .setHeader("X-Delay", delayMinutes * 60 * 1000)
                            .build()
            );
        } catch (Exception e) {
            //log.error("Failed to re-queue certificate", e);
        }
    }

    // ========== CACHE METHODS ========== //
    private void cacheCertificate(Certificate certificate) {
        try {
            String json = objectMapper.writeValueAsString(mapToDTO(certificate));
            redisTemplate.opsForValue().set(
                    CACHE_PREFIX + certificate.getCertificateId(),
                    json,
                    CACHE_TTL
            );
        } catch (Exception e) {
            //log.warn("Failed to cache certificate", e);
        }
    }

    private CertificateDTO getCachedCertificate(String certificateId) {
        try {
            String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + certificateId);
            return cached != null ? objectMapper.readValue(cached, CertificateDTO.class) : null;
        } catch (Exception e) {
            //log.warn("Failed to read cached certificate", e);
            return null;
        }
    }

    // ========== DTO MAPPING ========== //
    private CertificateDTO mapToDTO(Certificate certificate) {
        CertificateDTO dto = new CertificateDTO();
        dto.setCertificateId(certificate.getCertificateId());
        dto.setRecipientName(certificate.getRecipientName());
        dto.setRecipientEmail(certificate.getRecipientEmail());
        dto.setCourseName(certificate.getCourseName());
        dto.setIssuingOrganization(certificate.getIssuingOrganization());
        dto.setIpfsHash(certificate.getIpfsHash());
        dto.setBlockchainTransactionHash(certificate.getBlockchainTransactionHash());
        dto.setStatus(certificate.getStatus());
        dto.setIssuedAt(certificate.getIssuedAt());
        return dto;
    }
}


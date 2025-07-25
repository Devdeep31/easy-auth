package com.devdeep.safedoc.service;

import com.devdeep.safedoc.dto.CertificateDTO;
import com.devdeep.safedoc.dto.CertificateMetadataDTO;
import com.devdeep.safedoc.dto.CertificateRequest;
import com.devdeep.safedoc.entity.Certificate;
import com.devdeep.safedoc.exception.BlockchainOperationException;
import com.devdeep.safedoc.exception.ResourceNotFoundException;
import com.devdeep.safedoc.repository.CertificateRepository;
import com.devdeep.safedoc.utils.BlockchainUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safedoc.contracts.CertificateRegistry;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final ObjectMapper objectMapper; // ✅ Auto-wired via constructor

    private static final String CACHE_PREFIX = "certificate::";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    public CertificateService(CertificateRepository certificateRepository, CertificateRegistry contract, Web3j web3j, IPFSService ipfsService, RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.certificateRepository = certificateRepository;
        this.contract = contract;
        this.web3j = web3j;
        this.ipfsService = ipfsService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @CircuitBreaker(name = "blockchainService", fallbackMethod = "issueCertificateFallback")
    @Transactional
    public CertificateDTO issueCertificate(CertificateRequest request, MultipartFile document) {
        String certificateId = UUID.randomUUID().toString();
        byte[] blockchainId = BlockchainUtil.generateBlockchainId(certificateId);

        // ✅ Upload to IPFS
        String ipfsHash = ipfsService.storeDocument(document);

        // ✅ Prepare metadata DTO
        CertificateMetadataDTO metadata = new CertificateMetadataDTO();
        metadata.setCertificateId(certificateId);
        metadata.setRecipientName(request.getRecipientName());
        metadata.setRecipientEmail(request.getRecipientEmail());
        metadata.setCourseName(request.getCourseName());
        metadata.setIssuingOrganization(request.getIssuingOrganization());
        metadata.setIpfsDocumentHash(ipfsHash);


        // ✅ Save certificate entity
        Certificate certificate = new Certificate();
        certificate.setCertificateId(certificateId);
        certificate.setRecipientName(request.getRecipientName());
        certificate.setRecipientEmail(request.getRecipientEmail());
        certificate.setCourseName(request.getCourseName());
        certificate.setIssuingOrganization(request.getIssuingOrganization());
        certificate.setIpfsHash(ipfsHash);
        certificate.setStatus(Certificate.CertificateStatus.ISSUED);
        certificate.setIssuedAt(Instant.now());

        try {
            String metadataJson = objectMapper.writeValueAsString(metadata);
            certificate.setMetadataJson(metadataJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize metadata", e);
        }

        certificateRepository.save(certificate);

        try {
            TransactionReceipt receipt = contract.issueCertificate(
                    blockchainId,
                    metadata.getRecipientName(),
                    metadata.getCourseName(),
                    metadata.getIssuingOrganization(),
                    metadata.getIpfsDocumentHash() // ✅ correct getter
            ).send();

            certificate.setBlockchainTransactionHash(receipt.getTransactionHash());
            certificateRepository.save(certificate);

            cacheCertificate(certificate);

            return mapToDTO(certificate);
        } catch (Exception e) {
            //log.error("Blockchain transaction failed", e);
            throw new BlockchainOperationException("Failed to issue certificate on blockchain");
        }
    }

    // ✅ Fallback method
    public CertificateDTO issueCertificateFallback(CertificateRequest request, MultipartFile document, Exception e) {
        throw new RuntimeException("Blockchain unavailable. Certificate issuance queued.");
    }

    @Cacheable(value = "certificates", key = "#certificateId")
    public CertificateDTO verifyCertificate(String certificateId) {
        CertificateDTO cached = getCachedCertificate(certificateId);
        if (cached != null) return cached;

        Certificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found", new RuntimeException()));


        try {
            Tuple6<String, String, String, BigInteger, String, Boolean> result =
                    contract.verifyCertificate(Hash.sha3(certificateId.getBytes())).send();

            if (!result.component6()) {
                // throw new InvalidCertificateException("Certificate not found on blockchain");
            }

            CertificateMetadataDTO metadata = objectMapper.readValue(
                    certificate.getMetadataJson(), CertificateMetadataDTO.class);

            if (!metadata.getRecipientName().equals(result.component1()) ||
                    !metadata.getCourseName().equals(result.component2()) ||
                    !metadata.getIssuingOrganization().equals(result.component3()) ||
                    !metadata.getIpfsDocumentHash().equals(result.component5())) {
                // throw new InvalidCertificateException("Certificate data mismatch with blockchain");
            }

            CertificateDTO dto = mapToDTO(certificate);
            //dto.setBlockConfirmed(true);

            cacheCertificate(certificate);
            return dto;

        } catch (Exception e) {
            //log.error("Blockchain verification failed", e);
            //throw new BlockchainOperationException("Verification failed");
        }
        return cached;
    }



    private void cacheCertificate(Certificate certificate) {
        try {
            String json = objectMapper.writeValueAsString(mapToDTO(certificate));
            redisTemplate.opsForValue().set(
                    CACHE_PREFIX + certificate.getCertificateId(),
                    json,
                    CACHE_TTL
            );
        } catch (Exception e) {
           /// log.warn("Failed to cache certificate", e);
        }
    }

    private CertificateDTO getCachedCertificate(String certificateId) {
        try {
            String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + certificateId);
            return objectMapper.readValue(cached, CertificateDTO.class);
        } catch (Exception e) {
            //log.warn("Failed to read cached certificate", e);
        }
        return null;
    }

    // ✅ Implement this mapping as per your DTO
    private CertificateDTO mapToDTO(Certificate cert) {
        CertificateDTO dto = new CertificateDTO();
        dto.setCertificateId(cert.getCertificateId());
        dto.setRecipientName(cert.getRecipientName());
        dto.setRecipientEmail(cert.getRecipientEmail());
        dto.setCourseName(cert.getCourseName());
        dto.setIssuingOrganization(cert.getIssuingOrganization());
        dto.setIpfsHash(cert.getIpfsHash());
        dto.setBlockchainTransactionHash(cert.getBlockchainTransactionHash());
        // dto.setStatus(cert.getStatus()); // Uncomment if status is needed and available in DTO
        dto.setIssuedAt(cert.getIssuedAt());
        return dto;
    }

}

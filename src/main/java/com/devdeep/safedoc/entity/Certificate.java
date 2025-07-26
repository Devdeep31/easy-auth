package com.devdeep.safedoc.entity;



import com.devdeep.safedoc.dto.CertificateDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String certificateId = UUID.randomUUID().toString();

    
    private String recipientName;

    
    private String recipientEmail;

    
    private String courseName;

    
    private String issuingOrganization;

    @Column(nullable = false)
    private String issuerWalletAddress = "0x0000000000000000000000000000000000000000"; // Default null address

    @Column(unique = true)
    private String ipfsHash; // CID of document stored in IPFS

    @Column(unique = true)
    private String blockchainTransactionHash;

    @Enumerated(EnumType.STRING)
    private CertificateStatus status = CertificateStatus.PENDING;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column
    private Instant issuedAt;

    @Column
    private Instant revokedAt;

    // Additional metadata (optional)
    //@Column(columnDefinition = "TEXT")
    @Column(columnDefinition = "LONGTEXT")
    private String metadataJson; // Can store grades, scores, etc.

    // Version for optimistic locking
    @Version
    private Integer version;

    private Date issueDate;

    private String transactionHash;

    public enum CertificateStatus {
        PENDING,
        ISSUED,
        REVOKED,
        EXPIRED,
        FAILED,
        QUEUED
    }

    private CertificateStatus certificateStatus;

    private Integer retryCount;

    private Date queuedAt;

    public Date getQueuedAt() {
        return queuedAt;
    }

    public CertificateStatus getStatus() {
        return status;
    }

    public CertificateStatus getCertificateStatus() {
        return certificateStatus;
    }

    public void setCertificateStatus(CertificateStatus certificateStatus) {
        this.certificateStatus = certificateStatus;
    }

    public void setQueuedAt(Date queuedAt) {
        this.queuedAt = queuedAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(String certificateId) {
        this.certificateId = certificateId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getIssuingOrganization() {
        return issuingOrganization;
    }

    public void setIssuingOrganization(String issuingOrganization) {
        this.issuingOrganization = issuingOrganization;
    }

    public String getIssuerWalletAddress() {
        return issuerWalletAddress;
    }

    public void setIssuerWalletAddress(String issuerWalletAddress) {
        this.issuerWalletAddress = issuerWalletAddress;
    }

    public String getIpfsHash() {
        return ipfsHash;
    }

    public void setIpfsHash(String ipfsHash) {
        this.ipfsHash = ipfsHash;
    }

    public String getBlockchainTransactionHash() {
        return blockchainTransactionHash;
    }

    public void setBlockchainTransactionHash(String blockchainTransactionHash) {
        this.blockchainTransactionHash = blockchainTransactionHash;
    }



    public void setStatus(CertificateStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Date issueDate) {
        this.issueDate = issueDate;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }
}
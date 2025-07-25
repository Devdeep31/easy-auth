package com.devdeep.safedoc.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CertificateMetadataDTO {
    private String certificateId; // UUID
    private String recipientName;
    private String recipientEmail;
    private String recipientWalletAddress; // Optional: For blockchain-native certificates
    private String courseName;
    private String courseCode;
    private String issuingOrganization;
    private String issuerWalletAddress; // Blockchain address of issuer
    private String credentialType; // e.g., "Degree", "Badge", "Micro-Credential"
    private Instant issueDate;
    private Instant expirationDate; // Optional
    private String ipfsDocumentHash; // CID of the full certificate PDF
    private String ipfsMetadataHash; // CID of this metadata JSON
    private String blockchainTransactionHash;
    private String verificationUrl; // e.g., "https://yourdomain.com/verify/{certificateId}"
    private String blockchainNetwork; // e.g., "Ethereum Mainnet", "Polygon"
    private String smartContractAddress;
    private String ipfsHash;


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

    public String getRecipientWalletAddress() {
        return recipientWalletAddress;
    }

    public void setRecipientWalletAddress(String recipientWalletAddress) {
        this.recipientWalletAddress = recipientWalletAddress;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
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

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public Instant getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(Instant issueDate) {
        this.issueDate = issueDate;
    }

    public Instant getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Instant expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getIpfsDocumentHash() {
        return ipfsDocumentHash;
    }

    public void setIpfsDocumentHash(String ipfsDocumentHash) {
        this.ipfsDocumentHash = ipfsDocumentHash;
    }

    public String getIpfsMetadataHash() {
        return ipfsMetadataHash;
    }

    public void setIpfsMetadataHash(String ipfsMetadataHash) {
        this.ipfsMetadataHash = ipfsMetadataHash;
    }

    public String getBlockchainTransactionHash() {
        return blockchainTransactionHash;
    }

    public void setBlockchainTransactionHash(String blockchainTransactionHash) {
        this.blockchainTransactionHash = blockchainTransactionHash;
    }

    public String getVerificationUrl() {
        return verificationUrl;
    }

    public void setVerificationUrl(String verificationUrl) {
        this.verificationUrl = verificationUrl;
    }

    public String getBlockchainNetwork() {
        return blockchainNetwork;
    }

    public void setBlockchainNetwork(String blockchainNetwork) {
        this.blockchainNetwork = blockchainNetwork;
    }

    public String getSmartContractAddress() {
        return smartContractAddress;
    }

    public void setSmartContractAddress(String smartContractAddress) {
        this.smartContractAddress = smartContractAddress;
    }

    public String getIpfsHash() {
        return ipfsHash;
    }

    public void setIpfsHash(String ipfsHash) {
        this.ipfsHash = ipfsHash;
    }
}

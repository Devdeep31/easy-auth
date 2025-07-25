// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract CertificateRegistry {
    struct Certificate {
        string recipientName;
        string courseName;
        string issuingOrganization;
        uint256 issueDate;
        string ipfsHash;
    }

    mapping(bytes32 => Certificate) private certificates;
    address public owner;

    event CertificateIssued(bytes32 indexed certificateId, string ipfsHash);
    event CertificateRevoked(bytes32 indexed certificateId);

    constructor() {
        owner = msg.sender;
    }

    modifier onlyOwner() {
        require(msg.sender == owner, "Not authorized");
        _;
    }

    function issueCertificate(
        bytes32 certificateId,
        string memory recipientName,
        string memory courseName,
        string memory issuingOrganization,
        string memory ipfsHash
    ) external onlyOwner {
        require(certificates[certificateId].issueDate == 0, "Certificate already exists");

        certificates[certificateId] = Certificate({
            recipientName: recipientName,
            courseName: courseName,
            issuingOrganization: issuingOrganization,
            issueDate: block.timestamp,
            ipfsHash: ipfsHash
        });

        emit CertificateIssued(certificateId, ipfsHash);
    }

    function revokeCertificate(bytes32 certificateId) external onlyOwner {
        require(certificates[certificateId].issueDate != 0, "Certificate does not exist");
        delete certificates[certificateId];
        emit CertificateRevoked(certificateId);
    }

    function verifyCertificate(bytes32 certificateId) external view returns (
        string memory,
        string memory,
        string memory,
        uint256,
        string memory,
        bool
    ) {
        Certificate memory cert = certificates[certificateId];
        bool exists = cert.issueDate != 0;
        return (
            cert.recipientName,
            cert.courseName,
            cert.issuingOrganization,
            cert.issueDate,
            cert.ipfsHash,
            exists
        );
    }
}
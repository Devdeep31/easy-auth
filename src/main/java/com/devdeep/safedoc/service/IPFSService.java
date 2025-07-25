package com.devdeep.safedoc.service;

import com.devdeep.safedoc.exception.IPFSException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

@Service

public class IPFSService {
    private final IPFS ipfs;
    private final ObjectMapper objectMapper;

    public IPFSService(IPFS ipfs, ObjectMapper objectMapper) {
        this.ipfs = ipfs;
        this.objectMapper = objectMapper;
    }

    public String storeDocument(MultipartFile file) {
        try {
            // Convert MultipartFile to byte array
            byte[] fileBytes = file.getBytes();

            // Wrap in NamedStreamable
            NamedStreamable fileStream = new NamedStreamable.ByteArrayWrapper(file.getOriginalFilename(), fileBytes);

            // Add to IPFS
            MerkleNode result = ipfs.add(fileStream).get(0);

            return result.hash.toBase58();
        } catch (Exception e) {
            throw new IPFSException("Failed to store document in IPFS", e);
        }
    }

    public byte[] retrieveDocument(String ipfsHash) {
        try {
            // Use Multihash to wrap the string hash
            Multihash filePointer = Multihash.fromBase58(ipfsHash);
            return ipfs.cat(filePointer);
        } catch (Exception e) {
            throw new IPFSException("Failed to retrieve document from IPFS", e);
        }
    }
}
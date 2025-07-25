package com.devdeep.safedoc.utils;

import org.web3j.crypto.Hash;

public class BlockchainUtil {
    public static byte[] generateBlockchainId(String certificateId) {
        // Convert the String to byte array
        byte[] inputBytes = certificateId.getBytes();

        // Generate SHA-3 hash as byte[]
        byte[] blockchainId = Hash.sha3(inputBytes);

        return blockchainId;
    }
}

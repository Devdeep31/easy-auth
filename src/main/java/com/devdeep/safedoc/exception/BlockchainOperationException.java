package com.devdeep.safedoc.exception;

/**
 * Custom exception for blockchain-related errors.
 */
public class BlockchainOperationException extends RuntimeException {

    public BlockchainOperationException(String message) {
        super(message);
    }

    public BlockchainOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
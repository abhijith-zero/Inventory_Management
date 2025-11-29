package com.company.inventory.dao;

/**
 * Unchecked wrapper for SQL exceptions to avoid cluttering signatures.
 * You may change to a checked exception if you want explicit throws propagation.
 */
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
    public DataAccessException(Throwable cause) {
        super(cause);
    }

    public DataAccessException(String cause) {
        super(cause);
    }
}

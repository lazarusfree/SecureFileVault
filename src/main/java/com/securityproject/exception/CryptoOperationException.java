package com.securityproject.exception;

public class CryptoOperationException extends VaultException {
    public CryptoOperationException(String message) {
        super(message);
    }

    public CryptoOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

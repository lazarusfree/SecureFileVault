package com.securityproject.exception;

public class FileAccessDeniedException extends VaultException {
    public FileAccessDeniedException(String message) {
        super(message);
    }
}

package com.template.app.auth.infrastructure.exception;

/**
 * Exception thrown when password verification fails during sensitive operations
 * like account deletion or password change
 */
public class InvalidPasswordException extends RuntimeException {
    public InvalidPasswordException(String message) {
        super(message);
    }
}

package com.template.app.auth.infrastructure.exception;

public class InvalidOAuth2TokenException extends RuntimeException {
    public InvalidOAuth2TokenException(String message) {
        super(message);
    }

    public InvalidOAuth2TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.template.app.notification.exception;

/**
 * Exception thrown when email sending fails.
 * This is a runtime exception for easier handling in async contexts.
 */
public class EmailSendException extends RuntimeException {

    public EmailSendException(String message) {
        super(message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailSendException(Throwable cause) {
        super(cause);
    }
}

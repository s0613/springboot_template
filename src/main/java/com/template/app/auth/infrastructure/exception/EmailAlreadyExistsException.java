package com.template.app.auth.infrastructure.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    private final String email;
    private final String existingProvider;

    public EmailAlreadyExistsException(String email, String existingProvider) {
        super(String.format("Email %s is already registered with %s. Please login with %s instead.",
                email, existingProvider, existingProvider));
        this.email = email;
        this.existingProvider = existingProvider;
    }

    public String getEmail() {
        return email;
    }

    public String getExistingProvider() {
        return existingProvider;
    }
}

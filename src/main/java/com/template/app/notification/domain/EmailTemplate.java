package com.template.app.notification.domain;

/**
 * Email template types for different notification scenarios.
 * Each template has a corresponding Thymeleaf template file.
 */
public enum EmailTemplate {
    /**
     * Welcome email for new users
     */
    WELCOME("welcome", "Welcome to Template App"),

    /**
     * Password reset email
     */
    PASSWORD_RESET("password-reset", "Password Reset Request"),

    /**
     * Email verification
     */
    EMAIL_VERIFICATION("email-verification", "Verify Your Email"),

    /**
     * General notification
     */
    NOTIFICATION("notification", "Notification");

    private final String templateName;
    private final String defaultSubject;

    EmailTemplate(String templateName, String defaultSubject) {
        this.templateName = templateName;
        this.defaultSubject = defaultSubject;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getDefaultSubject() {
        return defaultSubject;
    }

    /**
     * Get the template file path for Thymeleaf
     */
    public String getTemplatePath() {
        return "email/" + templateName;
    }
}

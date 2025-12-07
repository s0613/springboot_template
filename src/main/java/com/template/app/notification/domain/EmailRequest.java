package com.template.app.notification.domain;

import lombok.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Request object for sending emails.
 * Supports template-based emails with variable substitution.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {

    /**
     * Recipient email address
     */
    private String to;

    /**
     * Email subject
     */
    private String subject;

    /**
     * Email template to use
     */
    private EmailTemplate template;

    /**
     * Variables to substitute in the template
     */
    @Builder.Default
    private Map<String, Object> templateVariables = new HashMap<>();

    /**
     * Optional attachment URLs (for PDF reports, etc.)
     */
    @Builder.Default
    private Map<String, String> attachments = new HashMap<>();

    /**
     * Helper method to add a template variable
     */
    public void addVariable(String key, Object value) {
        this.templateVariables.put(key, value);
    }

    /**
     * Helper method to add an attachment
     */
    public void addAttachment(String name, String url) {
        this.attachments.put(name, url);
    }
}

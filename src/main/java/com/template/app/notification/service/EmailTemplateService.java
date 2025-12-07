package com.template.app.notification.service;

import com.template.app.notification.domain.EmailTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Service for rendering email templates using Thymeleaf.
 * Supports dynamic variable substitution in email templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Renders an email template with the given variables.
     *
     * @param template  The email template to render
     * @param variables Variables to substitute in the template
     * @return Rendered HTML content
     */
    public String renderEmailBody(EmailTemplate template, Map<String, Object> variables) {
        log.debug("Rendering email template: {} with {} variables", template.getTemplateName(), variables.size());

        try {
            Context context = new Context();
            if (variables != null && !variables.isEmpty()) {
                context.setVariables(variables);
            }

            String html = templateEngine.process(template.getTemplatePath(), context);

            log.debug("Successfully rendered template: {}", template.getTemplateName());
            return html;

        } catch (Exception e) {
            log.error("Failed to render email template: {}", template.getTemplateName(), e);
            throw new RuntimeException("Failed to render email template: " + template.getTemplateName(), e);
        }
    }

    /**
     * Gets the subject for a given email template.
     *
     * @param template The email template
     * @return Email subject
     */
    public String getSubject(EmailTemplate template) {
        return template.getDefaultSubject();
    }

    /**
     * Gets the subject with custom variables (for future localization support).
     *
     * @param template  The email template
     * @param variables Variables that might be used in subject
     * @return Email subject
     */
    public String getSubject(EmailTemplate template, Map<String, Object> variables) {
        // For now, just return the default subject
        // In the future, this could support subject templates with variables
        return template.getDefaultSubject();
    }
}

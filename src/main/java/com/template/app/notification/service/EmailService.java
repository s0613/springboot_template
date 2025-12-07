package com.template.app.notification.service;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.*;
import com.template.app.auth.domain.entity.User;
import com.template.app.common.config.aws.AwsSesProperties;
import com.template.app.notification.domain.EmailRequest;
import com.template.app.notification.domain.EmailTemplate;
import com.template.app.notification.entity.EmailLog;
import com.template.app.notification.exception.EmailSendException;
import com.template.app.notification.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending emails using AWS SES.
 * Supports template-based emails with async processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aws.ses", name = "enabled", havingValue = "true")
public class EmailService {

    private final AmazonSimpleEmailService sesClient;
    private final EmailTemplateService emailTemplateService;
    private final EmailLogRepository emailLogRepository;
    private final AwsSesProperties sesProperties;
    private DeadLetterQueueService dlqService; // Lazy injection to avoid circular dependency

    private static final int MAX_EMAILS_PER_HOUR = 10;

    /**
     * Set DLQ service (for lazy injection to avoid circular dependency).
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setDlqService(DeadLetterQueueService dlqService) {
        this.dlqService = dlqService;
    }

    /**
     * Sends a welcome email to new users.
     *
     * @param user     The new user
     * @param loginUrl URL to login page
     */
    @Async
    public void sendWelcomeEmail(User user, String loginUrl) {
        log.info("Sending welcome email to user: {}", user.getEmail());

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new EmailSendException("User has no email address");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", user.getName());
        variables.put("loginUrl", loginUrl);

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .subject(emailTemplateService.getSubject(EmailTemplate.WELCOME))
                .template(EmailTemplate.WELCOME)
                .templateVariables(variables)
                .build();

        sendEmail(request);
    }

    /**
     * Sends a password reset email.
     *
     * @param user     The user requesting password reset
     * @param resetUrl URL to reset password page
     */
    @Async
    public void sendPasswordResetEmail(User user, String resetUrl) {
        log.info("Sending password reset email to user: {}", user.getEmail());

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new EmailSendException("User has no email address");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", user.getName());
        variables.put("resetUrl", resetUrl);
        variables.put("expirationMinutes", 30);

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .subject(emailTemplateService.getSubject(EmailTemplate.PASSWORD_RESET))
                .template(EmailTemplate.PASSWORD_RESET)
                .templateVariables(variables)
                .build();

        sendEmail(request);
    }

    /**
     * Sends an email verification email.
     *
     * @param user            The user
     * @param verificationUrl URL to verify email
     */
    @Async
    public void sendEmailVerification(User user, String verificationUrl) {
        log.info("Sending email verification to user: {}", user.getEmail());

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new EmailSendException("User has no email address");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("userName", user.getName());
        variables.put("verificationUrl", verificationUrl);

        EmailRequest request = EmailRequest.builder()
                .to(user.getEmail())
                .subject(emailTemplateService.getSubject(EmailTemplate.EMAIL_VERIFICATION))
                .template(EmailTemplate.EMAIL_VERIFICATION)
                .templateVariables(variables)
                .build();

        sendEmail(request);
    }

    /**
     * Generic email sending method.
     * Renders template, sends via SES, and logs the result.
     *
     * @param request The email request
     * @throws EmailSendException if sending fails
     */
    @Transactional
    public void sendEmail(EmailRequest request) {
        validateEmailRequest(request);
        checkRateLimit(request.getTo());

        String renderedHtml = null;
        String messageId = null;

        try {
            // Render email template
            renderedHtml = emailTemplateService.renderEmailBody(
                    request.getTemplate(),
                    request.getTemplateVariables()
            );

            // Build SES request
            SendEmailRequest sendRequest = buildSendEmailRequest(request, renderedHtml);

            // Send via SES
            log.debug("Sending email to: {} with subject: {}", request.getTo(), request.getSubject());
            SendEmailResult result = sesClient.sendEmail(sendRequest);
            messageId = result.getMessageId();

            log.info("Email sent successfully. MessageId: {}", messageId);

            // Log success
            EmailLog emailLog = EmailLog.success(
                    request.getTo(),
                    request.getSubject(),
                    request.getTemplate().name(),
                    messageId
            );
            emailLogRepository.save(emailLog);

        } catch (Exception e) {
            log.error("Failed to send email to: {}", request.getTo(), e);

            // Log failure
            EmailLog emailLog = EmailLog.failure(
                    request.getTo(),
                    request.getSubject(),
                    request.getTemplate().name(),
                    e.getMessage()
            );
            emailLogRepository.save(emailLog);

            // Add to DLQ for retry
            if (dlqService != null && renderedHtml != null) {
                dlqService.addToQueue("EMAIL", null, request.getTo(), renderedHtml, e.getMessage());
            }

            throw new EmailSendException("Failed to send email", e);
        }
    }

    /**
     * Simple email sending method for retry purposes.
     * Sends plain HTML email without template processing.
     *
     * @param to      Recipient email address
     * @param subject Email subject
     * @param body    Email body (HTML)
     * @throws EmailSendException if sending fails
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            Destination destination = new Destination().withToAddresses(to);
            Content subjectContent = new Content().withCharset("UTF-8").withData(subject);
            Content htmlContent = new Content().withCharset("UTF-8").withData(body);
            Content textContent = new Content().withCharset("UTF-8").withData(stripHtml(body));

            Body messageBody = new Body()
                    .withHtml(htmlContent)
                    .withText(textContent);

            Message message = new Message()
                    .withSubject(subjectContent)
                    .withBody(messageBody);

            String fromAddress = String.format("%s <%s>",
                    sesProperties.getFromName(),
                    sesProperties.getFromEmail());

            SendEmailRequest sendRequest = new SendEmailRequest()
                    .withSource(fromAddress)
                    .withDestination(destination)
                    .withMessage(message);

            SendEmailResult result = sesClient.sendEmail(sendRequest);
            log.info("Email sent successfully. MessageId: {}", result.getMessageId());

        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new EmailSendException("Failed to send email", e);
        }
    }

    /**
     * Validates email request parameters.
     */
    private void validateEmailRequest(EmailRequest request) {
        if (request.getTo() == null || request.getTo().isBlank()) {
            throw new EmailSendException("Recipient email address is required");
        }

        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new EmailSendException("Email subject is required");
        }

        if (request.getTemplate() == null) {
            throw new EmailSendException("Email template is required");
        }
    }

    /**
     * Checks if the recipient has exceeded the rate limit.
     */
    private void checkRateLimit(String email) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentEmailCount = emailLogRepository.countRecentEmailsToRecipient(email, oneHourAgo);

        if (recentEmailCount >= MAX_EMAILS_PER_HOUR) {
            log.warn("Rate limit exceeded for email: {}. Count: {}", email, recentEmailCount);
            throw new EmailSendException(
                    String.format("Rate limit exceeded. Maximum %d emails per hour.", MAX_EMAILS_PER_HOUR)
            );
        }
    }

    /**
     * Builds AWS SES SendEmailRequest.
     */
    private SendEmailRequest buildSendEmailRequest(EmailRequest request, String htmlBody) {
        Destination destination = new Destination()
                .withToAddresses(request.getTo());

        Content subjectContent = new Content()
                .withCharset("UTF-8")
                .withData(request.getSubject());

        Content htmlContent = new Content()
                .withCharset("UTF-8")
                .withData(htmlBody);

        // Also provide plain text version (fallback)
        Content textContent = new Content()
                .withCharset("UTF-8")
                .withData(stripHtml(htmlBody));

        Body body = new Body()
                .withHtml(htmlContent)
                .withText(textContent);

        Message message = new Message()
                .withSubject(subjectContent)
                .withBody(body);

        String fromAddress = String.format("%s <%s>",
                sesProperties.getFromName(),
                sesProperties.getFromEmail());

        return new SendEmailRequest()
                .withSource(fromAddress)
                .withDestination(destination)
                .withMessage(message);
    }

    /**
     * Simple HTML stripping for plain text fallback.
     */
    private String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]*>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .trim();
    }
}

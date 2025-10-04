package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.config.ApplicationProperties;
import com.oussamabenberkane.espritlivre.domain.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Service for sending emails.
 */
@Service
public class MailService {

    private static final Logger LOG = LoggerFactory.getLogger(MailService.class);

    private static final String USER = "user";
    private static final String BASE_URL = "baseUrl";

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private final ApplicationProperties applicationProperties;
    private final MessageSource messageSource;

    @Value("${jhipster.mail.from:noreply@espritlivre.dz}")
    private String from;

    public MailService(
        JavaMailSender javaMailSender,
        SpringTemplateEngine templateEngine,
        ApplicationProperties applicationProperties,
        MessageSource messageSource
    ) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.applicationProperties = applicationProperties;
        this.messageSource = messageSource;
    }

    @Async
    public void sendEmail(String to, String subject, String content, boolean isMultipart, boolean isHtml) {
        LOG.debug(
            "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
            isMultipart,
            isHtml,
            to,
            subject,
            content
        );

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper message = new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
            message.setTo(to);
            message.setFrom(from, "Esprit Livre");
            message.setSubject(subject);
            message.setText(content, isHtml);

            // Add headers to improve deliverability
            mimeMessage.setHeader("X-Mailer", "Esprit Livre Mailer");
            mimeMessage.setHeader("X-Priority", "3");

            javaMailSender.send(mimeMessage);
            LOG.info("Email successfully sent to '{}' with subject '{}'", to, subject);
        } catch (MessagingException e) {
            LOG.error("Failed to send email to '{}'. Error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Unexpected error while sending email to '{}'. Error: {}", to, e.getMessage(), e);
        }
    }

    @Async
    public void sendEmailFromTemplate(User user, String templateName, String titleKey) {
        if (user.getEmail() == null) {
            LOG.debug("Email doesn't exist for user '{}'", user.getLogin());
            return;
        }
        Locale locale = Locale.forLanguageTag(user.getLangKey());
        Context context = new Context(locale);
        context.setVariable(USER, user);
        context.setVariable(BASE_URL, "http://localhost:8080");
        String content = templateEngine.process(templateName, context);
        String subject = "Esprit Livre - " + titleKey;
        sendEmail(user.getEmail(), subject, content, false, true);
    }

    @Async
    public void sendNewUserRegistrationEmailToAdmin(User user) {
        LOG.debug("Sending registration notification email to admin for user: {}", user.getLogin());

        Locale locale = Locale.forLanguageTag(user.getLangKey() != null ? user.getLangKey() : "en");
        Context context = new Context(locale);

        // Format current timestamp
        Instant now = Instant.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

        context.setVariable(USER, user);
        context.setVariable("adminEmail", applicationProperties.getAdminEmail());
        context.setVariable("registrationDate", dateFormatter.format(now));
        context.setVariable("registrationTime", timeFormatter.format(now));

        String content = templateEngine.process("mail/newUserRegistration", context);
        String subject = messageSource.getMessage("email.admin.newuser.title", null, locale);

        sendEmail(applicationProperties.getAdminEmail(), subject, content, false, true);
    }

    @Async
    public void sendEmailChangeVerification(User user, String verificationUrl) {
        LOG.debug("Sending email change verification to: {}", user.getPendingEmail());

        Locale locale = Locale.forLanguageTag(user.getLangKey() != null ? user.getLangKey() : "en");
        Context context = new Context(locale);

        // Format expiry date and time
        Instant expiryInstant = user.getEmailVerificationTokenExpiry();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

        context.setVariable(USER, user);
        context.setVariable("verificationUrl", verificationUrl);
        context.setVariable("expiryDate", dateFormatter.format(expiryInstant));
        context.setVariable("expiryTime", timeFormatter.format(expiryInstant));
        context.setVariable("adminEmail", applicationProperties.getAdminEmail());

        String content = templateEngine.process("mail/emailChangeVerification", context);
        String subject = messageSource.getMessage("email.verification.title", null, locale);

        sendEmail(user.getPendingEmail(), subject, content, false, true);
    }
}

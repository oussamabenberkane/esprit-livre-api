package com.oussamabenberkane.espritlivre.service;

import com.oussamabenberkane.espritlivre.config.ApplicationProperties;
import com.oussamabenberkane.espritlivre.domain.User;
import com.oussamabenberkane.espritlivre.repository.UserRepository;
import com.oussamabenberkane.espritlivre.service.dto.OrderDTO;
import com.oussamabenberkane.espritlivre.service.dto.OrderItemDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
    private final UserRepository userRepository;

    @Value("${jhipster.mail.from:noreply@espritlivre.dz}")
    private String from;

    public MailService(
        JavaMailSender javaMailSender,
        SpringTemplateEngine templateEngine,
        ApplicationProperties applicationProperties,
        MessageSource messageSource,
        UserRepository userRepository
    ) {
        this.javaMailSender = javaMailSender;
        this.templateEngine = templateEngine;
        this.applicationProperties = applicationProperties;
        this.messageSource = messageSource;
        this.userRepository = userRepository;
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

    @Async
    public void sendNewOrderNotificationToAdmin(OrderDTO order, String adminPanelUrl) {
        LOG.debug("Sending new order notification to admin for order: {}", order.getUniqueId());

        String adminEmail = applicationProperties.getAdminEmail();
        if (adminEmail == null || adminEmail.isEmpty()) {
            LOG.warn("Admin email not configured, skipping order notification for order: {}", order.getUniqueId());
            return;
        }

        // Retrieve admin user and use their language preference, default to French if not found
        Locale locale = Locale.forLanguageTag("fr");
        Optional<User> adminUser = userRepository.findOneByEmailIgnoreCase(adminEmail);
        if (adminUser.isPresent() && adminUser.get().getLangKey() != null) {
            locale = Locale.forLanguageTag(adminUser.get().getLangKey());
            LOG.info("Using admin user language preference: {}", adminUser.get().getLangKey());
        } else {
            LOG.info("Admin user not found or no language preference set, using default locale: fr");
        }

        Context context = new Context(locale);

        // Format order date and time
        ZonedDateTime orderDateTime = order.getCreatedAt() != null ? order.getCreatedAt() : ZonedDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault());
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

        // Set basic order information
        context.setVariable("orderUniqueId", order.getUniqueId());
        context.setVariable("orderDate", dateFormatter.format(orderDateTime));
        context.setVariable("orderTime", timeFormatter.format(orderDateTime));
        context.setVariable("orderStatus", order.getStatus() != null ? order.getStatus().toString() : "PENDING");

        // Set customer information (only if not null/empty)
        if (order.getFullName() != null && !order.getFullName().isEmpty()) {
            context.setVariable("customerName", order.getFullName());
        }
        if (order.getPhone() != null && !order.getPhone().isEmpty()) {
            context.setVariable("customerPhone", order.getPhone());
        }
        if (order.getEmail() != null && !order.getEmail().isEmpty()) {
            context.setVariable("customerEmail", order.getEmail());
        }

        // Set shipping address (only if not null/empty)
        if (order.getWilaya() != null && !order.getWilaya().isEmpty()) {
            context.setVariable("wilaya", order.getWilaya());
        }
        if (order.getCity() != null && !order.getCity().isEmpty()) {
            context.setVariable("city", order.getCity());
        }
        if (order.getStreetAddress() != null && !order.getStreetAddress().isEmpty()) {
            context.setVariable("streetAddress", order.getStreetAddress());
        }
        if (order.getPostalCode() != null && !order.getPostalCode().isEmpty()) {
            context.setVariable("postalCode", order.getPostalCode());
        }

        // Set total amount
        if (order.getTotalAmount() != null) {
            context.setVariable("totalAmount", order.getTotalAmount());
        }

        // Set order items
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            List<Map<String, Object>> items = order.getOrderItems().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("bookTitle", item.getBookTitle());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    itemMap.put("totalPrice", item.getTotalPrice());
                    return itemMap;
                })
                .collect(Collectors.toList());
            context.setVariable("orderItems", items);
        }

        // Set admin panel URL
        context.setVariable("adminPanelUrl", adminPanelUrl);

        String content = templateEngine.process("mail/adminOrderNotification", context);
        String subject = messageSource.getMessage("email.admin.order.title", null, locale) + " #" + order.getUniqueId();

        // Send with high priority
        sendEmailWithPriority(adminEmail, subject, content, false, true, 1);
    }

    @Async
    public void sendEmailWithPriority(String to, String subject, String content, boolean isMultipart, boolean isHtml, int priority) {
        LOG.debug(
            "Send priority email[multipart '{}' and html '{}'] to '{}' with subject '{}' and priority={}",
            isMultipart,
            isHtml,
            to,
            subject,
            priority
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
            mimeMessage.setHeader("X-Priority", String.valueOf(priority));

            javaMailSender.send(mimeMessage);
            LOG.info("Priority email successfully sent to '{}' with subject '{}'", to, subject);
        } catch (MessagingException e) {
            LOG.error("Failed to send priority email to '{}'. Error: {}", to, e.getMessage(), e);
        } catch (Exception e) {
            LOG.error("Unexpected error while sending priority email to '{}'. Error: {}", to, e.getMessage(), e);
        }
    }
}

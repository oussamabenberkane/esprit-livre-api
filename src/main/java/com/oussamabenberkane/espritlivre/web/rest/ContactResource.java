package com.oussamabenberkane.espritlivre.web.rest;

import com.oussamabenberkane.espritlivre.service.MailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling contact form submissions.
 */
@RestController
@RequestMapping("/api")
public class ContactResource {

    private static final Logger LOG = LoggerFactory.getLogger(ContactResource.class);

    private final MailService mailService;

    public ContactResource(MailService mailService) {
        this.mailService = mailService;
    }

    /**
     * {@code POST /contact} : Submit a contact form.
     *
     * @param contactRequest the contact form data.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} if successful.
     */
    @PostMapping("/contact")
    public ResponseEntity<ContactResponse> submitContactForm(@Valid @RequestBody ContactRequest contactRequest) {
        LOG.debug("REST request to submit contact form from: {}", contactRequest.getEmail());

        try {
            mailService.sendContactFormNotificationToAdmin(
                contactRequest.getName(),
                contactRequest.getEmail(),
                contactRequest.getSubject(),
                contactRequest.getMessage()
            );

            ContactResponse response = new ContactResponse();
            response.setSuccess(true);
            response.setMessage("Votre message a été envoyé avec succès. Nous vous répondrons dans les plus brefs délais.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            LOG.error("Error sending contact form email: {}", e.getMessage(), e);

            ContactResponse response = new ContactResponse();
            response.setSuccess(false);
            response.setMessage("Une erreur s'est produite lors de l'envoi de votre message. Veuillez réessayer plus tard.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Request DTO for contact form submission.
     */
    public static class ContactRequest {

        @NotBlank(message = "Le nom est obligatoire")
        @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
        private String name;

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email doit être valide")
        private String email;

        @NotBlank(message = "Le sujet est obligatoire")
        @Size(min = 2, max = 200, message = "Le sujet doit contenir entre 2 et 200 caractères")
        private String subject;

        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 10, max = 5000, message = "Le message doit contenir entre 10 et 5000 caractères")
        private String message;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Response DTO for contact form submission.
     */
    public static class ContactResponse {

        private boolean success;
        private String message;

        // Getters and setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}

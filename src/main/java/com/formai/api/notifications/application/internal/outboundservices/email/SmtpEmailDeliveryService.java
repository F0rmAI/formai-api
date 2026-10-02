package com.formai.api.notifications.application.internal.outboundservices.email;

import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

// Sends through the SMTP relay configured in spring.mail (Brevo). Never log the destination, the
// body or the provider's error message: the body carries the password reset token and the error
// can echo the address. A failure is reported as false so the dispatcher retries it.
@Service
public class SmtpEmailDeliveryService implements EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailDeliveryService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public SmtpEmailDeliveryService(JavaMailSender mailSender,
                                    @Value("${notifications.email.from-address:}") String fromAddress,
                                    @Value("${notifications.email.from-name:FormAI}") String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public boolean send(String destination, String subject, String body) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Email \"{}\" not sent: SMTP_FROM_EMAIL is not configured", subject);
            return false;
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(destination);
            helper.setSubject(subject);
            helper.setText(body, false);
            mailSender.send(message);
            log.info("Email \"{}\" delivered to the SMTP relay", subject);
            return true;
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.warn("Email \"{}\" could not be sent ({})", subject, ex.getClass().getSimpleName());
            return false;
        }
    }
}

package com.formai.api.notifications.application.internal.outboundservices.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// Provisional: the transactional email provider is still to be decided, so the email is only
// logged. Never log the destination or the body: the body carries the password reset token.
@Service
public class SmtpEmailDeliveryService implements EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailDeliveryService.class);

    @Override
    public boolean send(String destination, String subject, String body) {
        log.info("Email \"{}\" due for delivery; no email provider is configured yet", subject);
        return true;
    }
}

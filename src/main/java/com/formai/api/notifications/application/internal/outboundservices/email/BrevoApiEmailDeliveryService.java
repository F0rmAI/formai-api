package com.formai.api.notifications.application.internal.outboundservices.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

// Sends through Brevo's transactional HTTP API because Railway blocks outbound SMTP. Never log the
// destination, body or provider response: the body carries the password reset token and the response
// can echo the address. A failure is reported as false so the dispatcher retries it.
@Service
public class BrevoApiEmailDeliveryService implements EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(BrevoApiEmailDeliveryService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;
    private final String fromName;

    public BrevoApiEmailDeliveryService(RestClient.Builder restClientBuilder,
                                        @Value("${notifications.email.brevo.base-url:https://api.brevo.com/v3}") String baseUrl,
                                        @Value("${notifications.email.api-key:}") String apiKey,
                                        @Value("${notifications.email.from-address:}") String fromAddress,
                                        @Value("${notifications.email.from-name:FormAI}") String fromName) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public boolean send(String destination, String subject, String body) {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("Email \"{}\" not sent: SMTP_FROM_EMAIL is not configured", subject);
            return false;
        }
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Email \"{}\" not sent: BREVO_API_KEY is not configured", subject);
            return false;
        }

        var request = new SendEmailRequest(new Sender(fromName, fromAddress),
                List.of(new Recipient(destination)), subject, body);
        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email \"{}\" delivered to Brevo", subject);
            return true;
        } catch (RestClientResponseException ex) {
            log.warn("Email \"{}\" could not be sent (HTTP {})", subject, ex.getStatusCode().value());
            return false;
        } catch (RestClientException ex) {
            log.warn("Email \"{}\" could not be sent ({})", subject, ex.getClass().getSimpleName());
            return false;
        }
    }

    private record Sender(String name, String email) {}

    private record Recipient(String email) {}

    private record SendEmailRequest(Sender sender, List<Recipient> to, String subject, String textContent) {}
}

package com.formai.api.notifications.application.internal.outboundservices.email;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static com.formai.api.notifications.NotificationsTestData.EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;
import static org.springframework.http.HttpMethod.POST;

class BrevoApiEmailDeliveryServiceTest {

    private static final String BASE_URL = "https://api.brevo.test/v3";
    private static final String API_KEY = "test-api-key";
    private static final String FROM_ADDRESS = "no-reply@formai.pe";
    private static final String SUBJECT = "Reset your FormAI password";
    private static final String BODY = "Use this link: http://localhost/reset?token=abc";

    @Test
    void shouldSendTheEmailFromTheVerifiedSender() {
        // Arrange
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new BrevoApiEmailDeliveryService(builder, BASE_URL, API_KEY, FROM_ADDRESS, "FormAI");
        server.expect(once(), requestTo(BASE_URL + "/smtp/email"))
                .andExpect(method(POST))
                .andExpect(header("api-key", API_KEY))
                .andExpect(content().contentType(APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.email").value(FROM_ADDRESS))
                .andExpect(jsonPath("$.sender.name").value("FormAI"))
                .andExpect(jsonPath("$.to[0].email").value(EMAIL))
                .andExpect(jsonPath("$.subject").value(SUBJECT))
                .andExpect(jsonPath("$.textContent").value(BODY))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(APPLICATION_JSON)
                        .body("{\"messageId\":\"<id@smtp-relay.mailin.fr>\"}"));

        // Act
        var delivered = service.send(EMAIL, SUBJECT, BODY);

        // Assert
        assertThat(delivered).isTrue();
        server.verify();
    }

    @Test
    void shouldReportAFailedDeliveryWhenBrevoRejectsTheEmail() {
        // Arrange
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new BrevoApiEmailDeliveryService(builder, BASE_URL, API_KEY, FROM_ADDRESS, "FormAI");
        server.expect(requestTo(BASE_URL + "/smtp/email"))
                .andRespond(withUnauthorizedRequest());

        // Act
        var delivered = service.send(EMAIL, SUBJECT, BODY);

        // Assert
        assertThat(delivered).isFalse();
        server.verify();
    }

    @Test
    void shouldReportAFailedDeliveryWhenBrevoIsUnavailable() {
        // Arrange
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new BrevoApiEmailDeliveryService(builder, BASE_URL, API_KEY, FROM_ADDRESS, "FormAI");
        server.expect(requestTo(BASE_URL + "/smtp/email"))
                .andRespond(withServerError());

        // Act
        var delivered = service.send(EMAIL, SUBJECT, BODY);

        // Assert
        assertThat(delivered).isFalse();
        server.verify();
    }

    @Test
    void shouldNotTryToSendWhenTheSenderIsNotConfigured() {
        // Arrange
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new BrevoApiEmailDeliveryService(builder, BASE_URL, API_KEY, "", "FormAI");

        // Act
        var delivered = service.send(EMAIL, SUBJECT, BODY);

        // Assert
        assertThat(delivered).isFalse();
        server.verify();
    }

    @Test
    void shouldNotTryToSendWhenTheApiKeyIsNotConfigured() {
        // Arrange
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new BrevoApiEmailDeliveryService(builder, BASE_URL, "", FROM_ADDRESS, "FormAI");

        // Act
        var delivered = service.send(EMAIL, SUBJECT, BODY);

        // Assert
        assertThat(delivered).isFalse();
        server.verify();
    }
}

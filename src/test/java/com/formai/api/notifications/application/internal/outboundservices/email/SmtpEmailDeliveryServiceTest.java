package com.formai.api.notifications.application.internal.outboundservices.email;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static com.formai.api.notifications.NotificationsTestData.EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailDeliveryServiceTest {

    private static final String SUBJECT = "Reset your FormAI password";
    private static final String BODY = "Use this link: http://localhost/reset?token=abc";

    @Mock
    JavaMailSender mailSender;

    private SmtpEmailDeliveryService serviceSendingFrom(String fromAddress) {
        return new SmtpEmailDeliveryService(mailSender, fromAddress, "FormAI");
    }

    @Test
    void shouldSendTheEmailFromTheVerifiedSender() throws Exception {
        // Arrange
        var message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);

        // Act
        var delivered = serviceSendingFrom("no-reply@formai.pe").send(EMAIL, SUBJECT, BODY);

        // Assert
        assertThat(delivered).isTrue();
        verify(mailSender).send(message);
        var from = (InternetAddress) message.getFrom()[0];
        assertThat(from.getAddress()).isEqualTo("no-reply@formai.pe");
        assertThat(from.getPersonal()).isEqualTo("FormAI");
        assertThat(message.getRecipients(Message.RecipientType.TO)[0]).hasToString(EMAIL);
        assertThat(message.getSubject()).isEqualTo(SUBJECT);
        assertThat(message.getContent()).isEqualTo(BODY);
    }

    @Test
    void shouldReportAFailedDeliveryWhenTheRelayRejectsTheEmail() {
        var message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("relay unavailable")).when(mailSender).send(message);

        assertThat(serviceSendingFrom("no-reply@formai.pe").send(EMAIL, SUBJECT, BODY)).isFalse();
    }

    @Test
    void shouldNotTryToSendWhenTheSenderIsNotConfigured() {
        assertThat(serviceSendingFrom("").send(EMAIL, SUBJECT, BODY)).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}

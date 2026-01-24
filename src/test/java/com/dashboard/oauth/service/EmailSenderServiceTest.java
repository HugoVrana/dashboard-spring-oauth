package com.dashboard.oauth.service;

import com.dashboard.oauth.environment.EmailProperties;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Email Sender Service")
@ExtendWith(MockitoExtension.class)
class EmailSenderServiceTest {

    @Mock
    private Resend resend;

    @Mock
    private Emails emails;

    private EmailSenderService emailSenderService;

    @BeforeEach
    void setUp() {
        EmailProperties emailProperties = new EmailProperties();
        emailSenderService = new EmailSenderService(resend, emailProperties);
    }

    @Test
    @DisplayName("Should return message ID when email is sent successfully")
    void shouldReturnMessageIdWhenEmailSentSuccessfully() throws ResendException {
        String expectedMessageId = "msg_" + UUID.randomUUID();
        CreateEmailResponse response = mock(CreateEmailResponse.class);

        when(resend.emails()).thenReturn(emails);
        when(response.getId()).thenReturn(expectedMessageId);
        when(emails.send(any(CreateEmailOptions.class))).thenReturn(response);

        String result = emailSenderService.sendEmail(
                "test@example.com",
                "Test Subject",
                "<p>Test content</p>"
        );

        assertThat(result).isEqualTo(expectedMessageId);
    }

    @Test
    @DisplayName("Should build email options with correct parameters")
    void shouldBuildEmailOptionsWithCorrectParameters() throws ResendException {
        String to = "recipient@example.com";
        String subject = "Test Subject";
        String content = "<p>Test HTML content</p>";

        CreateEmailResponse response = mock(CreateEmailResponse.class);
        when(resend.emails()).thenReturn(emails);
        when(response.getId()).thenReturn("msg_123");
        when(emails.send(any(CreateEmailOptions.class))).thenReturn(response);

        emailSenderService.sendEmail(to, subject, content);

        ArgumentCaptor<CreateEmailOptions> optionsCaptor = ArgumentCaptor.forClass(CreateEmailOptions.class);
        verify(emails).send(optionsCaptor.capture());

        CreateEmailOptions capturedOptions = optionsCaptor.getValue();
        assertThat(capturedOptions.getTo()).contains(to);
        assertThat(capturedOptions.getSubject()).isEqualTo(subject);
        assertThat(capturedOptions.getHtml()).isEqualTo(content);
        assertThat(capturedOptions.getFrom()).isEqualTo("Acme <onboarding@resend.dev>");
    }

    @Test
    @DisplayName("Should throw ResendException when sending fails")
    void shouldThrowResendExceptionWhenSendingFails() throws ResendException {
        when(resend.emails()).thenReturn(emails);
        when(emails.send(any(CreateEmailOptions.class)))
                .thenThrow(new ResendException("API Error"));

        assertThatThrownBy(() -> emailSenderService.sendEmail(
                "test@example.com",
                "Subject",
                "Content"
        ))
                .isInstanceOf(ResendException.class)
                .hasMessage("API Error");
    }
}

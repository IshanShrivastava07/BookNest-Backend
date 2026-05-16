package com.booknest.auth_service.service;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@booknest.com");
    }

    @Test
    void sendOtp_Success_ShouldSendHtmlMail() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendOtp("test@example.com", "123456");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendOtp_HtmlFailure_ShouldFallbackToSimpleMail() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        // Throw exception on first send
        doThrow(new RuntimeException("Mime failure")).when(mailSender).send(any(MimeMessage.class));

        emailService.sendOtp("test@example.com", "123456");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOtp_AllFailures_ShouldSwallowException() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mime failure")).when(mailSender).send(any(MimeMessage.class));
        doThrow(new RuntimeException("Simple failure")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendOtp("test@example.com", "123456");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendMail_ShouldCallSendOtp() throws Exception {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendMail("test@example.com", "Subject", "123456");

        verify(mailSender).send(any(MimeMessage.class));
    }
}

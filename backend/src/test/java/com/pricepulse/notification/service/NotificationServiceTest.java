package com.pricepulse.notification.service;

import com.pricepulse.auth.entity.User;
import com.pricepulse.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private NotificationService notificationService;
    private User user;
    private Product product;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(mailSender);
        user = new User();
        user.setEmail("alice@example.com");
        product = new Product();
        product.setId(2L);
        product.setDisplayName("Test Product");
        product.setUrl("https://example.com/product/2");
    }

    @Test
    void sendsPlainTextAlertEmailAndReturnsTrue() {
        boolean result = notificationService.sendAlertEmail(
                user, product, new BigDecimal("75.00"), new BigDecimal("80.00"));

        assertThat(result).isTrue();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("alice@example.com");
        assertThat(message.getSubject()).contains("Test Product");
        assertThat(message.getText()).contains("75.00", "80.00", product.getUrl());
    }

    @Test
    void catchesMailExceptionAndReturnsFalse() {
        doThrow(new MailException("SMTP unavailable") {})
                .when(mailSender).send(any(SimpleMailMessage.class));

        boolean result = notificationService.sendAlertEmail(
                user, product, new BigDecimal("75.00"), new BigDecimal("80.00"));

        assertThat(result).isFalse();
    }
}

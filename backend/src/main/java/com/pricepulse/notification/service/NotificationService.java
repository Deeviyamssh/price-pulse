package com.pricepulse.notification.service;

import com.pricepulse.auth.entity.User;
import com.pricepulse.product.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends the plain-text price-alert email. Mail failures are intentionally
     * converted to false so alert evaluation can persist the failed attempt
     * without aborting the scheduler run.
     */
    public boolean sendAlertEmail(User user, Product product, BigDecimal price, BigDecimal targetPrice) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Price alert: " + product.getDisplayName() + " is now $" + price);
            message.setText("Hi,\n\n"
                    + "The price of " + product.getDisplayName() + " has dropped to $" + price + ",\n"
                    + "which is at or below your target price of $" + targetPrice + ".\n\n"
                    + "View product: " + product.getUrl() + "\n\n"
                    + "— PricePulse");
            mailSender.send(message);
            return true;
        } catch (MailException ex) {
            log.error("Failed to send price alert email for product id={}: {}",
                    product.getId(), ex.getMessage(), ex);
            return false;
        }
    }
}

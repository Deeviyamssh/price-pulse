package com.pricepulse.alert.service;

import com.pricepulse.alert.entity.PriceAlert;
import com.pricepulse.alert.repository.PriceAlertRepository;
import com.pricepulse.notification.entity.NotificationChannel;
import com.pricepulse.notification.entity.NotificationLog;
import com.pricepulse.notification.repository.NotificationLogRepository;
import com.pricepulse.notification.service.NotificationService;
import com.pricepulse.product.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class AlertEvaluationService implements AlertEvaluator {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluationService.class);

    private final PriceAlertRepository priceAlertRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationService notificationService;

    public AlertEvaluationService(PriceAlertRepository priceAlertRepository,
                                  NotificationLogRepository notificationLogRepository,
                                  NotificationService notificationService) {
        this.priceAlertRepository = priceAlertRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Evaluates the active alert for a product after a successful price check.
     * A notification attempt is logged whether email delivery succeeds or fails.
     */
    @Override
    @Transactional
    public void evaluate(Product product, BigDecimal price) {
        PriceAlert alert = priceAlertRepository.findByProductIdAndActiveTrue(product.getId())
                .orElse(null);

        if (alert == null || price == null || price.compareTo(alert.getTargetPrice()) > 0) {
            return;
        }

        boolean sent = notificationService.sendAlertEmail(
                product.getUser(), product, price, alert.getTargetPrice());

        NotificationLog logEntry = new NotificationLog();
        logEntry.setPriceAlert(alert);
        logEntry.setChannel(NotificationChannel.EMAIL);
        logEntry.setSentAt(Instant.now());
        logEntry.setSuccess(sent);

        if (sent) {
            notificationLogRepository.save(logEntry);
            alert.setLastNotifiedAt(logEntry.getSentAt());
            alert.setActive(false);
            priceAlertRepository.save(alert);
        } else {
            logEntry.setErrorMessage("Unable to send price alert email");
            notificationLogRepository.save(logEntry);
            log.warn("Price alert notification failed for product id={}; alert remains active",
                    product.getId());
        }
    }
}

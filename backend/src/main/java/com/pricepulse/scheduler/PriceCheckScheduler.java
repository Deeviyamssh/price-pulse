package com.pricepulse.scheduler;

import com.pricepulse.pricing.service.PriceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Triggers periodic checks of all active products.
 */
@Component
public class PriceCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(PriceCheckScheduler.class);

    private final PriceRecordService priceRecordService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public PriceCheckScheduler(PriceRecordService priceRecordService) {
        this.priceRecordService = priceRecordService;
    }

    @Scheduled(fixedDelayString = "${pricepulse.scheduler.interval-ms:1800000}")
    public void runPriceChecks() {
        if (!running.compareAndSet(false, true)) {
            log.warn("Skipping scheduled price check run because a previous run is still in progress");
            return;
        }

        try {
            priceRecordService.checkAllActiveProducts();
        } catch (Exception e) {
            log.error("Unexpected error during scheduled price check run", e);
        } finally {
            running.set(false);
        }
    }
}

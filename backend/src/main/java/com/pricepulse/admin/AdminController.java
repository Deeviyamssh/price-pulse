package com.pricepulse.admin;

import com.pricepulse.pricing.service.PriceRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin endpoints for system maintenance tasks.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final PriceRecordService priceRecordService;

    public AdminController(PriceRecordService priceRecordService) {
        this.priceRecordService = priceRecordService;
    }

    /**
     * Manual trigger for price checks - useful for free tier deployments where
     * scheduled tasks don't run while the service is asleep.
     * Call this endpoint from an external cron service (e.g., cron-job.org) every 30 minutes.
     */
    @PostMapping("/trigger-price-check")
    public ResponseEntity<Map<String, String>> triggerPriceCheck() {
        priceRecordService.checkAllActiveProducts();
        return ResponseEntity.ok(Map.of("status", "Price check completed"));
    }
}

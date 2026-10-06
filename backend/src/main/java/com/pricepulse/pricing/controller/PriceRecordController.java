package com.pricepulse.pricing.controller;

import com.pricepulse.auth.entity.User;
import com.pricepulse.pricing.dto.PriceRecordResponse;
import com.pricepulse.pricing.service.PriceRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/prices")
public class PriceRecordController {

    private final PriceRecordService priceRecordService;

    public PriceRecordController(PriceRecordService priceRecordService) {
        this.priceRecordService = priceRecordService;
    }

    @GetMapping
    public ResponseEntity<List<PriceRecordResponse>> getPriceHistory(
            @PathVariable Long productId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(priceRecordService.getPriceHistory(user.getId(), productId));
    }
}

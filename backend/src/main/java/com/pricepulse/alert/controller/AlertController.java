package com.pricepulse.alert.controller;

import com.pricepulse.alert.dto.AlertResponse;
import com.pricepulse.alert.dto.SetAlertRequest;
import com.pricepulse.alert.service.AlertService;
import com.pricepulse.auth.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/{productId}/alert")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PutMapping
    public ResponseEntity<AlertResponse> setAlert(
            @PathVariable Long productId,
            @Valid @RequestBody SetAlertRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(alertService.setAlert(extractUserId(authentication), productId, request));
    }

    @DeleteMapping
    public ResponseEntity<AlertResponse> removeAlert(
            @PathVariable Long productId,
            Authentication authentication) {
        return ResponseEntity.ok(alertService.removeAlert(extractUserId(authentication), productId));
    }

    private Long extractUserId(Authentication authentication) {
        return ((User) authentication.getPrincipal()).getId();
    }
}

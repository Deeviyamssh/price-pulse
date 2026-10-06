package com.pricepulse.alert.service;

import com.pricepulse.alert.dto.AlertResponse;
import com.pricepulse.alert.dto.SetAlertRequest;
import com.pricepulse.alert.entity.PriceAlert;
import com.pricepulse.alert.repository.PriceAlertRepository;
import com.pricepulse.common.exception.ResourceNotFoundException;
import com.pricepulse.product.entity.Product;
import com.pricepulse.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AlertService {

    private final ProductRepository productRepository;
    private final PriceAlertRepository priceAlertRepository;

    public AlertService(ProductRepository productRepository,
                        PriceAlertRepository priceAlertRepository) {
        this.productRepository = productRepository;
        this.priceAlertRepository = priceAlertRepository;
    }

    @Transactional
    public AlertResponse setAlert(Long userId, Long productId, SetAlertRequest request) {
        Product product = productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        PriceAlert alert = priceAlertRepository.findByProductId(productId).orElse(null);
        if (alert == null) {
            alert = new PriceAlert();
            alert.setProduct(product);
            alert.setTargetPrice(request.targetPrice());
            alert.setActive(true);
            alert.setLastNotifiedAt(null);
        } else {
            BigDecimal oldTarget = alert.getTargetPrice();
            boolean targetChanged = oldTarget == null
                    || oldTarget.compareTo(request.targetPrice()) != 0;

            alert.setTargetPrice(request.targetPrice());

            if (alert.isActive() || targetChanged) {
                alert.setActive(true);
                alert.setLastNotifiedAt(null);
            }
        }

        return toResponse(priceAlertRepository.save(alert));
    }

    @Transactional
    public AlertResponse removeAlert(Long userId, Long productId) {
        productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        PriceAlert alert = priceAlertRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Price alert not found"));

        alert.setActive(false);
        return toResponse(priceAlertRepository.save(alert));
    }

    private AlertResponse toResponse(PriceAlert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getProduct().getId(),
                alert.getTargetPrice(),
                alert.isActive(),
                alert.getLastNotifiedAt()
        );
    }
}

package com.pricepulse.alert.repository;

import com.pricepulse.alert.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    Optional<PriceAlert> findByProductId(Long productId);

    Optional<PriceAlert> findByProductIdAndActiveTrue(Long productId);
}

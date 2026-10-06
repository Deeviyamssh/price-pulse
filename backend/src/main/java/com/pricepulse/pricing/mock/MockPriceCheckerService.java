package com.pricepulse.pricing.mock;

import com.pricepulse.pricing.service.PriceCheckerService;
import com.pricepulse.pricing.service.PriceResult;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulated {@link PriceCheckerService} for V1.
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>Derives a deterministic base price from the URL's hash code so that the
 *       same URL always produces prices around the same value.</li>
 *   <li>Applies a ±5 % random drift on every call to simulate market movement.</li>
 *   <li>Returns a FAILED result on ~15 % of calls to exercise failure-handling code.</li>
 * </ul>
 *
 * scraper or partner API is ready.</p>
 */
@Primary
@Service
public class MockPriceCheckerService implements PriceCheckerService {

    private static final String[] ERRORS = {
        "Connection timed out",
        "HTTP 404: Page not found",
        "Price element not found on page"
    };

    /** Probability of simulating a failed check on each invocation. */
    private static final double FAILURE_RATE = 0.15;

    @Override
    public PriceResult check(String url) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Simulate a check failure ~15 % of the time
        if (rng.nextDouble() < FAILURE_RATE) {
            String errorMessage = ERRORS[rng.nextInt(ERRORS.length)];
            return PriceResult.failed(errorMessage);
        }

        // Deterministic base price from URL hash → [10.00, 9999.99]
        // Math.abs on a long avoids the edge case where hashCode() == Integer.MIN_VALUE
        long hash = Math.abs((long) url.hashCode());
        BigDecimal base = BigDecimal.valueOf((hash % 999_000) / 100.0 + 10.00)
                .setScale(2, RoundingMode.HALF_UP);

        // ±5 % drift
        double factor = rng.nextDouble(0.95, 1.05);
        BigDecimal price = base.multiply(BigDecimal.valueOf(factor))
                .setScale(2, RoundingMode.HALF_UP);

        return PriceResult.success(price, "USD");
    }
}

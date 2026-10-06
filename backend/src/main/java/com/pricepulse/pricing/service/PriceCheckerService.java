package com.pricepulse.pricing.service;

/**
 * Abstraction for price retrieval.
 *
 * <p>Implementations must satisfy the "never throws" contract: every error condition
 * (network failure, parse error, HTTP error) must be captured in a
 * {@link PriceResult} with {@code status = FAILED} rather than propagated as an
 * exception. This allows the scheduler to call this method in a loop without a
 * single failure aborting the remaining checks.</p>
 *
 * <p>Swap path: implement this interface, annotate the new class with
 * {@code @Primary}, and remove {@code @Primary} from
 * {@code MockPriceCheckerService}. No other class needs to change.</p>
 */
public interface PriceCheckerService {

    /**
     * Fetch the current price for the product at the given URL.
     *
     * @param url the product page URL; never null
     * @return a {@link PriceResult} — never null, never throws
     */
    PriceResult check(String url);
}

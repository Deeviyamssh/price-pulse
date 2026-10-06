package com.pricepulse.pricing.repository;

import com.pricepulse.pricing.entity.CheckStatus;
import com.pricepulse.pricing.entity.PriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PriceRecord}.
 *
 * <p>All query methods include {@code productId} in the predicate so that ownership
 * enforcement is delegated to the service layer (which supplies only IDs belonging to
 * the requesting user).</p>
 */
@Repository
public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    /**
     * Returns the full price history for a product, ordered oldest-first.
     * Ties in {@code checkedAt} are broken by record {@code id} ascending.
     *
     * <p>Used by {@code GET /api/products/{id}/prices} (Requirements 10.1, 10.2).</p>
     *
     * @param productId the owning product's id
     * @return all records for the product, sorted {@code checkedAt ASC, id ASC}
     */
    List<PriceRecord> findByProductIdOrderByCheckedAtAscIdAsc(Long productId);

    /**
     * Returns the two most recent SUCCESS (or FAILED) price records for a product,
     * newest-first. Typically called with {@link CheckStatus#SUCCESS} to derive the
     * current price (index 0) and the previous price (index 1) for the dashboard.
     *
     * <p>Used by {@code GET /api/products} to compute {@code currentPrice} and
     * {@code previousPrice} (Requirement 5.2).</p>
     *
     * @param productId the owning product's id
     * @param status    the check status to filter by (usually SUCCESS)
     * @return up to 2 records, sorted {@code checkedAt DESC, id DESC}
     */
    List<PriceRecord> findTop2ByProductIdAndCheckStatusOrderByCheckedAtDescIdDesc(
            Long productId, CheckStatus status);

    /**
     * Returns the minimum price recorded for a product across all records with the
     * given check status. Used to compute {@code lowestRecordedPrice} on the dashboard.
     *
     * <p>Used by {@code GET /api/products} (Requirement 5.2).</p>
     *
     * @param productId the owning product's id
     * @param status    the check status to filter by (usually SUCCESS)
     * @return the minimum price, or {@link Optional#empty()} if no matching records exist
     */
    @Query("SELECT MIN(pr.price) FROM PriceRecord pr " +
           "WHERE pr.product.id = :productId AND pr.checkStatus = :status")
    Optional<BigDecimal> findMinPriceByProductIdAndCheckStatus(
            @Param("productId") Long productId,
            @Param("status") CheckStatus status);

    /**
     * Returns the single most-recent PriceRecord for a product, regardless of
     * check status. Used to populate {@code lastCheckedAt}, {@code lastCheckStatus},
     * and {@code errorMessage} on the dashboard summary (Requirements 5.2, 14.3).
     *
     * @param productId the owning product's id
     * @return the latest record, or {@link Optional#empty()} if no records exist
     */
    Optional<PriceRecord> findTopByProductIdOrderByCheckedAtDescIdDesc(Long productId);
}

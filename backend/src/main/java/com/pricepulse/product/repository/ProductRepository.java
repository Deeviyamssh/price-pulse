package com.pricepulse.product.repository;

import com.pricepulse.product.entity.Product;
import com.pricepulse.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Data-access layer for {@link Product}.
 * <p>
 * All queries that read or mutate user-owned products include {@code userId}
 * in the predicate (ownership enforcement — Req 3.3). The scheduler-facing
 * query uses only {@code status} because the scheduler has no user context.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Looks up a product by its id <em>and</em> its owner's id.
     * Returns {@code Optional.empty()} if the product does not exist
     * <strong>or</strong> if it belongs to a different user — the two cases
     * are intentionally indistinguishable to the caller (returns 404 either way).
     */
    Optional<Product> findByIdAndUserId(Long id, Long userId);

    /**
     * Returns all products owned by {@code userId}, newest first.
     * Used by the dashboard (Req 5.4 — ordered by {@code created_at} DESC).
     */
    List<Product> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Returns {@code true} if the user is already monitoring this URL.
     * Used to enforce the one-URL-per-user constraint (Req 4.2) at the
     * application layer (the DB UNIQUE constraint is the final guard).
     */
    boolean existsByUserIdAndUrl(Long userId, String url);

    /**
     * Returns all products with the given status.
     * Used by the scheduler to fetch every ACTIVE product for price-checking.
     */
    @Query("select p from Product p join fetch p.user where p.status = :status")
    List<Product> findByStatusWithUser(@Param("status") ProductStatus status);
}

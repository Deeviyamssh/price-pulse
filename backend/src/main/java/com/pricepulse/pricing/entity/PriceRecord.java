package com.pricepulse.pricing.entity;

import com.pricepulse.pricing.service.PriceResult;
import com.pricepulse.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * An append-only record of a single price-check attempt for a {@link Product}.
 * Maps to the {@code price_records} table.
 *
 * <p>On SUCCESS: {@code price} and {@code currency} are non-null; {@code errorMessage} is null.</p>
 * <p>On FAILED:  {@code price} and {@code currency} are null; {@code errorMessage} is non-null.</p>
 */
@Entity
@Table(name = "price_records")
public class PriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The product this price record belongs to. Loaded lazily to avoid pulling
     * the full Product graph on every price-record query.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The fetched price. Null when {@code checkStatus} is FAILED.
     */
    @Column(nullable = true, precision = 15, scale = 2)
    private BigDecimal price;

    /**
     * The currency code (e.g. "USD"). Null when {@code checkStatus} is FAILED.
     */
    @Column(nullable = true, length = 10)
    private String currency;

    /**
     * The timestamp at which the check was performed. Set on first persist
     * if not already provided.
     */
    @Column(name = "checked_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ NOT NULL DEFAULT NOW()")
    private Instant checkedAt;

    /**
     * Whether the price check succeeded or failed. Stored as a VARCHAR so
     * the column value is readable without consulting the enum class.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "check_status", nullable = false, length = 10)
    private CheckStatus checkStatus;

    /**
     * Human-readable description of the failure. Null when {@code checkStatus} is SUCCESS.
     */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    private void onPrePersist() {
        if (checkedAt == null) {
            checkedAt = Instant.now();
        }
    }

    // ── Static factory methods ───────────────────────────────────────────────

    /**
     * Build a {@code PriceRecord} from a {@link PriceResult} returned by the checker.
     *
     * <ul>
     *   <li>SUCCESS result → price, currency, and checkStatus=SUCCESS are set.</li>
     *   <li>FAILED result  → price=null, currency=null, checkStatus=FAILED, errorMessage set.</li>
     * </ul>
     *
     * @param product the product that was checked
     * @param result  the checker outcome
     * @return a new, unsaved {@code PriceRecord}
     */
    public static PriceRecord from(Product product, PriceResult result) {
        PriceRecord record = new PriceRecord();
        record.product = product;
        record.checkStatus = result.status();

        if (result.status() == CheckStatus.SUCCESS) {
            record.price = result.price();
            record.currency = result.currency();
            record.errorMessage = null;
        } else {
            record.price = null;
            record.currency = null;
            record.errorMessage = result.errorMessage();
        }

        return record;
    }

    /**
     * Build a FAILED {@code PriceRecord} from a raw exception message (used when
     * an unhandled exception escapes the checker call itself).
     *
     * @param product      the product that was being checked
     * @param errorMessage the exception message
     * @return a new, unsaved {@code PriceRecord} with check_status FAILED
     */
    public static PriceRecord failed(Product product, String errorMessage) {
        PriceRecord record = new PriceRecord();
        record.product = product;
        record.checkStatus = CheckStatus.FAILED;
        record.price = null;
        record.currency = null;
        record.errorMessage = errorMessage;
        return record;
    }

    // ── Getters and setters ──────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(Instant checkedAt) {
        this.checkedAt = checkedAt;
    }

    public CheckStatus getCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(CheckStatus checkStatus) {
        this.checkStatus = checkStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

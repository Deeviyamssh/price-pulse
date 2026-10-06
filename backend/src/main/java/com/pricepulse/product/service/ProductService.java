package com.pricepulse.product.service;

import com.pricepulse.alert.entity.PriceAlert;
import com.pricepulse.alert.repository.PriceAlertRepository;
import com.pricepulse.auth.entity.User;
import com.pricepulse.auth.repository.UserRepository;
import com.pricepulse.common.exception.ConflictException;
import com.pricepulse.common.exception.ResourceNotFoundException;
import com.pricepulse.pricing.entity.CheckStatus;
import com.pricepulse.pricing.entity.PriceRecord;
import com.pricepulse.pricing.repository.PriceRecordRepository;
import com.pricepulse.pricing.service.PriceCheckerService;
import com.pricepulse.pricing.service.PriceResult;
import com.pricepulse.product.dto.AddProductRequest;
import com.pricepulse.product.dto.ProductResponse;
import com.pricepulse.product.entity.Product;
import com.pricepulse.product.entity.ProductStatus;
import com.pricepulse.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for product management.
 *
 * <h3>Ownership enforcement</h3>
 * Every method that reads or mutates a product passes {@code userId} to
 * {@link ProductRepository#findByIdAndUserId} so that a user can never see,
 * pause, resume, or delete another user's product — returning 404 for both
 * "not found" and "wrong owner" cases (Req 3.3 / design principle).
 *
 * <h3>Summary field computation</h3>
 * {@link #buildSummary} performs up to 3 extra queries per product. This is an
 * intentional N+1 trade-off: it keeps the service simple and readable for an
 * MVP with a small number of products per user. Future optimization: replace
 * with a single JPQL projection or a native SQL query that computes all
 * summary fields in one round-trip.
 *
 * <h3>Alert fields</h3>
 * {@code targetPrice} and {@code alertActive} are stubs (null / false) until
 * task 7.1 introduces {@code PriceAlertRepository}. At that point, inject the
 * repository here and populate those fields inside {@link #buildSummary}.
 */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final PriceRecordRepository priceRecordRepository;
    private final PriceCheckerService priceCheckerService;
    private final UserRepository userRepository;
    private final PriceAlertRepository priceAlertRepository;

    public ProductService(ProductRepository productRepository,
                          PriceRecordRepository priceRecordRepository,
                          PriceCheckerService priceCheckerService,
                          UserRepository userRepository,
                          PriceAlertRepository priceAlertRepository) {
        this.productRepository = productRepository;
        this.priceRecordRepository = priceRecordRepository;
        this.priceCheckerService = priceCheckerService;
        this.userRepository = userRepository;
        this.priceAlertRepository = priceAlertRepository;
    }

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Creates a new product for the given user and immediately runs the first
     * price check (Req 4.1).
     *
     * <ul>
     *   <li>Duplicate URL for the same user → {@link ConflictException} (409).</li>
     *   <li>Price check failure → PriceRecord stored with FAILED status; product
     *       creation still succeeds (Req 4.5).</li>
     * </ul>
     *
     * @param userId  the authenticated user's id
     * @param request the validated request body
     * @return the created product with its initial summary fields
     */
    @Transactional
    public ProductResponse addProduct(Long userId, AddProductRequest request) {
        // Application-level duplicate check; DB UNIQUE constraint is the final backstop
        if (productRepository.existsByUserIdAndUrl(userId, request.url())) {
            throw new ConflictException("You are already monitoring this URL");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = new Product();
        product.setUser(user);
        product.setUrl(request.url());
        product.setDisplayName(request.displayName());
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        // Immediate first price check. Even if this throws, the product is created.
        // The catch block stores a FAILED PriceRecord so the failure is visible in the UI.
        PriceResult result;
        try {
            result = priceCheckerService.check(request.url());
        } catch (Exception ex) {
            log.error("First price check threw for product id={}: {}", product.getId(), ex.getMessage(), ex);
            result = PriceResult.failed(ex.getMessage() != null ? ex.getMessage() : "Unexpected error during first check");
        }

        priceRecordRepository.save(PriceRecord.from(product, result));

        return buildSummary(product);
    }

    /**
     * Returns all products owned by the given user, ordered by creation date
     * descending (Req 5.1, 5.4).
     */
    public List<ProductResponse> listProducts(Long userId) {
        return productRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::buildSummary)
                .toList();
    }

    /**
     * Returns a single product owned by the given user (Req 3.3).
     *
     * @throws ResourceNotFoundException if the product does not exist or belongs
     *                                   to a different user (returns 404 either way)
     */
    public ProductResponse getProduct(Long userId, Long productId) {
        Product product = productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return buildSummary(product);
    }

    /**
     * Deletes a product and all associated child records (Req 6.1).
     * The database {@code ON DELETE CASCADE} constraints on {@code price_records},
     * {@code price_alerts}, and {@code notification_logs} handle child deletion
     * atomically within the same transaction.
     *
     * @throws ResourceNotFoundException if the product does not exist or belongs
     *                                   to a different user (Req 6.2)
     */
    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        Product product = productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productRepository.delete(product);
        log.debug("Deleted product id={} for userId={}", productId, userId);
    }

    /**
     * Sets a product's status to PAUSED (Req 7.1).
     *
     * @throws ConflictException         if the product is already PAUSED (Req 7.4)
     * @throws ResourceNotFoundException if the product does not exist or is not owned
     *                                   by this user
     */
    @Transactional
    public ProductResponse pauseProduct(Long userId, Long productId) {
        Product product = productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStatus() == ProductStatus.PAUSED) {
            throw new ConflictException("Product is already paused");
        }

        product.setStatus(ProductStatus.PAUSED);
        productRepository.save(product);
        return buildSummary(product);
    }

    /**
     * Sets a product's status to ACTIVE (Req 7.2).
     *
     * @throws ConflictException         if the product is already ACTIVE (Req 7.4)
     * @throws ResourceNotFoundException if the product does not exist or is not owned
     *                                   by this user
     */
    @Transactional
    public ProductResponse resumeProduct(Long userId, Long productId) {
        Product product = productRepository.findByIdAndUserId(productId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStatus() == ProductStatus.ACTIVE) {
            throw new ConflictException("Product is already active");
        }

        product.setStatus(ProductStatus.ACTIVE);
        productRepository.save(product);
        return buildSummary(product);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Assembles a {@link ProductResponse} from a {@link Product} by running
     * additional repository queries for the computed summary fields (Req 5.2, 14.3).
     *
     * <p>Queries issued per call:
     * <ol>
     *   <li>{@code findTop2ByProductIdAndCheckStatus} — currentPrice + previousPrice</li>
     *   <li>{@code findMinPriceByProductIdAndCheckStatus} — lowestRecordedPrice</li>
     *   <li>{@code findTopByProductIdOrderByCheckedAtDescIdDesc} — lastCheckedAt,
     *       lastCheckStatus, errorMessage</li>
     * </ol>
     *
     * <p>Alert fields ({@code targetPrice}, {@code alertActive}) are null/false stubs
     * until {@code PriceAlertRepository} is introduced in task 7.1.
     */
    private ProductResponse buildSummary(Product product) {
        Long productId = product.getId();

        // --- Prices from SUCCESS records -----------------------------------------
        List<PriceRecord> topSuccess = priceRecordRepository
                .findTop2ByProductIdAndCheckStatusOrderByCheckedAtDescIdDesc(
                        productId, CheckStatus.SUCCESS);

        BigDecimal currentPrice  = topSuccess.size() >= 1 ? topSuccess.get(0).getPrice() : null;
        BigDecimal previousPrice = topSuccess.size() >= 2 ? topSuccess.get(1).getPrice() : null;

        BigDecimal lowestRecordedPrice = priceRecordRepository
                .findMinPriceByProductIdAndCheckStatus(productId, CheckStatus.SUCCESS)
                .orElse(null);

        // --- Most recent record (any status) — for last-check metadata -----------
        Optional<PriceRecord> latest = priceRecordRepository
                .findTopByProductIdOrderByCheckedAtDescIdDesc(productId);

        Instant lastCheckedAt    = latest.map(PriceRecord::getCheckedAt).orElse(null);
        String  lastCheckStatus  = latest.map(r -> r.getCheckStatus().name()).orElse(null);
        // errorMessage is null on SUCCESS records by construction (PriceRecord.from)
        String  errorMessage     = latest.map(PriceRecord::getErrorMessage).orElse(null);

        // --- Alert fields -------------------------------------------------------
        Optional<PriceAlert> alert = priceAlertRepository.findByProductId(productId);
        BigDecimal targetPrice = alert.map(PriceAlert::getTargetPrice).orElse(null);
        boolean alertActive = alert.map(PriceAlert::isActive).orElse(false);

        return new ProductResponse(
                product.getId(),
                product.getDisplayName(),
                product.getUrl(),
                product.getStatus().name(),
                product.getCreatedAt(),
                currentPrice,
                previousPrice,
                lowestRecordedPrice,
                targetPrice,
                lastCheckedAt,
                lastCheckStatus,
                errorMessage,
                alertActive
        );
    }
}

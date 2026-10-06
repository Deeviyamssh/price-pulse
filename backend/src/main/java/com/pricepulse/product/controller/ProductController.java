package com.pricepulse.product.controller;

import com.pricepulse.auth.entity.User;
import com.pricepulse.product.dto.AddProductRequest;
import com.pricepulse.product.dto.ProductResponse;
import com.pricepulse.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for product management.
 *
 * <h3>Authentication</h3>
 * Every endpoint is protected by the JWT filter (Spring Security requires
 * authentication for all paths outside {@code /api/auth/**}).
 * The controller extracts the authenticated user's id from the Spring Security
 * {@link Authentication} object — the principal was set by
 * {@link com.pricepulse.auth.security.JwtAuthFilter} as a {@link User} entity.
 *
 * <h3>Ownership</h3>
 * The controller passes {@code userId} to every service call; the service then
 * uses {@code findByIdAndUserId} so that cross-user access silently returns 404
 * (Req 3.3 — no 403, no existence leak).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * {@code POST /api/products}
     *
     * <p>Creates a new product, runs the first price check immediately, and
     * returns the created product with its initial summary fields (Req 4.1).
     * Returns 409 if the URL is already being monitored by this user (Req 4.2).
     */
    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(
            @Valid @RequestBody AddProductRequest request,
            Authentication auth) {
        ProductResponse response = productService.addProduct(extractUserId(auth), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * {@code GET /api/products}
     *
     * <p>Returns all products owned by the current user, ordered by creation date
     * descending, each with computed summary fields (Req 5.1, 5.2, 5.4).
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> listProducts(Authentication auth) {
        return ResponseEntity.ok(productService.listProducts(extractUserId(auth)));
    }

    /**
     * {@code GET /api/products/{id}}
     *
     * <p>Returns a single product with its summary fields. Returns 404 if the
     * product does not exist or belongs to a different user (Req 3.3).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(productService.getProduct(extractUserId(auth), id));
    }

    /**
     * {@code DELETE /api/products/{id}}
     *
     * <p>Deletes a product and all child records atomically via DB cascade (Req 6.1).
     * Returns 404 if the product does not exist or belongs to a different user (Req 6.2).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            Authentication auth) {
        productService.deleteProduct(extractUserId(auth), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code PATCH /api/products/{id}/pause}
     *
     * <p>Sets the product status to PAUSED (Req 7.1).
     * Returns 409 if the product is already PAUSED (Req 7.4).
     */
    @PatchMapping("/{id}/pause")
    public ResponseEntity<ProductResponse> pauseProduct(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(productService.pauseProduct(extractUserId(auth), id));
    }

    /**
     * {@code PATCH /api/products/{id}/resume}
     *
     * <p>Sets the product status to ACTIVE (Req 7.2).
     * Returns 409 if the product is already ACTIVE (Req 7.4).
     */
    @PatchMapping("/{id}/resume")
    public ResponseEntity<ProductResponse> resumeProduct(
            @PathVariable Long id,
            Authentication auth) {
        return ResponseEntity.ok(productService.resumeProduct(extractUserId(auth), id));
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    /**
     * Extracts the authenticated user's database id from the Spring Security
     * context. The principal is the {@link User} entity placed there by
     * {@link com.pricepulse.auth.security.JwtAuthFilter}.
     */
    private Long extractUserId(Authentication auth) {
        User user = (User) auth.getPrincipal();
        return user.getId();
    }
}

package com.pricepulse.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /api/products}.
 *
 * <p>Bean Validation enforces:
 * <ul>
 *   <li>{@code url} — non-blank and must be an absolute HTTP or HTTPS URL (Req 4.3)</li>
 *   <li>{@code displayName} — non-blank, at most 255 characters (Req 4.4)</li>
 * </ul>
 */
public record AddProductRequest(

        @NotBlank(message = "URL must not be blank")
        @Pattern(
                regexp = "https?://.+",
                message = "URL must be a well-formed absolute URL with http or https scheme"
        )
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        String url,

        @NotBlank(message = "Display name must not be blank")
        @Size(max = 255, message = "Display name must not exceed 255 characters")
        String displayName
) {}

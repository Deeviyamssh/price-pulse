package com.pricepulse;

import org.junit.jupiter.api.Test;

/**
 * Smoke test — verifies the application context loads without errors.
 *
 * Full context loading requires a running PostgreSQL instance (Testcontainers).
 * A lightweight version using @WebMvcTest or slice tests is preferred for CI;
 * this class is a placeholder until the datasource is wired.
 */
class PricePulseApplicationTests {

    @Test
    void contextLoads() {
        // Spring Boot context load test — will be expanded in integration tests
        // once Testcontainers PostgreSQL is configured (Task 10).
    }
}

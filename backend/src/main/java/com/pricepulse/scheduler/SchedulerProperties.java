package com.pricepulse.scheduler;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for scheduled price checks.
 *
 * <p>The value is expressed in milliseconds because Spring's
 * {@code fixedDelayString} consumes milliseconds. The validation bounds are
 * the equivalent of one minute through one day.</p>
 */
@ConfigurationProperties(prefix = "pricepulse.scheduler")
@Validated
public class SchedulerProperties {

    public static final long MIN_INTERVAL_MS = 60_000L;
    public static final long MAX_INTERVAL_MS = 86_400_000L;

    @Min(value = MIN_INTERVAL_MS, message = "Scheduler interval must be at least 1 minute")
    @Max(value = MAX_INTERVAL_MS, message = "Scheduler interval must be at most 1440 minutes")
    private long intervalMs = 1_800_000L;

    public long getIntervalMs() {
        return intervalMs;
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = intervalMs;
    }
}

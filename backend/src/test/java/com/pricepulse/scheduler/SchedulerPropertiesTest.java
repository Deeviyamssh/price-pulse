package com.pricepulse.scheduler;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerPropertiesTest {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void acceptsOneMinuteThroughOneDay() {
        SchedulerProperties properties = new SchedulerProperties();

        properties.setIntervalMs(SchedulerProperties.MIN_INTERVAL_MS);
        assertThat(validator.validate(properties)).isEmpty();

        properties.setIntervalMs(SchedulerProperties.MAX_INTERVAL_MS);
        assertThat(validator.validate(properties)).isEmpty();
    }

    @Test
    void rejectsIntervalsOutsideConfiguredRange() {
        SchedulerProperties properties = new SchedulerProperties();

        properties.setIntervalMs(SchedulerProperties.MIN_INTERVAL_MS - 1);
        Set<ConstraintViolation<SchedulerProperties>> tooShort = validator.validate(properties);
        assertThat(tooShort).hasSize(1);

        properties.setIntervalMs(SchedulerProperties.MAX_INTERVAL_MS + 1);
        Set<ConstraintViolation<SchedulerProperties>> tooLong = validator.validate(properties);
        assertThat(tooLong).hasSize(1);
    }
}

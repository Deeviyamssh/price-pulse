package com.pricepulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pricepulse.scheduler.SchedulerProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SchedulerProperties.class)
public class PricePulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricePulseApplication.class, args);
    }
}

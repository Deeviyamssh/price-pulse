package com.pricepulse.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;

/**
 * Controls authentication-cookie transport security.
 *
 * <p>Secure cookies are the default. Disabling Secure is permitted only for
 * explicitly selected local development profiles so an insecure cookie cannot
 * accidentally be enabled in staging or production.</p>
 */
@Component
@ConfigurationProperties(prefix = "pricepulse.auth")
public class AuthCookieProperties {

    private final Environment environment;
    private boolean cookieSecure = true;

    public AuthCookieProperties(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void validateCookieSecurity() {
        if (!cookieSecure && !isLocalDevelopmentProfileActive()) {
            throw new IllegalStateException(
                    "pricepulse.auth.cookie-secure=false is allowed only with the local or dev Spring profile");
        }
    }

    private boolean isLocalDevelopmentProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("local") || profile.equals("dev") || profile.equals("docker"));
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }
}

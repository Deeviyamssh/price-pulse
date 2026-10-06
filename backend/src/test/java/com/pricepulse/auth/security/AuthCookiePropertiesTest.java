package com.pricepulse.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthCookiePropertiesTest {

    @Test
    void defaultsToSecureCookies() {
        AuthCookieProperties properties = new AuthCookieProperties(new MockEnvironment());

        properties.validateCookieSecurity();

        assertThat(properties.isCookieSecure()).isTrue();
    }

    @Test
    void allowsInsecureCookiesInLocalProfile() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "local");
        AuthCookieProperties properties = new AuthCookieProperties(environment);
        properties.setCookieSecure(false);

        properties.validateCookieSecurity();

        assertThat(properties.isCookieSecure()).isFalse();
    }

    @Test
    void allowsInsecureCookiesInDevProfile() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "dev");
        AuthCookieProperties properties = new AuthCookieProperties(environment);
        properties.setCookieSecure(false);

        properties.validateCookieSecurity();

        assertThat(properties.isCookieSecure()).isFalse();
    }

    @Test
    void rejectsInsecureCookiesWithoutLocalDevelopmentProfile() {
        AuthCookieProperties properties = new AuthCookieProperties(new MockEnvironment());
        properties.setCookieSecure(false);

        assertThatThrownBy(properties::validateCookieSecurity)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only with the local or dev Spring profile");
    }

    @Test
    void rejectsInsecureCookiesInProductionProfile() {
        MockEnvironment environment = new MockEnvironment().withProperty("spring.profiles.active", "prod");
        AuthCookieProperties properties = new AuthCookieProperties(environment);
        properties.setCookieSecure(false);

        assertThatThrownBy(properties::validateCookieSecurity)
                .isInstanceOf(IllegalStateException.class);
    }
}

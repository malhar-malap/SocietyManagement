package com.society.jaswanti.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretGuardTests {

    // The value committed in src/test/resources/application.properties (public on GitHub)
    private static final String PUBLIC_TEST_SECRET = "9a4f2c8d3b7a1e6f45c8a0b3f267d8b1d4e6f3c8a9d2b5f8e3a9c8b5f6v8a3d9";

    private JwtService service(String secret, boolean allowPublic) {
        JwtService jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "secretKey", secret);
        ReflectionTestUtils.setField(jwt, "allowPublicTestSecret", allowPublic);
        return jwt;
    }

    @Test
    @DisplayName("The publicly committed test secret is refused outside tests")
    void publicSecretRefused() {
        assertThatThrownBy(() -> service(PUBLIC_TEST_SECRET, false).validateSecret())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("publicly known");
    }

    @Test
    @DisplayName("The test configuration may explicitly allow the public test secret")
    void publicSecretAllowedForTests() {
        assertThatCode(() -> service(PUBLIC_TEST_SECRET, true).validateSecret()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Missing, non-base64 and short secrets are refused; a fresh 64-byte secret is accepted")
    void weakSecretsRefused() {
        assertThatThrownBy(() -> service("", false).validateSecret()).hasMessageContaining("must be set");
        assertThatThrownBy(() -> service("not base64 !!!", false).validateSecret()).hasMessageContaining("base64");
        assertThatThrownBy(() -> service(Base64.getEncoder().encodeToString(new byte[16]), false).validateSecret())
                .hasMessageContaining("at least 32 bytes");
        byte[] fresh = new byte[64];
        new java.security.SecureRandom().nextBytes(fresh);
        assertThatCode(() -> service(Base64.getEncoder().encodeToString(fresh), false).validateSecret()).doesNotThrowAnyException();
    }
}

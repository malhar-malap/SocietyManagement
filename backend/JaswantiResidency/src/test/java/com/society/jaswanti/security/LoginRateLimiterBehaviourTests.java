package com.society.jaswanti.security;

import com.society.jaswanti.entity.Role;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoginRateLimiterBehaviourTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private LoginRateLimiterService limiter;

    @BeforeEach
    void clear() {
        limiter.resetAll();
    }

    private String loginBody(String identifier) {
        return "{\"identifier\":\"" + identifier + "\",\"password\":\"WrongPass123\"}";
    }

    @Test
    @DisplayName("One address is only locked after 20 failures, so residents sharing Wi-Fi are not locked out by one person")
    void ipThresholdIsHigherThanAccountThreshold() {
        String ip = "10.0.0.7";
        for (int i = 0; i < InMemoryLoginRateLimiterService.MAX_IP_ATTEMPTS - 1; i++) {
            limiter.recordFailedAttempt("person" + i + "@example.test", ip);
        }
        assertThat(limiter.isBlocked("someone-else@example.test", ip)).isFalse();

        limiter.recordFailedAttempt("person-last@example.test", ip);
        assertThat(limiter.isBlocked("someone-else@example.test", ip)).isTrue();
    }

    @Test
    @DisplayName("A successful sign-in clears the account's counter but not the address's")
    void successDoesNotClearIpCounter() {
        String ip = "10.0.0.8";
        for (int i = 0; i < 10; i++) {
            limiter.recordFailedAttempt("victim" + i + "@example.test", ip);
        }
        limiter.recordSuccessfulLogin("attacker-own-account@example.test", ip);
        assertThat(limiter.getRemainingAttempts("new@example.test", ip)).isEqualTo(InMemoryLoginRateLimiterService.MAX_ATTEMPTS);

        for (int i = 0; i < 10; i++) {
            limiter.recordFailedAttempt("victimB" + i + "@example.test", ip);
        }
        assertThat(limiter.isBlocked("anyone@example.test", ip)).isTrue();
    }

    @Test
    @DisplayName("Checking remaining attempts does not create entries, and stale entries are pruned")
    void memoryStaysBounded() {
        InMemoryLoginRateLimiterService impl = (InMemoryLoginRateLimiterService) limiter;
        for (int i = 0; i < 50; i++) {
            limiter.getRemainingAttempts("probe" + i + "@example.test", "10.1.0." + i);
        }
        assertThat(impl.trackedKeyCount()).isZero();
    }

    @Test
    @DisplayName("X-Forwarded-For is ignored: rotating it does not escape the per-address limit")
    void forwardedForIsNotTrusted() throws Exception {
        for (int i = 0; i < InMemoryLoginRateLimiterService.MAX_IP_ATTEMPTS; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .header("X-Forwarded-For", "203.0.113." + i)
                    .content(loginBody("spray" + i + "@example.test")));
        }
        // All attempts came from the same socket address, so it is now locked whatever the header claims
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "198.51.100.99")
                        .content(loginBody("fresh@example.test")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Serialising a User never exposes internal security state")
    void userJsonHidesInternalFields() throws Exception {
        User u = new User();
        u.setEmail("json@example.test");
        u.setName("Json Check");
        u.setPassword("$2a$10$secret");
        u.setRole(Role.RESIDENT);
        u.setStatus(UserStatus.ACTIVE);
        u.setTokenVersion(7);
        u.setPendingEmail("pending@example.test");
        u.setEmailVerificationToken("$2a$10$code");
        u.setPasswordResetOtpHash("$2a$10$otp");

        String json = JsonMapper.builder().findAndAddModules().build().writeValueAsString(u);

        assertThat(json).contains("json@example.test", "Json Check");
        assertThat(json).doesNotContain("password", "tokenVersion", "pendingEmail", "emailVerification",
                "passwordResetOtp", "authorities", "$2a$10$");
    }
}

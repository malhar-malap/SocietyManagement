package com.society.jaswanti.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigurationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Allowed origin receives valid CORS pre-flight response with Idempotency-Key and PATCH")
    void allowedOriginPreflightReceivesCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/bills/1/record-payment")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type,Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().exists("Access-Control-Allow-Methods"));
    }

    @Test
    @DisplayName("Disallowed origin does not receive Access-Control-Allow-Origin header")
    void disallowedOriginPreflightFails() throws Exception {
        mockMvc.perform(options("/api/bills/1/record-payment")
                        .header("Origin", "http://evil-attacker.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isForbidden());
    }
}

package com.society.jaswanti.security;

import com.society.jaswanti.entity.ActivityLog;
import com.society.jaswanti.repository.ActivityLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Behind Caddy/Nginx the backend sees every connection coming from the proxy. These run a real server (MockMvc
 * bypasses Tomcat's forwarded-header handling) to check the client IP and HTTPS are taken from the proxy's headers
 * only when the connection comes from a trusted proxy.
 */
class ReverseProxyHeadersTests {

    static HttpResponse<String> failedLogin(int port, String forwardedFor, String forwardedProto) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"identifier\":\"proxy-test@example.test\",\"password\":\"Wrong12345\"}"));
        if (forwardedFor != null) req.header("X-Forwarded-For", forwardedFor);
        if (forwardedProto != null) req.header("X-Forwarded-Proto", forwardedProto);
        return HttpClient.newHttpClient().send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    static String lastLoginIp(ActivityLogRepository logs) {
        List<ActivityLog> entries = logs.findAll().stream()
                .filter(l -> "LOGIN".equals(l.getAction()))
                .sorted(Comparator.comparing(ActivityLog::getId))
                .toList();
        assertThat(entries).isNotEmpty();
        return entries.get(entries.size() - 1).getIpAddress();
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    class FromTrustedProxy {
        @LocalServerPort int port;
        @Autowired ActivityLogRepository logs;

        @BeforeEach
        void clean() { logs.deleteAll(); }

        @Test
        @DisplayName("Requests through a trusted proxy record the real client IP, and HTTPS gets HSTS")
        void usesForwardedClientIpAndHttps() throws Exception {
            HttpResponse<String> res = failedLogin(port, "203.0.113.7", "https");
            assertThat(res.statusCode()).isEqualTo(401);
            assertThat(lastLoginIp(logs)).isEqualTo("203.0.113.7");
            assertThat(res.headers().firstValue("Strict-Transport-Security")).isPresent();
        }

        @Test
        @DisplayName("Plain HTTP through the proxy gets no HSTS header")
        void noHstsOverHttp() throws Exception {
            HttpResponse<String> res = failedLogin(port, "203.0.113.8", "http");
            assertThat(res.headers().firstValue("Strict-Transport-Security")).isEmpty();
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
            properties = "server.tomcat.remoteip.internal-proxies=10\\.255\\.255\\.255")
    class FromUntrustedAddress {
        @LocalServerPort int port;
        @Autowired ActivityLogRepository logs;

        @BeforeEach
        void clean() { logs.deleteAll(); }

        @Test
        @DisplayName("A client that is not a trusted proxy cannot fake its IP with X-Forwarded-For")
        void ignoresSpoofedHeader() throws Exception {
            failedLogin(port, "198.51.100.9", "https");
            assertThat(lastLoginIp(logs)).isNotEqualTo("198.51.100.9").isIn("127.0.0.1", "0:0:0:0:0:0:0:1");
        }
    }
}

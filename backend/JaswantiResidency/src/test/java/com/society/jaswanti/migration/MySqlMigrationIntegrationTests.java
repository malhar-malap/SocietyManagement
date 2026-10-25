package com.society.jaswanti.migration;

import com.society.jaswanti.JaswantiResidencyApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Boots the real application, with the real src/main application.properties (Flyway on, baseline 14,
 * ddl-auto=validate), against a throwaway MySQL database. H2 cannot run the MySQL-specific migrations, so these
 * tests are the only ones that exercise Flyway and schema validation end to end.
 *
 * Opt-in: they run only when -Dmysql.it.url points at a disposable MySQL server, for example
 *   ./mvnw test -Dtest=MySqlMigrationIntegrationTests -Dmysql.it.url=jdbc:mysql://127.0.0.1:3399/ -Dmysql.it.user=root
 * Each test creates its own it_* database and drops it afterwards. NEVER point this at a real or shared server.
 */
class MySqlMigrationIntegrationTests {

    private static final String BASE_URL = System.getProperty("mysql.it.url");
    private static final String USER = System.getProperty("mysql.it.user", "root");
    private static final String PASSWORD = System.getProperty("mysql.it.password", "");
    private static final String URL_PARAMS = "?useSSL=false&allowPublicKeyRetrieval=true";

    // Exact rows V2 seeds; the hashes are of admin123 and Temp_C-602 / Temp_C-101
    private static final String ADMIN_HASH = "$2a$10$DAmzyQm0rGLPdbw9BZ70B.6qyhhTH5jQ9ez3ZAKaI6U.6ZhGnpKXO";
    private static final String RESIDENT_602_HASH = "$2a$10$zaG2epASBBfSiKqZVCoSYufp3buFJNjVjI5d2H7yy3f/WvqabSQB2";
    private static final String RESIDENT_101_HASH = "$2a$10$9r75d7y/YRmqfiNFF4SFdOoHuknNcBEPme6HR5xbgvSJZhOdJ.lhq";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private String database;
    private JdbcTemplate jdbc;
    private ConfigurableApplicationContext app;

    @BeforeEach
    void createDatabase() {
        assumeTrue(BASE_URL != null && !BASE_URL.isBlank(),
                "Set -Dmysql.it.url=jdbc:mysql://host:port/ (a disposable MySQL server) to run MySQL integration tests");
        database = "it_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        serverJdbc().execute("CREATE DATABASE " + database);
        jdbc = new JdbcTemplate(new DriverManagerDataSource(BASE_URL + database + URL_PARAMS, USER, PASSWORD));
    }

    @AfterEach
    void dropDatabase() {
        if (app != null) {
            app.close();
        }
        if (database != null) {
            serverJdbc().execute("DROP DATABASE IF EXISTS " + database);
        }
    }

    private JdbcTemplate serverJdbc() {
        return new JdbcTemplate(new DriverManagerDataSource(BASE_URL + URL_PARAMS, USER, PASSWORD));
    }

    /** Starts the app with production configuration. Datasource keys are set directly so a local .env cannot redirect it. */
    private void startApp(String... extraArgs) {
        List<String> args = new ArrayList<>(List.of(
                "--spring.config.location=file:src/main/resources/application.properties",
                "--spring.datasource.url=" + BASE_URL + database + URL_PARAMS,
                "--spring.datasource.username=" + USER,
                "--spring.datasource.password=" + PASSWORD,
                "--JWT_SECRET=" + Base64.getEncoder().encodeToString(UUID.randomUUID().toString().repeat(3).getBytes()),
                "--MAIL_USERNAME=it@example.test",
                "--MAIL_PASSWORD=unused",
                "--server.port=0"));
        args.addAll(List.of(extraArgs));
        app = new SpringApplicationBuilder(JaswantiResidencyApplication.class).run(args.toArray(String[]::new));
    }

    private void loadLegacyHibernateSchema() throws Exception {
        try (Connection conn = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("mysql-it/legacy-hibernate-schema.sql"));
        }
    }

    private void insertUser(String email, String hash, boolean forcePasswordChange) {
        jdbc.update("INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, " +
                        "created_at, password_reset_otp_attempts) VALUES (?, 0, 1, ?, ?, 'RESIDENT', 'ACTIVE', ?, NOW(), 0)",
                email, email, hash, forcePasswordChange);
    }

    private void insertUserWithTokenVersion(String email, String hash, boolean forcePasswordChange) {
        jdbc.update("INSERT INTO users (email, email_verified, is_active, name, password, role, status, force_password_change, " +
                        "created_at, password_reset_otp_attempts, token_version) VALUES (?, 0, 1, ?, ?, 'RESIDENT', 'ACTIVE', ?, NOW(), 0, 0)",
                email, email, hash, forcePasswordChange);
    }

    private Map<String, Object> user(String email) {
        return jdbc.queryForMap("SELECT is_active, status, token_version FROM users WHERE email = ?", email);
    }

    private String columnType(String table, String column) {
        return jdbc.queryForObject("SELECT column_type FROM information_schema.columns " +
                "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?", String.class, table, column);
    }

    private List<String> appliedVersions() {
        return jdbc.queryForList("SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank", String.class);
    }

    /** ACTIVE accounts that still accept admin123 or Temp_<their room>. */
    private int activeAccountsWithDefaultCredentials() {
        int matches = 0;
        for (Map<String, Object> row : jdbc.queryForList("SELECT u.password, r.room_number FROM users u " +
                "LEFT JOIN rooms r ON r.resident_id = u.id WHERE u.is_active = 1 AND u.status = 'ACTIVE'")) {
            String hash = (String) row.get("password");
            String room = (String) row.get("room_number");
            if (encoder.matches("admin123", hash) || (room != null && encoder.matches("Temp_" + room, hash))) {
                matches++;
            }
        }
        return matches;
    }

    private int login(String identifier, String password) throws Exception {
        int port = ((WebServerApplicationContext) app).getWebServer().getPort();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"identifier\":\"" + identifier + "\",\"password\":\"" + password + "\"}"))
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    @Test
    @DisplayName("Fresh database: full migration chain applies, schema validates, defaults are retired, bootstrap works")
    void freshDatabase() throws Exception {
        startApp("--BOOTSTRAP_MASTER_ADMIN_ENABLED=true",
                "--BOOTSTRAP_MASTER_ADMIN_EMAIL=it-master@example.test",
                "--BOOTSTRAP_MASTER_ADMIN_NAME=IT Master",
                "--BOOTSTRAP_MASTER_ADMIN_PASSWORD=ItMasterPass123");

        assertThat(appliedVersions()).startsWith("1", "1.1", "2").endsWith("15", "16", "17", "18", "19");

        // No account is usable with a committed default credential, and no seeded row was deleted
        assertThat(activeAccountsWithDefaultCredentials()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email LIKE '%@jaswanti.com' AND is_active = 0 AND status = 'INACTIVE'", Integer.class))
                .isEqualTo(81);
        assertThat(login("admin@jaswanti.com", "admin123")).isNotEqualTo(200);

        // Master Admin bootstrap still works on a fresh database
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = 'it-master@example.test' AND role = 'MASTER_ADMIN' AND is_active = 1 AND status = 'ACTIVE'", Integer.class))
                .isEqualTo(1);
        assertThat(login("it-master@example.test", "ItMasterPass123")).isEqualTo(200);

        // System log (V17) works on MySQL: the bootstrap and both sign-in attempts were recorded
        assertThat(jdbc.queryForList("SELECT action FROM activity_logs ORDER BY id", String.class))
                .containsSubsequence("BOOTSTRAP_MASTER_ADMIN", "LOGIN", "LOGIN");
        assertThat(jdbc.queryForMap("SELECT user_id, actor_email, outcome FROM activity_logs WHERE action = 'LOGIN' ORDER BY id LIMIT 1"))
                .containsEntry("user_id", null).containsEntry("actor_email", "admin@jaswanti.com").containsEntry("outcome", "DENIED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM activity_logs WHERE action = 'LOGIN' AND outcome = 'SUCCESS' AND actor_role = 'MASTER_ADMIN'", Integer.class))
                .isEqualTo(1);

        // V16 shape
        assertThat(columnType("resident_payment_submissions", "screenshot_data")).isEqualTo("longblob");
        assertThat(columnType("complaints", "status")).contains("'APPROVED'").contains("'REJECTED'");
        assertThat(columnType("users", "pending_email")).isEqualTo("varchar(255)");
        // V19 leaves fresh databases' VARCHAR columns alone
        assertThat(columnType("maintenance_bills", "charge_type")).isEqualTo("varchar(32)");
    }

    @Test
    @DisplayName("Existing Hibernate-built database: baselined at 14, V15 and V16 run, seeded defaults are retired")
    void existingHibernateDatabase() throws Exception {
        loadLegacyHibernateSchema();
        // Reproduce enum columns frozen by an early Hibernate run (as found on the real dev database)
        jdbc.execute("ALTER TABLE maintenance_bills MODIFY COLUMN charge_type ENUM('MAINTENANCE','PENALTY','SINKING_FUND','SPECIAL_ASSESSMENT') NOT NULL");
        jdbc.execute("ALTER TABLE users MODIFY COLUMN role ENUM('ADMIN','RESIDENT') NOT NULL");
        insertUserWithTokenVersion("admin@jaswanti.com", ADMIN_HASH, false);
        jdbc.update("UPDATE users SET role = 'ADMIN', email_verified = 1 WHERE email = 'admin@jaswanti.com'");
        insertUserWithTokenVersion("resident602@jaswanti.com", RESIDENT_602_HASH, true);
        insertUserWithTokenVersion("resident101@jaswanti.com", encoder.encode("ChangedPass123"), false); // already changed their password
        jdbc.update("INSERT INTO rooms (room_number, floor, wing, noc_amount, noc_applicable, unit_category, resident_id) " +
                "SELECT 'C-602', 6, 'C', 0, 0, 'RESIDENTIAL', id FROM users WHERE email = 'resident602@jaswanti.com'");
        assertThat(fixtureReproducesPreFixState()).isTrue();

        startApp();

        assertThat(appliedVersions()).containsExactly("14", "15", "16", "17", "18", "19");

        assertThat(user("admin@jaswanti.com")).containsEntry("status", "INACTIVE").containsEntry("token_version", 1);
        assertThat(user("resident602@jaswanti.com")).containsEntry("status", "INACTIVE").containsEntry("token_version", 1);
        assertThat(user("resident101@jaswanti.com")).containsEntry("status", "ACTIVE").containsEntry("token_version", 0);
        assertThat(activeAccountsWithDefaultCredentials()).isZero();
        assertThat(login("admin@jaswanti.com", "admin123")).isNotEqualTo(200);
        assertThat(login("C-602", "Temp_C-602")).isNotEqualTo(200);
        assertThat(login("resident101@jaswanti.com", "ChangedPass123")).isEqualTo(200);

        // The 255-byte TINYBLOB upload columns are widened
        assertThat(columnType("resident_payment_submissions", "screenshot_data")).isEqualTo("longblob");
        assertThat(columnType("complaints", "image_data")).isEqualTo("longblob");
        assertThat(columnType("rooms", "share_certificate_data")).isEqualTo("longblob");

        // V17 on a Hibernate-built activity_logs (Hibernate-named FK): user_id is nullable and new columns exist
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() " +
                "AND table_name = 'activity_logs' AND column_name = 'user_id'", String.class)).isEqualTo("YES");
        assertThat(columnType("activity_logs", "outcome")).isEqualTo("varchar(16)");
        // V19: frozen enum columns are widened to the current Java enums (NOC bills, Master Admin role)
        assertThat(columnType("maintenance_bills", "charge_type")).contains("'NOC'");
        assertThat(columnType("users", "role")).contains("'MASTER_ADMIN'");
        assertThat(jdbc.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'users' AND column_name = 'role'", String.class)).isEqualTo("NO");
        // V18: setup-code attempt counter exists on a Hibernate-built schema too
        assertThat(columnType("users", "email_verification_attempts")).isEqualTo("int");
    }

    @Test
    @DisplayName("Existing database that predates token_version still migrates and retires the seeded admin")
    void existingDatabaseWithoutTokenVersion() throws Exception {
        loadLegacyHibernateSchema();
        jdbc.execute("ALTER TABLE users DROP COLUMN token_version");
        insertUser("admin@jaswanti.com", ADMIN_HASH, false);
        insertUser("resident101@jaswanti.com", RESIDENT_101_HASH, true);

        startApp();

        assertThat(columnType("users", "token_version")).isEqualTo("int");
        assertThat(user("admin@jaswanti.com")).containsEntry("status", "INACTIVE").containsEntry("token_version", 1);
        assertThat(user("resident101@jaswanti.com")).containsEntry("status", "INACTIVE");
    }

    /** Sanity check that the fixture really reproduces the pre-fix state (TINYBLOB uploads, no Flyway history). */
    private boolean fixtureReproducesPreFixState() {
        return "tinyblob".equals(columnType("resident_payment_submissions", "screenshot_data"))
                && jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() " +
                "AND table_name = 'flyway_schema_history'", Integer.class) == 0;
    }
}

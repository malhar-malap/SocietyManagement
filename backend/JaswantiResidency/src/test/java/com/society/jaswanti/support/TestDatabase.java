package com.society.jaswanti.support;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

/**
 * Empties every table in the in-memory test database. Test classes that assume a clean database call this at the
 * start of each test, so results no longer depend on which classes happened to run first (CI on Linux runs them in
 * a different order than macOS, and leftover rows broke FK-ordered deletes and count-based assertions).
 */
@Component
public class TestDatabase {

    private final JdbcTemplate jdbc;

    public TestDatabase(DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }

    public void wipe() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'",
                String.class);
        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            for (String table : tables) {
                jdbc.execute("TRUNCATE TABLE \"" + table + "\" RESTART IDENTITY");
            }
        } finally {
            jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}

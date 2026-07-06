package com.cnpc.promoretail.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void devDbFlywayMigrationsCreateRuleGovernanceTables() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     select table_name
                     from information_schema.tables
                     where table_schema = 'public'
                       and table_name in (
                         'import_batch',
                         'import_error_row',
                         'promotion_rule_draft',
                         'promotion_rule_version',
                         'promotion_rule_audit_log',
                         'checkout_calculation_record'
                       )
                     """)) {
            assertThat(tableNames(resultSet)).containsExactlyInAnyOrder(
                    "import_batch",
                    "import_error_row",
                    "promotion_rule_draft",
                    "promotion_rule_version",
                    "promotion_rule_audit_log",
                    "checkout_calculation_record"
            );
        }
    }

    private java.util.List<String> tableNames(ResultSet resultSet) throws Exception {
        java.util.List<String> names = new java.util.ArrayList<>();
        while (resultSet.next()) {
            names.add(resultSet.getString("table_name"));
        }
        return names;
    }
}

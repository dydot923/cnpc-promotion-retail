package com.cnpc.promoretail.ruleengine.datetrigger;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("dev-db")
@Testcontainers(disabledWithoutDocker = true)
class DateTriggerDatabaseTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private PromotionDateTriggerRepository repository;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Test
    void findAllImportedTriggersThroughV33() {
        assertThat(repository.findAllEnabled()).hasSize(13);
    }

    @Test
    void importedMonthlyAndRangeTriggersCanEvaluateDates() {
        assertThat(repository.findByRuleId("abv2-a4-cn98-volume-discount"))
                .singleElement()
                .satisfies(trigger -> {
                    assertThat(trigger.isTriggered(LocalDate.of(2026, 7, 8), LocalTime.NOON)).isTrue();
                    assertThat(trigger.isTriggered(LocalDate.of(2026, 7, 9), LocalTime.NOON)).isFalse();
                });
        assertThat(repository.findByRuleId("abv2-g4-event-night-discount"))
                .singleElement()
                .satisfies(trigger -> {
                    assertThat(trigger.isTriggered(LocalDate.of(2026, 7, 1), LocalTime.of(23, 0))).isTrue();
                    assertThat(trigger.isTriggered(LocalDate.of(2026, 7, 1), LocalTime.NOON)).isFalse();
                });
    }
}

package com.cnpc.promoretail.common.clock;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BusinessClockServiceTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-20T12:00:00Z"),
            ZoneId.of("Asia/Shanghai")
    );

    @Test
    void usesSystemDateUntilAnOverrideIsSet() {
        BusinessClockService service = new BusinessClockService("", FIXED_CLOCK);

        assertThat(service.current().businessDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(service.current().overrideEnabled()).isFalse();

        assertThat(service.setBusinessDate(LocalDate.of(2026, 7, 19)).businessDate())
                .isEqualTo(LocalDate.of(2026, 7, 19));
        assertThat(service.current().overrideEnabled()).isTrue();

        assertThat(service.reset().businessDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(service.current().overrideEnabled()).isFalse();
    }

    @Test
    void acceptsStartupOverride() {
        BusinessClockService service = new BusinessClockService("2026-07-27", FIXED_CLOCK);

        assertThat(service.current().businessDate()).isEqualTo(LocalDate.of(2026, 7, 27));
        assertThat(service.current().overrideEnabled()).isTrue();
    }
}

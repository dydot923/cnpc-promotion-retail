package com.cnpc.promoretail.common.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BusinessClockService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final Clock clock;
    private final AtomicReference<LocalDate> overrideDate;
    private final AtomicReference<Instant> updatedAt;

    @Autowired
    public BusinessClockService(@Value("${app.business-clock.override-date:}") String configuredOverrideDate) {
        this(configuredOverrideDate, Clock.system(BUSINESS_ZONE));
    }

    BusinessClockService(String configuredOverrideDate, Clock clock) {
        this.clock = clock;
        this.overrideDate = new AtomicReference<>(parseConfiguredDate(configuredOverrideDate));
        this.updatedAt = new AtomicReference<>(Instant.now(clock));
    }

    public BusinessClockState current() {
        LocalDate systemDate = LocalDate.now(clock);
        LocalDate configuredDate = overrideDate.get();
        return new BusinessClockState(
                configuredDate == null ? systemDate : configuredDate,
                systemDate,
                configuredDate != null,
                updatedAt.get()
        );
    }

    public BusinessClockState setBusinessDate(LocalDate businessDate) {
        overrideDate.set(businessDate);
        updatedAt.set(Instant.now(clock));
        return current();
    }

    public BusinessClockState reset() {
        overrideDate.set(null);
        updatedAt.set(Instant.now(clock));
        return current();
    }

    private static LocalDate parseConfiguredDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    public record BusinessClockState(
            LocalDate businessDate,
            LocalDate systemDate,
            boolean overrideEnabled,
            Instant updatedAt
    ) {
    }
}

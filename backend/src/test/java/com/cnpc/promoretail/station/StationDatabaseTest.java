package com.cnpc.promoretail.station;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.station.model.StationQuery;
import com.cnpc.promoretail.station.model.StationResponse;
import java.util.List;
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
class StationDatabaseTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private StationService stationService;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Test
    void findV26ImportedStations() {
        List<StationResponse> stations = stationService.stations(new StationQuery(null, null, null, null));

        assertThat(stations).hasSize(297);
        assertThat(stations)
                .allSatisfy(station -> {
                    assertThat(station.stationCode()).isNotBlank();
                    assertThat(station.stationName()).isNotBlank();
                    assertThat(station.stationType()).isNotBlank();
                    assertThat(station.province()).isNotBlank();
                });
    }

    @Test
    void stationQueryWithMultipleConditionsUsesImportedFields() {
        List<StationResponse> stations = stationService.stations(new StationQuery(null, null, null, null));
        StationResponse sample = stations.stream()
                .filter(station -> !station.city().isBlank())
                .filter(station -> !station.district().isBlank())
                .findFirst()
                .orElseThrow();

        List<StationResponse> filtered = stationService.stations(new StationQuery(
                sample.city(),
                sample.district(),
                sample.stationType(),
                null
        ));

        assertThat(filtered)
                .extracting(StationResponse::stationCode)
                .contains(sample.stationCode());
        assertThat(stationService.station(sample.stationCode()).stationName()).isEqualTo(sample.stationName());
    }
}

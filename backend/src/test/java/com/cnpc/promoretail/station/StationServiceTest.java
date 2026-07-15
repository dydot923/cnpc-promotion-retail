package com.cnpc.promoretail.station;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cnpc.promoretail.station.model.Station;
import com.cnpc.promoretail.station.model.StationQuery;
import com.cnpc.promoretail.station.model.StationResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class StationServiceTest {

    @Test
    void findByStationCodeReturnsStationDetail() {
        DefaultStationService service = service();

        StationResponse response = service.station("station-001");

        assertThat(response.stationCode()).isEqualTo("station-001");
        assertThat(response.city()).isEqualTo("Urumqi");
        assertThat(response.stationType()).isEqualTo("gas_station");
    }

    @Test
    void findByCityFiltersStations() {
        DefaultStationService service = service();

        List<StationResponse> responses = service.stations(new StationQuery("Urumqi", null, null, null));

        assertThat(responses)
                .extracting(StationResponse::stationCode)
                .containsExactly("station-001", "station-002");
    }

    @Test
    void findByStationTypeFiltersStations() {
        DefaultStationService service = service();

        List<StationResponse> responses =
                service.stations(new StationQuery(null, null, "gas_filling_station", null));

        assertThat(responses)
                .extracting(StationResponse::stationCode)
                .containsExactly("station-cng-001");
    }

    @Test
    void findBySalesScopeFiltersStations() {
        DefaultStationService service = service();

        List<StationResponse> responses = service.stations(new StationQuery(null, null, null, "car_wash"));

        assertThat(responses)
                .extracting(StationResponse::stationCode)
                .containsExactly("station-002");
    }

    @Test
    void stationNotFoundThrowsException() {
        DefaultStationService service = service();

        assertThatThrownBy(() -> service.station("missing"))
                .isInstanceOf(StationNotFoundException.class);
    }

    private DefaultStationService service() {
        InMemoryStationRepository repository = new InMemoryStationRepository();
        repository.save(station("station-001", "Urumqi", "Tianshan", "gas_station", List.of("fuel", "store")));
        repository.save(station("station-002", "Urumqi", "Shayibake", "gas_station", List.of("car_wash")));
        repository.save(station("station-cng-001", "Changji", "Changji", "gas_filling_station", List.of("fuel")));
        return new DefaultStationService(repository);
    }

    private Station station(
            String stationCode,
            String city,
            String district,
            String stationType,
            List<String> salesScope
    ) {
        return new Station(
                stationCode,
                "hos-" + stationCode,
                "Station " + stationCode,
                "branch",
                "prefecture",
                "Xinjiang",
                city,
                district,
                "address",
                null,
                null,
                "contact",
                "phone",
                stationType,
                salesScope,
                "",
                "test",
                1,
                false
        );
    }
}

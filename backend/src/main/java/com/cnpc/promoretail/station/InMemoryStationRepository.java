package com.cnpc.promoretail.station;

import com.cnpc.promoretail.station.model.Station;
import com.cnpc.promoretail.station.model.StationQuery;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryStationRepository implements StationRepository {

    public static final String DEFAULT_STATION_CODE = "1-A6501-C001-S001";

    private final ConcurrentMap<String, Station> stations = new ConcurrentHashMap<>();

    public InMemoryStationRepository() {
        seedDefaultStation();
    }

    @Override
    public List<Station> findByQuery(StationQuery query) {
        StationQuery effectiveQuery = query == null ? new StationQuery(null, null, null, null) : query;
        return stations.values().stream()
                .filter(station -> matches(effectiveQuery, station))
                .sorted(Comparator.comparing(Station::stationCode))
                .toList();
    }

    @Override
    public Optional<Station> findByStationCode(String stationCode) {
        if (stationCode == null || stationCode.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(stations.get(normalize(stationCode)));
    }

    public Station save(Station station) {
        stations.put(normalize(station.stationCode()), station);
        return station;
    }

    private void seedDefaultStation() {
        save(new Station(
                DEFAULT_STATION_CODE,
                "LA07",
                "\u5b9d\u5c71\u8def\u52a0\u6cb9\u7ad9",
                "\u4e4c\u9c81\u6728\u9f5036",
                "\u4e4c\u9c81\u6728\u9f50",
                "\u65b0\u7586",
                "\u4e4c\u9c81\u6728\u9f50",
                "\u6c99\u4f9d\u5df4\u514b\u533a",
                "\u5b9d\u5c71\u8def417\u53f7",
                new BigDecimal("87.592905"),
                new BigDecimal("43.800311"),
                "\u77f3\u5dcd",
                "13619948052",
                "gas_station",
                List.of("\u4e00\u5361\u901a\u9500\u552e\u7ad9\u70b9"),
                "\u6838\u5fc3\u533a\u57df\uff0c\u4ea4\u901a\u8981\u9053",
                "\u53c2\u80034-\u201c\u4e00\u5361\u901a\u201d\u9500\u552e\u7ad9\u70b9\u660e\u7ec6",
                55,
                false
        ));
    }

    private boolean matches(StationQuery query, Station station) {
        return matchesText(query.city(), station.city())
                && matchesText(query.district(), station.district())
                && matchesText(query.stationType(), station.stationType())
                && matchesScope(query.salesScope(), station);
    }

    private boolean matchesText(String expected, String actual) {
        return expected == null || expected.equalsIgnoreCase(actual);
    }

    private boolean matchesScope(String expectedScope, Station station) {
        if (expectedScope == null) {
            return true;
        }
        return station.salesScope().stream()
                .anyMatch(scope -> expectedScope.equalsIgnoreCase(scope));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}

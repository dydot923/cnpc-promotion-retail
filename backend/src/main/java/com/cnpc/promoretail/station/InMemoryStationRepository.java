package com.cnpc.promoretail.station;

import com.cnpc.promoretail.station.model.Station;
import com.cnpc.promoretail.station.model.StationQuery;
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

    private final ConcurrentMap<String, Station> stations = new ConcurrentHashMap<>();

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

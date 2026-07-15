package com.cnpc.promoretail.station;

import com.cnpc.promoretail.station.model.StationQuery;
import com.cnpc.promoretail.station.model.StationResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultStationService implements StationService {

    private final StationRepository stationRepository;

    public DefaultStationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    public List<StationResponse> stations(StationQuery query) {
        return stationRepository.findByQuery(query).stream()
                .sorted(Comparator.comparing(station -> station.stationCode()))
                .map(StationResponse::from)
                .toList();
    }

    @Override
    public StationResponse station(String stationCode) {
        return stationRepository.findByStationCode(stationCode)
                .map(StationResponse::from)
                .orElseThrow(() -> new StationNotFoundException(stationCode));
    }
}

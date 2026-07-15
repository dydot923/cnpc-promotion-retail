package com.cnpc.promoretail.station;

import com.cnpc.promoretail.station.model.Station;
import com.cnpc.promoretail.station.model.StationQuery;
import java.util.List;
import java.util.Optional;

public interface StationRepository {

    List<Station> findByQuery(StationQuery query);

    Optional<Station> findByStationCode(String stationCode);
}

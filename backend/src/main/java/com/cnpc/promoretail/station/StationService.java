package com.cnpc.promoretail.station;

import com.cnpc.promoretail.station.model.StationQuery;
import com.cnpc.promoretail.station.model.StationResponse;
import java.util.List;

public interface StationService {

    List<StationResponse> stations(StationQuery query);

    StationResponse station(String stationCode);
}

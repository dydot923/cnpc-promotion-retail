package com.cnpc.promoretail.station;

import com.cnpc.promoretail.common.api.ApiResponse;
import com.cnpc.promoretail.station.model.StationQuery;
import com.cnpc.promoretail.station.model.StationResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    public ApiResponse<List<StationResponse>> stations(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String stationType,
            @RequestParam(required = false) String salesScope
    ) {
        return ApiResponse.ok(stationService.stations(new StationQuery(city, district, stationType, salesScope)));
    }

    @GetMapping("/{stationCode}")
    public ApiResponse<StationResponse> station(@PathVariable String stationCode) {
        return ApiResponse.ok(stationService.station(stationCode));
    }
}

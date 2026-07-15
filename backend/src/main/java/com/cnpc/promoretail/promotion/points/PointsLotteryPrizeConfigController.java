package com.cnpc.promoretail.promotion.points;

import com.cnpc.promoretail.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points-lottery/prize-configs")
public class PointsLotteryPrizeConfigController {

    private final PointsLotteryPrizeConfigRepository repository;

    public PointsLotteryPrizeConfigController(PointsLotteryPrizeConfigRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ApiResponse<List<PointsLotteryPrizeConfigResponse>> list(
            @RequestParam(defaultValue = PointsLotteryPrizeConfig.DEFAULT_ACTIVITY_CODE) String activityCode
    ) {
        return ApiResponse.ok(repository.findByActivityCode(activityCode).stream()
                .map(PointsLotteryPrizeConfigResponse::from)
                .toList());
    }

    @PostMapping
    public ApiResponse<PointsLotteryPrizeConfigResponse> create(
            @Valid @RequestBody PointsLotteryPrizeConfigRequest request
    ) {
        return ApiResponse.ok(PointsLotteryPrizeConfigResponse.from(repository.save(request.toConfig(null))));
    }

    @PutMapping("/{prizeId}")
    public ApiResponse<PointsLotteryPrizeConfigResponse> update(
            @PathVariable String prizeId,
            @Valid @RequestBody PointsLotteryPrizeConfigRequest request
    ) {
        return ApiResponse.ok(PointsLotteryPrizeConfigResponse.from(repository.save(request.toConfig(prizeId))));
    }
}

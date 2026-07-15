package com.cnpc.promoretail.promotion.points;

import com.cnpc.promoretail.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/{memberCode}/points")
public class PointsPromotionController {

    private final PointsPromotionService pointsPromotionService;

    public PointsPromotionController(PointsPromotionService pointsPromotionService) {
        this.pointsPromotionService = pointsPromotionService;
    }

    @PostMapping("/exchange-discount")
    public ApiResponse<PointsExchangeResponse> exchangeDiscount(
            @PathVariable String memberCode,
            @Valid @RequestBody PointsExchangeRequest request
    ) {
        return ApiResponse.ok(pointsPromotionService.exchangeDiscount(memberCode, request));
    }

    @PostMapping("/lottery-draws")
    public ApiResponse<PointsLotteryDrawResponse> draw(
            @PathVariable String memberCode,
            @RequestBody PointsLotteryDrawRequest request
    ) {
        return ApiResponse.ok(pointsPromotionService.draw(memberCode, request));
    }

    @GetMapping("/lottery-draws")
    public ApiResponse<List<PointsLotteryDrawResponse>> lotteryDraws(
            @PathVariable String memberCode,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ApiResponse.ok(pointsPromotionService.lotteryDraws(memberCode, limit));
    }
}

package com.cnpc.promoretail.promotion.points;

import java.util.List;
import java.util.Optional;

public interface PointsLotteryPrizeConfigRepository {

    List<PointsLotteryPrizeConfig> findByActivityCode(String activityCode);

    List<PointsLotteryPrizeConfig> findActiveByActivityCode(String activityCode);

    Optional<PointsLotteryPrizeConfig> findByPrizeId(String prizeId);

    PointsLotteryPrizeConfig save(PointsLotteryPrizeConfig config);
}

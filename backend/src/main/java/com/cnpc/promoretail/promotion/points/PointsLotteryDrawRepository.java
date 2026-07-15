package com.cnpc.promoretail.promotion.points;

import java.util.List;

public interface PointsLotteryDrawRepository {

    PointsLotteryDraw save(PointsLotteryDraw draw);

    List<PointsLotteryDraw> findByMemberCode(String memberCode, int limit);
}

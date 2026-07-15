package com.cnpc.promoretail.promotion.points;

import java.util.List;

public interface PointsActivityRepository {

    List<PointsActivity> findActive();
}

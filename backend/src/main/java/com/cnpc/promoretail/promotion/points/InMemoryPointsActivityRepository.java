package com.cnpc.promoretail.promotion.points;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryPointsActivityRepository implements PointsActivityRepository {

    private final CopyOnWriteArrayList<PointsActivity> activities = new CopyOnWriteArrayList<>();

    public void save(PointsActivity activity) {
        activities.removeIf(existing -> existing.activityId().equals(activity.activityId()));
        activities.add(activity);
    }

    @Override
    public List<PointsActivity> findActive() {
        return activities.stream()
                .filter(PointsActivity::active)
                .toList();
    }
}

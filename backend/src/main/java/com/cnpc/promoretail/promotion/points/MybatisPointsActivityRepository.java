package com.cnpc.promoretail.promotion.points;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.points.persistence.PointsActivityEntity;
import com.cnpc.promoretail.promotion.points.persistence.PointsActivityMapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisPointsActivityRepository implements PointsActivityRepository {

    private final PointsActivityMapper mapper;

    public MybatisPointsActivityRepository(PointsActivityMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PointsActivity> findActive() {
        return mapper.selectList(new LambdaQueryWrapper<PointsActivityEntity>()
                        .eq(PointsActivityEntity::getStatus, "ACTIVE"))
                .stream()
                .map(PointsActivityEntity::toActivity)
                .toList();
    }
}

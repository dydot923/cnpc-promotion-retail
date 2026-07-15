package com.cnpc.promoretail.promotion.points;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.points.persistence.PointsLotteryDrawEntity;
import com.cnpc.promoretail.promotion.points.persistence.PointsLotteryDrawMapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisPointsLotteryDrawRepository implements PointsLotteryDrawRepository {

    private final PointsLotteryDrawMapper mapper;

    public MybatisPointsLotteryDrawRepository(PointsLotteryDrawMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public PointsLotteryDraw save(PointsLotteryDraw draw) {
        mapper.insert(PointsLotteryDrawEntity.from(draw));
        return draw;
    }

    @Override
    public List<PointsLotteryDraw> findByMemberCode(String memberCode, int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 200));
        return mapper.selectList(new LambdaQueryWrapper<PointsLotteryDrawEntity>()
                        .eq(PointsLotteryDrawEntity::getMemberCode, memberCode)
                        .orderByDesc(PointsLotteryDrawEntity::getCreatedAt)
                        .last("limit " + effectiveLimit))
                .stream()
                .map(PointsLotteryDrawEntity::toDraw)
                .toList();
    }
}

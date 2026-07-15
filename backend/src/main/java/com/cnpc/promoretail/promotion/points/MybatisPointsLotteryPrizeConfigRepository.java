package com.cnpc.promoretail.promotion.points;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.promotion.points.persistence.PointsLotteryPrizeConfigEntity;
import com.cnpc.promoretail.promotion.points.persistence.PointsLotteryPrizeConfigMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisPointsLotteryPrizeConfigRepository implements PointsLotteryPrizeConfigRepository {

    private final PointsLotteryPrizeConfigMapper mapper;

    public MybatisPointsLotteryPrizeConfigRepository(PointsLotteryPrizeConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<PointsLotteryPrizeConfig> findByActivityCode(String activityCode) {
        String effectiveActivityCode = activityCode == null || activityCode.isBlank()
                ? PointsLotteryPrizeConfig.DEFAULT_ACTIVITY_CODE
                : activityCode.trim();
        return mapper.selectList(new LambdaQueryWrapper<PointsLotteryPrizeConfigEntity>()
                        .eq(PointsLotteryPrizeConfigEntity::getActivityCode, effectiveActivityCode)
                        .orderByAsc(PointsLotteryPrizeConfigEntity::getPrizeId))
                .stream()
                .map(PointsLotteryPrizeConfigEntity::toConfig)
                .toList();
    }

    @Override
    public List<PointsLotteryPrizeConfig> findActiveByActivityCode(String activityCode) {
        return findByActivityCode(activityCode).stream()
                .filter(PointsLotteryPrizeConfig::active)
                .sorted(Comparator.comparingInt(PointsLotteryPrizeConfig::weight).reversed()
                        .thenComparing(PointsLotteryPrizeConfig::prizeId))
                .toList();
    }

    @Override
    public Optional<PointsLotteryPrizeConfig> findByPrizeId(String prizeId) {
        if (prizeId == null || prizeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectById(prizeId.trim()))
                .map(PointsLotteryPrizeConfigEntity::toConfig);
    }

    @Override
    public PointsLotteryPrizeConfig save(PointsLotteryPrizeConfig config) {
        PointsLotteryPrizeConfigEntity entity = PointsLotteryPrizeConfigEntity.from(config);
        if (mapper.selectById(config.prizeId()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return config;
    }
}

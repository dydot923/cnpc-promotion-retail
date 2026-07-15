package com.cnpc.promoretail.promotion.points;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryPointsLotteryPrizeConfigRepository implements PointsLotteryPrizeConfigRepository {

    private final ConcurrentMap<String, PointsLotteryPrizeConfig> configs = new ConcurrentHashMap<>();

    public InMemoryPointsLotteryPrizeConfigRepository() {
        save(PointsLotteryPrizeConfig.defaultNoPrize());
        save(PointsLotteryPrizeConfig.defaultStoreCoupon());
    }

    @Override
    public List<PointsLotteryPrizeConfig> findByActivityCode(String activityCode) {
        String effectiveActivityCode = activityCode == null || activityCode.isBlank()
                ? PointsLotteryPrizeConfig.DEFAULT_ACTIVITY_CODE
                : activityCode.trim();
        return configs.values().stream()
                .filter(config -> effectiveActivityCode.equals(config.activityCode()))
                .sorted(Comparator.comparing(PointsLotteryPrizeConfig::prizeId))
                .toList();
    }

    @Override
    public List<PointsLotteryPrizeConfig> findActiveByActivityCode(String activityCode) {
        return findByActivityCode(activityCode).stream()
                .filter(PointsLotteryPrizeConfig::active)
                .toList();
    }

    @Override
    public Optional<PointsLotteryPrizeConfig> findByPrizeId(String prizeId) {
        if (prizeId == null || prizeId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(configs.get(prizeId.trim()));
    }

    @Override
    public PointsLotteryPrizeConfig save(PointsLotteryPrizeConfig config) {
        configs.put(config.prizeId(), config);
        return config;
    }
}

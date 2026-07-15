package com.cnpc.promoretail.promotion.points;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryPointsLotteryDrawRepository implements PointsLotteryDrawRepository {

    private final CopyOnWriteArrayList<PointsLotteryDraw> draws = new CopyOnWriteArrayList<>();

    @Override
    public PointsLotteryDraw save(PointsLotteryDraw draw) {
        draws.add(draw);
        return draw;
    }

    @Override
    public List<PointsLotteryDraw> findByMemberCode(String memberCode, int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 200));
        return draws.stream()
                .filter(draw -> draw.memberCode().equals(memberCode))
                .sorted(Comparator.comparing(PointsLotteryDraw::createdAt).reversed())
                .limit(effectiveLimit)
                .toList();
    }
}

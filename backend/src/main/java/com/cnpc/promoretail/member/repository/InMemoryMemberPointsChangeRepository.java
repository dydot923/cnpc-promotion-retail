package com.cnpc.promoretail.member.repository;

import com.cnpc.promoretail.member.model.MemberPointsChange;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryMemberPointsChangeRepository implements MemberPointsChangeRepository {

    private final CopyOnWriteArrayList<MemberPointsChange> changes = new CopyOnWriteArrayList<>();

    @Override
    public MemberPointsChange save(MemberPointsChange change) {
        changes.add(change);
        return change;
    }

    @Override
    public List<MemberPointsChange> findByMemberCode(String memberCode, int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 200));
        return changes.stream()
                .filter(change -> change.memberCode().equals(memberCode))
                .sorted(Comparator.comparing(MemberPointsChange::occurredAt).reversed())
                .limit(effectiveLimit)
                .toList();
    }
}

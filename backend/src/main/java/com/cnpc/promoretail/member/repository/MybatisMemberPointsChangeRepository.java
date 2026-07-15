package com.cnpc.promoretail.member.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.member.model.MemberPointsChange;
import com.cnpc.promoretail.member.persistence.MemberPointsChangeEntity;
import com.cnpc.promoretail.member.persistence.MemberPointsChangeMapper;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisMemberPointsChangeRepository implements MemberPointsChangeRepository {

    private final MemberPointsChangeMapper mapper;

    public MybatisMemberPointsChangeRepository(MemberPointsChangeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public MemberPointsChange save(MemberPointsChange change) {
        mapper.insert(MemberPointsChangeEntity.from(change));
        return change;
    }

    @Override
    public List<MemberPointsChange> findByMemberCode(String memberCode, int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 200));
        return mapper.selectList(new LambdaQueryWrapper<MemberPointsChangeEntity>()
                        .eq(MemberPointsChangeEntity::getMemberCode, memberCode)
                        .orderByDesc(MemberPointsChangeEntity::getOccurredAt)
                        .last("limit " + effectiveLimit))
                .stream()
                .map(MemberPointsChangeEntity::toChange)
                .toList();
    }
}

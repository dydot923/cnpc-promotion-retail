package com.cnpc.promoretail.member.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.model.MemberLevel;
import com.cnpc.promoretail.member.persistence.MemberEntity;
import com.cnpc.promoretail.member.persistence.MemberLevelEntity;
import com.cnpc.promoretail.member.persistence.MemberLevelMapper;
import com.cnpc.promoretail.member.persistence.MemberMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"dev-db", "postgres"})
public class MybatisMemberRepository implements MemberRepository {

    private final MemberMapper memberMapper;
    private final MemberLevelMapper memberLevelMapper;

    public MybatisMemberRepository(MemberMapper memberMapper, MemberLevelMapper memberLevelMapper) {
        this.memberMapper = memberMapper;
        this.memberLevelMapper = memberLevelMapper;
    }

    @Override
    public Optional<Member> findByMemberCode(String memberCode) {
        return Optional.ofNullable(memberMapper.selectOne(new LambdaQueryWrapper<MemberEntity>()
                        .eq(MemberEntity::getMemberCode, memberCode)))
                .map(MemberEntity::toMember);
    }

    @Override
    public Optional<Member> findByPhone(String phone) {
        return Optional.ofNullable(memberMapper.selectOne(new LambdaQueryWrapper<MemberEntity>()
                        .eq(MemberEntity::getPhone, phone)))
                .map(MemberEntity::toMember);
    }

    @Override
    public List<Member> findAll() {
        return memberMapper.selectList(new LambdaQueryWrapper<MemberEntity>()
                        .orderByAsc(MemberEntity::getMemberCode))
                .stream()
                .map(MemberEntity::toMember)
                .toList();
    }

    @Override
    public Member save(Member member) {
        MemberEntity entity = toEntity(member);
        Instant now = Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDemoData(false);
        memberMapper.insert(entity);
        return member;
    }

    @Override
    public Member update(Member member) {
        MemberEntity existing = memberMapper.selectOne(new LambdaQueryWrapper<MemberEntity>()
                .eq(MemberEntity::getMemberCode, member.memberCode()));
        if (existing == null) {
            throw new IllegalArgumentException("Member not found: " + member.memberCode());
        }
        MemberEntity entity = toEntity(member);
        entity.setId(existing.getId());
        entity.setDemoData(existing.getDemoData());
        entity.setCreatedAt(existing.getCreatedAt());
        entity.setUpdatedAt(Instant.now());
        memberMapper.updateById(entity);
        return member;
    }

    @Override
    public Member adjustPoints(String memberCode, long change) {
        Member current = findByMemberCode(memberCode)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberCode));
        return update(current.withPoints(
                Math.max(0, current.totalPoints() + change),
                Math.max(0, current.availablePoints() + change)
        ));
    }

    @Override
    public Optional<MemberLevel> findLevelByCode(String levelCode) {
        return Optional.ofNullable(memberLevelMapper.selectOne(new LambdaQueryWrapper<MemberLevelEntity>()
                        .eq(MemberLevelEntity::getLevelCode, normalizeLevelCode(levelCode))))
                .map(MemberLevelEntity::toMemberLevel);
    }

    @Override
    public List<MemberLevel> findAllLevels() {
        return memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevelEntity>()
                        .orderByAsc(MemberLevelEntity::getPriority))
                .stream()
                .map(MemberLevelEntity::toMemberLevel)
                .toList();
    }

    private MemberEntity toEntity(Member member) {
        MemberEntity entity = new MemberEntity();
        entity.setMemberCode(member.memberCode());
        entity.setMemberName(member.memberName());
        entity.setPhone(member.phone().isBlank() ? null : member.phone());
        entity.setLevelCode(member.levelCode());
        entity.setTotalPoints(member.totalPoints());
        entity.setAvailablePoints(member.availablePoints());
        entity.setBirthday(member.birthday());
        entity.setProvince(member.province().isBlank() ? null : member.province());
        entity.setEEnjoyCardNo(member.eEnjoyCardNo().isBlank() ? null : member.eEnjoyCardNo());
        entity.setUsualProvince(member.usualProvince().isBlank() ? null : member.usualProvince());
        entity.setRegisteredAt(member.registeredAt());
        entity.setCardOpenedAt(member.cardOpenedAt());
        entity.setMemberTags(member.memberTags());
        entity.setStatus(member.status());
        return entity;
    }

    private String normalizeLevelCode(String value) {
        if (value == null || value.isBlank()) {
            return "normal";
        }
        String normalized = value.trim().toLowerCase();
        return "ordinary".equals(normalized) ? "normal" : normalized;
    }
}

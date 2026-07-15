package com.cnpc.promoretail.member.repository;

import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.model.MemberLevel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!dev-db & !postgres")
public class InMemoryMemberRepository implements MemberRepository {

    private final ConcurrentMap<String, Member> members = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MemberLevel> levels = new ConcurrentHashMap<>();

    public InMemoryMemberRepository() {
        seedLevels();
        seedMembers();
    }

    @Override
    public Optional<Member> findByMemberCode(String memberCode) {
        return Optional.ofNullable(members.get(normalize(memberCode)));
    }

    @Override
    public Optional<Member> findByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return Optional.empty();
        }
        return members.values().stream()
                .filter(member -> phone.equals(member.phone()))
                .findFirst();
    }

    @Override
    public List<Member> findAll() {
        return members.values().stream()
                .sorted(Comparator.comparing(Member::memberCode))
                .toList();
    }

    @Override
    public Member save(Member member) {
        members.put(normalize(member.memberCode()), member);
        return member;
    }

    @Override
    public Member update(Member member) {
        members.put(normalize(member.memberCode()), member);
        return member;
    }

    @Override
    public Member adjustPoints(String memberCode, long change) {
        String key = normalize(memberCode);
        Member updated = members.compute(key, (ignored, current) -> {
            if (current == null) {
                return null;
            }
            long total = Math.max(0, current.totalPoints() + change);
            long available = Math.max(0, current.availablePoints() + change);
            return current.withPoints(total, available);
        });
        if (updated == null) {
            throw new IllegalArgumentException("Member not found: " + memberCode);
        }
        return updated;
    }

    @Override
    public Optional<MemberLevel> findLevelByCode(String levelCode) {
        return Optional.ofNullable(levels.get(normalizeLevelCode(levelCode)));
    }

    @Override
    public List<MemberLevel> findAllLevels() {
        return levels.values().stream()
                .sorted(Comparator.comparingInt(MemberLevel::priority))
                .toList();
    }

    private void seedLevels() {
        saveLevel(new MemberLevel("normal", "普通会员", BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ZERO, List.of("基础会员价"), 1));
        saveLevel(new MemberLevel("silver", "银卡会员", new BigDecimal("0.9500"), new BigDecimal("1.5000"),
                new BigDecimal("1000.00"), List.of("生日券", "专属活动"), 2));
        saveLevel(new MemberLevel("gold", "金卡会员", new BigDecimal("0.9000"), new BigDecimal("2.0000"),
                new BigDecimal("5000.00"), List.of("生日券", "节日券", "专属客服"), 3));
        saveLevel(new MemberLevel("platinum", "铂金会员", new BigDecimal("0.8500"), new BigDecimal("3.0000"),
                new BigDecimal("20000.00"), List.of("专属权益包", "优先服务"), 4));
    }

    private void seedMembers() {
        List.of(
                new Member("member-001", "演示金卡会员", "13900000001", "gold", 5200, 1200,
                        LocalDate.of(1990, 7, 8), "新疆", "ACTIVE", List.of("gasoline_customer")),
                new Member("member-002", "演示银卡会员", "13900000002", "silver", 1800, 320,
                        LocalDate.of(1994, 7, 18), "新疆", "ACTIVE", List.of("diesel_customer")),
                new Member("demo-member-sequence", "演示序列券会员", "13900000016", "gold", 5000, 800,
                        LocalDate.of(1996, 7, 16), "新疆", "ACTIVE", List.of("gasoline_customer"))
        ).forEach(member -> members.put(normalize(member.memberCode()), member));
    }

    private void saveLevel(MemberLevel level) {
        levels.put(normalizeLevelCode(level.levelCode()), level);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String normalizeLevelCode(String value) {
        String normalized = normalize(value);
        return "ordinary".equals(normalized) ? "normal" : normalized;
    }
}

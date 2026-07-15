package com.cnpc.promoretail.member.repository;

import com.cnpc.promoretail.member.model.Member;
import com.cnpc.promoretail.member.model.MemberLevel;
import java.util.List;
import java.util.Optional;

public interface MemberRepository {

    Optional<Member> findByMemberCode(String memberCode);

    Optional<Member> findByPhone(String phone);

    List<Member> findAll();

    Member save(Member member);

    Member update(Member member);

    Member adjustPoints(String memberCode, long change);

    Optional<MemberLevel> findLevelByCode(String levelCode);

    List<MemberLevel> findAllLevels();
}

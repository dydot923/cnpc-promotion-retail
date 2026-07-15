package com.cnpc.promoretail.member.repository;

import com.cnpc.promoretail.member.model.MemberPointsChange;
import java.util.List;

public interface MemberPointsChangeRepository {

    MemberPointsChange save(MemberPointsChange change);

    List<MemberPointsChange> findByMemberCode(String memberCode, int limit);
}

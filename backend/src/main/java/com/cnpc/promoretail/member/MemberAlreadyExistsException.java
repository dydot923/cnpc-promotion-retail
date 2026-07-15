package com.cnpc.promoretail.member;

public class MemberAlreadyExistsException extends RuntimeException {

    public MemberAlreadyExistsException(String memberCode) {
        super("Member already exists: " + memberCode);
    }
}

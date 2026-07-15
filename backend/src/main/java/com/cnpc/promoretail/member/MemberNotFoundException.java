package com.cnpc.promoretail.member;

public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(String identifier) {
        super("会员不存在：" + identifier);
    }
}

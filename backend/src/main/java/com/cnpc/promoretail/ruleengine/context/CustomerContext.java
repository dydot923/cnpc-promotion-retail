package com.cnpc.promoretail.ruleengine.context;

import java.util.List;

public record CustomerContext(
        boolean member,
        String memberLevel,
        List<String> availableCouponIds,
        Integer memberBirthMonth,
        String paymentMethod,
        String memberCode,
        List<String> memberTags,
        Integer memberLevelPriority
) {

    public CustomerContext {
        availableCouponIds = availableCouponIds == null ? List.of() : List.copyOf(availableCouponIds);
        if (memberBirthMonth != null && (memberBirthMonth < 1 || memberBirthMonth > 12)) {
            throw new IllegalArgumentException("memberBirthMonth must be between 1 and 12");
        }
        paymentMethod = paymentMethod == null ? "" : paymentMethod;
        memberCode = memberCode == null ? "" : memberCode;
        memberTags = memberTags == null ? List.of() : List.copyOf(memberTags);
    }

    public CustomerContext(
            boolean member,
            String memberLevel,
            List<String> availableCouponIds,
            Integer memberBirthMonth,
            String paymentMethod,
            String memberCode
    ) {
        this(member, memberLevel, availableCouponIds, memberBirthMonth, paymentMethod, memberCode, List.of(), null);
    }

    public CustomerContext(
            boolean member,
            String memberLevel,
            List<String> availableCouponIds,
            Integer memberBirthMonth,
            String paymentMethod,
            String memberCode,
            List<String> memberTags
    ) {
        this(member, memberLevel, availableCouponIds, memberBirthMonth, paymentMethod, memberCode, memberTags, null);
    }

    public CustomerContext(
            boolean member,
            String memberLevel,
            List<String> availableCouponIds,
            Integer memberBirthMonth,
            String paymentMethod
    ) {
        this(member, memberLevel, availableCouponIds, memberBirthMonth, paymentMethod, "");
    }

    public CustomerContext(boolean member, String memberLevel, List<String> availableCouponIds, Integer memberBirthMonth) {
        this(member, memberLevel, availableCouponIds, memberBirthMonth, "");
    }

    public CustomerContext(boolean member, String memberLevel, List<String> availableCouponIds) {
        this(member, memberLevel, availableCouponIds, null, "");
    }

    public static CustomerContext anonymous() {
        return new CustomerContext(false, null, List.of(), null, "", "");
    }

    public boolean eEnjoyCardPayment() {
        return "E_ENJOY_CARD".equalsIgnoreCase(paymentMethod)
                || "E享卡".equalsIgnoreCase(paymentMethod)
                || "e享卡".equalsIgnoreCase(paymentMethod);
    }
}

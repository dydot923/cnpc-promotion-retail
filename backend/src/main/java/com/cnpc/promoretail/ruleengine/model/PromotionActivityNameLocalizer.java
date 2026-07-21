package com.cnpc.promoretail.ruleengine.model;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PromotionActivityNameLocalizer {

    private static final Pattern G7_SAFE_PRICE = Pattern.compile(
            "^G7\\s+safe-price\\s+single\\s+item\\s+promotion-(.+)$",
            Pattern.CASE_INSENSITIVE
    );

    private PromotionActivityNameLocalizer() {
    }

    public static String localize(String activityName) {
        if (activityName == null || activityName.isBlank()) {
            return activityName;
        }
        String name = activityName.trim();
        Matcher g7Matcher = G7_SAFE_PRICE.matcher(name);
        if (g7Matcher.matches()) {
            return "非非促销-单品安全价-" + g7Matcher.group(1);
        }

        String localized = switch (name.toLowerCase(Locale.ROOT)) {
            case "a5 day10 super recharge 1000 normal" -> "超级十惠-普通客户单笔充值1000元";
            case "a5 day10 super recharge 1000 gold+" -> "超级十惠-黄金及以上客户单笔充值1000元";
            case "a5 day10 super recharge 2000 normal" -> "超级十惠-普通客户单笔充值2000元";
            case "a5 day10 super recharge 2000 gold+" -> "超级十惠-黄金及以上客户单笔充值2000元";
            case "a6 small recharge 666 coupon package" -> "非十惠日小额充值666元赠券包";
            default -> name;
        };
        return localized
                .replaceAll("(?i)-GASOLINE$", "-汽油")
                .replaceAll("(?i)-DIESEL$", "-柴油");
    }
}

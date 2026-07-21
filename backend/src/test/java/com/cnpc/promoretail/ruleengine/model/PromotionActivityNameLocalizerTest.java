package com.cnpc.promoretail.ruleengine.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PromotionActivityNameLocalizerTest {

    @Test
    void localizesGeneratedPromotionNamesAndFuelSuffixes() {
        assertThat(PromotionActivityNameLocalizer.localize(
                "G7 safe-price single item promotion-70001573"))
                .isEqualTo("非非促销-单品安全价-70001573");
        assertThat(PromotionActivityNameLocalizer.localize(
                "A5 Day10 Super Recharge 1000 Gold+"))
                .isEqualTo("超级十惠-黄金及以上客户单笔充值1000元");
        assertThat(PromotionActivityNameLocalizer.localize(
                "A6 small recharge 666 coupon package"))
                .isEqualTo("非十惠日小额充值666元赠券包");
        assertThat(PromotionActivityNameLocalizer.localize(
                "加油换购-大水4瓶-GASOLINE"))
                .isEqualTo("加油换购-大水4瓶-汽油");
        assertThat(PromotionActivityNameLocalizer.localize(
                "加油换购-大水4瓶-DIESEL"))
                .isEqualTo("加油换购-大水4瓶-柴油");
    }

    @Test
    void preservesChineseNamesAndProductSpecifications() {
        assertThat(PromotionActivityNameLocalizer.localize(
                "9.9元专区-可口可乐 汽水 500ML"))
                .isEqualTo("9.9元专区-可口可乐 汽水 500ML");
    }
}

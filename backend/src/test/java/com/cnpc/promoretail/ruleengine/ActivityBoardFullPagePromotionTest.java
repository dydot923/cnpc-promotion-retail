package com.cnpc.promoretail.ruleengine;

import static org.assertj.core.api.Assertions.assertThat;

import com.cnpc.promoretail.ruleengine.context.CustomerContext;
import com.cnpc.promoretail.ruleengine.context.FuelContext;
import com.cnpc.promoretail.ruleengine.context.FuelType;
import com.cnpc.promoretail.ruleengine.context.StationContext;
import com.cnpc.promoretail.ruleengine.model.CalculationResult;
import com.cnpc.promoretail.ruleengine.model.GiftCoupon;
import com.cnpc.promoretail.ruleengine.model.PromotionCandidate;
import com.cnpc.promoretail.support.PostgresIntegrationTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ActivityBoardFullPagePromotionTest extends PostgresIntegrationTestSupport {

    private static final StationContext STATION = new StationContext("station-001", "gas_station", "\u65b0\u7586");
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 23);
    private static final LocalTime BUSINESS_TIME = LocalTime.of(10, 0);

    @Test
    void monthlyDay7CouponRemainsGasOnly() {
        CalculationResult gas = calculate(order(
                STATION,
                new CustomerContext(true, "GOLD", List.of()),
                new FuelContext(FuelType.CNG, null, new BigDecimal("500"), BigDecimal.ZERO),
                List.of(),
                LocalDate.of(2026, 7, 7),
                BUSINESS_TIME,
                List.of()));
        PromotionCandidate gasCandidate = candidate(gas, "abv2-a1-day7-gas-coupon");
        assertThat(gasCandidate.coupons()).extracting(GiftCoupon::amount)
                .containsExactly(new BigDecimal("10.00"), new BigDecimal("6.00"));
        assertThat(gasCandidate.coupons()).extracting(GiftCoupon::quantity).containsExactly(1, 2);

        CalculationResult shopOnly = calculate(order(
                STATION,
                new CustomerContext(true, "GOLD", List.of()),
                FuelContext.empty(),
                List.of(syntheticItem("shop-item", "便利店商品", 1, "500.00", "包装饮料")),
                LocalDate.of(2026, 7, 7),
                BUSINESS_TIME,
                List.of()));
        assertThat(shopOnly.availableCandidates())
                .noneMatch(item -> item.ruleId().equals("abv2-a1-day7-gas-coupon"));
    }

    @Test
    void waterMilkAndFertilizerRulesUseBoardPrices() {
        assertPayable("abv2-nono-water-gesang-330-case", "70545523", 12, "2.50", "24.90");
        assertPayable("abv2-nono-water-gesang-500-case", "70545526", 12, "3.00", "27.90");
        assertPayable("abv2-nono-water-wuyishan-333-case", "70251989", 12, "3.00", "31.90");
        assertPayable("abv2-nono-water-wuyishan-513-case", "70251198", 12, "3.50", "39.90");
        assertPayable("abv2-nono-water-wuyishan-45l-case", "70254265", 2, "25.00", "50.00");
        assertPayable("abv2-nono-milk-organic-250-case", "70559365", 10, "6.00", "50.00");
        assertPayable("abv2-nono-milk-dream-250-case", "70559370", 10, "7.50", "60.00");
        assertPayable("abv2-nono-milk-beer-300-case", "70224290", 12, "5.00", "55.00");
        assertPayable("abv2-nono-fertilizer-two-bags-95", "70440943", 2, "152.50", "289.75");
    }

    @Test
    void cottonTissueEnergyAndBeerRulesUseBoardPrices() {
        assertPayable("abv2-nono-cotton-film-9-95", "demo-cotton-film", 9, "2250.00", "19237.50");
        assertPayable("abv2-nono-cotton-film-27-93", "demo-cotton-film", 27, "2250.00", "56497.50");
        assertPayable("abv2-nono-tissue-3pack-bogo", "70341453", 2, "15.00", "15.00");
        assertPayable("abv2-nono-energy-dongpeng-250-pack3", "70235652", 3, "4.00", "9.00");
        assertPayable("abv2-nono-energy-mix-pack2", "70356177", 2, "6.00", "9.00");
        assertPayable("abv2-nono-nongfu-pack2", "70166516", 2, "5.00", "8.00");
        assertPayable("abv2-nono-beer-ipa98-case", "70531507", 2, "49.90", "68.00");
        assertPayable("abv2-nono-beer-ale92-case", "70531511", 6, "7.00", "26.00");
        assertPayable("abv2-nono-beer-craft95-case", "70531509", 6, "20.00", "68.00");
        assertPayable("abv2-nono-beer-superx-500-pack3", "70410728", 3, "6.90", "15.00");
        assertPayable("abv2-nono-beer-heineken-500-pack3", "70199632", 3, "10.00", "21.00");
        assertPayable("abv2-nono-beer-snow-330-pack3", "70186321", 3, "4.90", "9.00");
    }

    @Test
    void lubeRulesUseCaseAndBuyOneGetOnePrices() {
        assertPayable("abv2-nono-lube-washer-0c-pack2", "70536790", 2, "8.00", "12.00");
        assertPayable("abv2-nono-lube-washer-0c-case9", "70536790", 9, "8.00", "35.00");
        assertPayable("abv2-nono-lube-washer-minus40-bogo", "70536789", 2, "12.00", "12.00");
        assertPayable("abv2-nono-lube-gas-additive-case6", "70192479", 6, "49.90", "150.00");
        assertPayable("abv2-nono-lube-aus32-10kg-pack2", "70536791", 2, "50.00", "60.00");
        assertPayable("abv2-nono-lube-aus32-20kg-pack2", "70536792", 2, "75.00", "100.00");
    }

    private void assertPayable(
            String ruleId,
            String productCode,
            int quantity,
            String unitPrice,
            String expectedPayable
    ) {
        CalculationResult result = calculate(order(
                STATION,
                CustomerContext.anonymous(),
                FuelContext.empty(),
                List.of(syntheticItem(productCode, productCode, quantity, unitPrice, "活动看板商品")),
                BUSINESS_DATE,
                BUSINESS_TIME,
                List.of()));
        assertThat(candidate(result, ruleId).payableAmount()).isEqualByComparingTo(expectedPayable);
    }
}

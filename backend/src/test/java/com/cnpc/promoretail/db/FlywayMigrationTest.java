package com.cnpc.promoretail.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void devDbFlywayMigrationsCreateRuleGovernanceTables() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     select table_name
                     from information_schema.tables
                     where table_schema = 'public'
                       and table_name in (
                         'import_batch',
                         'import_error_row',
                         'promotion_rule_draft',
                         'promotion_rule_version',
                         'promotion_rule_audit_log',
                         'checkout_calculation_record',
                         'coupon_template',
                         'coupon',
                         'member',
                         'member_level',
                         'member_points_change',
                         'checkout_transaction',
                         'checkout_transaction_item',
                         'inventory_alert_record',
                         'station',
                         'promotion_date_trigger',
                         'points_activity',
                         'points_lottery_draw',
                         'points_lottery_prize_config',
                         'promotion_excluded_category',
                         'promotion_station_scope',
                         'benefit_package',
                         'benefit_package_item',
                         'benefit_package_purchase',
                         'bundle',
                         'bundle_item',
                         'product_group',
                         'product_group_item'
                       )
                     """)) {
            assertThat(tableNames(resultSet)).containsExactlyInAnyOrder(
                    "import_batch",
                    "import_error_row",
                    "promotion_rule_draft",
                    "promotion_rule_version",
                    "promotion_rule_audit_log",
                    "checkout_calculation_record",
                    "coupon_template",
                    "coupon",
                    "member",
                    "member_level",
                    "member_points_change",
                    "checkout_transaction",
                    "checkout_transaction_item",
                    "inventory_alert_record",
                    "station",
                    "promotion_date_trigger",
                    "points_activity",
                    "points_lottery_draw",
                    "points_lottery_prize_config",
                    "promotion_excluded_category",
                    "promotion_station_scope",
                    "benefit_package",
                    "benefit_package_item",
                    "benefit_package_purchase",
                    "bundle",
                    "bundle_item",
                    "product_group",
                    "product_group_item"
            );
        }

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            assertThat(count(statement, "select count(*) from bundle")).isGreaterThanOrEqualTo(2);
            assertThat(count(statement, "select count(*) from bundle_item")).isGreaterThanOrEqualTo(5);
            assertThat(count(statement, "select count(*) from coupon_template where coupon_template_id like 'demo-%'"))
                    .isGreaterThanOrEqualTo(3);
            assertThat(count(statement, "select count(*) from coupon where coupon_id like 'demo-coupon-%'"))
                    .isGreaterThanOrEqualTo(6);
            assertThat(count(statement, "select count(*) from promotion_rule_draft where source_import_id = 'demo-seed-v1'"))
                    .isGreaterThanOrEqualTo(5);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where rule_id in (
                        'abv2-a4-cn98-volume-discount',
                        'abv2-g1-day7-gas-filling-discount',
                        'abv2-g2-day9-gas-station-discount',
                        'abv2-a3-gas-filling-discount',
                        'abv2-g4-event-beer-coupon',
                        'abv2-g4-event-night-discount'
                    )
                      and status = 'CONFIRMED'
                      and rule_json ->> 'version' = 'activity-board-v2'
                    """)).isEqualTo(6);
            assertThat(count(statement, """
                    select count(*)
                    from coupon_template
                    where ((coupon_template_id = 'activation-gasoline-12'
                            and face_value = 12.00 and min_spend_amount = 230.00)
                       or (coupon_template_id = 'activation-diesel-20'
                            and face_value = 20.00 and min_spend_amount = 400.00))
                      and issue_quantity = 3
                      and per_customer_limit = 3
                    """)).isEqualTo(2);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where rule_id = 'abv2-e2-wing-card-399-coupon'
                      and status = 'CONFIRMED'
                      and (condition_json ->> 'minCartAmount')::numeric = 399.00
                      and (benefit_json ->> 'giftCouponQuantity')::int = 2
                    """)).isEqualTo(1);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where rule_id in (
                        'abv2-e2-ilite-500-jia-case-coupon',
                        'abv2-e2-ilite-500-li-case-coupon'
                    )
                      and (condition_json ->> 'minProductQuantity')::int = 6
                    """)).isEqualTo(2);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2-g7-resolution'
                      and status = 'CONFIRMED'
                    """)).isEqualTo(81);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2-g7-resolution'
                      and status = 'DRAFT'
                      and (benefit_json ->> 'fixedPrice')::numeric = 0
                    """)).isEqualTo(0);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2-p0-status-confirmation'
                      and status = 'CONFIRMED'
                    """)).isEqualTo(6);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2-e2-case-coupon-completion'
                      and status = 'CONFIRMED'
                    """)).isEqualTo(2);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2-a5-recharge-coupon'
                      and status = 'CONFIRMED'
                      and rule_type = 'GIFT_COUPON'
                      and (condition_json ->> 'minRechargeAmount')::numeric in (1000.00, 2000.00)
                    """)).isEqualTo(4);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2-small-recharge-666'
                      and rule_id = 'abv2-a6-small-recharge-666'
                      and status = 'CONFIRMED'
                      and condition_json -> 'dateCondition' ->> 'type' = 'EXCLUDE_MONTHLY_DATES'
                    """)).isEqualTo(1);
            assertThat(count(statement, """
                    select count(*)
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'coupon'
                      and column_name = 'discount_rate'
                    """)).isEqualTo(1);
            assertThat(count(statement, """
                    select count(*)
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'coupon_template'
                      and column_name = 'discount_rate'
                    """)).isEqualTo(1);
            assertThat(count(statement, """
                    select count(*)
                    from information_schema.columns
                    where table_schema = 'public'
                      and column_name = 'is_demo_data'
                      and table_name in (
                        'product',
                        'product_price',
                        'inventory_snapshot',
                        'coupon_template',
                        'coupon',
                        'bundle',
                        'bundle_item',
                        'product_group',
                        'product_group_item',
                        'promotion_rule_draft',
                        'promotion_rule_version',
                        'promotion_rule_audit_log'
                      )
                    """)).isEqualTo(12);
            assertThat(count(statement, "select count(*) from product_group where source = 'ACTIVITY_BOARD_V2'"))
                    .isEqualTo(19);
            assertThat(count(statement, """
                    select count(*)
                    from product_group_item item
                    join product_group grp on grp.id = item.group_id
                    where grp.source = 'ACTIVITY_BOARD_V2'
                    """)).isEqualTo(25);
            assertThat(count(statement, "select count(*) from bundle where activity_id = 'activity-board-v2'"))
                    .isEqualTo(3);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2'
                    """)).isEqualTo(217);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where rule_id = 'abv2-g5-mid-autumn-composite'
                      and status = 'CONFIRMED'
                      and rule_type = 'COMPOSITE'
                      and jsonb_array_length(benefit_json -> 'compositeComponents') = 2
                    """)).isEqualTo(1);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2'
                      and source_sheet_name = '参考2-9.9元商品专区'
                      and rule_type = 'FIXED_PRICE'
                    """)).isEqualTo(190);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2'
                      and source_sheet_name = '参考2-9.9元商品专区'
                      and rule_type = 'FIXED_PRICE'
                      and rule_json ->> 'exclusiveGroup' = 'G3_99_ZONE'
                      and (rule_json ->> 'priority')::int = 10
                    """)).isEqualTo(190);
            assertThat(count(statement, """
                    select count(*)
                    from import_error_row
                    where import_version = 'activity-board-v2-99-zone-full-import'
                      and error_code = 'MISSING_PRODUCT_CODE'
                    """)).isZero();
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where rule_id like 'abv2-99-zone-%'
                      and status = 'CONFIRMED'
                    """)).isEqualTo(194);
            assertThat(count(statement, """
                    select count(*)
                    from coupon_template
                    where coupon_template_id in ('province-feature-half-2026', 'province-feature-70-2026')
                    """)).isEqualTo(2);
            assertThat(count(statement, """
                    select count(*)
                    from coupon_template
                    where coupon_template_id in (
                        'a5-day10-gasoline-12',
                        'a5-day10-store-12',
                        'a5-day10-carwash-10',
                        'a5-day10-highgrade-gasoline-15'
                    )
                      and source_activity = 'activity-board-v2-a5'
                    """)).isEqualTo(4);
            assertThat(count(statement, """
                    select count(*)
                    from coupon_template
                    where coupon_template_id in (
                        'new-member-gasoline-10',
                        'new-member-highgrade-gasoline-15',
                        'new-member-store-12',
                        'new-member-carwash-10',
                        'activation-gasoline-10',
                        'activation-diesel-10'
                    )
                      and source_activity in (
                        'activity-board-v2-new-member',
                        'activity-board-v2-potential-member'
                      )
                      and per_customer_limit = 1
                    """)).isEqualTo(6);
            assertThat(count(statement, """
                    select count(*)
                    from coupon_template
                    where source_activity in (
                        'activity-board-v2-small-recharge-666',
                        'activity-board-v2-rfm-recovery',
                        'activity-board-v2-birthday',
                        'activity-board-v2-sign-in',
                        'activity-board-v2-group-buy',
                        'activity-board-v2-industry-certification',
                        'activity-board-v2-ecommerce'
                    )
                      and is_demo_data = false
                    """)).isEqualTo(26);
            assertThat(count(statement, """
                    select jsonb_array_length(applicable_product_codes)
                    from coupon_template
                    where coupon_template_id = 'province-feature-half-2026'
                    """)).isEqualTo(171);
            assertThat(count(statement, """
                    select count(*)
                    from coupon
                    where coupon_id in ('demo-province-half-001', 'demo-province-70-001')
                    """)).isEqualTo(2);
            assertThat(count(statement, """
                    select count(*)
                    from coupon
                    where coupon_template_id in ('province-feature-half-2026', 'province-feature-70-2026')
                      and coupon_id like 'demo-province-%'
                      and is_demo_data = true
                    """)).isGreaterThanOrEqualTo(10);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where rule_id = 'abv2-g6-cotton-film-9-gift-pack'
                      and benefit_json ? 'giftItemOptions'
                    """)).isEqualTo(1);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2'
                      and rule_id in (
                        'abv2-bundle-abv2-driving-package',
                        'abv2-bundle-abv2-water-drink-package',
                        'abv2-bundle-abv2-long-haul-package'
                      )
                      and rule_json ->> 'exclusiveGroup' = 'H1_EXCHANGE'
                      and (rule_json ->> 'priority')::int = 20
                    """)).isEqualTo(3);
            assertThat(count(statement, """
                    select count(*)
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'coupon'
                      and column_name in ('sequence_group', 'sequence_order')
                    """)).isEqualTo(2);
            assertThat(count(statement, """
                    select count(*)
                    from coupon
                    where sequence_group = 'wechat-shake-2026'
                      and is_demo_data = true
                    """)).isEqualTo(3);
            assertThat(count(statement, """
                    select count(*)
                    from coupon
                    where sequence_group = 'wechat-shake-2026'
                      and holder_member_id = 'demo-member-sequence'
                      and operator_id = 'flyway-demo'
                    """)).isEqualTo(3);
            assertThat(count(statement, """
                    select count(*)
                    from product
                    where product_code like 'demo-%'
                      and is_demo_data = true
                    """)).isEqualTo(1);
            assertThat(count(statement, """
                    select count(*)
                    from product_price
                    where import_version = 'activity-board-v2-demo-price'
                      and is_demo_data = true
                    """)).isEqualTo(30);
            assertThat(count(statement, """
                    select count(*)
                    from inventory_snapshot
                    where import_version = 'activity-board-v2-demo-inventory'
                      and is_demo_data = true
                    """)).isEqualTo(30);
            assertThat(count(statement, """
                    select count(*)
                    from coupon_template
                    where coupon_template_id like 'demo-%'
                      and is_demo_data = true
                    """)).isGreaterThanOrEqualTo(3);
            assertThat(count(statement, """
                    select count(*)
                    from coupon
                    where coupon_id like 'demo-%'
                      and is_demo_data = true
                    """)).isGreaterThanOrEqualTo(8);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'demo-seed-v1'
                      and is_demo_data = true
                    """)).isGreaterThanOrEqualTo(5);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_rule_draft
                    where source_import_id = 'activity-board-v2'
                      and is_demo_data = false
                    """)).isEqualTo(217);
            assertThat(count(statement, """
                    select count(*)
                    from bundle
                    where activity_id like 'demo-%'
                      and is_demo_data = true
                    """)).isGreaterThanOrEqualTo(2);
            assertThat(count(statement, """
                    select count(*)
                    from bundle
                    where activity_id = 'activity-board-v2'
                      and is_demo_data = false
                    """)).isEqualTo(3);
            assertThat(count(statement, "select count(*) from station where source_sheet_name = '参考4-“一卡通”销售站点明细'"))
                    .isGreaterThanOrEqualTo(290);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_date_trigger
                    where activity_code like 'activity-board-v2-%'
                    """)).isEqualTo(13);
            assertThat(count(statement, """
                    select count(*)
                    from points_activity
                    where activity_id in ('points-abv2-a1-day7-cng-lng', 'points-abv2-g2-day9-store')
                      and points_multiplier = 3.0000
                      and status = 'ACTIVE'
                    """)).isEqualTo(2);
            assertThat(count(statement, """
                    select count(*)
                    from coupon_template
                    where coupon_template_id in (
                        'points-exchange-90-off',
                        'points-lottery-store-10',
                        'xinjiang-tour-card-gasoline-100',
                        'lng-benefit-15',
                        'cng-benefit-3'
                    )
                      and is_demo_data = false
                    """)).isEqualTo(5);
            assertThat(count(statement, """
                    select count(*)
                    from points_lottery_draw
                    """)).isEqualTo(0);
            assertThat(count(statement, """
                    select count(*)
                    from points_lottery_prize_config
                    where activity_code = 'activity-board-v2-g2-points-lottery'
                      and prize_id in ('g2-lottery-no-prize', 'g2-lottery-store-10')
                      and status = 'ACTIVE'
                    """)).isEqualTo(2);
            assertThat(count(statement, """
                    select count(*)
                    from promotion_excluded_category
                    where category_name in ('香烟', '化肥')
                    """)).isEqualTo(8);
            assertThat(count(statement, "select count(*) from promotion_station_scope")).isEqualTo(4);
            assertThat(count(statement, "select count(*) from benefit_package")).isEqualTo(15);
            assertThat(count(statement, "select count(*) from benefit_package_item")).isEqualTo(142);
            assertThat(count(statement, """
                    select count(*)
                    from benefit_package_item
                    where package_code = 'benefit-package-xinjiang-tour-card-2026'
                      and item_name like '%100元汽油券%'
                      and quantity = 2
                    """)).isEqualTo(1);
        }
    }

    private java.util.List<String> tableNames(ResultSet resultSet) throws Exception {
        java.util.List<String> names = new java.util.ArrayList<>();
        while (resultSet.next()) {
            names.add(resultSet.getString("table_name"));
        }
        return names;
    }

    private long count(Statement statement, String sql) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}

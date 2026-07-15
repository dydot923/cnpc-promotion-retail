insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-p0-status-confirmation', 'PROMOTION', 'data/活动看板.xlsx', 6, 0, 0, 0, 2
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with rules(rule_id, activity_name, rule_type, priority, exclusive_group, stackable, source_row,
           condition_json, benefit_json) as (
    values
    (
        'abv2-a1-day7-gas-coupon', 'A1逢7气惠多档位赠券', 'GIFT_COUPON', 30,
        'coupon_gift', true, 4,
        jsonb_build_object(
            'fuelTypes', jsonb_build_array('CNG', 'LNG'),
            'memberRequired', true,
            'dateCondition', jsonb_build_object('type', 'MONTHLY_DATES', 'dates', jsonb_build_array(7, 17, 27)),
            'stationProvinces', jsonb_build_array('新疆')
        ),
        jsonb_build_object(
            'type', 'GIFT_COUPON',
            'giftCouponTiers', jsonb_build_array(
                jsonb_build_object('thresholdAmount', 500.00, 'coupons', jsonb_build_array(
                    jsonb_build_object('couponName', '10元LNG券', 'amount', 10.00, 'quantity', 1,
                        'useThreshold', 500.00, 'validDays', 15),
                    jsonb_build_object('couponName', '6元便利店商品券', 'amount', 6.00, 'quantity', 2,
                        'useThreshold', 30.00, 'validDays', 60))),
                jsonb_build_object('thresholdAmount', 1000.00, 'coupons', jsonb_build_array(
                    jsonb_build_object('couponName', '30元LNG券', 'amount', 30.00, 'quantity', 1,
                        'useThreshold', 1000.00, 'validDays', 15),
                    jsonb_build_object('couponName', '12元便利店商品券', 'amount', 12.00, 'quantity', 1,
                        'useThreshold', 50.00, 'validDays', 60))),
                jsonb_build_object('thresholdAmount', 1500.00, 'coupons', jsonb_build_array(
                    jsonb_build_object('couponName', '60元LNG券', 'amount', 60.00, 'quantity', 1,
                        'useThreshold', 1500.00, 'validDays', 15),
                    jsonb_build_object('couponName', '12元便利店商品券', 'amount', 12.00, 'quantity', 2,
                        'useThreshold', 50.00, 'validDays', 60))),
                jsonb_build_object('thresholdAmount', 2000.00, 'coupons', jsonb_build_array(
                    jsonb_build_object('couponName', '100元LNG券', 'amount', 100.00, 'quantity', 1,
                        'useThreshold', 2000.00, 'validDays', 15),
                    jsonb_build_object('couponName', '12元便利店商品券', 'amount', 12.00, 'quantity', 3,
                        'useThreshold', 50.00, 'validDays', 60)))
            )
        )
    ),
    (
        'abv2-e1-gasoline-gift-coupons', 'E1汽油满230赠香烟券和便利店券', 'GIFT_COUPON', 35,
        'coupon_gift', true, 25,
        jsonb_build_object('fuelTypes', jsonb_build_array('GASOLINE'), 'minFuelAmount', 230.00,
            'memberRequired', true),
        jsonb_build_object('type', 'GIFT_COUPON', 'giftCouponTiers', jsonb_build_array(
            jsonb_build_object('thresholdAmount', 230.00, 'coupons', jsonb_build_array(
                jsonb_build_object('couponName', '15元香烟券', 'amount', 15.00, 'quantity', 1,
                    'useThreshold', 300.00, 'validDays', 60),
                jsonb_build_object('couponName', '6元便利店商品券', 'amount', 6.00, 'quantity', 1,
                    'useThreshold', 30.00, 'validDays', 60)))))
    ),
    (
        'abv2-e1-diesel-gift-coupons', 'E1柴油满280赠香烟券和便利店券', 'GIFT_COUPON', 35,
        'coupon_gift', true, 25,
        jsonb_build_object('fuelTypes', jsonb_build_array('DIESEL'), 'minFuelAmount', 280.00,
            'memberRequired', true),
        jsonb_build_object('type', 'GIFT_COUPON', 'giftCouponTiers', jsonb_build_array(
            jsonb_build_object('thresholdAmount', 280.00, 'coupons', jsonb_build_array(
                jsonb_build_object('couponName', '15元香烟券', 'amount', 15.00, 'quantity', 1,
                    'useThreshold', 300.00, 'validDays', 60),
                jsonb_build_object('couponName', '6元便利店商品券', 'amount', 6.00, 'quantity', 1,
                    'useThreshold', 30.00, 'validDays', 60)))))
    ),
    (
        'abv2-e2-ilite-250-case-coupon', 'E2伊力特250ml整件赠汽油券', 'GIFT_COUPON', 35,
        'coupon_gift', true, 26,
        jsonb_build_object('productCodes', jsonb_build_array('70690981'), 'minCartAmount', 116.00,
            'memberRequired', true, 'minProductQuantity', 10),
        jsonb_build_object('type', 'GIFT_COUPON', 'giftCouponName', '100元汽油券',
            'giftCouponAmount', 100.00, 'giftCouponQuantity', 2,
            'giftCouponUseThreshold', 201.00, 'giftCouponValidDays', 60)
    ),
    (
        'abv2-f1-cng-gift-water', 'F1 CNG满50赠2瓶矿泉水', 'GIFT_ITEM', 35,
        'gift', true, 27,
        jsonb_build_object('fuelTypes', jsonb_build_array('CNG'), 'minFuelAmount', 50.00),
        jsonb_build_object('type', 'GIFT_ITEM', 'giftItemCode', '70545526',
            'giftItemName', '格桑泉500ml矿泉水', 'giftItemQuantity', 2)
    ),
    (
        'abv2-f1-lng-gift-water', 'F1 LNG满1000赠4瓶矿泉水', 'GIFT_ITEM', 35,
        'gift', true, 27,
        jsonb_build_object('fuelTypes', jsonb_build_array('LNG'), 'minFuelAmount', 1000.00),
        jsonb_build_object('type', 'GIFT_ITEM', 'giftItemCode', '70545526',
            'giftItemName', '格桑泉500ml矿泉水', 'giftItemQuantity', 4)
    )
), prepared as (
    select
        rule_id,
        rule_type,
        source_row,
        jsonb_build_object(
            'ruleId', rule_id,
            'activityName', activity_name,
            'ruleType', rule_type,
            'priority', priority,
            'exclusiveGroup', exclusive_group,
            'stackable', stackable,
            'status', 'CONFIRMED',
            'condition', condition_json,
            'benefit', benefit_json,
            'version', 'activity-board-v2'
        ) as rule_json
    from rules
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select
    'draft-' || rule_id, rule_id, 'activity-board-v2-p0-status-confirmation',
    '一体化营销活动看版', source_row, rule_type, 'CONFIRMED',
    rule_json -> 'condition', rule_json -> 'benefit', rule_json,
    false, 'flyway-activity-board-v2', now(), false
from prepared
on conflict (rule_id) do update
set status = excluded.status,
    condition_json = excluded.condition_json,
    benefit_json = excluded.benefit_json,
    rule_json = excluded.rule_json,
    updated_at = now()
where not promotion_rule_draft.manual_locked;

insert into promotion_rule_version (
    version_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, rule_json, created_by, confirmed_at, confirmed_by,
    change_reason, is_demo_data
)
select
    'ver-' || rule_id || '-v22', rule_id, source_import_id, source_sheet_name,
    source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2',
    now(), 'flyway-activity-board-v2', 'P0 activity status confirmed by V22', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-p0-status-confirmation'
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id,
    change_reason, is_demo_data
)
select
    'audit-' || rule_id || '-v22', rule_id, 'CONFIRMED', null, 'CONFIRMED',
    'flyway-activity-board-v2', 'P0 activity status confirmed by V22', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-p0-status-confirmation'
on conflict (audit_id) do nothing;

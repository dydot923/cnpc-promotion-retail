insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-g4-event', 'PROMOTION', 'data/活动看板.xlsx', 2, 0, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with prepared(rule_id, rule_type, rule_json) as (
    values
    (
        'abv2-g4-event-beer-coupon',
        'GIFT_COUPON',
        jsonb_build_object(
            'ruleId', 'abv2-g4-event-beer-coupon',
            'activityName', 'G4-1赛事期间啤酒满66赠20元汽油券',
            'ruleType', 'GIFT_COUPON',
            'priority', 30,
            'exclusiveGroup', null,
            'stackable', true,
            'status', 'CONFIRMED',
            'condition', jsonb_build_object(
                'productCodes', jsonb_build_array(),
                'excludedCategories', jsonb_build_array(),
                'fuelTypes', jsonb_build_array(),
                'stationTypes', jsonb_build_array(),
                'daysOfMonth', jsonb_build_array(),
                'startDate', '2026-06-12',
                'endDate', '2026-08-09',
                'minCartAmount', 66.00,
                'minFuelAmount', 0,
                'memberRequired', true,
                'minInventoryQuantity', 0,
                'dateCondition', jsonb_build_object(
                    'type', 'DATE_RANGE',
                    'dates', jsonb_build_array(),
                    'fromDate', '2026-06-12',
                    'toDate', '2026-08-09'
                ),
                'timeRangeCondition', null,
                'stationProvinces', jsonb_build_array(),
                'memberLevels', jsonb_build_array(),
                'birthdayMonthRequired', false,
                'minFuelVolume', 0,
                'includedCategories', jsonb_build_array('啤酒'),
                'minProductQuantity', 0
            ),
            'benefit', jsonb_build_object(
                'type', 'GIFT_COUPON',
                'giftCouponName', '20元高标号汽油券',
                'giftCouponAmount', 20.00,
                'giftCouponQuantity', 1,
                'giftCouponUseThreshold', 200.00,
                'giftCouponValidDays', 60
            ),
            'version', 'activity-board-v2'
        )
    ),
    (
        'abv2-g4-event-night-discount',
        'PERCENTAGE_DISCOUNT',
        jsonb_build_object(
            'ruleId', 'abv2-g4-event-night-discount',
            'activityName', 'G4-2赛事夜间指定品类8.8折',
            'ruleType', 'PERCENTAGE_DISCOUNT',
            'priority', 40,
            'exclusiveGroup', 'storewide-discount',
            'stackable', false,
            'status', 'CONFIRMED',
            'condition', jsonb_build_object(
                'productCodes', jsonb_build_array(),
                'excludedCategories', jsonb_build_array(),
                'fuelTypes', jsonb_build_array(),
                'stationTypes', jsonb_build_array(),
                'daysOfMonth', jsonb_build_array(),
                'startDate', '2026-06-12',
                'endDate', '2026-08-09',
                'minCartAmount', 0,
                'minFuelAmount', 0,
                'memberRequired', false,
                'minInventoryQuantity', 0,
                'dateCondition', jsonb_build_object(
                    'type', 'DATE_RANGE',
                    'dates', jsonb_build_array(),
                    'fromDate', '2026-06-12',
                    'toDate', '2026-08-09'
                ),
                'timeRangeCondition', jsonb_build_object(
                    'from', '18:00:00',
                    'to', '02:00:00'
                ),
                'stationProvinces', jsonb_build_array(),
                'memberLevels', jsonb_build_array(),
                'birthdayMonthRequired', false,
                'minFuelVolume', 0,
                'includedCategories', jsonb_build_array('咖啡', '啤酒', '瓜子', '雪糕', '膨化', '肉脯'),
                'minProductQuantity', 0
            ),
            'benefit', jsonb_build_object(
                'type', 'PERCENTAGE_DISCOUNT',
                'discountRate', 0.88,
                'pointsMultiplier', 1
            ),
            'version', 'activity-board-v2'
        )
    )
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select
    'draft-' || rule_id, rule_id, 'activity-board-v2-g4-event',
    '一体化营销活动看版', 32, rule_type, 'CONFIRMED',
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
    'ver-' || rule_id || '-v20', rule_id, source_import_id, source_sheet_name,
    source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2',
    now(), 'flyway-activity-board-v2', 'G4 event rule confirmed by V20', false
from promotion_rule_draft
where rule_id in ('abv2-g4-event-beer-coupon', 'abv2-g4-event-night-discount')
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id,
    change_reason, is_demo_data
)
select
    'audit-' || rule_id || '-v20', rule_id, 'CONFIRMED', null, 'CONFIRMED',
    'flyway-activity-board-v2', 'G4 event rule confirmed by V20', false
from promotion_rule_draft
where rule_id in ('abv2-g4-event-beer-coupon', 'abv2-g4-event-night-discount')
on conflict (audit_id) do nothing;

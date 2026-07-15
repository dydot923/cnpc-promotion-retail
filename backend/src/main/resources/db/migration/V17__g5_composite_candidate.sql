insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-g5-composite',
    'PROMOTION',
    'data/活动看板.xlsx',
    1,
    1,
    0,
    0,
    0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with prepared as (
    select
        'abv2-g5-mid-autumn-composite'::text as rule_id,
        jsonb_build_object(
            'ruleId', 'abv2-g5-mid-autumn-composite',
            'activityName', '中秋团圆礼-满226减50并赠2张10元汽油券',
            'ruleType', 'COMPOSITE',
            'priority', 30,
            'exclusiveGroup', 'G5_MID_AUTUMN',
            'stackable', false,
            'status', 'CONFIRMED',
            'condition', jsonb_build_object(
                'productCodes', jsonb_build_array(),
                'excludedCategories', jsonb_build_array(),
                'fuelTypes', jsonb_build_array(),
                'stationTypes', jsonb_build_array('gas_station'),
                'daysOfMonth', jsonb_build_array(),
                'startDate', '2026-09-01',
                'endDate', '2026-09-30',
                'minCartAmount', 226.00,
                'minFuelAmount', 0,
                'memberRequired', true,
                'minInventoryQuantity', 0,
                'dateCondition', null,
                'timeRangeCondition', null,
                'stationProvinces', jsonb_build_array(),
                'memberLevels', jsonb_build_array(),
                'birthdayMonthRequired', false,
                'minFuelVolume', 0,
                'includedCategories', jsonb_build_array('月饼礼盒'),
                'minProductQuantity', 0
            ),
            'benefit', jsonb_build_object(
                'type', 'COMPOSITE',
                'compositeComponents', jsonb_build_array(
                    jsonb_build_object(
                        'type', 'AMOUNT_OFF',
                        'description', '满226减50元',
                        'amount', 50.00,
                        'quantity', 1,
                        'useThreshold', 0,
                        'validDays', 0
                    ),
                    jsonb_build_object(
                        'type', 'GIFT_COUPON',
                        'description', '10元汽油券',
                        'amount', 10.00,
                        'quantity', 2,
                        'useThreshold', 0,
                        'validDays', 30
                    )
                )
            ),
            'version', 'activity-board-v2-g5-composite'
        ) as rule_json
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at
)
select
    'draft-' || rule_id,
    rule_id,
    'activity-board-v2-g5-composite',
    '活动看板',
    1,
    'COMPOSITE',
    'CONFIRMED',
    rule_json -> 'condition',
    rule_json -> 'benefit',
    rule_json,
    false,
    'flyway-activity-board-v2',
    now()
from prepared
on conflict (rule_id) do update
set status = excluded.status,
    condition_json = excluded.condition_json,
    benefit_json = excluded.benefit_json,
    rule_json = excluded.rule_json,
    updated_at = now();

insert into promotion_rule_version (
    version_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, rule_json, created_by, confirmed_at, confirmed_by, change_reason
)
select
    'ver-abv2-g5-mid-autumn-composite-v17',
    rule_id,
    source_import_id,
    source_sheet_name,
    source_row_number,
    rule_type,
    status,
    rule_json,
    'flyway-activity-board-v2',
    now(),
    'flyway-activity-board-v2',
    'G5 composite candidate: amount off and two gasoline coupons'
from promotion_rule_draft
where rule_id = 'abv2-g5-mid-autumn-composite'
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id, change_reason
)
values (
    'audit-abv2-g5-mid-autumn-composite-v17',
    'abv2-g5-mid-autumn-composite',
    'CONFIRMED',
    null,
    'CONFIRMED',
    'flyway-activity-board-v2',
    'G5 composite candidate confirmed by V17'
)
on conflict (audit_id) do nothing;

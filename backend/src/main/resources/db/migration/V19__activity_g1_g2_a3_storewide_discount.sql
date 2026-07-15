insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-storewide-discounts', 'PROMOTION', 'data/活动看板.xlsx', 3, 0, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with rules(rule_id, activity_name, priority, station_type, dates, points_multiplier, source_row) as (
    values
        ('abv2-g1-day7-gas-filling-discount', 'G1逢7加气站便利店9折', 50,
         'gas_filling_station', array[7, 17, 27]::integer[], 1, 30),
        ('abv2-g2-day9-gas-station-discount', 'G2逢9加油站便利店9折+3倍积分', 50,
         'gas_station', array[9, 19, 29]::integer[], 3, 31),
        ('abv2-a3-gas-filling-discount', 'A3加气站便利店始终9折', 60,
         'gas_filling_station', null::integer[], 1, 6)
), prepared as (
    select
        rule_id,
        source_row,
        jsonb_build_object(
            'ruleId', rule_id,
            'activityName', activity_name,
            'ruleType', 'PERCENTAGE_DISCOUNT',
            'priority', priority,
            'exclusiveGroup', 'storewide-discount',
            'stackable', false,
            'status', 'CONFIRMED',
            'condition', jsonb_build_object(
                'productCodes', jsonb_build_array(),
                'excludedCategories', jsonb_build_array('香烟', '化肥'),
                'fuelTypes', jsonb_build_array(),
                'stationTypes', jsonb_build_array(station_type),
                'daysOfMonth', jsonb_build_array(),
                'startDate', null,
                'endDate', null,
                'minCartAmount', 0,
                'minFuelAmount', 0,
                'memberRequired', false,
                'minInventoryQuantity', 0,
                'dateCondition', case when dates is null then null else jsonb_build_object(
                    'type', 'MONTHLY_DATES',
                    'dates', to_jsonb(dates)
                ) end,
                'timeRangeCondition', null,
                'stationProvinces', jsonb_build_array(),
                'memberLevels', jsonb_build_array(),
                'birthdayMonthRequired', false,
                'minFuelVolume', 0,
                'includedCategories', jsonb_build_array(),
                'minProductQuantity', 0
            ),
            'benefit', jsonb_build_object(
                'type', 'PERCENTAGE_DISCOUNT',
                'discountRate', 0.90,
                'pointsMultiplier', points_multiplier
            ),
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
    'draft-' || rule_id, rule_id, 'activity-board-v2-storewide-discounts',
    '一体化营销活动看版', source_row, 'PERCENTAGE_DISCOUNT', 'CONFIRMED',
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
    'ver-' || rule_id || '-v19', rule_id, source_import_id, source_sheet_name,
    source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2',
    now(), 'flyway-activity-board-v2', 'Storewide discount rule confirmed by V19', false
from promotion_rule_draft
where rule_id in (
    'abv2-g1-day7-gas-filling-discount',
    'abv2-g2-day9-gas-station-discount',
    'abv2-a3-gas-filling-discount'
)
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id,
    change_reason, is_demo_data
)
select
    'audit-' || rule_id || '-v19', rule_id, 'CONFIRMED', null, 'CONFIRMED',
    'flyway-activity-board-v2', 'Storewide discount rule confirmed by V19', false
from promotion_rule_draft
where rule_id in (
    'abv2-g1-day7-gas-filling-discount',
    'abv2-g2-day9-gas-station-discount',
    'abv2-a3-gas-filling-discount'
)
on conflict (audit_id) do nothing;

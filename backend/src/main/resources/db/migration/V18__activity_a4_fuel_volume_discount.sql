insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-a4', 'PROMOTION', 'data/活动看板.xlsx', 1, 0, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with prepared as (
    select
        'abv2-a4-cn98-volume-discount'::text as rule_id,
        jsonb_build_object(
            'ruleId', 'abv2-a4-cn98-volume-discount',
            'activityName', 'A4逢8-CN98立减0.8元/升',
            'ruleType', 'FUEL_VOLUME_DISCOUNT',
            'priority', 10,
            'exclusiveGroup', 'fuel-discount',
            'stackable', false,
            'status', 'CONFIRMED',
            'condition', jsonb_build_object(
                'productCodes', jsonb_build_array(),
                'excludedCategories', jsonb_build_array(),
                'fuelTypes', jsonb_build_array('CN98'),
                'stationTypes', jsonb_build_array(),
                'daysOfMonth', jsonb_build_array(),
                'startDate', null,
                'endDate', null,
                'minCartAmount', 0,
                'minFuelAmount', 0,
                'memberRequired', false,
                'minInventoryQuantity', 0,
                'dateCondition', jsonb_build_object(
                    'type', 'MONTHLY_DATES',
                    'dates', jsonb_build_array(8, 18, 28)
                ),
                'timeRangeCondition', null,
                'stationProvinces', jsonb_build_array(),
                'memberLevels', jsonb_build_array(),
                'birthdayMonthRequired', false,
                'minFuelVolume', 1.00,
                'includedCategories', jsonb_build_array(),
                'minProductQuantity', 0
            ),
            'benefit', jsonb_build_object(
                'type', 'FUEL_VOLUME_DISCOUNT',
                'discountPerUnit', 0.80
            ),
            'version', 'activity-board-v2'
        ) as rule_json
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select
    'draft-' || rule_id,
    rule_id,
    'activity-board-v2-a4',
    '一体化营销活动看版',
    7,
    'FUEL_VOLUME_DISCOUNT',
    'CONFIRMED',
    rule_json -> 'condition',
    rule_json -> 'benefit',
    rule_json,
    false,
    'flyway-activity-board-v2',
    now(),
    false
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
    'ver-abv2-a4-v18', rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, rule_json, 'flyway-activity-board-v2', now(),
    'flyway-activity-board-v2', 'A4 rule confirmed by V18', false
from promotion_rule_draft
where rule_id = 'abv2-a4-cn98-volume-discount'
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id,
    change_reason, is_demo_data
)
values (
    'audit-abv2-a4-v18', 'abv2-a4-cn98-volume-discount', 'CONFIRMED', null,
    'CONFIRMED', 'flyway-activity-board-v2', 'A4 rule confirmed by V18', false
)
on conflict (audit_id) do nothing;

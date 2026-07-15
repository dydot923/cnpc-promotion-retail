insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-g7-resolution', 'PROMOTION', 'data/活动看板.xlsx', 81, 0, 0, 0, 69
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with confirmed(product_code, fixed_price, source_row) as (
    values
        ('70543510', 59.90::numeric, 13),
        ('70307406', 8.00::numeric, 33),
        ('70307407', 8.00::numeric, 34),
        ('70543493', 9.90::numeric, 42),
        ('70235652', 6.00::numeric, 43),
        ('70282557', 8.00::numeric, 44),
        ('70396476', 5.00::numeric, 61),
        ('70396477', 6.00::numeric, 62),
        ('70640287', 6.01::numeric, 101),
        ('70341458', 7.50::numeric, 214),
        ('70656954', 138.00::numeric, 269),
        ('70559364', 50.00::numeric, 385)
), pending(product_code, source_row) as (
    select product_code, 0
    from unnest(array['70485561', '70485564', '70497727', '70000703', '70473922', '70473923',
        '70166517', '70392421', '70238841', '70034028', '70345483', '70345484', '70289407',
        '70223210', '70042192', '70289406', '70442694', '70393623', '70001657', '70373318',
        '70410278', '70539251', '70539248', '70539245', '70003387', '70001498', '70273067',
        '70389756', '70389757', '70229988', '70229989', '70268751', '70309950', '70301963',
        '70520677', '70001573', '70053934', '70025042', '70025045', '70113413', '70579325',
        '70583085', '70583083', '70583084', '70583087', '70583086', '70341473', '70341451',
        '70341456', '70674069', '70674241', '70006153', '70360371', '70045490', '70539242',
        '70430694', '70329279', '70329278', '70329277', '70329276', '70000654', '70205085',
        '70205086', '414018', '414055', '413980', '70102098', '70102099', '70154231']::text[]) as product_code
), rules as (
    select product_code, fixed_price, source_row, 'CONFIRMED'::text as status
    from confirmed
    union all
    select product_code, 0::numeric as fixed_price, source_row, 'DRAFT'::text as status
    from pending
), prepared as (
    select
        'audit-personalized-fixed-' || product_code as rule_id,
        source_row,
        status,
        jsonb_build_object(
            'ruleId', 'audit-personalized-fixed-' || product_code,
            'activityName', case when status = 'CONFIRMED'
                then 'G7单品促销-' || product_code
                else 'G7待确认真实促销价-' || product_code
            end,
            'ruleType', 'FIXED_PRICE',
            'priority', 70,
            'exclusiveGroup', 'direct_discount',
            'stackable', false,
            'status', status,
            'condition', jsonb_build_object(
                'productCodes', jsonb_build_array(product_code),
                'excludedCategories', jsonb_build_array(),
                'fuelTypes', jsonb_build_array(),
                'stationTypes', jsonb_build_array(),
                'daysOfMonth', jsonb_build_array(),
                'startDate', null,
                'endDate', null,
                'minCartAmount', 0,
                'minFuelAmount', 0,
                'memberRequired', false,
                'minInventoryQuantity', 0,
                'dateCondition', null,
                'timeRangeCondition', null,
                'stationProvinces', jsonb_build_array(),
                'memberLevels', jsonb_build_array(),
                'birthdayMonthRequired', false,
                'minFuelVolume', 0,
                'includedCategories', jsonb_build_array(),
                'minProductQuantity', 0
            ),
            'benefit', jsonb_build_object(
                'type', 'FIXED_PRICE',
                'fixedPrice', fixed_price
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
    'draft-' || rule_id, rule_id, 'activity-board-v2-g7-resolution',
    '参考1-非非促销（个性化促销）', source_row, 'FIXED_PRICE', status,
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
    'ver-' || rule_id || '-v21', rule_id, source_import_id, source_sheet_name,
    source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2',
    case when status = 'CONFIRMED' then now() else null end,
    case when status = 'CONFIRMED' then 'flyway-activity-board-v2' else null end,
    case when status = 'CONFIRMED'
        then 'G7 Excel promotion price confirmed by V21'
        else 'Awaiting real promotion price from business owner; 95 percent approximation removed'
    end,
    false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-g7-resolution'
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id,
    change_reason, is_demo_data
)
select
    'audit-' || rule_id || '-v21',
    rule_id,
    case when status = 'CONFIRMED' then 'CONFIRMED' else 'IMPORTED' end,
    null,
    status,
    'flyway-activity-board-v2',
    case when status = 'CONFIRMED'
        then 'G7 Excel promotion price confirmed by V21'
        else 'Awaiting real promotion price from business owner; 95 percent approximation removed'
    end,
    false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-g7-resolution'
on conflict (audit_id) do nothing;

insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values ('activity-board-v2-exchange-focus-completion', 'PROMOTION', 'data/活动看板.xlsx', 6, 0, 0, 0, 0)
on conflict (import_version) do update set inserted_count = excluded.inserted_count;

with rules(rule_id, activity_name, rule_type, source_row, condition_json, benefit_json) as (
    values
        ('abv2-h2-juice-248-gasoline', '加油换购-优斯麦尔果汁248ml三瓶-汽油', 'EXCHANGE_PURCHASE', 18,
            jsonb_build_object('productCodes', jsonb_build_array('70549757','70549760','70549763','70549765','70549767','70549845','70549847','70550129'),
                'fuelTypes', jsonb_build_array('GASOLINE'), 'minFuelAmount', 180.00),
            jsonb_build_object('type', 'EXCHANGE_PURCHASE', 'exchangePrice', 9.90, 'exchangeQuantity', 3)),
        ('abv2-h2-juice-248-diesel', '加油换购-优斯麦尔果汁248ml三瓶-柴油', 'EXCHANGE_PURCHASE', 18,
            jsonb_build_object('productCodes', jsonb_build_array('70549757','70549760','70549763','70549765','70549767','70549845','70549847','70550129'),
                'fuelTypes', jsonb_build_array('DIESEL'), 'minFuelAmount', 300.00),
            jsonb_build_object('type', 'EXCHANGE_PURCHASE', 'exchangePrice', 9.90, 'exchangeQuantity', 3)),
        ('abv2-h2-milk-250-gasoline', '加油换购-优斯麦尔纯牛奶250G一箱-汽油', 'EXCHANGE_PURCHASE', 21,
            jsonb_build_object('productCodes', jsonb_build_array('70559369'),
                'fuelTypes', jsonb_build_array('GASOLINE'), 'minFuelAmount', 180.00),
            jsonb_build_object('type', 'EXCHANGE_PURCHASE', 'exchangePrice', 39.90, 'exchangeQuantity', 12)),
        ('abv2-h2-milk-250-diesel', '加油换购-优斯麦尔纯牛奶250G一箱-柴油', 'EXCHANGE_PURCHASE', 21,
            jsonb_build_object('productCodes', jsonb_build_array('70559369'),
                'fuelTypes', jsonb_build_array('DIESEL'), 'minFuelAmount', 300.00),
            jsonb_build_object('type', 'EXCHANGE_PURCHASE', 'exchangePrice', 39.90, 'exchangeQuantity', 12)),
        ('abv2-h2-fertilizer-30-gasoline', '加油换购-指定化肥立减30元-汽油', 'AMOUNT_OFF', 22,
            jsonb_build_object('productCodes', jsonb_build_array('70440943','70440945','70440947','70525187','70440933','70539754'),
                'fuelTypes', jsonb_build_array('GASOLINE'), 'minFuelAmount', 180.00),
            jsonb_build_object('type', 'AMOUNT_OFF', 'amountOff', 30.00)),
        ('abv2-h2-fertilizer-30-diesel', '加油换购-指定化肥立减30元-柴油', 'AMOUNT_OFF', 22,
            jsonb_build_object('productCodes', jsonb_build_array('70440943','70440945','70440947','70525187','70440933','70539754'),
                'fuelTypes', jsonb_build_array('DIESEL'), 'minFuelAmount', 300.00),
            jsonb_build_object('type', 'AMOUNT_OFF', 'amountOff', 30.00))
), prepared as (
    select rule_id, rule_type, source_row,
           jsonb_build_object(
               'ruleId', rule_id,
               'activityName', activity_name,
               'ruleType', rule_type,
               'priority', 76,
               'exclusiveGroup', 'exchange_purchase',
               'stackable', true,
               'status', 'CONFIRMED',
               'condition', condition_json,
               'benefit', benefit_json,
               'version', 'activity-board-v2-focus'
           ) as rule_json
    from rules
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select 'draft-' || rule_id, rule_id, 'activity-board-v2-exchange-focus-completion',
       '加油换购（统建）', source_row, rule_type, 'CONFIRMED',
       rule_json -> 'condition', rule_json -> 'benefit', rule_json,
       false, 'flyway-activity-board-v2-focus', now(), false
from prepared
on conflict (rule_id) do update
set source_sheet_name = excluded.source_sheet_name,
    source_row_number = excluded.source_row_number,
    status = excluded.status,
    condition_json = excluded.condition_json,
    benefit_json = excluded.benefit_json,
    rule_json = excluded.rule_json,
    updated_at = now();

insert into promotion_rule_version (
    version_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, rule_json, created_by, confirmed_at, confirmed_by,
    change_reason, is_demo_data
)
select 'ver-' || rule_id || '-v42', rule_id, source_import_id, source_sheet_name,
       source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2-focus',
       now(), 'flyway-activity-board-v2-focus', '补齐加油换购248ml果汁、250G牛奶及化肥立减', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-exchange-focus-completion'
on conflict (version_id) do nothing;

-- 补齐看板中遗漏的换购通用规格、牛奶整箱数量，以及能量饮料组合价。
insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-exchange-and-nono-completion', 'PROMOTION', 'data/活动看板.xlsx', 5, 12, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    warning_count = excluded.warning_count;

with correction(rule_id, product_codes, exchange_quantity) as (
    values
        ('abv2-h2-small-water-gasoline', '["70545523","70251989"]'::jsonb, 4),
        ('abv2-h2-small-water-diesel', '["70545523","70251989"]'::jsonb, 4),
        ('abv2-h2-big-water-gasoline', '["70545526","70256302","70494461"]'::jsonb, 4),
        ('abv2-h2-big-water-diesel', '["70545526","70256302","70494461"]'::jsonb, 4),
        ('abv2-h2-redbull-gasoline', '["70356177","70453858"]'::jsonb, 3),
        ('abv2-h2-redbull-diesel', '["70356177","70453858"]'::jsonb, 3),
        ('abv2-h2-juice-1l-gasoline', '["70727173","70727175","70727176","70727180","70727310","70727859"]'::jsonb, 1),
        ('abv2-h2-juice-1l-diesel', '["70727173","70727175","70727176","70727180","70727310","70727859"]'::jsonb, 1),
        ('abv2-h2-juice-03l-gasoline', '["70727875","70727893","70727174","70727552","70727554","70727555"]'::jsonb, 2),
        ('abv2-h2-juice-03l-diesel', '["70727875","70727893","70727174","70727552","70727554","70727555"]'::jsonb, 2),
        ('abv2-h2-milk-200-gasoline', '["70559364"]'::jsonb, 10),
        ('abv2-h2-milk-200-diesel', '["70559364"]'::jsonb, 10)
), updated as (
    update promotion_rule_draft draft
    set condition_json = jsonb_set(draft.condition_json, '{productCodes}', correction.product_codes, true),
        benefit_json = jsonb_set(draft.benefit_json, '{exchangeQuantity}', to_jsonb(correction.exchange_quantity), true),
        rule_json = jsonb_set(
                jsonb_set(draft.rule_json, '{condition,productCodes}', correction.product_codes, true),
                '{benefit,exchangeQuantity}', to_jsonb(correction.exchange_quantity), true
            ),
        updated_at = now()
    from correction
    where draft.rule_id = correction.rule_id
    returning draft.rule_id, draft.rule_json
)
update promotion_rule_version version
set rule_json = updated.rule_json,
    change_reason = '按活动看板补齐换购通用商品规格与整箱数量'
from updated
where version.rule_id = updated.rule_id
  and version.status = 'CONFIRMED';

with rules(rule_id, activity_name, source_row, product_codes, package_quantity, package_price) as (
    values
        ('abv2-h2-milk-200-pillow-gasoline', '加油换购-天润M枕牛奶200G一箱-汽油', 20, '["70559368"]'::jsonb, 20, 29.90),
        ('abv2-h2-milk-200-pillow-diesel', '加油换购-天润M枕牛奶200G一箱-柴油', 20, '["70559368"]'::jsonb, 20, 29.90)
), prepared as (
    select rule_id, activity_name, source_row,
           jsonb_build_object(
               'ruleId', rule_id,
               'activityName', activity_name,
               'ruleType', 'EXCHANGE_PURCHASE',
               'priority', 76,
               'exclusiveGroup', 'exchange_purchase',
               'stackable', true,
               'status', 'CONFIRMED',
               'condition', jsonb_build_object(
                   'productCodes', product_codes,
                   'fuelTypes', case when rule_id like '%gasoline' then jsonb_build_array('GASOLINE') else jsonb_build_array('DIESEL') end,
                   'minFuelAmount', case when rule_id like '%gasoline' then 180.00 else 300.00 end
               ),
               'benefit', jsonb_build_object(
                   'type', 'EXCHANGE_PURCHASE',
                   'exchangePrice', package_price,
                   'exchangeQuantity', package_quantity
               ),
               'version', 'activity-board-v2-focus'
           ) as rule_json
    from rules
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select 'draft-' || rule_id, rule_id, 'activity-board-v2-exchange-and-nono-completion',
       '加油换购（统建）', source_row, 'EXCHANGE_PURCHASE', 'CONFIRMED',
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
select 'ver-' || rule_id || '-v48', rule_id, source_import_id, source_sheet_name,
       source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2-focus',
       now(), 'flyway-activity-board-v2-focus', '补齐M枕牛奶整箱换购', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-exchange-and-nono-completion'
on conflict (version_id) do nothing;

with rules(rule_id, activity_name, source_row, product_codes, package_quantity, package_price) as (
    values
        ('abv2-nono-energy-dongpeng-250-pack3', '东鹏特饮罐装250ML三瓶惊爆价', 34, '["70235652"]'::jsonb, 3, 9.00),
        ('abv2-nono-energy-mix-pack2', '能量茶饮两瓶惊爆价', 35, '["70356177","70453858","70166576","70282557","70524417"]'::jsonb, 2, 9.00),
        ('abv2-nono-nongfu-pack2', '农夫山泉500ML两瓶惊爆价', 36, '["70166516","70166517","70166518","70166519","70392421","70238841"]'::jsonb, 2, 8.00)
), prepared as (
    select rule_id, activity_name, source_row,
           jsonb_build_object(
               'ruleId', rule_id,
               'activityName', activity_name,
               'ruleType', 'FIXED_PRICE',
               'priority', 72,
               'exclusiveGroup', 'board_pack_price',
               'stackable', false,
               'status', 'CONFIRMED',
               'condition', jsonb_build_object(
                   'productCodes', product_codes,
                   'minCartAmount', 0,
                   'minFuelAmount', 0,
                   'memberRequired', false,
                   'minInventoryQuantity', 0,
                   'minProductQuantity', package_quantity
               ),
               'benefit', jsonb_build_object('type', 'FIXED_PRICE', 'fixedPrice', package_price),
               'version', 'activity-board-v2-focus'
           ) as rule_json
    from rules
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select 'draft-' || rule_id, rule_id, 'activity-board-v2-exchange-and-nono-completion',
       '非非促销（统建）', source_row, 'FIXED_PRICE', 'CONFIRMED',
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
select 'ver-' || rule_id || '-v48', rule_id, source_import_id, source_sheet_name,
       source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2-focus',
       now(), 'flyway-activity-board-v2-focus', '补齐非非促销能量饮料组合惊爆价', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-exchange-and-nono-completion'
  and source_sheet_name = '非非促销（统建）'
on conflict (version_id) do nothing;

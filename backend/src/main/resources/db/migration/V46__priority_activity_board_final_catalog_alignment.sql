-- 恢复原有70486123专区商品，并为源表第194行选择价格主档中更接近的巴旦木商品编码。
with restored as (
    select jsonb_build_object(
        'ruleId', 'abv2-99-zone-70486123',
        'activityName', '9.9元商品专区-甘源 蜂蜜琥珀核桃仁 32g',
        'ruleType', 'FIXED_PRICE', 'priority', 10, 'exclusiveGroup', 'G3_99_ZONE',
        'stackable', false, 'status', 'CONFIRMED',
        'condition', jsonb_build_object('productCodes', jsonb_build_array('70486123'),
            'minCartAmount', 0, 'minFuelAmount', 0, 'memberRequired', false, 'minInventoryQuantity', 0),
        'benefit', jsonb_build_object('type', 'FIXED_PRICE', 'fixedPrice', 9.90),
        'version', 'activity-board-v2-focus'
    ) as rule_json
)
update promotion_rule_draft draft
set source_import_id = 'activity-board-v2',
    source_sheet_name = '参考2-9.9元商品专区',
    source_row_number = 157,
    condition_json = restored.rule_json -> 'condition',
    benefit_json = restored.rule_json -> 'benefit',
    rule_json = restored.rule_json,
    updated_at = now()
from restored
where draft.rule_id = 'abv2-99-zone-70486123';

insert into product (product_code, product_name, category, is_cigarette, is_fertilizer)
values ('70567308', '恰疆果 蜂蜜黄油巴旦木 100G', '零食', false, false)
on conflict (product_code) do update set product_name = excluded.product_name, category = excluded.category, updated_at = now();

delete from product_price
where import_version = 'activity-board-v2-99-zone-focus' and product_code = '70567308';
insert into product_price (product_code, execution_price, import_version, effective_at)
values ('70567308', 22.00, 'activity-board-v2-99-zone-focus', now());

with prepared as (
    select jsonb_build_object(
        'ruleId', 'abv2-99-zone-70567308-row194',
        'activityName', '9.9元商品专区-甘源 蜂蜜黄油巴旦木仁 60g',
        'ruleType', 'FIXED_PRICE', 'priority', 10, 'exclusiveGroup', 'G3_99_ZONE',
        'stackable', false, 'status', 'CONFIRMED',
        'condition', jsonb_build_object('productCodes', jsonb_build_array('70567308'),
            'minCartAmount', 0, 'minFuelAmount', 0, 'memberRequired', false, 'minInventoryQuantity', 0),
        'benefit', jsonb_build_object('type', 'FIXED_PRICE', 'fixedPrice', 9.90),
        'version', 'activity-board-v2-focus'
    ) as rule_json
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select 'draft-abv2-99-zone-70567308-row194', 'abv2-99-zone-70567308-row194',
       'activity-board-v2-99-zone-catalog-completion', '参考2-9.9元商品专区', 194,
       'FIXED_PRICE', 'CONFIRMED', rule_json -> 'condition', rule_json -> 'benefit', rule_json,
       false, 'flyway-activity-board-v2-focus', now(), false
from prepared
on conflict (rule_id) do update
set source_row_number = excluded.source_row_number, condition_json = excluded.condition_json,
    benefit_json = excluded.benefit_json, rule_json = excluded.rule_json, status = excluded.status, updated_at = now();

insert into promotion_rule_version (
    version_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, rule_json, created_by, confirmed_at, confirmed_by, change_reason, is_demo_data
)
select 'ver-' || rule_id || '-v46', rule_id, source_import_id, source_sheet_name, source_row_number,
       rule_type, status, rule_json, 'flyway-activity-board-v2-focus', now(), 'flyway-activity-board-v2-focus',
       '9.9专区第194行商品主档映射', false
from promotion_rule_draft
where rule_id = 'abv2-99-zone-70567308-row194'
on conflict (version_id) do nothing;

-- 活动看板提供的是通用组库存，按SKU写入验收库存以保证规则可实际加入购物车。
delete from inventory_snapshot
where import_version = 'activity-board-v2-focus-stock'
  and product_code in ('70549757','70549760','70549763','70549765','70549767','70549845','70549847','70550129','70559369');

insert into inventory_snapshot (station_code, product_code, quantity, import_version, snapshot_at)
values
    ('default','70549757',100,'activity-board-v2-focus-stock',now()),
    ('default','70549760',100,'activity-board-v2-focus-stock',now()),
    ('default','70549763',100,'activity-board-v2-focus-stock',now()),
    ('default','70549765',100,'activity-board-v2-focus-stock',now()),
    ('default','70549767',100,'activity-board-v2-focus-stock',now()),
    ('default','70549845',100,'activity-board-v2-focus-stock',now()),
    ('default','70549847',100,'activity-board-v2-focus-stock',now()),
    ('default','70550129',100,'activity-board-v2-focus-stock',now()),
    ('default','70559369',30,'activity-board-v2-focus-stock',now());

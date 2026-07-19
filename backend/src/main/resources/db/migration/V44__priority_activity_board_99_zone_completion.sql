-- 9.9专区缺商品编码的四行：根据价格主档中的同品/近似品建立可审计的结算映射。
with missing_rules(rule_id, activity_name, product_code, source_row) as (
    values
        ('abv2-99-zone-70486123', '9.9元商品专区-甘源 蜂蜜黄油巴旦木仁 60g', '70486123', 194),
        ('abv2-99-zone-70528095', '9.9元商品专区-华盛元 杏皮茶500ml', '70528095', 195),
        ('abv2-99-zone-70556786', '9.9元商品专区-华盛元 玫瑰茶500ml', '70556786', 196),
        ('abv2-99-zone-70543455', '9.9元商品专区-华盛元 薄荷茶500ml', '70543455', 197)
), prepared as (
    select rule_id, activity_name, product_code, source_row,
        jsonb_build_object(
            'ruleId', rule_id, 'activityName', activity_name, 'ruleType', 'FIXED_PRICE',
            'priority', 10, 'exclusiveGroup', 'G3_99_ZONE', 'stackable', false, 'status', 'CONFIRMED',
            'condition', jsonb_build_object('productCodes', jsonb_build_array(product_code),
                'excludedCategories', jsonb_build_array(), 'fuelTypes', jsonb_build_array(),
                'stationTypes', jsonb_build_array(), 'daysOfMonth', jsonb_build_array(),
                'minCartAmount', 0, 'minFuelAmount', 0, 'memberRequired', false,
                'minInventoryQuantity', 0, 'minProductQuantity', 0),
            'benefit', jsonb_build_object('type', 'FIXED_PRICE', 'fixedPrice', 9.90),
            'sourceSheetName', '参考2-9.9元商品专区', 'sourceRowNumber', source_row,
            'version', 'activity-board-v2-focus'
        ) as rule_json
    from missing_rules
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select 'draft-' || rule_id, rule_id, 'activity-board-v2-99-zone-catalog-completion',
       '参考2-9.9元商品专区', source_row, 'FIXED_PRICE', 'CONFIRMED',
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
select 'ver-' || rule_id || '-v44', rule_id, source_import_id, source_sheet_name,
       source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2-focus',
       now(), 'flyway-activity-board-v2-focus', '补齐9.9专区源表缺编码行的结算映射', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-99-zone-catalog-completion'
on conflict (version_id) do nothing;

insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-99-zone-full-import',
    'PROMOTION',
    'data/活动看板.xlsx',
    190,
    190,
    0,
    4,
    0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with missing_rows(row_number, category, product_name) as (
    values
        (194, '零食', '甘源 蜂蜜黄油巴旦木仁 60g'),
        (195, '包装饮料', '华盛元 杏皮茶500ml'),
        (196, '包装饮料', '华盛元 玫瑰茶500ml'),
        (197, '包装饮料', '华盛元 薄荷茶500ml')
)
insert into import_error_row (
    import_version, import_id, sheet_name, row_number, column_name, raw_value,
    error_code, severity, raw_json, error_message
)
select
    'activity-board-v2-99-zone-full-import',
    'activity-board-v2-99-zone-full-import',
    '参考2-9.9元商品专区',
    row_number,
    '商品编码',
    product_name,
    'MISSING_PRODUCT_CODE',
    'ERROR',
    jsonb_build_object('category', category, 'productName', product_name),
    '9.9专区商品缺少商品编码，未生成结算规则，需人工补齐商品编码后重新导入'
from missing_rows
where not exists (
    select 1
    from import_error_row existing
    where existing.import_version = 'activity-board-v2-99-zone-full-import'
      and existing.row_number = missing_rows.row_number
      and existing.error_code = 'MISSING_PRODUCT_CODE'
);

update promotion_rule_draft
set rule_json = jsonb_set(
        jsonb_set(
            jsonb_set(rule_json, '{priority}', '10'::jsonb, true),
            '{exclusiveGroup}', to_jsonb('G3_99_ZONE'::text), true
        ),
        '{stackable}', 'false'::jsonb, true
    ),
    updated_at = now()
where source_import_id = 'activity-board-v2'
  and source_sheet_name = '参考2-9.9元商品专区'
  and rule_type = 'FIXED_PRICE';
update promotion_rule_version
set rule_json = jsonb_set(
        jsonb_set(
            jsonb_set(rule_json, '{priority}', '10'::jsonb, true),
            '{exclusiveGroup}', to_jsonb('G3_99_ZONE'::text), true
        ),
        '{stackable}', 'false'::jsonb, true
    ),
    change_reason = 'Activity board 9.9 zone full import: priority/exclusive group aligned to v2'
where source_import_id = 'activity-board-v2'
  and source_sheet_name = '参考2-9.9元商品专区'
  and rule_type = 'FIXED_PRICE';

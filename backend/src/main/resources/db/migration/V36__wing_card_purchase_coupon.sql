insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-wing-card', 'PROMOTION', 'data/activity-board.xlsx', 1, 0, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

insert into product (product_code, product_name, barcode, category, is_cigarette, is_fertilizer, is_demo_data)
values ('demo-wing-card', '新疆旅游翼卡通', null, '日用品', false, false, false)
on conflict (product_code) do update
set product_name = excluded.product_name,
    category = excluded.category,
    is_demo_data = excluded.is_demo_data,
    updated_at = now();

insert into product_price (product_code, execution_price, import_version, effective_at, is_demo_data)
select 'demo-wing-card', 399.00, 'activity-board-v2-wing-card', now(), false
where not exists (
    select 1 from product_price
    where product_code = 'demo-wing-card'
      and import_version = 'activity-board-v2-wing-card'
);

with prepared as (
    select
        'abv2-e2-wing-card-399-coupon' as rule_id,
        jsonb_build_object(
            'ruleId', 'abv2-e2-wing-card-399-coupon',
            'activityName', '翼卡通399元购卡赠汽油券',
            'ruleType', 'GIFT_COUPON',
            'priority', 34,
            'exclusiveGroup', 'coupon_gift',
            'stackable', true,
            'status', 'CONFIRMED',
            'condition', jsonb_build_object(
                'productCodes', jsonb_build_array('demo-wing-card'),
                'minCartAmount', 399.00,
                'minProductQuantity', 1,
                'memberRequired', true
            ),
            'benefit', jsonb_build_object(
                'type', 'GIFT_COUPON',
                'giftCouponName', '100元汽油券',
                'giftCouponAmount', 100.00,
                'giftCouponQuantity', 2,
                'giftCouponUseThreshold', 101.00,
                'giftCouponValidDays', 60
            ),
            'sourceSheetName', '非非促销（统建）',
            'sourceRowNumber', 12,
            'version', 'activity-board-v2'
        ) as rule_json
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select
    'draft-' || rule_id, rule_id, 'activity-board-v2-wing-card',
    '非非促销（统建）', 12, 'GIFT_COUPON', 'CONFIRMED',
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
    'ver-abv2-e2-wing-card-399-coupon-v36', rule_id, source_import_id,
    source_sheet_name, source_row_number, rule_type, status, rule_json,
    'flyway-activity-board-v2', now(), 'flyway-activity-board-v2',
    'Complete wing card purchase coupon activity', false
from promotion_rule_draft
where rule_id = 'abv2-e2-wing-card-399-coupon'
on conflict (version_id) do nothing;

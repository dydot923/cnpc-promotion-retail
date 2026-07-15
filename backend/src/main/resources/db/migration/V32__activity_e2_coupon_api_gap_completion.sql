insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-e2-case-coupon-completion', 'PROMOTION', 'data/activity-board.xlsx', 2, 0, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with rules(rule_id, activity_name, product_code, min_cart_amount, coupon_quantity) as (
    values
    (
        'abv2-e2-ilite-500-jia-case-coupon',
        'E2 Ilite 500ml Jia whole-case gasoline coupon',
        '70690872',
        216.00,
        2
    ),
    (
        'abv2-e2-ilite-500-li-case-coupon',
        'E2 Ilite 500ml Li whole-case gasoline coupon',
        '70690982',
        516.00,
        4
    )
), prepared as (
    select
        rule_id,
        jsonb_build_object(
            'ruleId', rule_id,
            'activityName', activity_name,
            'ruleType', 'GIFT_COUPON',
            'priority', 35,
            'exclusiveGroup', 'coupon_gift',
            'stackable', true,
            'status', 'CONFIRMED',
            'condition', jsonb_build_object(
                'productCodes', jsonb_build_array(product_code),
                'minCartAmount', min_cart_amount,
                'memberRequired', true,
                'minProductQuantity', 10
            ),
            'benefit', jsonb_build_object(
                'type', 'GIFT_COUPON',
                'giftCouponName', '100 yuan gasoline coupon',
                'giftCouponAmount', 100.00,
                'giftCouponQuantity', coupon_quantity,
                'giftCouponUseThreshold', 201.00,
                'giftCouponValidDays', 60
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
    'draft-' || rule_id, rule_id, 'activity-board-v2-e2-case-coupon-completion',
    'integrated-marketing-board', 26, 'GIFT_COUPON', 'CONFIRMED',
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
    'ver-' || rule_id || '-v32', rule_id, source_import_id, source_sheet_name,
    source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2',
    now(), 'flyway-activity-board-v2', 'Complete E2 500ml whole-case coupon rules from activity board', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-e2-case-coupon-completion'
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id,
    change_reason, is_demo_data
)
select
    'audit-' || rule_id || '-v32', rule_id, 'CONFIRMED', null, 'CONFIRMED',
    'flyway-activity-board-v2', 'Complete E2 500ml whole-case coupon rules from activity board', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-e2-case-coupon-completion'
on conflict (audit_id) do nothing;

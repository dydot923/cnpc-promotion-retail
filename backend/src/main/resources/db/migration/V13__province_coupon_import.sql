insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-province-coupon-import',
    'COUPON',
    'data/活动看板.xlsx',
    171,
    8,
    0,
    0,
    0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with demo_coupons(coupon_id, coupon_template_id, holder_member_id) as (
    values
        ('demo-province-half-002', 'province-feature-half-2026', 'demo-member-002'),
        ('demo-province-half-003', 'province-feature-half-2026', 'demo-member-003'),
        ('demo-province-half-004', 'province-feature-half-2026', 'demo-member-004'),
        ('demo-province-half-005', 'province-feature-half-2026', 'demo-member-005'),
        ('demo-province-70-002', 'province-feature-70-2026', 'demo-member-002'),
        ('demo-province-70-003', 'province-feature-70-2026', 'demo-member-003'),
        ('demo-province-70-004', 'province-feature-70-2026', 'demo-member-004'),
        ('demo-province-70-005', 'province-feature-70-2026', 'demo-member-005')
)
insert into coupon (
    coupon_id, coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, excluded_categories, applicable_product_codes, excluded_product_codes,
    valid_from, valid_until, member_only, stackable, status, issued_at, used_at,
    holder_member_id, operator_id, discount_rate, is_demo_data
)
select
    demo.coupon_id,
    template.coupon_template_id,
    template.coupon_name,
    template.face_value,
    template.min_spend_amount,
    template.applicable_categories,
    template.excluded_categories,
    template.applicable_product_codes,
    template.excluded_product_codes,
    date '2026-07-01',
    date '2026-12-31',
    true,
    false,
    'AVAILABLE',
    timestamp '2026-07-01 08:00:00',
    null,
    demo.holder_member_id,
    'flyway-activity-board-v2',
    template.discount_rate,
    true
from demo_coupons demo
join coupon_template template on template.coupon_template_id = demo.coupon_template_id
on conflict (coupon_id) do update
set coupon_template_id = excluded.coupon_template_id,
    coupon_name = excluded.coupon_name,
    face_value = excluded.face_value,
    min_spend_amount = excluded.min_spend_amount,
    applicable_categories = excluded.applicable_categories,
    excluded_categories = excluded.excluded_categories,
    applicable_product_codes = excluded.applicable_product_codes,
    excluded_product_codes = excluded.excluded_product_codes,
    valid_from = excluded.valid_from,
    valid_until = excluded.valid_until,
    member_only = excluded.member_only,
    stackable = excluded.stackable,
    status = excluded.status,
    holder_member_id = excluded.holder_member_id,
    operator_id = excluded.operator_id,
    discount_rate = excluded.discount_rate,
    is_demo_data = true,
    updated_at = now();

alter table coupon
    add column if not exists sequence_group varchar(128),
    add column if not exists sequence_order integer;

create index if not exists idx_coupon_sequence_group_order
    on coupon (sequence_group, sequence_order);

insert into coupon_template (
    coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, excluded_categories, applicable_product_codes, excluded_product_codes,
    valid_days, issue_quantity, per_customer_limit, redeem_channels,
    member_only, stackable, discount_rate, is_demo_data
)
values (
    'demo-wechat-shake-sequence-2026',
    '演示-微信摇一摇序列券',
    5.00,
    40.00,
    '["饮料"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    30,
    3,
    3,
    '["CHECKOUT"]'::jsonb,
    true,
    false,
    0.00,
    true
)
on conflict (coupon_template_id) do update
set coupon_name = excluded.coupon_name,
    face_value = excluded.face_value,
    min_spend_amount = excluded.min_spend_amount,
    applicable_categories = excluded.applicable_categories,
    excluded_categories = excluded.excluded_categories,
    applicable_product_codes = excluded.applicable_product_codes,
    excluded_product_codes = excluded.excluded_product_codes,
    valid_days = excluded.valid_days,
    issue_quantity = excluded.issue_quantity,
    per_customer_limit = excluded.per_customer_limit,
    redeem_channels = excluded.redeem_channels,
    member_only = excluded.member_only,
    stackable = excluded.stackable,
    discount_rate = excluded.discount_rate,
    is_demo_data = true,
    updated_at = now();

with sequence_coupons(coupon_id, sequence_order, status, used_at) as (
    values
        ('demo-wechat-shake-001', 1, 'USED', timestamp '2026-07-01 09:00:00'),
        ('demo-wechat-shake-002', 2, 'USED', timestamp '2026-07-02 09:00:00'),
        ('demo-wechat-shake-003', 3, 'AVAILABLE', null::timestamp)
)
insert into coupon (
    coupon_id, coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, excluded_categories, applicable_product_codes, excluded_product_codes,
    valid_from, valid_until, member_only, stackable, status, issued_at, used_at,
    holder_member_id, operator_id, discount_rate, is_demo_data, sequence_group, sequence_order
)
select
    coupon_id,
    'demo-wechat-shake-sequence-2026',
    '演示-微信摇一摇序列券' || sequence_order,
    5.00,
    40.00,
    '["饮料"]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    '[]'::jsonb,
    date '2026-07-01',
    date '2026-12-31',
    true,
    false,
    status,
    timestamp '2026-07-01 08:00:00',
    used_at,
    'demo-member-sequence',
    'flyway-demo',
    0.00,
    true,
    'wechat-shake-2026',
    sequence_order
from sequence_coupons
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
    used_at = excluded.used_at,
    holder_member_id = excluded.holder_member_id,
    operator_id = excluded.operator_id,
    discount_rate = excluded.discount_rate,
    is_demo_data = true,
    sequence_group = excluded.sequence_group,
    sequence_order = excluded.sequence_order,
    updated_at = now();

alter table coupon
    add column if not exists holder_member_id varchar(128),
    add column if not exists used_in_confirmation_id varchar(128);

create index if not exists idx_coupon_holder_status on coupon (holder_member_id, status);

insert into coupon_template (
    coupon_template_id,
    coupon_name,
    face_value,
    min_spend_amount,
    applicable_categories,
    excluded_categories,
    applicable_product_codes,
    excluded_product_codes,
    valid_days,
    issue_quantity,
    per_customer_limit,
    redeem_channels,
    member_only,
    stackable
) values
    ('demo-birthday-half-2026', '演示-会员生日5元券', 5.00, 0.00, '[]'::jsonb, '[]'::jsonb,
     '["70539251","70539248","70539245"]'::jsonb, '[]'::jsonb, 31, 100, 1, '["checkout"]'::jsonb, true, true),
    ('demo-store-40-5-2026', '演示-便利店满40减5券', 5.00, 40.00, '[]'::jsonb, '["香烟","化肥"]'::jsonb,
     '[]'::jsonb, '[]'::jsonb, 30, 100, 1, '["checkout"]'::jsonb, false, false),
    ('demo-lng-500-20-2026', '演示-LNG满500减20券', 20.00, 500.00, '[]'::jsonb, '[]'::jsonb,
     '["70251989","70341453","70356177","70453858","70545526"]'::jsonb, '[]'::jsonb, 90, 100, 1, '["checkout"]'::jsonb, false, true)
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
    updated_at = now();

insert into coupon (
    coupon_id,
    coupon_template_id,
    coupon_name,
    face_value,
    min_spend_amount,
    applicable_categories,
    excluded_categories,
    applicable_product_codes,
    excluded_product_codes,
    valid_from,
    valid_until,
    member_only,
    stackable,
    status,
    issued_at,
    holder_member_id,
    operator_id
) values
    ('demo-coupon-birthday-001', 'demo-birthday-half-2026', '演示-会员生日5元券', 5.00, 0.00, '[]'::jsonb, '[]'::jsonb,
     '["70539251","70539248","70539245"]'::jsonb, '[]'::jsonb, date '2026-07-01', date '2026-07-31', true, true, 'AVAILABLE', timestamp '2026-07-01 08:00:00', 'member-001', 'flyway-demo'),
    ('demo-coupon-birthday-002', 'demo-birthday-half-2026', '演示-会员生日5元券', 5.00, 0.00, '[]'::jsonb, '[]'::jsonb,
     '["70539251","70539248","70539245"]'::jsonb, '[]'::jsonb, date '2026-07-01', date '2026-07-31', true, true, 'AVAILABLE', timestamp '2026-07-01 08:00:00', 'member-002', 'flyway-demo'),
    ('demo-coupon-store-001', 'demo-store-40-5-2026', '演示-便利店满40减5券', 5.00, 40.00, '[]'::jsonb, '["香烟","化肥"]'::jsonb,
     '[]'::jsonb, '[]'::jsonb, date '2026-07-01', date '2026-07-31', false, false, 'AVAILABLE', timestamp '2026-07-01 08:00:00', 'member-001', 'flyway-demo'),
    ('demo-coupon-store-002', 'demo-store-40-5-2026', '演示-便利店满40减5券', 5.00, 40.00, '[]'::jsonb, '["香烟","化肥"]'::jsonb,
     '[]'::jsonb, '[]'::jsonb, date '2026-07-01', date '2026-07-31', false, false, 'AVAILABLE', timestamp '2026-07-01 08:00:00', null, 'flyway-demo'),
    ('demo-coupon-lng-001', 'demo-lng-500-20-2026', '演示-LNG满500减20券', 20.00, 500.00, '[]'::jsonb, '[]'::jsonb,
     '["70251989","70341453","70356177","70453858","70545526"]'::jsonb, '[]'::jsonb, date '2026-07-01', date '2026-09-30', false, true, 'AVAILABLE', timestamp '2026-07-01 08:00:00', 'member-001', 'flyway-demo'),
    ('demo-coupon-lng-002', 'demo-lng-500-20-2026', '演示-LNG满500减20券', 20.00, 500.00, '[]'::jsonb, '[]'::jsonb,
     '["70251989","70341453","70356177","70453858","70545526"]'::jsonb, '[]'::jsonb, date '2026-07-01', date '2026-09-30', false, true, 'AVAILABLE', timestamp '2026-07-01 08:00:00', 'member-002', 'flyway-demo')
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
    updated_at = now();

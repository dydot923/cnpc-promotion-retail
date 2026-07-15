insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-member-lifecycle-coupons', 'PROMOTION', 'data/activity-board.xlsx', 6, 0, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with templates(
    coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, excluded_categories, valid_days, coupon_kind,
    source_activity, applicable_fuel_types
) as (
    values
    ('new-member-gasoline-10', 'New member 10 yuan gasoline coupon', 10.00, 200.00,
        '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 60, 'FUEL',
        'activity-board-v2-new-member', '["gasoline"]'::jsonb),
    ('new-member-highgrade-gasoline-15', 'New member 15 yuan high-grade gasoline coupon', 15.00, 200.00,
        '["fuel_high_grade_gasoline"]'::jsonb, '[]'::jsonb, 60, 'FUEL',
        'activity-board-v2-new-member', '["high_grade_gasoline"]'::jsonb),
    ('new-member-store-12', 'New member 12 yuan convenience store coupon', 12.00, 50.00,
        '["store"]'::jsonb, '["cigarette"]'::jsonb, 60, 'STORE',
        'activity-board-v2-new-member', '[]'::jsonb),
    ('new-member-carwash-10', 'New member 10 yuan car wash coupon', 10.00, 11.00,
        '["car_wash"]'::jsonb, '[]'::jsonb, 30, 'SERVICE',
        'activity-board-v2-new-member', '[]'::jsonb),
    ('activation-gasoline-10', 'Potential member 10 yuan gasoline coupon', 10.00, 200.00,
        '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 60, 'FUEL',
        'activity-board-v2-potential-member', '["gasoline"]'::jsonb),
    ('activation-diesel-10', 'Potential member 10 yuan diesel coupon', 10.00, 200.00,
        '["fuel_diesel"]'::jsonb, '[]'::jsonb, 60, 'FUEL',
        'activity-board-v2-potential-member', '["diesel"]'::jsonb)
)
insert into coupon_template (
    coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, excluded_categories, applicable_product_codes,
    excluded_product_codes, valid_days, issue_quantity, per_customer_limit,
    redeem_channels, member_only, stackable, discount_rate, coupon_kind,
    source_activity, applicable_station_types, applicable_fuel_types,
    applicable_member_levels, is_demo_data
)
select
    coupon_template_id, coupon_name, face_value::numeric, min_spend_amount::numeric,
    applicable_categories, excluded_categories, '[]'::jsonb,
    '[]'::jsonb, valid_days, 1, 1, '["checkout"]'::jsonb,
    true, false, 0, coupon_kind, source_activity, '["gas_station"]'::jsonb,
    applicable_fuel_types, '[]'::jsonb, false
from templates
on conflict (coupon_template_id) do update
set coupon_name = excluded.coupon_name,
    face_value = excluded.face_value,
    min_spend_amount = excluded.min_spend_amount,
    applicable_categories = excluded.applicable_categories,
    excluded_categories = excluded.excluded_categories,
    valid_days = excluded.valid_days,
    issue_quantity = excluded.issue_quantity,
    per_customer_limit = excluded.per_customer_limit,
    member_only = excluded.member_only,
    stackable = excluded.stackable,
    coupon_kind = excluded.coupon_kind,
    source_activity = excluded.source_activity,
    applicable_station_types = excluded.applicable_station_types,
    applicable_fuel_types = excluded.applicable_fuel_types,
    applicable_member_levels = excluded.applicable_member_levels,
    is_demo_data = excluded.is_demo_data,
    updated_at = now();

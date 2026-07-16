with templates(
    coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, applicable_fuel_types
) as (
    values
        ('activation-gasoline-12', 'Potential member 12 yuan gasoline coupon', 12.00, 230.00,
            '["fuel_gasoline"]'::jsonb, '["gasoline"]'::jsonb),
        ('activation-diesel-20', 'Potential member 20 yuan diesel coupon', 20.00, 400.00,
            '["fuel_diesel"]'::jsonb, '["diesel"]'::jsonb)
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
    applicable_categories, '[]'::jsonb, '[]'::jsonb, '[]'::jsonb,
    60, 3, 3, '["checkout"]'::jsonb, true, false, 0, 'FUEL',
    'activity-board-v2-potential-member', '["gas_station"]'::jsonb,
    applicable_fuel_types, '[]'::jsonb, false
from templates
on conflict (coupon_template_id) do update
set coupon_name = excluded.coupon_name,
    face_value = excluded.face_value,
    min_spend_amount = excluded.min_spend_amount,
    applicable_categories = excluded.applicable_categories,
    valid_days = excluded.valid_days,
    issue_quantity = excluded.issue_quantity,
    per_customer_limit = excluded.per_customer_limit,
    source_activity = excluded.source_activity,
    applicable_station_types = excluded.applicable_station_types,
    applicable_fuel_types = excluded.applicable_fuel_types,
    updated_at = now();

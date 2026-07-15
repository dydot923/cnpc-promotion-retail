insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-a5-recharge-coupon', 'PROMOTION', 'data/activity-board.xlsx', 4, 0, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with templates(
    coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, excluded_categories, valid_days, coupon_kind
) as (
    values
    ('a5-day10-gasoline-12', 'A5 Day10 12 yuan gasoline coupon', 12.00, 200.00,
        '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 60, 'FUEL'),
    ('a5-day10-store-12', 'A5 Day10 12 yuan convenience store coupon', 12.00, 50.00,
        '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 60, 'STORE'),
    ('a5-day10-carwash-10', 'A5 Day10 10 yuan car wash coupon', 10.00, 11.00,
        '["car_wash"]'::jsonb, '[]'::jsonb, 30, 'SERVICE'),
    ('a5-day10-highgrade-gasoline-15', 'A5 Day10 15 yuan high-grade gasoline coupon', 15.00, 200.00,
        '["fuel_high_grade_gasoline"]'::jsonb, '[]'::jsonb, 60, 'FUEL')
)
insert into coupon_template (
    coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, excluded_categories, applicable_product_codes,
    excluded_product_codes, valid_days, issue_quantity, per_customer_limit,
    redeem_channels, member_only, stackable, discount_rate, coupon_kind,
    source_activity, is_demo_data
)
select
    coupon_template_id, coupon_name, face_value::numeric, min_spend_amount::numeric,
    applicable_categories, excluded_categories, '[]'::jsonb,
    '[]'::jsonb, valid_days, 0, 0, '["checkout"]'::jsonb,
    true, false, 0, coupon_kind, 'activity-board-v2-a5', false
from templates
on conflict (coupon_template_id) do update
set coupon_name = excluded.coupon_name,
    face_value = excluded.face_value,
    min_spend_amount = excluded.min_spend_amount,
    applicable_categories = excluded.applicable_categories,
    excluded_categories = excluded.excluded_categories,
    valid_days = excluded.valid_days,
    member_only = excluded.member_only,
    stackable = excluded.stackable,
    coupon_kind = excluded.coupon_kind,
    source_activity = excluded.source_activity,
    is_demo_data = excluded.is_demo_data,
    updated_at = now();

with rules(rule_id, activity_name, priority, member_levels, min_recharge_amount, coupons) as (
    values
    (
        'abv2-a5-day10-super-1000-normal',
        'A5 Day10 Super Recharge 1000 Normal',
        40,
        '[]'::jsonb,
        1000.00,
        jsonb_build_array(
            jsonb_build_object('couponTemplateId', 'a5-day10-gasoline-12', 'couponName', 'A5 Day10 12 yuan gasoline coupon', 'amount', 12.00, 'quantity', 2, 'useThreshold', 200.00, 'validDays', 60),
            jsonb_build_object('couponTemplateId', 'a5-day10-store-12', 'couponName', 'A5 Day10 12 yuan convenience store coupon', 'amount', 12.00, 'quantity', 3, 'useThreshold', 50.00, 'validDays', 60),
            jsonb_build_object('couponTemplateId', 'a5-day10-carwash-10', 'couponName', 'A5 Day10 10 yuan car wash coupon', 'amount', 10.00, 'quantity', 3, 'useThreshold', 11.00, 'validDays', 30)
        )
    ),
    (
        'abv2-a5-day10-super-1000-gold',
        'A5 Day10 Super Recharge 1000 Gold+',
        35,
        '["gold"]'::jsonb,
        1000.00,
        jsonb_build_array(
            jsonb_build_object('couponTemplateId', 'a5-day10-gasoline-12', 'couponName', 'A5 Day10 12 yuan gasoline coupon', 'amount', 12.00, 'quantity', 2, 'useThreshold', 200.00, 'validDays', 60),
            jsonb_build_object('couponTemplateId', 'a5-day10-store-12', 'couponName', 'A5 Day10 12 yuan convenience store coupon', 'amount', 12.00, 'quantity', 3, 'useThreshold', 50.00, 'validDays', 60),
            jsonb_build_object('couponTemplateId', 'a5-day10-carwash-10', 'couponName', 'A5 Day10 10 yuan car wash coupon', 'amount', 10.00, 'quantity', 3, 'useThreshold', 11.00, 'validDays', 30),
            jsonb_build_object('couponTemplateId', 'a5-day10-highgrade-gasoline-15', 'couponName', 'A5 Day10 15 yuan high-grade gasoline coupon', 'amount', 15.00, 'quantity', 1, 'useThreshold', 200.00, 'validDays', 60)
        )
    ),
    (
        'abv2-a5-day10-super-2000-normal',
        'A5 Day10 Super Recharge 2000 Normal',
        30,
        '[]'::jsonb,
        2000.00,
        jsonb_build_array(
            jsonb_build_object('couponTemplateId', 'a5-day10-gasoline-12', 'couponName', 'A5 Day10 12 yuan gasoline coupon', 'amount', 12.00, 'quantity', 5, 'useThreshold', 200.00, 'validDays', 60),
            jsonb_build_object('couponTemplateId', 'a5-day10-store-12', 'couponName', 'A5 Day10 12 yuan convenience store coupon', 'amount', 12.00, 'quantity', 6, 'useThreshold', 50.00, 'validDays', 60),
            jsonb_build_object('couponTemplateId', 'a5-day10-carwash-10', 'couponName', 'A5 Day10 10 yuan car wash coupon', 'amount', 10.00, 'quantity', 6, 'useThreshold', 11.00, 'validDays', 30)
        )
    ),
    (
        'abv2-a5-day10-super-2000-gold',
        'A5 Day10 Super Recharge 2000 Gold+',
        25,
        '["gold"]'::jsonb,
        2000.00,
        jsonb_build_array(
            jsonb_build_object('couponTemplateId', 'a5-day10-gasoline-12', 'couponName', 'A5 Day10 12 yuan gasoline coupon', 'amount', 12.00, 'quantity', 5, 'useThreshold', 200.00, 'validDays', 60),
            jsonb_build_object('couponTemplateId', 'a5-day10-store-12', 'couponName', 'A5 Day10 12 yuan convenience store coupon', 'amount', 12.00, 'quantity', 6, 'useThreshold', 50.00, 'validDays', 60),
            jsonb_build_object('couponTemplateId', 'a5-day10-carwash-10', 'couponName', 'A5 Day10 10 yuan car wash coupon', 'amount', 10.00, 'quantity', 6, 'useThreshold', 11.00, 'validDays', 30),
            jsonb_build_object('couponTemplateId', 'a5-day10-highgrade-gasoline-15', 'couponName', 'A5 Day10 15 yuan high-grade gasoline coupon', 'amount', 15.00, 'quantity', 2, 'useThreshold', 200.00, 'validDays', 60)
        )
    )
), prepared as (
    select
        rule_id,
        jsonb_build_object(
            'ruleId', rule_id,
            'activityName', activity_name,
            'ruleType', 'GIFT_COUPON',
            'priority', priority,
            'exclusiveGroup', 'recharge_coupon',
            'stackable', false,
            'status', 'CONFIRMED',
            'condition', jsonb_build_object(
                'productCodes', jsonb_build_array(),
                'excludedCategories', jsonb_build_array(),
                'fuelTypes', jsonb_build_array(),
                'stationTypes', jsonb_build_array(),
                'daysOfMonth', jsonb_build_array(),
                'startDate', null,
                'endDate', null,
                'minCartAmount', 0,
                'minFuelAmount', 0,
                'memberRequired', true,
                'minInventoryQuantity', 0,
                'dateCondition', jsonb_build_object(
                    'type', 'MONTHLY_DATES',
                    'dates', jsonb_build_array(10, 20, 30)
                ),
                'timeRangeCondition', null,
                'stationProvinces', jsonb_build_array(),
                'memberLevels', member_levels,
                'birthdayMonthRequired', false,
                'memberTags', jsonb_build_array(),
                'minFuelVolume', 0,
                'includedCategories', jsonb_build_array(),
                'minProductQuantity', 0,
                'minRechargeAmount', min_recharge_amount
            ),
            'benefit', jsonb_build_object(
                'type', 'GIFT_COUPON',
                'giftCouponTiers', jsonb_build_array(
                    jsonb_build_object(
                        'thresholdAmount', min_recharge_amount,
                        'coupons', coupons
                    )
                )
            ),
            'version', 'activity-board-v2-a5-recharge'
        ) as rule_json
    from rules
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select
    'draft-' || rule_id, rule_id, 'activity-board-v2-a5-recharge-coupon',
    'activity-board.xlsx', 9, 'GIFT_COUPON', 'CONFIRMED',
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
    'ver-' || rule_id || '-v28', rule_id, source_import_id, source_sheet_name,
    source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2',
    now(), 'flyway-activity-board-v2', 'A5 recharge coupon rule confirmed by V28', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-a5-recharge-coupon'
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id,
    change_reason, is_demo_data
)
select
    'audit-' || rule_id || '-v28', rule_id, 'CONFIRMED', null, 'CONFIRMED',
    'flyway-activity-board-v2', 'A5 recharge coupon rule confirmed by V28', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-a5-recharge-coupon'
on conflict (audit_id) do nothing;

insert into promotion_date_trigger (
    activity_code, rule_id, trigger_type, days_of_month,
    start_date, end_date, time_from, time_to, description,
    source_sheet_name, source_row_number
)
values
    ('activity-board-v2-a5-1000-normal', 'abv2-a5-day10-super-1000-normal', 'MONTHLY_DATES', '[10,20,30]'::jsonb, null, null, null, null, 'A5 day10 recharge coupon', 'activity-board.xlsx', 9),
    ('activity-board-v2-a5-1000-gold', 'abv2-a5-day10-super-1000-gold', 'MONTHLY_DATES', '[10,20,30]'::jsonb, null, null, null, null, 'A5 day10 recharge coupon gold+', 'activity-board.xlsx', 9),
    ('activity-board-v2-a5-2000-normal', 'abv2-a5-day10-super-2000-normal', 'MONTHLY_DATES', '[10,20,30]'::jsonb, null, null, null, null, 'A5 day10 recharge coupon', 'activity-board.xlsx', 9),
    ('activity-board-v2-a5-2000-gold', 'abv2-a5-day10-super-2000-gold', 'MONTHLY_DATES', '[10,20,30]'::jsonb, null, null, null, null, 'A5 day10 recharge coupon gold+', 'activity-board.xlsx', 9)
on conflict (activity_code, rule_id, trigger_type) do update
set days_of_month = excluded.days_of_month,
    description = excluded.description;

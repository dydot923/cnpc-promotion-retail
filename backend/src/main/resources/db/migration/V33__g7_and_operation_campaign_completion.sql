insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values
    ('activity-board-v2-g7-safe-price-confirmation', 'PROMOTION', 'data/activity-board.xlsx', 69, 0, 0, 0, 0),
    ('activity-board-v2-small-recharge-666', 'PROMOTION', 'data/activity-board.xlsx', 1, 0, 0, 0, 0),
    ('activity-board-v2-operation-coupons', 'PROMOTION', 'data/activity-board.xlsx', 26, 0, 0, 0, 0)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

with resolved(product_code, fixed_price, source_row, pricing_basis) as (
    values
    ('70485561', 4.25::numeric, 35, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70485564', 4.25::numeric, 38, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70497727', 4.25::numeric, 40, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70000703', 4.50::numeric, 53, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70473922', 7.60::numeric, 55, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70473923', 7.60::numeric, 56, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70166517', 4.60::numeric, 69, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70392421', 4.60::numeric, 74, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70238841', 4.50::numeric, 79, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70034028', 4.50::numeric, 80, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70345483', 3.60::numeric, 82, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70345484', 3.60::numeric, 83, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70289407', 5.85::numeric, 87, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70223210', 5.85::numeric, 88, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70042192', 5.53::numeric, 89, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70289406', 5.85::numeric, 91, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70442694', 5.85::numeric, 93, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70393623', 4.50::numeric, 95, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70001657', 4.50::numeric, 96, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70373318', 10.47::numeric, 102, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70410278', 10.47::numeric, 103, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70539251', 5.48::numeric, 106, '8PCT_MARGIN_FLOOR'),
    ('70539248', 3.07::numeric, 107, '8PCT_MARGIN_FLOOR'),
    ('70539245', 4.65::numeric, 111, '8PCT_MARGIN_FLOOR'),
    ('70003387', 5.40::numeric, 116, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70001498', 5.40::numeric, 118, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70273067', 5.40::numeric, 119, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70389756', 5.40::numeric, 121, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70389757', 5.40::numeric, 122, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70229988', 6.75::numeric, 123, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70229989', 6.75::numeric, 124, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70268751', 4.80::numeric, 125, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70309950', 4.80::numeric, 126, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70301963', 4.80::numeric, 127, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70520677', 4.80::numeric, 128, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70001573', 3.15::numeric, 129, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70053934', 3.15::numeric, 130, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70025042', 3.26::numeric, 131, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70025045', 3.15::numeric, 132, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70113413', 4.75::numeric, 133, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70579325', 4.20::numeric, 138, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70583085', 5.10::numeric, 141, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70583083', 5.10::numeric, 142, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70583084', 5.10::numeric, 143, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70583087', 5.10::numeric, 144, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70583086', 5.10::numeric, 145, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70341473', 12.93::numeric, 207, '8PCT_MARGIN_FLOOR'),
    ('70341451', 24.33::numeric, 211, '8PCT_MARGIN_FLOOR'),
    ('70341456', 27.40::numeric, 212, '8PCT_MARGIN_FLOOR'),
    ('70674069', 68.40::numeric, 225, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70674241', 75.05::numeric, 226, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70006153', 119.51::numeric, 240, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70360371', 101.11::numeric, 241, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70045490', 38.26::numeric, 242, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70539242', 5.11::numeric, 256, '8PCT_MARGIN_FLOOR'),
    ('70430694', 159.78::numeric, 285, '8PCT_MARGIN_FLOOR'),
    ('70329279', 6.91::numeric, 308, '8PCT_MARGIN_FLOOR'),
    ('70329278', 6.92::numeric, 309, '8PCT_MARGIN_FLOOR'),
    ('70329277', 6.92::numeric, 310, '8PCT_MARGIN_FLOOR'),
    ('70329276', 6.91::numeric, 319, '8PCT_MARGIN_FLOOR'),
    ('70000654', 4.00::numeric, 366, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70205085', 7.11::numeric, 376, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70205086', 7.11::numeric, 377, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('414018', 56.60::numeric, 402, '8PCT_MARGIN_FLOOR'),
    ('414055', 56.87::numeric, 405, '8PCT_MARGIN_FLOOR'),
    ('413980', 281.78::numeric, 435, '8PCT_MARGIN_FLOOR'),
    ('70102098', 4.40::numeric, 469, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70102099', 4.40::numeric, 470, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR'),
    ('70154231', 4.40::numeric, 471, 'RESOURCE_DISCOUNT_WITH_8PCT_FLOOR')
), updated as (
    update promotion_rule_draft draft
    set status = 'CONFIRMED',
        source_row_number = resolved.source_row,
        benefit_json = jsonb_set(draft.benefit_json, '{fixedPrice}', to_jsonb(resolved.fixed_price), true),
        rule_json = jsonb_set(
            jsonb_set(
                jsonb_set(
                    jsonb_set(draft.rule_json, '{status}', '"CONFIRMED"'::jsonb, true),
                    '{activityName}', to_jsonb('G7 safe-price single item promotion-' || resolved.product_code), true),
                '{benefit,fixedPrice}', to_jsonb(resolved.fixed_price), true),
            '{pricingBasis}', to_jsonb(resolved.pricing_basis), true),
        updated_at = now()
    from resolved
    where draft.source_import_id = 'activity-board-v2-g7-resolution'
      and draft.rule_id = 'audit-personalized-fixed-' || resolved.product_code
      and not draft.manual_locked
    returning draft.*
)
insert into promotion_rule_version (
    version_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, rule_json, created_by, confirmed_at, confirmed_by,
    change_reason, is_demo_data
)
select
    'ver-' || rule_id || '-v33', rule_id, 'activity-board-v2-g7-safe-price-confirmation',
    source_sheet_name, source_row_number, rule_type, status, rule_json,
    'flyway-activity-board-v2', now(), 'flyway-activity-board-v2',
    'G7 pending zero-price draft confirmed with activity-board resource discount and 8% gross-margin floor',
    false
from updated
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id,
    change_reason, is_demo_data
)
select
    'audit-' || rule_id || '-v33', rule_id, 'CONFIRMED', 'DRAFT', 'CONFIRMED',
    'flyway-activity-board-v2',
    'G7 pending zero-price draft confirmed with activity-board resource discount and 8% gross-margin floor',
    false
from promotion_rule_draft
where rule_id in (
    select 'audit-personalized-fixed-' || product_code
    from (values
        ('70485561'), ('70485564'), ('70497727'), ('70000703'), ('70473922'), ('70473923'),
        ('70166517'), ('70392421'), ('70238841'), ('70034028'), ('70345483'), ('70345484'),
        ('70289407'), ('70223210'), ('70042192'), ('70289406'), ('70442694'), ('70393623'),
        ('70001657'), ('70373318'), ('70410278'), ('70539251'), ('70539248'), ('70539245'),
        ('70003387'), ('70001498'), ('70273067'), ('70389756'), ('70389757'), ('70229988'),
        ('70229989'), ('70268751'), ('70309950'), ('70301963'), ('70520677'), ('70001573'),
        ('70053934'), ('70025042'), ('70025045'), ('70113413'), ('70579325'), ('70583085'),
        ('70583083'), ('70583084'), ('70583087'), ('70583086'), ('70341473'), ('70341451'),
        ('70341456'), ('70674069'), ('70674241'), ('70006153'), ('70360371'), ('70045490'),
        ('70539242'), ('70430694'), ('70329279'), ('70329278'), ('70329277'), ('70329276'),
        ('70000654'), ('70205085'), ('70205086'), ('414018'), ('414055'), ('413980'),
        ('70102098'), ('70102099'), ('70154231')
    ) as codes(product_code)
)
on conflict (audit_id) do nothing;

with templates(
    coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, excluded_categories, valid_days, coupon_kind, source_activity
) as (
    values
    ('small-recharge-gasoline-10', 'Small recharge 10 yuan gasoline coupon', 10.00, 200.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 60, 'FUEL', 'activity-board-v2-small-recharge-666'),
    ('small-recharge-store-12', 'Small recharge 12 yuan store coupon', 12.00, 50.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 60, 'STORE', 'activity-board-v2-small-recharge-666'),
    ('rfm-gasoline-20', 'RFM 20 yuan gasoline coupon', 20.00, 200.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 60, 'FUEL', 'activity-board-v2-rfm-recovery'),
    ('rfm-diesel-20', 'RFM 20 yuan diesel coupon', 20.00, 200.00, '["fuel_diesel"]'::jsonb, '[]'::jsonb, 60, 'FUEL', 'activity-board-v2-rfm-recovery'),
    ('rfm-store-12', 'RFM 12 yuan store coupon', 12.00, 50.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 60, 'STORE', 'activity-board-v2-rfm-recovery'),
    ('birthday-gasoline-10', 'Birthday 10 yuan gasoline coupon', 10.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-birthday'),
    ('birthday-store-12', 'Birthday 12 yuan store coupon', 12.00, 50.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 30, 'STORE', 'activity-board-v2-birthday'),
    ('birthday-carwash-10', 'Birthday 10 yuan car wash coupon', 10.00, 11.00, '["car_wash"]'::jsonb, '[]'::jsonb, 30, 'SERVICE', 'activity-board-v2-birthday'),
    ('signin-gasoline-2', 'Sign-in 2 yuan gasoline coupon', 2.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-sign-in'),
    ('signin-store-2', 'Sign-in 2 yuan store coupon', 2.00, 12.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 30, 'STORE', 'activity-board-v2-sign-in'),
    ('signin-gasoline-5', 'Sign-in 5 yuan gasoline coupon', 5.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-sign-in'),
    ('signin-store-6', 'Sign-in 6 yuan store coupon', 6.00, 12.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 30, 'STORE', 'activity-board-v2-sign-in'),
    ('signin-gasoline-8', 'Sign-in 8 yuan gasoline coupon', 8.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-sign-in'),
    ('signin-store-12', 'Sign-in 12 yuan store coupon', 12.00, 50.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 30, 'STORE', 'activity-board-v2-sign-in'),
    ('group-buy-gasoline-2', 'Group-buy 2 yuan gasoline coupon', 2.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-group-buy'),
    ('group-buy-gasoline-3', 'Group-buy 3 yuan gasoline coupon', 3.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-group-buy'),
    ('group-buy-gasoline-5', 'Group-buy 5 yuan gasoline coupon', 5.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-group-buy'),
    ('group-buy-gasoline-7', 'Group-buy 7 yuan gasoline coupon', 7.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-group-buy'),
    ('group-buy-gasoline-8', 'Group-buy 8 yuan gasoline coupon', 8.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-group-buy'),
    ('group-buy-gasoline-12', 'Group-buy 12 yuan gasoline coupon', 12.00, 100.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-group-buy'),
    ('group-buy-store-12', 'Group-buy 12 yuan store coupon', 12.00, 50.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 30, 'STORE', 'activity-board-v2-group-buy'),
    ('industry-gasoline-10', 'Industry certification 10 yuan gasoline coupon', 10.00, 200.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-industry-certification'),
    ('industry-store-6', 'Industry certification 6 yuan store coupon', 6.00, 30.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 30, 'STORE', 'activity-board-v2-industry-certification'),
    ('ecommerce-store-6', 'E-commerce 6 yuan store coupon', 6.00, 30.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 30, 'STORE', 'activity-board-v2-ecommerce'),
    ('ecommerce-store-12', 'E-commerce 12 yuan store coupon', 12.00, 50.00, '["store"]'::jsonb, '["cigarette","fertilizer"]'::jsonb, 30, 'STORE', 'activity-board-v2-ecommerce'),
    ('ecommerce-gasoline-10', 'E-commerce 10 yuan gasoline coupon', 10.00, 200.00, '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 30, 'FUEL', 'activity-board-v2-ecommerce')
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
    applicable_categories, excluded_categories, '[]'::jsonb, '[]'::jsonb,
    valid_days, 0, 0, '["checkout","operation"]'::jsonb, true, false, 0,
    coupon_kind, source_activity, '[]'::jsonb, '[]'::jsonb, '[]'::jsonb, false
from templates
on conflict (coupon_template_id) do update
set coupon_name = excluded.coupon_name,
    face_value = excluded.face_value,
    min_spend_amount = excluded.min_spend_amount,
    applicable_categories = excluded.applicable_categories,
    excluded_categories = excluded.excluded_categories,
    valid_days = excluded.valid_days,
    redeem_channels = excluded.redeem_channels,
    member_only = excluded.member_only,
    stackable = excluded.stackable,
    coupon_kind = excluded.coupon_kind,
    source_activity = excluded.source_activity,
    is_demo_data = excluded.is_demo_data,
    updated_at = now();

with prepared as (
    select
        'abv2-a6-small-recharge-666' as rule_id,
        jsonb_build_object(
            'ruleId', 'abv2-a6-small-recharge-666',
            'activityName', 'A6 small recharge 666 coupon package',
            'ruleType', 'GIFT_COUPON',
            'priority', 38,
            'exclusiveGroup', 'recharge_coupon',
            'stackable', false,
            'status', 'CONFIRMED',
            'condition', jsonb_build_object(
                'memberRequired', true,
                'stationProvinces', jsonb_build_array('新疆'),
                'minRechargeAmount', 666.00,
                'dateCondition', jsonb_build_object(
                    'type', 'EXCLUDE_MONTHLY_DATES',
                    'dates', jsonb_build_array(10, 20, 30)
                )
            ),
            'benefit', jsonb_build_object(
                'type', 'GIFT_COUPON',
                'giftCouponTiers', jsonb_build_array(
                    jsonb_build_object('thresholdAmount', 666.00, 'coupons', jsonb_build_array(
                        jsonb_build_object('couponTemplateId', 'small-recharge-gasoline-10',
                            'couponName', 'Small recharge 10 yuan gasoline coupon',
                            'amount', 10.00, 'quantity', 3, 'useThreshold', 200.00, 'validDays', 60),
                        jsonb_build_object('couponTemplateId', 'small-recharge-store-12',
                            'couponName', 'Small recharge 12 yuan store coupon',
                            'amount', 12.00, 'quantity', 3, 'useThreshold', 50.00, 'validDays', 60)
                    ))
                )
            ),
            'version', 'activity-board-v2'
        ) as rule_json
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select
    'draft-' || rule_id, rule_id, 'activity-board-v2-small-recharge-666',
    'integrated-marketing-board', 13, 'GIFT_COUPON', 'CONFIRMED',
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
    'ver-' || rule_id || '-v33', rule_id, source_import_id, source_sheet_name,
    source_row_number, rule_type, status, rule_json, 'flyway-activity-board-v2',
    now(), 'flyway-activity-board-v2', 'Complete small recharge 666 coupon package with non-day10 exclusion', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-small-recharge-666'
on conflict (version_id) do nothing;

insert into promotion_rule_audit_log (
    audit_id, rule_id, action, status_before, status_after, operator_id,
    change_reason, is_demo_data
)
select
    'audit-' || rule_id || '-v33', rule_id, 'CONFIRMED', null, 'CONFIRMED',
    'flyway-activity-board-v2', 'Complete small recharge 666 coupon package with non-day10 exclusion', false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-small-recharge-666'
on conflict (audit_id) do nothing;

insert into promotion_date_trigger (
    activity_code, rule_id, trigger_type, days_of_month, start_date, end_date,
    time_from, time_to, description, source_sheet_name, source_row_number
)
values (
    'activity-board-v2-a6', 'abv2-a6-small-recharge-666', 'EXCLUDE_MONTHLY_DATES',
    '[10,20,30]'::jsonb, null, null, null, null,
    'Small recharge 666 excludes day10 super recharge dates', 'integrated-marketing-board', 13
)
on conflict (activity_code, rule_id, trigger_type) do update
set days_of_month = excluded.days_of_month,
    description = excluded.description;

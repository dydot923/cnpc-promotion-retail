with demo_exchange_rules as (
    select *
    from (values
        ('demo-exchange-gasoline-water', '演示-汽油满180元换购格桑泉水', '70545526', array['GASOLINE']::text[], 180.00::numeric, 2.00::numeric, 4),
        ('demo-exchange-diesel-redbull', '演示-柴油满300元换购红牛', '70356177', array['DIESEL']::text[], 300.00::numeric, 10.00::numeric, 1),
        ('demo-exchange-fuel-long-haul-bundle', '演示-汽柴油满200元换购LNG长途包代表商品', '70453858', array['GASOLINE','DIESEL']::text[], 200.00::numeric, 5.00::numeric, 1)
    ) as rules(rule_id, activity_name, product_code, fuel_types, min_fuel_amount, exchange_price, exchange_quantity)
),
prepared_exchange_rules as (
    select
        rule_id,
        activity_name,
        jsonb_build_object(
            'productCodes', jsonb_build_array(product_code),
            'excludedCategories', '[]'::jsonb,
            'fuelTypes', to_jsonb(fuel_types),
            'stationTypes', jsonb_build_array('gas_station'),
            'daysOfMonth', '[]'::jsonb,
            'minCartAmount', 0,
            'minFuelAmount', min_fuel_amount,
            'memberRequired', false,
            'minInventoryQuantity', 0,
            'stationProvinces', '[]'::jsonb,
            'memberLevels', '[]'::jsonb,
            'birthdayMonthRequired', false,
            'minFuelVolume', 0
        ) as condition_json,
        jsonb_build_object(
            'type', 'EXCHANGE_PURCHASE',
            'exchangePrice', exchange_price,
            'exchangeQuantity', exchange_quantity
        ) as benefit_json
    from demo_exchange_rules
)
insert into promotion_rule_draft (
    draft_id,
    rule_id,
    source_import_id,
    source_sheet_name,
    source_row_number,
    rule_type,
    status,
    condition_json,
    benefit_json,
    rule_json,
    manual_locked,
    created_by
)
select
    'draft-' || rule_id,
    rule_id,
    'demo-seed-v1',
    'DEMO_EXCHANGE',
    row_number() over (order by rule_id),
    'EXCHANGE_PURCHASE',
    'CONFIRMED',
    condition_json,
    benefit_json,
    jsonb_build_object(
        'ruleId', rule_id,
        'activityName', activity_name,
        'ruleType', 'EXCHANGE_PURCHASE',
        'priority', 70,
        'exclusiveGroup', 'exchange_purchase',
        'stackable', true,
        'status', 'CONFIRMED',
        'condition', condition_json,
        'benefit', benefit_json,
        'version', 'demo-exchange-v1'
    ),
    true,
    'flyway-demo'
from prepared_exchange_rules
on conflict (rule_id) do update
set status = excluded.status,
    condition_json = excluded.condition_json,
    benefit_json = excluded.benefit_json,
    rule_json = excluded.rule_json,
    manual_locked = excluded.manual_locked,
    updated_at = now();

with demo_bundle_rules as (
    select *
    from (values
        ('demo-bundle-cng-water-drink', '演示-CNG水饮包组合价', 'bundle-cng-water-drink', array['70251989','70356177']::text[]),
        ('demo-bundle-lng-long-haul', '演示-LNG长途包组合价', 'bundle-lng-long-haul', array['70453858','70341453','70545526']::text[])
    ) as rules(rule_id, activity_name, bundle_id, product_codes)
),
prepared_bundle_rules as (
    select
        rule_id,
        activity_name,
        bundle_id,
        jsonb_build_object(
            'productCodes', to_jsonb(product_codes),
            'excludedCategories', '[]'::jsonb,
            'fuelTypes', '[]'::jsonb,
            'stationTypes', '[]'::jsonb,
            'daysOfMonth', '[]'::jsonb,
            'minCartAmount', 0,
            'minFuelAmount', 0,
            'memberRequired', false,
            'minInventoryQuantity', 0,
            'stationProvinces', '[]'::jsonb,
            'memberLevels', '[]'::jsonb,
            'birthdayMonthRequired', false,
            'minFuelVolume', 0
        ) as condition_json,
        jsonb_build_object(
            'type', 'BUNDLE_PRICE',
            'bundleId', bundle_id,
            'bundleItems', '[]'::jsonb,
            'bundlePrice', 0
        ) as benefit_json
    from demo_bundle_rules
)
insert into promotion_rule_draft (
    draft_id,
    rule_id,
    source_import_id,
    source_sheet_name,
    source_row_number,
    rule_type,
    status,
    condition_json,
    benefit_json,
    rule_json,
    manual_locked,
    created_by
)
select
    'draft-' || rule_id,
    rule_id,
    'demo-seed-v1',
    'DEMO_BUNDLE',
    row_number() over (order by rule_id),
    'BUNDLE_PRICE',
    'CONFIRMED',
    condition_json,
    benefit_json,
    jsonb_build_object(
        'ruleId', rule_id,
        'activityName', activity_name,
        'ruleType', 'BUNDLE_PRICE',
        'priority', 60,
        'exclusiveGroup', 'direct_discount',
        'stackable', false,
        'status', 'CONFIRMED',
        'condition', condition_json,
        'benefit', benefit_json,
        'version', 'demo-bundle-v1'
    ),
    true,
    'flyway-demo'
from prepared_bundle_rules
on conflict (rule_id) do update
set status = excluded.status,
    condition_json = excluded.condition_json,
    benefit_json = excluded.benefit_json,
    rule_json = excluded.rule_json,
    manual_locked = excluded.manual_locked,
    updated_at = now();

insert into promotion_rule_audit_log (
    audit_id,
    rule_id,
    action,
    status_before,
    status_after,
    operator_id,
    change_reason
)
select
    'audit-seed-' || rule_id,
    rule_id,
    'CONFIRMED',
    null,
    'CONFIRMED',
    'flyway-demo',
    'Seeded demo promotion rule'
from promotion_rule_draft
where source_import_id = 'demo-seed-v1'
on conflict (audit_id) do nothing;

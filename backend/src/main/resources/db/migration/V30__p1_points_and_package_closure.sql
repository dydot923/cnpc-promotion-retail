create table if not exists points_lottery_draw (
    draw_id varchar(128) primary key,
    member_code varchar(64) not null,
    activity_code varchar(128) not null,
    points_cost integer not null default 500,
    prize_type varchar(64) not null default 'NO_PRIZE',
    prize_coupon_id varchar(128),
    result_label varchar(256),
    business_date date not null,
    station_code varchar(64),
    operator_id varchar(128),
    operator_name varchar(128),
    created_at timestamptz not null default now()
);
create index if not exists idx_points_lottery_draw_member on points_lottery_draw (member_code, created_at desc);
create index if not exists idx_points_lottery_draw_activity on points_lottery_draw (activity_code, business_date);

insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-p1-points-package-closure', 'PROMOTION', 'data/activity-board.xlsx', 5, 0, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

insert into promotion_date_trigger (
    activity_code, rule_id, trigger_type, days_of_month, start_date, end_date,
    time_from, time_to, description, source_sheet_name, source_row_number
)
values
    ('activity-board-v2-g2-points-exchange', 'abv2-g2-points-exchange-90-off',
        'MONTHLY_DATES', '[9,19,29]'::jsonb, null, null, null, null,
        'Points exchange 90% off coupon trigger days', '非非促销（统建）', 6),
    ('activity-board-v2-g2-points-lottery', 'abv2-g2-points-lottery',
        'MONTHLY_DATES', '[1,9,10,11,19,20,21,29,30,31]'::jsonb, null, null, null, null,
        '500 points lottery draw active days', '非非促销（统建）', 6)
on conflict (activity_code, rule_id, trigger_type) do update
set days_of_month = excluded.days_of_month,
    description = excluded.description,
    source_sheet_name = excluded.source_sheet_name,
    source_row_number = excluded.source_row_number;

with templates(
    coupon_template_id, coupon_name, face_value, min_spend_amount,
    applicable_categories, excluded_categories, valid_days, discount_rate,
    coupon_kind, source_activity
) as (
    values
    ('points-exchange-90-off', 'Points exchange 90% off coupon', 0.00, 0.00,
        '["store"]'::jsonb, '["cigarette","fertilizer","香烟","化肥"]'::jsonb, 30, 0.9000,
        'DISCOUNT', 'activity-board-v2-g2'),
    ('points-lottery-store-10', 'Points lottery 10 yuan store coupon', 10.00, 50.00,
        '["store"]'::jsonb, '["cigarette","fertilizer","香烟","化肥"]'::jsonb, 30, 0.0000,
        'STORE', 'activity-board-v2-g2'),
    ('xinjiang-tour-card-gasoline-100', 'Xinjiang travel card 100 yuan gasoline coupon', 100.00, 200.00,
        '["fuel_gasoline"]'::jsonb, '[]'::jsonb, 60, 0.0000,
        'FUEL', 'activity-board-v2-tour-card'),
    ('lng-benefit-15', 'LNG package 15 yuan LNG coupon', 15.00, 1000.00,
        '["fuel_lng","LNG"]'::jsonb, '[]'::jsonb, 365, 0.0000,
        'FUEL', 'activity-board-v2-lng-cng-package'),
    ('cng-benefit-3', 'CNG package 3 yuan CNG coupon', 3.00, 30.00,
        '["fuel_cng","CNG"]'::jsonb, '[]'::jsonb, 365, 0.0000,
        'FUEL', 'activity-board-v2-lng-cng-package')
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
    valid_days, 0, 0, '["checkout"]'::jsonb, true, false,
    discount_rate::numeric, coupon_kind, source_activity, '[]'::jsonb,
    '[]'::jsonb, '[]'::jsonb, false
from templates
on conflict (coupon_template_id) do update
set coupon_name = excluded.coupon_name,
    face_value = excluded.face_value,
    min_spend_amount = excluded.min_spend_amount,
    applicable_categories = excluded.applicable_categories,
    excluded_categories = excluded.excluded_categories,
    valid_days = excluded.valid_days,
    discount_rate = excluded.discount_rate,
    coupon_kind = excluded.coupon_kind,
    source_activity = excluded.source_activity,
    member_only = excluded.member_only,
    stackable = excluded.stackable,
    is_demo_data = excluded.is_demo_data,
    updated_at = now();

insert into benefit_package (
    package_code, package_name, sales_channel, sale_price,
    status, source_sheet_name, source_row_number
)
values (
    'benefit-package-xinjiang-tour-card-2026',
    'Xinjiang travel card 2026',
    'tour-card-station',
    0.00,
    'ACTIVE',
    '参考4-“一卡通”销售站点明细',
    9001
)
on conflict (package_code) do update
set package_name = excluded.package_name,
    sales_channel = excluded.sales_channel,
    sale_price = excluded.sale_price,
    status = excluded.status,
    source_sheet_name = excluded.source_sheet_name,
    source_row_number = excluded.source_row_number,
    updated_at = now();

delete from benefit_package_item
where package_code = 'benefit-package-xinjiang-tour-card-2026';

insert into benefit_package_item (
    package_code, item_name, quantity, remark, source_row_number
)
values (
    'benefit-package-xinjiang-tour-card-2026',
    '100元汽油券（满200元使用）',
    2,
    '购买新疆旅游一卡通赠2张100元汽油券',
    9002
);

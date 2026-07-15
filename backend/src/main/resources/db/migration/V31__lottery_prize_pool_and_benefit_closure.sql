create table if not exists points_lottery_prize_config (
    prize_id varchar(128) primary key,
    activity_code varchar(128) not null,
    prize_name varchar(256) not null,
    prize_type varchar(64) not null default 'NO_PRIZE',
    coupon_template_id varchar(128),
    coupon_name varchar(256),
    face_value numeric(18, 2) not null default 0,
    min_spend_amount numeric(18, 2) not null default 0,
    applicable_categories jsonb not null default '[]'::jsonb,
    excluded_categories jsonb not null default '[]'::jsonb,
    valid_days integer not null default 30,
    weight integer not null default 0,
    status varchar(32) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_points_lottery_prize_config_activity
    on points_lottery_prize_config (activity_code, status);

insert into points_lottery_prize_config (
    prize_id, activity_code, prize_name, prize_type, coupon_template_id,
    coupon_name, face_value, min_spend_amount, applicable_categories,
    excluded_categories, valid_days, weight, status
)
values
    (
        'g2-lottery-no-prize',
        'activity-board-v2-g2-points-lottery',
        'No prize',
        'NO_PRIZE',
        null,
        null,
        0.00,
        0.00,
        '[]'::jsonb,
        '[]'::jsonb,
        30,
        50,
        'ACTIVE'
    ),
    (
        'g2-lottery-store-10',
        'activity-board-v2-g2-points-lottery',
        '10 yuan store coupon',
        'COUPON',
        'points-lottery-store-10',
        'Points lottery 10 yuan store coupon',
        10.00,
        50.00,
        '["store"]'::jsonb,
        '["cigarette","fertilizer","香烟","化肥"]'::jsonb,
        30,
        50,
        'ACTIVE'
    )
on conflict (prize_id) do update
set activity_code = excluded.activity_code,
    prize_name = excluded.prize_name,
    prize_type = excluded.prize_type,
    coupon_template_id = excluded.coupon_template_id,
    coupon_name = excluded.coupon_name,
    face_value = excluded.face_value,
    min_spend_amount = excluded.min_spend_amount,
    applicable_categories = excluded.applicable_categories,
    excluded_categories = excluded.excluded_categories,
    valid_days = excluded.valid_days,
    weight = excluded.weight,
    status = excluded.status,
    updated_at = now();

insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-p1-lottery-prize-pool', 'PROMOTION', 'docs/27-doubao-促销活动落地检查清单.md', 2, 0, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

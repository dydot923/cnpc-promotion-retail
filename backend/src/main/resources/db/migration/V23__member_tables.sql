create table if not exists member_level (
    id bigserial primary key,
    level_code varchar(32) not null unique,
    level_name varchar(128) not null,
    discount_rate numeric(5, 4) not null default 1.0000,
    points_multiplier numeric(8, 4) not null default 1.0000,
    min_consumption numeric(18, 2) not null default 0,
    benefits jsonb not null default '[]'::jsonb,
    priority integer not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists member (
    id bigserial primary key,
    member_code varchar(64) not null unique,
    member_name varchar(128) not null,
    phone varchar(32) unique,
    level_code varchar(32) not null,
    total_points bigint not null default 0,
    available_points bigint not null default 0,
    birthday date,
    province varchar(64),
    status varchar(32) not null default 'ACTIVE',
    is_demo_data boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_member_phone on member (phone);
create index if not exists idx_member_level_code on member (level_code);
create index if not exists idx_member_status on member (status);

create index if not exists idx_coupon_holder_member_id on coupon (holder_member_id);

insert into member_level (
    level_code, level_name, discount_rate, points_multiplier, min_consumption, benefits, priority
) values
    ('normal', '普通会员', 1.0000, 1.0000, 0.00, '["基础会员价"]'::jsonb, 1),
    ('silver', '银卡会员', 0.9500, 1.5000, 1000.00, '["生日券", "专属活动"]'::jsonb, 2),
    ('gold', '金卡会员', 0.9000, 2.0000, 5000.00, '["生日券", "节日券", "专属客服"]'::jsonb, 3),
    ('platinum', '铂金会员', 0.8500, 3.0000, 20000.00, '["专属权益包", "优先服务"]'::jsonb, 4)
on conflict (level_code) do update
set level_name = excluded.level_name,
    discount_rate = excluded.discount_rate,
    points_multiplier = excluded.points_multiplier,
    min_consumption = excluded.min_consumption,
    benefits = excluded.benefits,
    priority = excluded.priority,
    updated_at = now();

insert into member (
    member_code, member_name, phone, level_code, total_points, available_points,
    birthday, province, status, is_demo_data
) values
    ('member-001', '演示金卡会员', '13900000001', 'gold', 5200, 1200, date '1990-07-08', '新疆', 'ACTIVE', true),
    ('member-002', '演示银卡会员', '13900000002', 'silver', 1800, 320, date '1994-07-18', '新疆', 'ACTIVE', true),
    ('demo-member-002', '演示省区会员2', '13900000012', 'gold', 5000, 800, date '1992-07-12', '新疆', 'ACTIVE', true),
    ('demo-member-003', '演示省区会员3', '13900000013', 'gold', 5000, 800, date '1993-07-13', '新疆', 'ACTIVE', true),
    ('demo-member-004', '演示省区会员4', '13900000014', 'gold', 5000, 800, date '1994-07-14', '新疆', 'ACTIVE', true),
    ('demo-member-005', '演示省区会员5', '13900000015', 'gold', 5000, 800, date '1995-07-15', '新疆', 'ACTIVE', true),
    ('demo-member-sequence', '演示序列券会员', '13900000016', 'gold', 5000, 800, date '1996-07-16', '新疆', 'ACTIVE', true)
on conflict (member_code) do update
set member_name = excluded.member_name,
    phone = excluded.phone,
    level_code = excluded.level_code,
    total_points = excluded.total_points,
    available_points = excluded.available_points,
    birthday = excluded.birthday,
    province = excluded.province,
    status = excluded.status,
    is_demo_data = excluded.is_demo_data,
    updated_at = now();

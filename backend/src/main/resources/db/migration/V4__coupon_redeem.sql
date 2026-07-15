create table if not exists coupon_template (
    id bigserial primary key,
    coupon_template_id varchar(128) not null unique,
    coupon_name varchar(256) not null,
    face_value numeric(18, 2) not null,
    min_spend_amount numeric(18, 2) not null default 0,
    applicable_categories jsonb not null default '[]'::jsonb,
    excluded_categories jsonb not null default '[]'::jsonb,
    applicable_product_codes jsonb not null default '[]'::jsonb,
    excluded_product_codes jsonb not null default '[]'::jsonb,
    valid_days integer not null default 0,
    issue_quantity integer not null default 0,
    per_customer_limit integer not null default 0,
    redeem_channels jsonb not null default '[]'::jsonb,
    member_only boolean not null default false,
    stackable boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists coupon (
    id bigserial primary key,
    coupon_id varchar(128) not null unique,
    coupon_template_id varchar(128) not null,
    coupon_name varchar(256) not null,
    face_value numeric(18, 2) not null,
    min_spend_amount numeric(18, 2) not null default 0,
    applicable_categories jsonb not null default '[]'::jsonb,
    excluded_categories jsonb not null default '[]'::jsonb,
    applicable_product_codes jsonb not null default '[]'::jsonb,
    excluded_product_codes jsonb not null default '[]'::jsonb,
    valid_from date,
    valid_until date,
    member_only boolean not null default false,
    stackable boolean not null default false,
    status varchar(32) not null,
    issued_at timestamp,
    used_at timestamp,
    operator_id varchar(128),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_coupon_status on coupon (status);
create index if not exists idx_coupon_template_id on coupon (coupon_template_id);
create index if not exists idx_coupon_valid_until on coupon (valid_until);

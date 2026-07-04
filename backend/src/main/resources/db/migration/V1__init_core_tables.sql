create table if not exists product (
    id bigserial primary key,
    product_code varchar(64) not null unique,
    product_name varchar(512) not null,
    barcode varchar(128),
    category varchar(128),
    is_cigarette boolean not null default false,
    is_fertilizer boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_product_barcode on product (barcode);

create table if not exists product_price (
    id bigserial primary key,
    product_code varchar(64) not null,
    execution_price numeric(18, 2) not null,
    import_version varchar(64) not null,
    effective_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

create index if not exists idx_product_price_code on product_price (product_code);

create table if not exists inventory_snapshot (
    id bigserial primary key,
    station_code varchar(64) not null default 'default',
    product_code varchar(64) not null,
    quantity numeric(18, 3) not null,
    import_version varchar(64) not null,
    snapshot_at timestamptz not null default now(),
    unique (station_code, product_code, import_version)
);

create table if not exists promotion_activity (
    id bigserial primary key,
    activity_code varchar(64) not null unique,
    activity_name varchar(256) not null,
    source_workbook varchar(256),
    source_sheet varchar(256),
    status varchar(32) not null,
    version varchar(64) not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists promotion_rule (
    id bigserial primary key,
    rule_id varchar(64) not null unique,
    activity_code varchar(64) not null,
    rule_type varchar(64) not null,
    priority integer not null default 0,
    exclusive_group varchar(128),
    stackable boolean not null default false,
    status varchar(32) not null,
    condition_json jsonb not null,
    benefit_json jsonb not null,
    version varchar(64) not null,
    manual_locked boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_promotion_rule_type_status on promotion_rule (rule_type, status);
create index if not exists idx_promotion_rule_condition_json on promotion_rule using gin (condition_json);

create table if not exists import_batch (
    id bigserial primary key,
    import_version varchar(64) not null unique,
    import_type varchar(64) not null,
    source_file varchar(512) not null,
    inserted_count integer not null default 0,
    updated_count integer not null default 0,
    skipped_count integer not null default 0,
    invalid_count integer not null default 0,
    created_at timestamptz not null default now()
);

create table if not exists import_error_row (
    id bigserial primary key,
    import_version varchar(64) not null,
    sheet_name varchar(256),
    row_number integer,
    raw_json jsonb,
    error_message varchar(1024) not null,
    created_at timestamptz not null default now()
);

create table if not exists audit_log (
    id bigserial primary key,
    operator_id varchar(128),
    action varchar(128) not null,
    target_type varchar(128) not null,
    target_id varchar(128),
    detail_json jsonb,
    created_at timestamptz not null default now()
);


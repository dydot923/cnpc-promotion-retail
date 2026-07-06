alter table import_batch
    add column if not exists warning_count integer not null default 0;

alter table import_error_row
    add column if not exists import_id varchar(64),
    add column if not exists column_name varchar(256),
    add column if not exists raw_value text,
    add column if not exists error_code varchar(64),
    add column if not exists severity varchar(32) not null default 'ERROR';

update import_error_row
set import_id = import_version
where import_id is null;

create index if not exists idx_import_error_row_import_id on import_error_row (import_id);
create index if not exists idx_import_error_row_severity on import_error_row (severity);

create table if not exists promotion_rule_draft (
    id bigserial primary key,
    draft_id varchar(128) not null unique,
    rule_id varchar(128) not null,
    source_import_id varchar(64) not null,
    source_sheet_name varchar(256) not null,
    source_row_number integer not null,
    rule_type varchar(64) not null,
    status varchar(32) not null,
    condition_json jsonb not null,
    benefit_json jsonb not null,
    rule_json jsonb not null,
    manual_locked boolean not null default false,
    created_by varchar(128) not null default 'system',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists idx_promotion_rule_draft_rule_id on promotion_rule_draft (rule_id);
create index if not exists idx_promotion_rule_draft_status on promotion_rule_draft (status);

create table if not exists promotion_rule_version (
    id bigserial primary key,
    version_id varchar(128) not null unique,
    rule_id varchar(128) not null,
    source_import_id varchar(64) not null,
    source_sheet_name varchar(256) not null,
    source_row_number integer not null,
    rule_type varchar(64) not null,
    status varchar(32) not null,
    rule_json jsonb not null,
    created_at timestamptz not null default now(),
    created_by varchar(128) not null default 'system',
    confirmed_at timestamptz,
    confirmed_by varchar(128),
    change_reason varchar(1024)
);

create index if not exists idx_promotion_rule_version_rule_status on promotion_rule_version (rule_id, status);

create table if not exists promotion_rule_audit_log (
    id bigserial primary key,
    audit_id varchar(128) not null unique,
    rule_id varchar(128) not null,
    action varchar(64) not null,
    status_before varchar(32),
    status_after varchar(32),
    operator_id varchar(128) not null default 'system',
    change_reason varchar(1024),
    created_at timestamptz not null default now()
);

create index if not exists idx_promotion_rule_audit_log_rule_id on promotion_rule_audit_log (rule_id);

create table if not exists checkout_calculation_record (
    id bigserial primary key,
    calculation_id varchar(128) not null unique,
    request_snapshot jsonb not null,
    result_snapshot jsonb not null,
    rule_version_ids jsonb not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_checkout_calculation_record_created_at on checkout_calculation_record (created_at);

alter table audit_log
    add column if not exists audit_id varchar(128),
    add column if not exists action_type varchar(128),
    add column if not exists entity_type varchar(128),
    add column if not exists entity_id varchar(128),
    add column if not exists before_snapshot jsonb,
    add column if not exists after_snapshot jsonb,
    add column if not exists operator_name varchar(128),
    add column if not exists operated_at timestamptz,
    add column if not exists reason varchar(1024);

update audit_log
set audit_id = concat('audit-', id)
where audit_id is null;

update audit_log
set action_type = action
where action_type is null;

update audit_log
set entity_type = target_type
where entity_type is null;

update audit_log
set entity_id = target_id
where entity_id is null;

update audit_log
set operated_at = created_at
where operated_at is null;

alter table audit_log
    alter column audit_id set not null,
    alter column action_type set not null,
    alter column entity_type set not null,
    alter column entity_id set not null,
    alter column operated_at set not null;

create unique index if not exists idx_audit_log_audit_id on audit_log (audit_id);
create index if not exists idx_audit_log_entity on audit_log (entity_type, entity_id);
create index if not exists idx_audit_log_action_type on audit_log (action_type);

create table if not exists checkout_confirmation (
    id bigserial primary key,
    confirmation_id varchar(128) not null unique,
    calculation_id varchar(128) not null,
    selected_candidate_id varchar(128) not null,
    selected_candidate_snapshot jsonb not null,
    operator_id varchar(128) not null default 'system',
    operator_name varchar(128),
    skipped boolean not null default false,
    confirmed_at timestamptz not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists idx_checkout_confirmation_calculation_id
    on checkout_confirmation (calculation_id);

create table if not exists replenishment_list (
    id bigserial primary key,
    list_id varchar(128) not null unique,
    list_name varchar(256) not null,
    status varchar(32) not null,
    items jsonb not null,
    total_items integer not null default 0,
    created_by varchar(128) not null default 'system',
    created_at timestamptz not null default now(),
    updated_by varchar(128) not null default 'system',
    updated_at timestamptz not null default now()
);

create index if not exists idx_replenishment_list_status
    on replenishment_list (status);

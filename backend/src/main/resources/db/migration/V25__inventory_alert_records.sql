create table if not exists inventory_alert_record (
    id bigserial primary key,
    alert_id varchar(256) not null unique,
    status varchar(32) not null default 'OPEN',
    handled_by varchar(128),
    handled_at timestamptz,
    handle_note varchar(1024),
    replenishment_list_id varchar(128),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_inventory_alert_record_status
    on inventory_alert_record (status);

create index if not exists idx_inventory_alert_record_replenishment_list
    on inventory_alert_record (replenishment_list_id);

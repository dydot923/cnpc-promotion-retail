create table if not exists checkout_transaction (
    id bigserial primary key,
    txn_no varchar(128) not null unique,
    confirmation_id varchar(128) not null unique,
    calculation_id varchar(128) not null,
    selected_candidate_id varchar(128) not null,
    total_amount numeric(18, 2) not null,
    discount_amount numeric(18, 2) not null default 0,
    payable_amount numeric(18, 2) not null,
    payment_method varchar(32),
    operator_id varchar(128),
    operator_name varchar(128),
    member_code varchar(64),
    station_code varchar(64),
    status varchar(32) not null default 'CONFIRMED',
    created_at timestamptz not null default now()
);

create index if not exists idx_checkout_transaction_calculation_id
    on checkout_transaction (calculation_id);

create index if not exists idx_checkout_transaction_member_code
    on checkout_transaction (member_code);

create index if not exists idx_checkout_transaction_station_code
    on checkout_transaction (station_code);

create index if not exists idx_checkout_transaction_created_at
    on checkout_transaction (created_at);

create table if not exists checkout_transaction_item (
    id bigserial primary key,
    transaction_id bigint not null references checkout_transaction(id) on delete cascade,
    product_code varchar(64) not null,
    product_name varchar(512) not null,
    barcode varchar(128),
    category varchar(128),
    unit_price numeric(18, 2) not null,
    actual_price numeric(18, 2) not null,
    quantity integer not null,
    subtotal numeric(18, 2) not null,
    applied_promo_id varchar(128),
    applied_coupon_code varchar(512)
);

create index if not exists idx_checkout_transaction_item_transaction_id
    on checkout_transaction_item (transaction_id);

create index if not exists idx_checkout_transaction_item_product_code
    on checkout_transaction_item (product_code);

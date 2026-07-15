create table if not exists bundle (
    id varchar(64) primary key,
    name varchar(200) not null,
    bundle_price numeric(18, 2) not null,
    threshold_amount numeric(18, 2) not null default 0,
    activity_id varchar(64),
    status varchar(20) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists bundle_item (
    id bigserial primary key,
    bundle_id varchar(64) not null references bundle(id),
    product_code varchar(50) not null,
    quantity integer not null default 1,
    created_at timestamptz not null default now()
);

create index if not exists idx_bundle_item_bundle_id on bundle_item (bundle_id);
create index if not exists idx_bundle_item_product_code on bundle_item (product_code);

create table if not exists product_group (
    id varchar(64) primary key,
    name varchar(200) not null,
    source varchar(128) not null default 'DEMO',
    created_at timestamptz not null default now()
);

create table if not exists product_group_item (
    id bigserial primary key,
    group_id varchar(64) not null references product_group(id),
    product_code varchar(50) not null,
    created_at timestamptz not null default now(),
    unique (group_id, product_code)
);

create index if not exists idx_product_group_item_group_id on product_group_item (group_id);
create index if not exists idx_product_group_item_product_code on product_group_item (product_code);

insert into bundle (id, name, bundle_price, threshold_amount, activity_id, status)
values
    ('bundle-cng-water-drink', '演示-CNG水饮包', 7.65, 50.00, 'demo-lng-cng-v1', 'ACTIVE'),
    ('bundle-lng-long-haul', '演示-LNG长途包', 20.40, 1000.00, 'demo-lng-cng-v1', 'ACTIVE')
on conflict (id) do update
set name = excluded.name,
    bundle_price = excluded.bundle_price,
    threshold_amount = excluded.threshold_amount,
    activity_id = excluded.activity_id,
    status = excluded.status,
    updated_at = now();

insert into bundle_item (bundle_id, product_code, quantity)
select 'bundle-cng-water-drink', '70251989', 1
where not exists (
    select 1 from bundle_item where bundle_id = 'bundle-cng-water-drink' and product_code = '70251989'
);
insert into bundle_item (bundle_id, product_code, quantity)
select 'bundle-cng-water-drink', '70356177', 1
where not exists (
    select 1 from bundle_item where bundle_id = 'bundle-cng-water-drink' and product_code = '70356177'
);
insert into bundle_item (bundle_id, product_code, quantity)
select 'bundle-lng-long-haul', '70453858', 1
where not exists (
    select 1 from bundle_item where bundle_id = 'bundle-lng-long-haul' and product_code = '70453858'
);
insert into bundle_item (bundle_id, product_code, quantity)
select 'bundle-lng-long-haul', '70341453', 1
where not exists (
    select 1 from bundle_item where bundle_id = 'bundle-lng-long-haul' and product_code = '70341453'
);
insert into bundle_item (bundle_id, product_code, quantity)
select 'bundle-lng-long-haul', '70545526', 1
where not exists (
    select 1 from bundle_item where bundle_id = 'bundle-lng-long-haul' and product_code = '70545526'
);

insert into product_group (id, name, source)
values
    ('group-redbull-common', '红牛（2款通用）', 'ACTIVITY_WORKBOOK_DEMO'),
    ('group-water-common', '格桑泉/小水通用', 'ACTIVITY_WORKBOOK_DEMO')
on conflict (id) do update
set name = excluded.name,
    source = excluded.source;

insert into product_group_item (group_id, product_code)
values
    ('group-redbull-common', '70356177'),
    ('group-redbull-common', '70453858'),
    ('group-water-common', '70251989'),
    ('group-water-common', '70545526')
on conflict (group_id, product_code) do nothing;

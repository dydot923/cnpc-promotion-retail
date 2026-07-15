alter table product
    add column if not exists is_demo_data boolean not null default false;

alter table product_price
    add column if not exists is_demo_data boolean not null default false;

alter table inventory_snapshot
    add column if not exists is_demo_data boolean not null default false;

alter table coupon_template
    add column if not exists is_demo_data boolean not null default false;

alter table coupon
    add column if not exists is_demo_data boolean not null default false;

alter table bundle
    add column if not exists is_demo_data boolean not null default false;

alter table bundle_item
    add column if not exists is_demo_data boolean not null default false;

alter table product_group
    add column if not exists is_demo_data boolean not null default false;

alter table product_group_item
    add column if not exists is_demo_data boolean not null default false;

alter table promotion_rule_draft
    add column if not exists is_demo_data boolean not null default false;

alter table promotion_rule_version
    add column if not exists is_demo_data boolean not null default false;

alter table promotion_rule_audit_log
    add column if not exists is_demo_data boolean not null default false;

update product
set is_demo_data = true
where product_code like 'demo-%';

update product_price
set is_demo_data = true
where import_version like '%demo%';

update inventory_snapshot
set is_demo_data = true
where import_version like '%demo%';

update coupon_template
set is_demo_data = true
where coupon_template_id like 'demo-%';

update coupon
set is_demo_data = true
where coupon_id like 'demo-%';

update bundle
set is_demo_data = true
where activity_id like 'demo-%';

update bundle_item item
set is_demo_data = true
from bundle b
where b.id = item.bundle_id
  and b.is_demo_data = true;

update product_group
set is_demo_data = true
where source in ('DEMO', 'ACTIVITY_WORKBOOK_DEMO');

update product_group_item item
set is_demo_data = true
from product_group g
where g.id = item.group_id
  and g.is_demo_data = true;

update promotion_rule_draft
set is_demo_data = true
where source_import_id = 'demo-seed-v1'
   or rule_id like 'demo-%';

update promotion_rule_version
set is_demo_data = true
where source_import_id = 'demo-seed-v1'
   or rule_id like 'demo-%';

update promotion_rule_audit_log
set is_demo_data = true
where operator_id = 'flyway-demo'
   or rule_id like 'demo-%';

create index if not exists idx_product_is_demo_data on product (is_demo_data);
create index if not exists idx_coupon_is_demo_data on coupon (is_demo_data);
create index if not exists idx_promotion_rule_draft_is_demo_data on promotion_rule_draft (is_demo_data);

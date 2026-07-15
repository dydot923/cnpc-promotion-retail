insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-product-group-mapping',
    'PROMOTION',
    'data/活动看板.xlsx',
    19,
    25,
    0,
    0,
    0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    skipped_count = excluded.skipped_count,
    invalid_count = excluded.invalid_count,
    warning_count = excluded.warning_count;

update product_group
set is_demo_data = false
where source = 'ACTIVITY_BOARD_V2';

update product_group_item item
set is_demo_data = false
from product_group grp
where grp.id = item.group_id
  and grp.source = 'ACTIVITY_BOARD_V2';

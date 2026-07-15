insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-exchange-purchase-bundle',
    'PROMOTION',
    'data/活动看板.xlsx',
    3,
    3,
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

update promotion_rule_draft
set rule_json = jsonb_set(
        jsonb_set(
            jsonb_set(rule_json, '{priority}', '20'::jsonb, true),
            '{exclusiveGroup}', to_jsonb('H1_EXCHANGE'::text), true
        ),
        '{stackable}', 'false'::jsonb, true
    ),
    updated_at = now()
where source_import_id = 'activity-board-v2'
  and rule_id in (
      'abv2-bundle-abv2-driving-package',
      'abv2-bundle-abv2-water-drink-package',
      'abv2-bundle-abv2-long-haul-package'
  );

update promotion_rule_version
set rule_json = jsonb_set(
        jsonb_set(
            jsonb_set(rule_json, '{priority}', '20'::jsonb, true),
            '{exclusiveGroup}', to_jsonb('H1_EXCHANGE'::text), true
        ),
        '{stackable}', 'false'::jsonb, true
    ),
    change_reason = 'Activity board exchange bundle import: exclusive group aligned to v2'
where source_import_id = 'activity-board-v2'
  and rule_id in (
      'abv2-bundle-abv2-driving-package',
      'abv2-bundle-abv2-water-drink-package',
      'abv2-bundle-abv2-long-haul-package'
  );

update bundle
set is_demo_data = false
where activity_id = 'activity-board-v2';

update bundle_item item
set is_demo_data = false
from bundle b
where b.id = item.bundle_id
  and b.activity_id = 'activity-board-v2';

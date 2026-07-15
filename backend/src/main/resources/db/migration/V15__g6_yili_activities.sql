insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-g6-yili-activities',
    'PROMOTION',
    'data/活动看板.xlsx',
    10,
    10,
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
set rule_json = jsonb_set(rule_json, '{version}', to_jsonb('activity-board-v2'::text), true),
    updated_at = now()
where source_import_id = 'activity-board-v2'
  and rule_id like 'abv2-g6-%';
update promotion_rule_version
set rule_json = jsonb_set(rule_json, '{version}', to_jsonb('activity-board-v2'::text), true),
    change_reason = 'Activity board G6 Yili activities import completed'
where source_import_id = 'activity-board-v2'
  and rule_id like 'abv2-g6-%';

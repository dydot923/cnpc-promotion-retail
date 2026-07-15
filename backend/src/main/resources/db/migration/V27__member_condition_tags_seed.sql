-- Backfill demo member tags used by member-condition rule matching.

update member
set member_tags = '["gasoline_customer"]'::jsonb
where member_code in ('member-001', 'demo-member-sequence')
  and (member_tags is null or member_tags = '[]'::jsonb);

update member
set member_tags = '["diesel_customer"]'::jsonb
where member_code = 'member-002'
  and (member_tags is null or member_tags = '[]'::jsonb);

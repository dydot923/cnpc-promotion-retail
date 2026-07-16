with corrected_rules(rule_id, priority, source_row_number) as (
    values
        ('abv2-g6-cotton-film-9-gift-pack', 79, 15),
        ('abv2-g6-store-36-gift-choice', 73, 16),
        ('abv2-g6-cigarette-200-gift-choice', 74, 17),
        ('abv2-g6-cigarette-555-gift-ilite250', 73, 18),
        ('abv2-g6-cigarette-888-gift-ilite500', 72, 19)
)
update promotion_rule_draft draft
set source_sheet_name = '非非促销（统建）',
    source_row_number = corrected.source_row_number,
    rule_json = jsonb_set(
        jsonb_set(
            jsonb_set(draft.rule_json, '{priority}', to_jsonb(corrected.priority), true),
            '{sourceSheetName}', to_jsonb('非非促销（统建）'::text), true),
        '{sourceRowNumber}', to_jsonb(corrected.source_row_number), true),
    updated_at = now()
from corrected_rules corrected
where draft.rule_id = corrected.rule_id;

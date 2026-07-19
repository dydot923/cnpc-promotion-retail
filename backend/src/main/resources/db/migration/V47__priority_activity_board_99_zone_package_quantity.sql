-- 9.9 元专区按看板“促销包装系数”结算，而不是把 9.90 元错误地当成单件价格。
with package_quantity as (
    select case
        when source_row_number between 4 and 74 then 1
        when source_row_number between 75 and 132 then 2
        when source_row_number between 133 and 173 then 3
        when source_row_number between 174 and 183 then 4
        when source_row_number between 184 and 185 then 5
        when source_row_number between 186 and 188 then 3
        when source_row_number = 189 then 2
        when source_row_number = 190 then 3
        when source_row_number between 191 and 192 then 1
        when source_row_number = 193 then 2
        when source_row_number = 194 then 1
        when source_row_number = 195 then 2
        when source_row_number between 196 and 197 then 3
        else 1
    end as min_product_quantity,
    rule_id
    from promotion_rule_draft
    where rule_id like 'abv2-99-zone-%'
), updated_drafts as (
    update promotion_rule_draft draft
    set condition_json = jsonb_set(
            draft.condition_json,
            '{minProductQuantity}',
            to_jsonb(package_quantity.min_product_quantity),
            true
        ),
        rule_json = jsonb_set(
            draft.rule_json,
            '{condition,minProductQuantity}',
            to_jsonb(package_quantity.min_product_quantity),
            true
        ),
        updated_at = now()
    from package_quantity
    where draft.rule_id = package_quantity.rule_id
    returning draft.rule_id, draft.rule_json
)
update promotion_rule_version version
set rule_json = updated_drafts.rule_json,
    change_reason = '9.9 元专区按活动看板促销包装系数结算'
from updated_drafts
where version.rule_id = updated_drafts.rule_id
  and version.status = 'CONFIRMED';

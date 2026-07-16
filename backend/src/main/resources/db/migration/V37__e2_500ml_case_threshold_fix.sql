with corrections(rule_id, min_cart_amount, coupon_quantity, activity_name) as (
    values
        ('abv2-e2-ilite-500-jia-case-coupon', 708.00, 2, '伊力特佳藏500ML整件赠汽油券'),
        ('abv2-e2-ilite-500-li-case-coupon', 1788.00, 4, '伊力特礼藏500ML整件赠汽油券')
)
update promotion_rule_draft draft
set source_sheet_name = '非非促销（统建）',
    source_row_number = 14,
    condition_json = jsonb_set(
        jsonb_set(draft.condition_json, '{minCartAmount}', to_jsonb(correction.min_cart_amount), true),
        '{minProductQuantity}', '6'::jsonb, true),
    benefit_json = jsonb_set(draft.benefit_json, '{giftCouponName}', to_jsonb('100元汽油券'::text), true),
    rule_json = jsonb_set(
        jsonb_set(
            jsonb_set(
                jsonb_set(
                    jsonb_set(draft.rule_json, '{activityName}', to_jsonb(correction.activity_name), true),
                    '{condition,minCartAmount}', to_jsonb(correction.min_cart_amount), true),
                '{condition,minProductQuantity}', '6'::jsonb, true),
            '{benefit,giftCouponName}', to_jsonb('100元汽油券'::text), true),
        '{sourceSheetName}', to_jsonb('非非促销（统建）'::text), true),
    updated_at = now()
from corrections correction
where draft.rule_id = correction.rule_id;

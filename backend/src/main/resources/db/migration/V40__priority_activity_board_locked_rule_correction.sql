-- V39 的目标规则曾被早期导入标记为人工锁定，重点验收修正必须同步到实际结算快照。
with correction(rule_id, activity_name, min_cart_amount, fixed_price, coupon_name, coupon_amount,
                coupon_quantity, coupon_threshold, min_product_quantity) as (
    values
        ('abv2-g6-store-36-gift-choice', '便利店满40赠品二选一', 40.00, null, null, null, null, null, 0),
        ('abv2-g6-ilite-250-fixed', '伊力特250ml会员价-2瓶116元', 116.00, 58.00, null, null, null, null, 2),
        ('abv2-g6-ilite-250-coupon', '伊力特250ml会员赠16元非油券', 116.00, null, '16元非油券', 16.00, 1, 66.00, 2),
        ('abv2-g6-ilite-500-jia-fixed', '伊力特500ml佳藏会员价-2瓶216元', 216.00, 108.00, null, null, null, null, 2),
        ('abv2-g6-ilite-500-jia-coupon', '伊力特500ml佳藏会员赠16元非油券', 216.00, null, '16元非油券', 16.00, 1, 66.00, 2),
        ('abv2-g6-ilite-500-li-fixed', '伊力特500ml礼藏会员价-2瓶516元', 516.00, 258.00, null, null, null, null, 2),
        ('abv2-g6-ilite-500-li-coupon', '伊力特500ml礼藏会员赠16元非油券', 516.00, null, '16元非油券', 16.00, 2, 66.00, 2)
), prepared as (
    select draft.rule_id,
           c.activity_name,
           case when c.rule_id = 'abv2-g6-store-36-gift-choice' then draft.condition_json
                else jsonb_set(draft.condition_json, '{minProductQuantity}', to_jsonb(c.min_product_quantity), true) end as condition_json,
           case when c.coupon_amount is not null then
                    jsonb_set(jsonb_set(jsonb_set(jsonb_set(draft.benefit_json,
                        '{giftCouponName}', to_jsonb(c.coupon_name), true),
                        '{giftCouponAmount}', to_jsonb(c.coupon_amount), true),
                        '{giftCouponQuantity}', to_jsonb(c.coupon_quantity), true),
                        '{giftCouponUseThreshold}', to_jsonb(c.coupon_threshold), true)
                when c.fixed_price is not null then jsonb_set(draft.benefit_json, '{fixedPrice}', to_jsonb(c.fixed_price), true)
                else draft.benefit_json end as benefit_json,
           case when c.coupon_amount is not null then
                    jsonb_set(jsonb_set(jsonb_set(jsonb_set(jsonb_set(jsonb_set(jsonb_set(draft.rule_json,
                        '{activityName}', to_jsonb(c.activity_name), true),
                        '{condition,minCartAmount}', to_jsonb(c.min_cart_amount), true),
                        '{condition,minProductQuantity}', to_jsonb(c.min_product_quantity), true),
                        '{benefit,giftCouponName}', to_jsonb(c.coupon_name), true),
                        '{benefit,giftCouponAmount}', to_jsonb(c.coupon_amount), true),
                        '{benefit,giftCouponQuantity}', to_jsonb(c.coupon_quantity), true),
                        '{benefit,giftCouponUseThreshold}', to_jsonb(c.coupon_threshold), true)
                when c.fixed_price is not null then
                    jsonb_set(jsonb_set(jsonb_set(draft.rule_json,
                        '{activityName}', to_jsonb(c.activity_name), true),
                        '{condition,minCartAmount}', to_jsonb(c.min_cart_amount), true),
                        '{benefit,fixedPrice}', to_jsonb(c.fixed_price), true)
                else jsonb_set(jsonb_set(draft.rule_json,
                        '{activityName}', to_jsonb(c.activity_name), true),
                        '{condition,minCartAmount}', to_jsonb(c.min_cart_amount), true)
           end as rule_json
    from promotion_rule_draft draft
    join correction c on c.rule_id = draft.rule_id
), updated as (
    update promotion_rule_draft draft
    set source_sheet_name = '非非促销（统建）',
        source_row_number = 13 + case when prepared.rule_id = 'abv2-g6-store-36-gift-choice' then 3 else 0 end,
        condition_json = prepared.condition_json,
        benefit_json = prepared.benefit_json,
        rule_json = prepared.rule_json,
        updated_at = now()
    from prepared
    where draft.rule_id = prepared.rule_id
    returning draft.*
)
update promotion_rule_version version
set source_sheet_name = updated.source_sheet_name,
    source_row_number = updated.source_row_number,
    rule_json = updated.rule_json,
    change_reason = '重点活动看板锁定规则修正：满40元、16元非油券及组合价'
from updated
where version.rule_id = updated.rule_id
  and version.status = 'CONFIRMED';

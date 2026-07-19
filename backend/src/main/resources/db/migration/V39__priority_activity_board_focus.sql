-- 重点验收修正：只覆盖“加油换购 / 非非促销 / 9.9元专区”看板的可执行参数。
-- 采用幂等更新，保留人工锁定规则的治理边界。

with corrections(rule_id, activity_name, source_sheet, source_row, min_cart_amount, fixed_price,
                 coupon_name, coupon_amount, coupon_quantity, coupon_threshold, min_product_quantity) as (
    values
        ('abv2-g6-store-36-gift-choice', '便利店满40赠品二选一', '非非促销（统建）', 16,
            40.00, null, null, null, null, null, 0),
        ('abv2-g6-ilite-250-fixed', '伊力特250ml会员价-2瓶116元', '非非促销（统建）', 13,
            116.00, 58.00, null, null, null, null, 2),
        ('abv2-g6-ilite-250-coupon', '伊力特250ml会员赠16元非油券', '非非促销（统建）', 13,
            116.00, null, '16元非油券', 16.00, 1, 66.00, 2),
        ('abv2-g6-ilite-500-jia-fixed', '伊力特500ml佳藏会员价-2瓶216元', '非非促销（统建）', 13,
            216.00, 108.00, null, null, null, null, 2),
        ('abv2-g6-ilite-500-jia-coupon', '伊力特500ml佳藏会员赠16元非油券', '非非促销（统建）', 13,
            216.00, null, '16元非油券', 16.00, 1, 66.00, 2),
        ('abv2-g6-ilite-500-li-fixed', '伊力特500ml礼藏会员价-2瓶516元', '非非促销（统建）', 13,
            516.00, 258.00, null, null, null, null, 2),
        ('abv2-g6-ilite-500-li-coupon', '伊力特500ml礼藏会员赠16元非油券', '非非促销（统建）', 13,
            516.00, null, '16元非油券', 16.00, 2, 66.00, 2)
), base as (
    select draft.rule_id, c.activity_name, c.source_sheet, c.source_row, c.fixed_price,
           c.coupon_name, c.coupon_amount, c.coupon_quantity, c.coupon_threshold,
           jsonb_set(jsonb_set(draft.rule_json, '{activityName}', to_jsonb(c.activity_name), true),
               '{condition,minCartAmount}', to_jsonb(c.min_cart_amount), true) as base_rule_json,
           jsonb_set(draft.condition_json, '{minCartAmount}', to_jsonb(c.min_cart_amount), true) as base_condition_json,
           draft.benefit_json
    from promotion_rule_draft draft
    join corrections c on c.rule_id = draft.rule_id
    where not draft.manual_locked
), prepared as (
    select rule_id, activity_name, source_sheet, source_row,
           case when rule_id = 'abv2-g6-store-36-gift-choice' then base_condition_json
                else jsonb_set(base_condition_json, '{minProductQuantity}', to_jsonb(2), true) end as next_condition_json,
           case when coupon_amount is not null then
                jsonb_set(jsonb_set(jsonb_set(jsonb_set(benefit_json,
                    '{giftCouponName}', to_jsonb(coupon_name), true),
                    '{giftCouponAmount}', to_jsonb(coupon_amount), true),
                    '{giftCouponQuantity}', to_jsonb(coupon_quantity), true),
                    '{giftCouponUseThreshold}', to_jsonb(coupon_threshold), true)
                when fixed_price is not null then jsonb_set(benefit_json, '{fixedPrice}', to_jsonb(fixed_price), true)
                else benefit_json end as next_benefit_json,
           case when coupon_amount is not null then
                jsonb_set(jsonb_set(jsonb_set(jsonb_set(jsonb_set(base_rule_json,
                    '{condition,minProductQuantity}', to_jsonb(2), true),
                    '{benefit,giftCouponName}', to_jsonb(coupon_name), true),
                    '{benefit,giftCouponAmount}', to_jsonb(coupon_amount), true),
                    '{benefit,giftCouponQuantity}', to_jsonb(coupon_quantity), true),
                    '{benefit,giftCouponUseThreshold}', to_jsonb(coupon_threshold), true)
                when fixed_price is not null then
                jsonb_set(jsonb_set(base_rule_json, '{condition,minProductQuantity}', to_jsonb(2), true),
                    '{benefit,fixedPrice}', to_jsonb(fixed_price), true)
                else base_rule_json end as next_rule_json
    from base
), updated as (
    update promotion_rule_draft draft
    set source_sheet_name = p.source_sheet,
        source_row_number = p.source_row,
        rule_json = p.next_rule_json,
        condition_json = p.next_condition_json,
        benefit_json = p.next_benefit_json,
        updated_at = now()
    from prepared p
    where draft.rule_id = p.rule_id
    returning draft.*
)
update promotion_rule_version version
set source_sheet_name = updated.source_sheet_name,
    source_row_number = updated.source_row_number,
    rule_json = updated.rule_json,
    change_reason = '重点活动看板参数修正：满40元、16元非油券及伊力特组合价'
from updated
where version.rule_id = updated.rule_id
  and version.status = 'CONFIRMED';

-- 将后续规则中的来源/门槛同步到版本快照，避免旧版本被结算查询读取。
with tier_corrections(rule_id, min_cart_amount, coupon_quantity, activity_name) as (
    values
        ('abv2-e2-ilite-250-case-coupon', 680.00, 2, '伊力特250ml整件10瓶赠100元汽油券2张'),
        ('abv2-e2-ilite-500-jia-case-coupon', 708.00, 2, '伊力特佳藏500ml整件6瓶赠100元汽油券2张'),
        ('abv2-e2-ilite-500-li-case-coupon', 1788.00, 4, '伊力特礼藏500ml整件6瓶赠100元汽油券4张')
), updated as (
    update promotion_rule_draft draft
    set source_sheet_name = '非非促销（统建）',
        source_row_number = 14,
        rule_json = jsonb_set(jsonb_set(jsonb_set(draft.rule_json,
            '{activityName}', to_jsonb(t.activity_name), true),
            '{condition,minCartAmount}', to_jsonb(t.min_cart_amount), true),
            '{benefit,giftCouponQuantity}', to_jsonb(t.coupon_quantity), true),
        condition_json = jsonb_set(draft.condition_json, '{minCartAmount}', to_jsonb(t.min_cart_amount), true),
        benefit_json = jsonb_set(draft.benefit_json, '{giftCouponQuantity}', to_jsonb(t.coupon_quantity), true),
        updated_at = now()
    from tier_corrections t
    where draft.rule_id = t.rule_id and not draft.manual_locked
    returning draft.*
)
update promotion_rule_version version
set source_sheet_name = updated.source_sheet_name,
    source_row_number = updated.source_row_number,
    rule_json = updated.rule_json,
    change_reason = '重点活动看板整件伊力特门槛修正'
from updated
where version.rule_id = updated.rule_id
  and version.status = 'CONFIRMED';

-- 9.9 元专区中四条没有编码的看板行无法安全结算，继续保留导入错误；有编码的190条全部维持固定价9.9规则。
update promotion_rule_draft
set source_sheet_name = '参考2-9.9元商品专区',
    updated_at = now()
where source_sheet_name = '参考2-9.9元商品专区'
  and rule_type = 'FIXED_PRICE'
  and not manual_locked;

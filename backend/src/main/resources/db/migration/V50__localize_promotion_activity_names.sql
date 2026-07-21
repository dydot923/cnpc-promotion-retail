-- 面向收银员统一使用中文活动名称；商品品牌和规格保持商品主档原文。
with g7_names as (
    select draft.rule_id,
           '非非促销-单品安全价-' || product.product_name as activity_name
    from promotion_rule_draft draft
    join lateral (
        select jsonb_array_elements_text(draft.rule_json -> 'condition' -> 'productCodes') as product_code
        limit 1
    ) code on true
    join product on product.product_code = code.product_code
    where draft.source_import_id = 'activity-board-v2-g7-resolution'
), updated as (
    update promotion_rule_draft draft
    set rule_json = jsonb_set(draft.rule_json, '{activityName}', to_jsonb(g7_names.activity_name), true),
        updated_at = now()
    from g7_names
    where draft.rule_id = g7_names.rule_id
    returning draft.rule_id
)
select count(*) from updated;

with localized(rule_id, activity_name) as (
    values
        ('abv2-a5-day10-super-1000-normal', '超级十惠-普通客户单笔充值1000元'),
        ('abv2-a5-day10-super-1000-gold', '超级十惠-黄金及以上客户单笔充值1000元'),
        ('abv2-a5-day10-super-2000-normal', '超级十惠-普通客户单笔充值2000元'),
        ('abv2-a5-day10-super-2000-gold', '超级十惠-黄金及以上客户单笔充值2000元'),
        ('abv2-a6-small-recharge-666', '非十惠日小额充值666元赠券包')
)
update promotion_rule_draft draft
set rule_json = jsonb_set(draft.rule_json, '{activityName}', to_jsonb(localized.activity_name), true),
    updated_at = now()
from localized
where draft.rule_id = localized.rule_id;

update promotion_rule_draft
set rule_json = jsonb_set(
        rule_json,
        '{activityName}',
        to_jsonb(replace(replace(rule_json ->> 'activityName', '-GASOLINE', '-汽油'), '-DIESEL', '-柴油')),
        true
    ),
    updated_at = now()
where rule_json ->> 'activityName' like '%-GASOLINE'
   or rule_json ->> 'activityName' like '%-DIESEL';

-- 同步历史确认版本，规则管理和收银结算看到同一中文名称。
update promotion_rule_version version
set rule_json = jsonb_set(
        version.rule_json,
        '{activityName}',
        to_jsonb(draft.rule_json ->> 'activityName'),
        true
    )
from promotion_rule_draft draft
where version.rule_id = draft.rule_id
  and (
      draft.source_import_id in (
          'activity-board-v2-g7-resolution',
          'activity-board-v2-a5-recharge-coupon',
          'activity-board-v2-small-recharge-666'
      )
      or draft.rule_json ->> 'activityName' like '%-汽油'
      or draft.rule_json ->> 'activityName' like '%-柴油'
  );

-- 9.9专区中编码存在但价格主档未导入的商品，补录看板价格字段作为可结算主档。
insert into product (product_code, product_name, category, is_cigarette, is_fertilizer)
values
    ('70373302', '蔬菜小圆饼', '饼干/糕点', false, false),
    ('70373382', '牛乳饼干（海盐味）', '饼干/糕点', false, false),
    ('70434451', '牛乳饼干（原味）', '饼干/糕点', false, false),
    ('70533838', '糯米锅巴（藤椒牛肉味）', '零食', false, false),
    ('70533839', '糯米锅巴（蟹香咸蛋黄味）', '零食', false, false),
    ('70533842', '糯米锅巴（黑松露火腿味）', '零食', false, false),
    ('70553214', '卷心小面包', '面包', false, false),
    ('70595181', '灌汤花生（香辣味）', '零食', false, false),
    ('70595182', '灌汤花生（卤香味）', '零食', false, false),
    ('70598779', '面包脆蒜香味', '饼干/糕点', false, false),
    ('70598783', '面包脆芝士味', '饼干/糕点', false, false),
    ('70603258', '水韵好客奶酪海苔脆50g', '饼干/糕点', false, false),
    ('70711037', '溜溜梅 海南芒果抱抱梅 70g', '零食', false, false),
    ('70711038', '溜溜梅 3味电解质冰沙梅冻 100g', '零食', false, false)
on conflict (product_code) do update
set product_name = excluded.product_name, category = excluded.category, updated_at = now();

delete from product_price
where import_version = 'activity-board-v2-99-zone-focus'
  and product_code in ('70373302','70373382','70434451','70533838','70533839','70533842',
      '70553214','70595181','70595182','70598779','70598783','70603258','70711037','70711038');

with prices(product_code, price) as (
    values
        ('70373302', 9.90),('70373382', 9.90),('70434451', 9.90),
        ('70533838', 9.90),('70533839', 9.90),('70533842', 9.90),
        ('70553214', 9.90),('70595181', 9.90),('70595182', 9.90),
        ('70598779', 9.90),('70598783', 9.90),('70603258', 9.90),
        ('70711037', 9.90),('70711038', 9.90)
)
insert into product_price (product_code, execution_price, import_version, effective_at)
select product_code, price, 'activity-board-v2-99-zone-focus', now() from prices;

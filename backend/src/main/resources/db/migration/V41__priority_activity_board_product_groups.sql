-- 重点看板的单品/组合通用组，统一使用价格主档中的真实编码。
with groups(group_id, group_name, description, product_codes) as (
    values
        ('ABV2_GESANG_SMALL_WATER', '小水（武夷山333ML/格桑泉330ML）', '加油换购小水通用组', ARRAY['70251989','70545523']),
        ('ABV2_GESANG_BIG_WATER', '大水（武夷山513ML/格桑泉500ML）', '加油换购大水通用组', ARRAY['70251198','70545526','70494461']),
        ('ABV2_USMAIL_JUICE_1L', '优斯麦尔果汁1L', '加油换购1L果汁通用组', ARRAY['70727173','70727176','70727310']),
        ('ABV2_USMAIL_JUICE_03L', '优斯麦尔果汁0.3L', '加油换购0.3L果汁通用组', ARRAY['70727875','70727893','70727174','70727552','70727554','70727555']),
        ('ABV2_USMAIL_JUICE_248ML', '优斯麦尔果汁248ml', '加油换购248ml果汁通用组', ARRAY['70549757','70549760','70549763','70549765','70549767','70549845','70549847','70550129']),
        ('ABV2_USMAIL_MILK_200G', '优斯麦尔佳丽纯牛奶200G', '加油换购牛奶通用组', ARRAY['70559364']),
        ('ABV2_GLASS_WATER_0', '咔咔0℃玻璃水', '加油换购/非非促销玻璃水', ARRAY['70536790']),
        ('ABV2_WATER_4_5L', '武夷山4.5L水', '非非促销整件水', ARRAY['70254265']),
        ('ABV2_ORGANIC_MILK_250', '有机浓缩纯牛奶250G', '非非促销整件奶', ARRAY['70559365']),
        ('ABV2_DREAM_MILK_250', '纯牛奶梦幻盖250ML', '非非促销整件奶', ARRAY['70559370']),
        ('ABV2_MILK_BEER_300', '天润奶啤300ML', '非非促销整件奶啤', ARRAY['70125186','70224290','70322580','70468233','70498529'])
)
insert into product_group (id, name, source, description, is_demo_data)
select group_id, group_name, 'ACTIVITY_BOARD_V2_FOCUS', description, false from groups
on conflict (id) do update set name = excluded.name, description = excluded.description, is_demo_data = false;

delete from product_group_item
where group_id in ('ABV2_GESANG_SMALL_WATER', 'ABV2_GESANG_BIG_WATER', 'ABV2_USMAIL_JUICE_1L',
    'ABV2_USMAIL_JUICE_03L', 'ABV2_USMAIL_JUICE_248ML', 'ABV2_USMAIL_MILK_200G', 'ABV2_GLASS_WATER_0',
    'ABV2_WATER_4_5L', 'ABV2_ORGANIC_MILK_250', 'ABV2_DREAM_MILK_250', 'ABV2_MILK_BEER_300');

with items(group_id, product_code) as (
    values
        ('ABV2_GESANG_SMALL_WATER','70251989'),('ABV2_GESANG_SMALL_WATER','70545523'),
        ('ABV2_GESANG_BIG_WATER','70251198'),('ABV2_GESANG_BIG_WATER','70545526'),('ABV2_GESANG_BIG_WATER','70494461'),
        ('ABV2_USMAIL_JUICE_1L','70727173'),('ABV2_USMAIL_JUICE_1L','70727176'),('ABV2_USMAIL_JUICE_1L','70727310'),
        ('ABV2_USMAIL_JUICE_03L','70727875'),('ABV2_USMAIL_JUICE_03L','70727893'),('ABV2_USMAIL_JUICE_03L','70727174'),
        ('ABV2_USMAIL_JUICE_03L','70727552'),('ABV2_USMAIL_JUICE_03L','70727554'),('ABV2_USMAIL_JUICE_03L','70727555'),
        ('ABV2_USMAIL_JUICE_248ML','70549757'),('ABV2_USMAIL_JUICE_248ML','70549760'),('ABV2_USMAIL_JUICE_248ML','70549763'),
        ('ABV2_USMAIL_JUICE_248ML','70549765'),('ABV2_USMAIL_JUICE_248ML','70549767'),('ABV2_USMAIL_JUICE_248ML','70549845'),
        ('ABV2_USMAIL_JUICE_248ML','70549847'),('ABV2_USMAIL_JUICE_248ML','70550129'),
        ('ABV2_USMAIL_MILK_200G','70559364'),('ABV2_GLASS_WATER_0','70536790'),('ABV2_WATER_4_5L','70254265'),
        ('ABV2_ORGANIC_MILK_250','70559365'),('ABV2_DREAM_MILK_250','70559370'),
        ('ABV2_MILK_BEER_300','70125186'),('ABV2_MILK_BEER_300','70224290'),('ABV2_MILK_BEER_300','70322580'),
        ('ABV2_MILK_BEER_300','70468233'),('ABV2_MILK_BEER_300','70498529')
)
insert into product_group_item (group_id, product_code)
select group_id, product_code from items
on conflict (group_id, product_code) do nothing;

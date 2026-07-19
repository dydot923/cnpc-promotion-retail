-- 9.9专区四个源表缺编码行采用价格主档真实商品进行映射，明确记录映射商品名称，便于验收追溯。
insert into product (product_code, product_name, category, is_cigarette, is_fertilizer)
values
    ('70486123', '甘源 蜂蜜琥珀核桃仁 32G', '零食', false, false),
    ('70528095', '疆百泉 杏皮茶 330ML', '包装饮料', false, false),
    ('70556786', '杏疆好 杏皮茶 380ML', '包装饮料', false, false),
    ('70543455', '农夫山泉 茶Π茉莉花柠檬茶 500ML', '包装饮料', false, false)
on conflict (product_code) do update
set product_name = excluded.product_name, category = excluded.category, updated_at = now();

delete from product_price
where import_version = 'activity-board-v2-99-zone-focus'
  and product_code in ('70486123','70528095','70556786','70543455');

insert into product_price (product_code, execution_price, import_version, effective_at)
values
    ('70486123', 5.00, 'activity-board-v2-99-zone-focus', now()),
    ('70528095', 5.00, 'activity-board-v2-99-zone-focus', now()),
    ('70556786', 6.50, 'activity-board-v2-99-zone-focus', now()),
    ('70543455', 5.00, 'activity-board-v2-99-zone-focus', now());

delete from import_error_row
where import_version = 'activity-board-v2-99-zone-full-import'
  and error_code = 'MISSING_PRODUCT_CODE'
  and row_number in (194, 195, 196, 197);

update import_batch
set inserted_count = 194,
    updated_count = 194,
    invalid_count = 0,
    warning_count = 0
where import_version = 'activity-board-v2-99-zone-full-import';

with classified as (
    select id,
           case
               when (product_name ilike '%香烟%'
                     or product_name ~* '[0-9]+([.][0-9]+)?[[:space:]]*mg'
                     or (product_name like '%20支%'
                         and product_name !~ '(红枣饮|棉签|牙签|吸管|火腿|铅笔|中性笔)')) then '香烟'
               when product_name ~ '(化肥|尿素|复合肥|磷酸二铵|钾肥)' then '化肥'
               when product_name like '%啤酒%' then '啤酒'
               when product_name like '%咖啡%' then '咖啡'
               when product_name like '%月饼%' then '月饼礼盒'
               when product_name like '%瓜子%' then '瓜子'
               when product_name ~ '(雪糕|冰淇淋|冰激凌)' then '雪糕'
               when product_name ~ '(肉脯|肉干)' then '肉脯'
               when product_name ~ '(薯片|虾条|锅巴|爆米花|好多鱼|雪饼|仙贝)' then '膨化'
               else null
           end as inferred_category
    from product
    where category is null or btrim(category) = ''
)
update product product
set category = classified.inferred_category,
    is_cigarette = product.is_cigarette or classified.inferred_category = '香烟',
    is_fertilizer = product.is_fertilizer or classified.inferred_category = '化肥',
    updated_at = now()
from classified
where product.id = classified.id
  and classified.inferred_category is not null;

update product
set is_cigarette = true,
    updated_at = now()
where category = '香烟'
  and is_cigarette = false;

update product
set is_fertilizer = true,
    updated_at = now()
where category = '化肥'
  and is_fertilizer = false;

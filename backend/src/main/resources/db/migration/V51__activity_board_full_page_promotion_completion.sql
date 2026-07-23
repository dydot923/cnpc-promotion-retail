-- 落实“非非促销（统建）”第20-43行：整件价、阶梯折扣、买一赠一及惊爆价。
insert into import_batch (
    import_version, import_type, source_file, inserted_count, updated_count,
    skipped_count, invalid_count, warning_count
)
values (
    'activity-board-v2-full-page-completion', 'PROMOTION', 'data/活动看板.xlsx', 33, 4, 0, 0, 0
)
on conflict (import_version) do update
set inserted_count = excluded.inserted_count,
    updated_count = excluded.updated_count,
    warning_count = excluded.warning_count;

-- 看板没有商品编码的棉包膜使用既有占位编码，但改为收银员可搜索的正式名称和看板基准价。
insert into product (
    product_code, product_name, barcode, category,
    is_cigarette, is_fertilizer, is_demo_data
)
values (
    'demo-cotton-film', '棉包膜（活动看板）', null, '化工农资', false, true, false
)
on conflict (product_code) do update
set product_name = excluded.product_name,
    category = excluded.category,
    is_fertilizer = excluded.is_fertilizer,
    is_demo_data = false,
    updated_at = now();

delete from product_price
where product_code = 'demo-cotton-film'
  and import_version = 'activity-board-v2-full-page-completion';

insert into product_price (
    product_code, execution_price, import_version, effective_at, is_demo_data
)
values (
    'demo-cotton-film', 2250.00, 'activity-board-v2-full-page-completion', now(), false
);

insert into inventory_snapshot (
    station_code, product_code, quantity, import_version, snapshot_at, is_demo_data
)
values (
    'default', 'demo-cotton-film', 100, 'activity-board-v2-full-page-completion', now(), false
)
on conflict (station_code, product_code, import_version) do update
set quantity = excluded.quantity,
    snapshot_at = excluded.snapshot_at,
    is_demo_data = false;

with rules(
    rule_id, activity_name, rule_type, priority, exclusive_group, source_row,
    product_codes, min_quantity, fixed_price, discount_rate
) as (
    values
        -- 自有水
        ('abv2-nono-water-gesang-330-case', '自有水-格桑泉330ML整件24.9元', 'FIXED_PRICE', 60, 'board_pack_price_water_gesang_330', 20,
            '["70256301","70545523","70545528","70545530"]'::jsonb, 12, 24.90, null),
        ('abv2-nono-water-gesang-500-case', '自有水-格桑泉500ML整件27.9元', 'FIXED_PRICE', 60, 'board_pack_price_water_gesang_500', 21,
            '["70256302","70494461","70545526"]'::jsonb, 12, 27.90, null),
        ('abv2-nono-water-wuyishan-333-case', '自有水-武夷山333ML整件31.9元', 'FIXED_PRICE', 60, 'board_pack_price_water_wuyishan_333', 22,
            '["70251989"]'::jsonb, 12, 31.90, null),
        ('abv2-nono-water-wuyishan-513-case', '自有水-武夷山513ML整件39.9元', 'FIXED_PRICE', 60, 'board_pack_price_water_wuyishan_513', 23,
            '["70251198"]'::jsonb, 12, 39.90, null),
        ('abv2-nono-water-wuyishan-45l-case', '自有水-武夷山4.5L两瓶整件50元', 'FIXED_PRICE', 60, 'board_pack_price_water_wuyishan_45l', 24,
            '["70254265"]'::jsonb, 2, 50.00, null),

        -- 中油奶饮
        ('abv2-nono-milk-organic-250-case', '中油奶饮-有机浓缩纯牛奶整件50元', 'FIXED_PRICE', 60, 'board_pack_price_milk_organic_250', 25,
            '["70559365","70688487"]'::jsonb, 10, 50.00, null),
        ('abv2-nono-milk-dream-250-case', '中油奶饮-梦幻盖纯牛奶整件60元', 'FIXED_PRICE', 60, 'board_pack_price_milk_dream_250', 26,
            '["70559370"]'::jsonb, 10, 60.00, null),
        ('abv2-nono-milk-beer-300-case', '中油奶饮-奶啤300ML整件55元', 'FIXED_PRICE', 60, 'board_pack_price_milk_beer_300', 27,
            '["70125186","70224290","70322580","70468233","70498529","70498531","70727309"]'::jsonb, 12, 55.00, null),

        -- 中油农资
        ('abv2-nono-fertilizer-two-bags-95', '中油农资-指定化肥满2袋享95折', 'PERCENTAGE_DISCOUNT', 58, 'board_fertilizer_two_bags', 28,
            '["70440943","70440945","70440947","70525187","70440933","70539754"]'::jsonb, 2, null, 0.95),
        ('abv2-nono-cotton-film-9-95', '中油农资-棉包膜满9卷享95折', 'PERCENTAGE_DISCOUNT', 57, 'board_cotton_film_discount', 29,
            '["demo-cotton-film"]'::jsonb, 9, null, 0.95),
        ('abv2-nono-cotton-film-27-93', '中油农资-棉包膜满27卷享93折', 'PERCENTAGE_DISCOUNT', 56, 'board_cotton_film_discount', 30,
            '["demo-cotton-film"]'::jsonb, 27, null, 0.93),
        ('abv2-nono-cotton-film-45-91', '中油农资-棉包膜满45卷享91折', 'PERCENTAGE_DISCOUNT', 55, 'board_cotton_film_discount', 31,
            '["demo-cotton-film"]'::jsonb, 45, null, 0.91),
        ('abv2-nono-cotton-film-90-88', '中油农资-棉包膜满90卷享88折', 'PERCENTAGE_DISCOUNT', 54, 'board_cotton_film_discount', 32,
            '["demo-cotton-film"]'::jsonb, 90, null, 0.88),

        -- 好客壹生
        ('abv2-nono-tissue-3pack-bogo', '好客壹生3包装纸面巾买一赠一', 'FIXED_PRICE', 60, 'board_pack_price_tissue_3pack', 33,
            '["70341453"]'::jsonb, 2, 15.00, null),
        ('abv2-nono-tissue-single-bogo', '好客壹生单包装纸面巾买一赠一', 'FIXED_PRICE', 60, 'board_pack_price_tissue_single', 33,
            '["70341452"]'::jsonb, 2, 4.00, null),

        -- 能量茶饮（同时校正V48同名规则的互斥组，避免不同商品相互排斥）
        ('abv2-nono-energy-dongpeng-250-pack3', '能量茶饮-东鹏特饮250ML三瓶9元', 'FIXED_PRICE', 60, 'board_pack_price_energy_dongpeng_250', 34,
            '["70235652"]'::jsonb, 3, 9.00, null),
        ('abv2-nono-energy-mix-pack2', '能量茶饮-红牛等指定饮料两瓶9元', 'FIXED_PRICE', 60, 'board_pack_price_energy_mix', 35,
            '["70356177","70453858","70166576","70282557","70524417"]'::jsonb, 2, 9.00, null),
        ('abv2-nono-nongfu-pack2', '能量茶饮-农夫山泉指定饮料两瓶8元', 'FIXED_PRICE', 60, 'board_pack_price_energy_nongfu', 36,
            '["70166516","70166517","70166518","70166519","70392421","70238841"]'::jsonb, 2, 8.00, null),

        -- 啤酒
        ('abv2-nono-beer-ipa98-single', '啤酒-好客三酉IPA98单瓶39.9元', 'FIXED_PRICE', 61, 'board_pack_price_beer_ipa98', 37,
            '["70531507"]'::jsonb, 1, 39.90, null),
        ('abv2-nono-beer-ipa98-case', '啤酒-好客三酉IPA98两瓶整件68元', 'FIXED_PRICE', 60, 'board_pack_price_beer_ipa98', 37,
            '["70531507"]'::jsonb, 2, 68.00, null),
        ('abv2-nono-beer-ale92-single', '啤酒-好客三酉艾尔92单瓶5元', 'FIXED_PRICE', 61, 'board_pack_price_beer_ale92', 37,
            '["70531511"]'::jsonb, 1, 5.00, null),
        ('abv2-nono-beer-ale92-case', '啤酒-好客三酉艾尔92六瓶整件26元', 'FIXED_PRICE', 60, 'board_pack_price_beer_ale92', 37,
            '["70531511"]'::jsonb, 6, 26.00, null),
        ('abv2-nono-beer-craft95-single', '啤酒-好客三酉精酿95单瓶12元', 'FIXED_PRICE', 61, 'board_pack_price_beer_craft95', 37,
            '["70531509"]'::jsonb, 1, 12.00, null),
        ('abv2-nono-beer-craft95-case', '啤酒-好客三酉精酿95六瓶整件68元', 'FIXED_PRICE', 60, 'board_pack_price_beer_craft95', 37,
            '["70531509"]'::jsonb, 6, 68.00, null),
        ('abv2-nono-beer-superx-500-pack3', '啤酒-雪花勇闯天涯SuperX三罐15元', 'FIXED_PRICE', 60, 'board_pack_price_beer_superx_500', 38,
            '["70410728"]'::jsonb, 3, 15.00, null),
        ('abv2-nono-beer-heineken-500-pack3', '啤酒-喜力经典500ML三罐21元', 'FIXED_PRICE', 60, 'board_pack_price_beer_heineken_500', 38,
            '["70199632"]'::jsonb, 3, 21.00, null),
        ('abv2-nono-beer-snow-330-pack3', '啤酒-雪花勇闯天涯330ML三罐9元', 'FIXED_PRICE', 60, 'board_pack_price_beer_snow_330', 38,
            '["70186321"]'::jsonb, 3, 9.00, null),

        -- 中油润辅
        ('abv2-nono-lube-washer-0c-pack2', '中油润辅-0℃玻璃水两瓶12元', 'FIXED_PRICE', 61, 'board_pack_price_lube_washer_0c', 39,
            '["70536790"]'::jsonb, 2, 12.00, null),
        ('abv2-nono-lube-washer-0c-case9', '中油润辅-0℃玻璃水九瓶整件35元', 'FIXED_PRICE', 60, 'board_pack_price_lube_washer_0c', 39,
            '["70536790"]'::jsonb, 9, 35.00, null),
        ('abv2-nono-lube-washer-minus40-bogo', '中油润辅--40℃玻璃水买一赠一', 'FIXED_PRICE', 60, 'board_pack_price_lube_washer_minus40', 40,
            '["70536789"]'::jsonb, 2, 12.00, null),
        ('abv2-nono-lube-gas-additive-case6', '中油润辅-昆仑之星汽油复合剂六支150元', 'FIXED_PRICE', 60, 'board_pack_price_lube_gas_additive', 41,
            '["407075","70192479"]'::jsonb, 6, 150.00, null),
        ('abv2-nono-lube-aus32-10kg-pack2', '中油润辅-AUS32尾气净化液10KG两桶60元', 'FIXED_PRICE', 60, 'board_pack_price_lube_aus32_10kg', 42,
            '["406696","409630","456857","70246423","70536791","70579075"]'::jsonb, 2, 60.00, null),
        ('abv2-nono-lube-aus32-20kg-pack2', '中油润辅-AUS32尾气净化液20KG两桶100元', 'FIXED_PRICE', 60, 'board_pack_price_lube_aus32_20kg', 43,
            '["456960","79405852","70536792","70579077"]'::jsonb, 2, 100.00, null)
), prepared as (
    select rule_id,
           rule_type,
           source_row,
           jsonb_build_object(
               'ruleId', rule_id,
               'activityName', activity_name,
               'ruleType', rule_type,
               'priority', priority,
               'exclusiveGroup', exclusive_group,
               'stackable', false,
               'status', 'CONFIRMED',
               'condition', jsonb_build_object(
                   'productCodes', product_codes,
                   'excludedCategories', jsonb_build_array(),
                   'fuelTypes', jsonb_build_array(),
                   'stationTypes', jsonb_build_array(),
                   'daysOfMonth', jsonb_build_array(),
                   'minCartAmount', 0,
                   'minFuelAmount', 0,
                   'memberRequired', false,
                   'minInventoryQuantity', 0,
                   'minProductQuantity', min_quantity
               ),
               'benefit', case
                   when rule_type = 'FIXED_PRICE'
                       then jsonb_build_object('type', 'FIXED_PRICE', 'fixedPrice', fixed_price)
                   else jsonb_build_object('type', 'PERCENTAGE_DISCOUNT', 'discountRate', discount_rate)
               end,
               'sourceSheetName', '非非促销（统建）',
               'sourceRowNumber', source_row,
               'version', 'activity-board-v2-full-page'
           ) as rule_json
    from rules
)
insert into promotion_rule_draft (
    draft_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, condition_json, benefit_json, rule_json,
    manual_locked, created_by, updated_at, is_demo_data
)
select 'draft-' || rule_id,
       rule_id,
       'activity-board-v2-full-page-completion',
       '非非促销（统建）',
       source_row,
       rule_type,
       'CONFIRMED',
       rule_json -> 'condition',
       rule_json -> 'benefit',
       rule_json,
       false,
       'flyway-activity-board-v2-full-page',
       now(),
       false
from prepared
on conflict (rule_id) do update
set source_import_id = excluded.source_import_id,
    source_sheet_name = excluded.source_sheet_name,
    source_row_number = excluded.source_row_number,
    rule_type = excluded.rule_type,
    status = excluded.status,
    condition_json = excluded.condition_json,
    benefit_json = excluded.benefit_json,
    rule_json = excluded.rule_json,
    updated_at = now(),
    is_demo_data = false;

insert into promotion_rule_version (
    version_id, rule_id, source_import_id, source_sheet_name, source_row_number,
    rule_type, status, rule_json, created_by, confirmed_at, confirmed_by,
    change_reason, is_demo_data
)
select 'ver-' || rule_id || '-v51',
       rule_id,
       source_import_id,
       source_sheet_name,
       source_row_number,
       rule_type,
       status,
       rule_json,
       'flyway-activity-board-v2-full-page',
       now(),
       'flyway-activity-board-v2-full-page',
       '落实活动看板非非促销第20-43行全部商品规则',
       false
from promotion_rule_draft
where source_import_id = 'activity-board-v2-full-page-completion'
on conflict (version_id) do nothing;

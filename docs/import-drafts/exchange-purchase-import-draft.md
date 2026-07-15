# 加油换购与组合包结构化导入草案

- 来源：`data/活动看板.xlsx` / `加油换购（统建）`
- 组合包：3 个
- 单品换购规则：12 条
- 规则版本：`activity-board-v2`

## 组合包

| bundleId | 名称 | 门槛油品金额 | 包价 | 商品明细 |
| --- | --- | ---: | ---: | --- |
| bundle-abv2-driving-package | 驾驶包 | 200.00 | 25.00 | 70356177x3, 70536790x2, 70341453x1 |
| bundle-abv2-water-drink-package | 水饮包 | 200.00 | 25.00 | 70536790x2, 70545526x4, 70125186x2, 70655834x2 |
| bundle-abv2-long-haul-package | 长途出行包 | 500.00 | 25.00 | 70545526x4, 70329538x2, 70481068x1, 70021292x1, 70498610x1, 70657932x10 |

## 单品换购

| ruleId | 油品 | 门槛 | 商品编码 | 换购价 | 数量 |
| --- | --- | ---: | --- | ---: | ---: |
| abv2-h2-small-water-gasoline | GASOLINE | 180 | 70545523 | 2.0 | 4 |
| abv2-h2-small-water-diesel | DIESEL | 300 | 70545523 | 2.0 | 4 |
| abv2-h2-big-water-gasoline | GASOLINE | 180 | 70545526 | 4.0 | 4 |
| abv2-h2-big-water-diesel | DIESEL | 300 | 70545526 | 4.0 | 4 |
| abv2-h2-redbull-gasoline | GASOLINE | 180 | 70356177 | 12.0 | 3 |
| abv2-h2-redbull-diesel | DIESEL | 300 | 70356177 | 12.0 | 3 |
| abv2-h2-juice-1l-gasoline | GASOLINE | 180 | 70727173 | 9.9 | 1 |
| abv2-h2-juice-1l-diesel | DIESEL | 300 | 70727173 | 9.9 | 1 |
| abv2-h2-juice-03l-gasoline | GASOLINE | 180 | 70727875 | 9.9 | 2 |
| abv2-h2-juice-03l-diesel | DIESEL | 300 | 70727875 | 9.9 | 2 |
| abv2-h2-milk-200-gasoline | GASOLINE | 180 | 70559364 | 19.9 | 1 |
| abv2-h2-milk-200-diesel | DIESEL | 300 | 70559364 | 19.9 | 1 |

## 规则说明

- 汽油与柴油门槛分开建模。
- 组合包只通过后端 `BUNDLE_PRICE` 规则参与计算。
- 商品组与组合包明细均落入结构化表，避免前端或 Controller 写促销判断。

# 数据导入执行阶段报告

日期：2026-07-10

## 1. 执行范围

本轮按 `docs/工程约束标准_v2_架构师修订版.md` 执行数据导入阶段，重点补齐活动看板结构化落地、导入缺口记录、券核销序列能力和 Flyway 验证。

## 2. 真实 Excel 读取结论

- `data/活动看板.xlsx` 存在，读取到 9 个工作表。
- `参考2-9.9元商品专区`：真实数据行 194 行，其中 190 行有商品编码，4 行缺商品编码。
- `参考3-会员生日&省区特色专用券范围`：工作表总行数 179 行，扣除 3 行表头后，真实 SKU 数据为 171 行。
- `data/价格.xlsx` 和 `data/库存.xlsx` 已只读核对，未修改。

## 3. Flyway 增量迁移

- `V11__product_group_mapping_completion.sql`：补 product_group 导入批次记录，并确认真实活动看板映射非 demo。
- `V12__99_zone_full_import.sql`：补 9.9 专区导入批次、4 条缺编码异常、9.9 规则互斥组和优先级。
- `V13__province_coupon_import.sql`：补省区特色券 demo 实例，全部 `is_demo_data = true`。
- `V14__exchange_purchase_bundle.sql`：补 H1 换购组合包导入批次，统一 `exclusiveGroup = H1_EXCHANGE`、`priority = 20`。
- `V15__g6_yili_activities.sql`：补 G6 伊力特买赠活动批次记录。
- `V16__coupon_sequence.sql`：补序列券字段和微信摇一摇演示序列券。

## 4. 代码补充

- `Coupon` 增加 `sequenceGroup`、`sequenceOrder`。
- `CouponEntity` 增加序列字段持久化映射。
- `CustomerContext` 增加 `paymentMethod` 和 e 享卡支付判断。
- `CheckoutCalculateRequest` 增加 `paymentMethod` 透传字段。
- `CouponRedeemBenefitCalculator` 增加序列券顺序核销和第 3 张起 e 享卡支付限制。

## 5. 测试覆盖

- Flyway 测试断言 V16 迁移、9.9 异常行、9.9 互斥组、H1 互斥组、省区券 demo 实例和序列券字段。
- 券核销测试增加 3 个序列券用例：
  - 已核销第 1 张后允许核销第 2 张。
  - 未核销第 1 张时阻止第 2 张。
  - 第 3 张起非 e 享卡支付被阻止，e 享卡支付允许。

## 6. 约束检查

- 未修改 `data/` 原始 Excel。
- 演示券实例和演示 SKU 保持 `is_demo_data = true`。
- 促销判断仍在后端 `ruleengine`。
- 前端不计算最终应付金额。
- 不可用序列券返回结构化 `BlockedPromotion` 原因。
- 原价兜底未改变。

## 7. 未完成项

- G5 复合候选仍未合并为单个 composite candidate。
- `bundle_item` 仍以具体 SKU 为主，后续可继续补 product_group 引用解析。
- 积分、充值、小程序派券生命周期仍属于外围系统设计，不进入本轮 checkout 价格计算。


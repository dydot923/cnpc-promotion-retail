# 第一阶段实施计划：促销智能计算引擎优先

## 1. 已识别数据源

### data/价格.xlsx

- 工作表：`0`
- 规模：12757 行，4 列；其中首行为表头，约 12756 条商品价格记录。
- 字段：`商品编码`、`商品名称`、`商品条码`、`执行价`
- 样例：
  - `70030041`，`黄山 金皖硬盒香烟(包) 13MG`，`6901028225106`，`28`
  - `70227684`，`真龙 (起源) 11MG`，`6901028011778`，`25`

### data/库存.xlsx

- 工作表：`0`
- 规模：455 行，4 列；其中首行为表头，约 454 条本站库存记录。
- 字段：`商品编码`、`商品名称`、`商品条码`、`库存数量`
- 样例：
  - `70030041`，`黄山 金皖硬盒香烟(包) 13MG`，`6901028225106`，`24.0`
  - `70390760`，`卧龙 猫耳酥甜辣味 138G`，`6931286064308`，`4.0`

### data/活动看板.xlsx

识别到 9 个工作表：

| 工作表 | 行列规模 | 主要字段/结构 | 建模用途 |
| --- | --- | --- | --- |
| 一体化营销活动看版 | 约 39 行 x 7 列 | 活动渠道、活动类型、优惠内容、备注 | 活动总览、自然语言规则来源 |
| 会员权益包 | 164 行 x 14 列 | 销售渠道、序号、权益包类型、权益定价、券明细、数量、备注 | 会员权益包、赠券、券包规则 |
| 加油换购（统建） | 22 行 x 13 列 | 序号、品类、商品名称、促销政策、促销数量、促销价、主油承担、非油承担、价格到位率、毛利率、优先级 | 加油换购、组合包、单品换购 |
| 非非促销（统建） | 43 行 x 15 列 | 类型、重点品类、营销事件、推广主题、活动内容；下方另有商品级促销表 | 全场折扣、满减、买赠、惊爆价、整件优惠 |
| LNG+CNG | 45 行 x 9 列 | 类型、商品编码、商品名称、售价、对应单品编码、对应单一商品、对应数量、规格 | LNG/CNG 权益包与包内商品 |
| 参考1-非非促销（个性化促销） | 514 行 x 16 列 | 商品编码、商品名称、采购价、库存成本、促销价、建议零售价、促销后毛利率、促销资源、库存数量 | 个性化促销商品池 |
| 参考2-9.9元商品专区 | 197 行 x 15 列 | 商品品类、商品编码、商品名称、采购价、挂牌零售价、数量、促销金额、毛利率 | `fixed_price` 9.9 元专区 |
| 参考3-会员生日&省区特色专用券范围 | 179 行 x 14 列 | 品类、商品编码、商品名称、建议零售价、可核销券种 | 会员生日券、省区特色券适用范围 |
| 参考4-“一卡通”销售站点明细 | 302 行 x 14 列 | 分公司、地市、站点名称、HOS 编码、网点编码、联系人、经纬度 | 站点范围与敏感联系人数据 |

活动看板大量使用合并单元格和空值继承，导入时必须保留原始行号、工作表名、导入版本，并生成异常报告。

## 2. 系统目标理解

一期目标不是先做页面，而是先把真实 Excel 转为结构化商品、价格、库存和促销规则数据，并建立可解释、可测试、可追溯的促销智能计算引擎。

系统应能在结算时返回：

- 原价金额、应付金额、优惠金额；
- 推荐促销方案；
- 其他可选促销方案；
- 不可用促销及不可用原因；
- 解释文本；
- 规则版本；
- 库存警告；
- 原价兜底方案。

## 3. 技术栈

- 后端：Java 21、Spring Boot 3.x、PostgreSQL、MyBatis Plus、Flyway、EasyExcel、Springdoc OpenAPI、JUnit 5、AssertJ、Mockito、Testcontainers。
- 前端：React 18、TypeScript、Vite、Ant Design、TanStack Query、Zustand。

## 4. 核心约束

1. 促销逻辑只能集中在后端 `ruleengine`。
2. 前端、Controller、普通 Service 不写促销判断逻辑。
3. 金额使用 `BigDecimal`，禁止 `double` 和 `float`。
4. 商品编码、条码使用 `String`。
5. 原价结算始终可用。
6. 不可用促销也必须返回不可用原因。
7. Excel 导入必须生成异常报告，不得静默丢弃异常行。
8. 不得静默覆盖人工修正规则。
9. AI 海报不能作为价格和促销文案的真相来源。

## 5. 第一阶段里程碑

### M1：工程与规则模型

- 创建 `backend/`、`frontend/` 基础结构。
- 创建 Spring Boot 后端工程。
- 建立 `common/auth/product/price/inventory/promotion/ruleengine/checkout/replenishment/poster/importcenter/audit` 包边界。
- 定义 `OrderContext`、`FuelContext`、`CustomerContext`、`CartItem`。
- 定义 `PromotionRule`、`PromotionCandidate`、`BlockedPromotion`、`CalculationResult`。

### M2：规则引擎骨架

- 实现 `PromotionEngine`。
- 实现 `ConditionMatcher`。
- 实现 `BenefitCalculator` 扩展点。
- 实现 `ConflictResolver`、`CandidateRanker`、`ExplanationBuilder`。
- 首批接入 `fixed_price`、`percentage_discount`、`amount_off`、`exchange_purchase`、`gift_item`、`gift_coupon`、`bundle_price`。

### M3：真实样例测试

- 9.9 元固定价。
- 逢 9 全场 9 折。
- 香烟、化肥排除。
- 汽油/柴油满额换购。
- 买赠、赠券、组合包。
- 库存不足、油品金额不足。
- 多促销互斥。
- 原价兜底和不可用原因。

### M4：导入设计与 API

- `POST /api/import/prices`
- `POST /api/import/inventory`
- `POST /api/import/promotions`
- EasyExcel 字符串读取商品编码和条码，`BigDecimal` 读取金额。
- 合并单元格、空值继承、异常行报告、待确认规则状态。

### M5：结算 API

- `POST /api/checkout/calculate`
- `POST /api/checkout/confirm`
- Controller 仅做参数接收和响应，促销计算全部走 `ruleengine`。


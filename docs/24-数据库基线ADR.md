# ADR: 数据库基线与后续新增规则

## 状态

已采纳。

## 背景

`docs/21-doubao-缺失功能分析与开发文档.md` 和 `docs/22-doubao-系统架构与数据库设计文档.md` 中规划了 V23-V30 的新增迁移，包括会员、券、商品组、组合包、交易和库存预警等表。

但当前代码库实际已经在 V1-V22 中提前落地了部分能力：

- V4 已创建 `coupon_template` 和 `coupon`。
- V5 已创建 `bundle`、`bundle_item`、`product_group`、`product_group_item`。
- V3 已创建 `checkout_confirmation` 和 `replenishment_list`。
- V2 已创建 `checkout_calculation_record`、规则草稿、规则版本和规则审计表。

因此，后续数据库新增不能直接照搬 21/22 号文档中的 V23-V30 表结构，否则会产生同名表、重复能力或字段口径冲突。

## 决策

1. 以当前 Flyway V1-V22 迁移和现有 MyBatis Entity 为事实基线。
2. 后续迁移只从 V23 往后追加，不改写 V1-V22。
3. 已存在的能力采用“演进现有表”策略：
   - `coupon_template` 继续作为券模板表。
   - `coupon` 继续作为券实例表，不另建 `coupon_instance`。
   - `bundle` / `bundle_item` 继续作为组合包表。
   - `product_group` / `product_group_item` 继续作为商品组表。
   - `checkout_confirmation` 继续作为结算确认表。
4. 尚未存在的能力新增表：
   - V23 新增 `member_level`、`member`。
   - V24 新增交易流水表，使用 `checkout_transaction`、`checkout_transaction_item`，避免使用 `transaction` 作为表名。
   - V25 新增库存预警处理状态表 `inventory_alert_record`，保留当前实时计算模型。
5. 新增接口和领域模型优先对齐现有代码命名，而不是文档草案中的旧字段名。

## 当前表口径

| 能力 | 当前事实表 | 说明 |
| --- | --- | --- |
| 商品 | `product` | 商品编码、名称、条码、品类、香烟/化肥标记 |
| 价格 | `product_price` | 执行价和导入版本 |
| 库存快照 | `inventory_snapshot` | 站点、商品、数量、导入版本 |
| 促销规则 | `promotion_rule` | 历史基础表，当前主要通过规则版本和草稿治理 |
| 规则草稿 | `promotion_rule_draft` | 导入后待确认规则 |
| 规则版本 | `promotion_rule_version` | CONFIRMED/DISABLED 规则版本 |
| 规则审计 | `promotion_rule_audit_log` | 规则治理操作日志 |
| 结算计算记录 | `checkout_calculation_record` | 保存计算请求和结果快照 |
| 结算确认 | `checkout_confirmation` | 保存被选择的候选方案快照 |
| 交易流水 | `checkout_transaction` / `checkout_transaction_item` | 保存确认后的交易头和商品明细 |
| 券模板 | `coupon_template` | 字段为 `coupon_template_id`、`face_value`、`min_spend_amount` 等 |
| 券实例 | `coupon` | 字段为 `coupon_id`、`holder_member_id`、`status`、`used_at` 等 |
| 会员 | `member` / `member_level` | 会员基础信息、等级、积分、生日月和状态 |
| 组合包 | `bundle` / `bundle_item` | 主键为字符串 `id` |
| 商品组 | `product_group` / `product_group_item` | 主键为字符串 `id` |
| 库存预警处理记录 | `inventory_alert_record` | 保存预警处理状态、处理人、处理时间和关联补货清单 |
| 补货清单 | `replenishment_list` | 保存补货清单快照 |
| 通用审计 | `audit_log` | 业务操作审计 |

## 与 21/22 号文档规划的映射

| 文档规划 | 当前处理 |
| --- | --- |
| `coupon_instance` | 不新增，映射到当前 `coupon` 表 |
| `template_code` | 不新增同义字段，继续使用 `coupon_template_id` |
| `coupon_code` | 不新增同义字段，继续使用 `coupon_id` |
| `bundle_code` | 不新增同义字段，继续使用 `bundle.id` |
| `group_code` | 不新增同义字段，继续使用 `product_group.id` |
| `transaction` | 不使用该名称，后续新增 `checkout_transaction` |
| `inventory_alert` | 不直接保存实时预警快照；处理状态保存到 `inventory_alert_record` |

## 当前执行进展（2026-07-13）

- V23 已新增会员基础表，并接入会员识别、会员券查询和 checkout 服务端加载会员券。
- V24 已新增结算交易流水和交易明细表，确认结算后生成可查询的交易事实记录。
- V25 已新增库存预警处理记录表，保留实时计算预警，同时支持处理状态持久化。

## 后果

正面影响：

- 避免重复建表和字段口径漂移。
- 后续开发可以直接复用现有仓储、实体和迁移测试。
- 会员、券、交易新增会沿着现有业务闭环演进。

约束：

- 21/22 号文档中的数据库表名不能再直接作为实现依据。
- 新增功能需要优先查当前迁移和实体，再决定是否新建表或补字段。

## 验收

- 新增迁移从 V23 开始。
- 全量 Flyway 迁移在空库中通过。
- `mvn test` 通过。
- 文档和接口说明不再要求创建已存在的同义表。

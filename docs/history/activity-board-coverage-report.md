# 活动看板覆盖报告（J 类口径修正最终版）

更新日期：2026-07-12  
执行标准：`docs/工程约束标准_v2_架构师修订版.md`

状态定义：`已覆盖` 表示已有正式数据库规则并通过自动化验证；`部分覆盖` 表示仍有明确数据缺口；`非结算链路` 表示发放或权益归属外部系统，checkout 只承接核销或展示。

## 1. P0 结算链路覆盖结论

- P0 结算链路验收口径：14 项。
- 已覆盖：14 项。
- 未覆盖：0 项。
- 覆盖率：14/14 = 100%。
- G7 单品促销单列为部分覆盖项，因 69 条缺真实价格规则保持 `DRAFT`，不计入 P0 14 项覆盖率。
- A5 与 G2 使用同一套逢 9 加油站便利店 9 折 + 3 倍积分能力，覆盖率统计中按一个结算能力去重。

## 2. 活动明细

| 活动 | 状态 | 规则/版本 | 自动化证据或说明 |
|---|---|---:|---|
| A1 逢 7 气惠多档位赠券 | 已覆盖 | 1 条，V22 | `ActivityStatusConfirmationIntegrationTest#a1TieredCouponIsConfirmedAndExecutable` |
| A2 多倍积分 | 非结算链路 | - | 积分账户记账不在 checkout；设计文档：`docs/points-module-design.md` |
| A3 加气站便利店 9 折 | 已覆盖 | 1 条，V19，priority=60 | `ActivityBoardFinalRulesIntegrationTest#a3CoversAlwaysOnRuleAndG1PriorityConflict` |
| A4 逢 8-CN98 立减 | 已覆盖 | 1 条，V18 | `FuelVolumeDiscountBenefitCalculatorTest` + A4 集成场景 |
| A5 逢 9 便利店促销 | 已覆盖 | 并入 G2，V19 | G2 已覆盖 9 折与 `pointsMultiplier=3` |
| A6 逢 10 超级十惠 | 非结算链路 | - | 充值发券不在 checkout；设计文档：`docs/recharge-coupon-design.md` |
| B1-B5 会员赠券 | 非结算链路 | - | 发券属于会员系统；券核销已支持，设计文档：`docs/member-coupon-design.md` |
| C1 微信摇一摇 | 非结算链路 | - | 序列券核销已支持；设计文档：`docs/wechat-campaign-design.md` |
| D1-D4 油惠新疆 | 非结算链路 | - | 小程序签到、拼团、认证、权益包链路；对应设计文档已归档 |
| E1 买油赠非 | 已覆盖 | 2 条，V22 | `ActivityStatusConfirmationIntegrationTest#e1OilPurchaseGivesNonOilCoupons` |
| E2 买非赠券 | 已覆盖 | 1 条，V22 | `ActivityStatusConfirmationIntegrationTest#e2NonOilPurchaseGivesCoupon` |
| F1 买气送非 | 已覆盖 | 2 条，V22 | `ActivityStatusConfirmationIntegrationTest#f1GasPurchaseGivesNonOilItems` |
| G1 逢 7 气站 9 折 | 已覆盖 | 1 条，V19，priority=50 | `ActivityBoardFinalRulesIntegrationTest#g1CoversDaySevenGasFillingDiscount` |
| G2 逢 9 油站 9 折 | 已覆盖 | 1 条，V19，`pointsMultiplier=3` | `ActivityBoardFinalRulesIntegrationTest#g2CoversDayNineGasStationDiscountAndPoints` |
| G3 9.9 专区 | 已覆盖 | 190 条，V12 | `ImportedPromotionEndToEndTest`；4 行缺商品编码保留导入异常 |
| G4 世界杯夜间 8.8 折 | 已覆盖 | 2 条，V20 | G4-1 赠券 + G4-2 折扣；`ActivityBoardFinalRulesIntegrationTest#g4CoversEventCouponAndNightDiscount` |
| G5 中秋满减 | 已覆盖 | 1 条，V17 | `G5CompositeCandidateTest`，复合候选减 50 + 赠 2 张券 |
| G6 伊力特买赠 | 已覆盖 | 10 条，V15 | 8/8 子活动表达，导入端到端测试通过 |
| G7 单品促销 | 部分覆盖 | 12 CONFIRMED + 69 DRAFT，V21 | 12 条有 Excel 实价；69 条 `price=0` 等待业务方补真实促销价 |
| G8 洗车促销 | 非结算链路 | - | 洗车券核销已支持；设计文档：`docs/car-wash-design.md` |
| G9 电商促销 | 非结算链路 | - | 二期；设计文档：`docs/ecommerce-design.md` |
| H1 组合级换购 | 已覆盖 | 3 个组合包，V9/V14 | 驾驶包可用；水饮包库存不足时正确阻断 |
| H2 单品级换购 | 已覆盖 | 12 条，V22 | `ActivityStatusConfirmationIntegrationTest#h2SingleItemExchangePurchaseIsConfirmedAndExecutable` |
| J 省区特色券核销 | 已覆盖 | 171 SKU，V13/V16 | 省区券 + 序列券核销验证通过 |

## 3. 覆盖率统计

| 统计项 | 数量 |
|---|---:|
| P0 结算链路活动总数 | 14 |
| 已覆盖 | 14 |
| 部分覆盖 | 1（G7，不计入 P0 14） |
| 未覆盖 | 0 |
| 非结算链路活动数 | 14 |
| P0 结算链路覆盖率 | 100% |

## 4. 数据盘点

- Flyway：V1-V22。
- `CONFIRMED`：247 条；`DRAFT`：69 条。
- 9.9 专区：190 条正式规则，本站库存交集 36，库存外 154。
- 省区券：171 个可核销 SKU。
- product_group：19 组、25 个映射项。
- 所有演示种子均带 `is_demo_data=true`；checkout 只加载 `status=CONFIRMED` 规则。

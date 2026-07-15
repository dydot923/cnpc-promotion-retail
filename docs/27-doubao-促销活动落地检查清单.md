# 促销活动落地检查清单

## 文档说明

本文档基于 `docs/19-doubao-促销功能分析与规则引擎设计.md`、`docs/20-doubao-核心产品需求文档.md`、`docs/21-doubao-缺失功能分析与开发文档.md`、`docs/22-doubao-系统架构与数据库设计文档.md`，整合促销活动百分百落地所需的所有功能、数据、接口和代码检查项。

---

## 一、促销活动类型检查清单

### 1.1 油品促销

| 促销类型 | 规则引擎 | 数据导入 | 条件匹配 | 赠券发放 | 会员关联 | 状态 | 缺失项 |
|---------|---------|---------|---------|---------|---------|------|--------|
| 油品立减（CN98立减0.8元/升） | ✅ FUEL_VOLUME_DISCOUNT | ❌ | ⚠️ 日期触发需P0-3 | - | - | ⚠️ | 规则数据导入 |
| 油品消费赠券（逢7气惠） | ✅ GIFT_COUPON | ❌ | ⚠️ 日期触发需P0-3 | ⚠️ | ⚠️ | ❌ | 规则数据、券模板、会员关联 |

### 1.2 非油促销

| 促销类型 | 规则引擎 | 数据导入 | 条件匹配 | 排除品类 | 状态 | 缺失项 |
|---------|---------|---------|---------|---------|------|--------|
| 固定价促销（9.9元专区） | ✅ FIXED_PRICE | ✅ | ✅ | - | ✅ | - |
| 全场折扣（逢9便利店9折） | ✅ PERCENTAGE_DISCOUNT | ❌ | ⚠️ 日期触发需P0-3 | ❌ | ⚠️ | ❌ | 规则数据、排除品类校验 |
| 满减（中秋团圆礼） | ✅ AMOUNT_OFF | ❌ | ⚠️ 日期触发需P0-3 | - | ⚠️ | ❌ | 规则数据、会员关联 |
| 买赠（啤酒买赠） | ✅ GIFT_ITEM | ❌ | ⚠️ 日期触发需P0-3 | - | - | ❌ | 规则数据 |
| 整件优惠 | ✅ BUNDLE_PRICE | ❌ | - | - | - | ❌ | 规则数据 |
| 惊爆价（逐光赛场夜间8.8折） | ✅ PERCENTAGE_DISCOUNT | ❌ | ⚠️ 日期触发+时间范围 | - | - | ❌ | 规则数据 |

### 1.3 加油换购

| 促销类型 | 规则引擎 | 数据导入 | 条件匹配 | 状态 | 缺失项 |
|---------|---------|---------|---------|------|--------|
| 组合包换购（行车包、水饮包） | ✅ EXCHANGE_PURCHASE | ✅ | ✅ | ✅ | - |
| 单品换购 | ✅ EXCHANGE_PURCHASE | ❌ | ✅ | ❌ | 规则数据 |

### 1.4 会员促销

| 促销类型 | 规则引擎 | 数据导入 | 会员等级 | 生日月 | 状态 | 缺失项 |
|---------|---------|---------|---------|---------|------|--------|
| 会员生日券 | ✅ COUPON_REDEEM | ❌ | ❌ | ❌ | ❌ | 规则数据、生日月条件匹配 |
| 会员权益包 | ✅ 独立模块 | ✅ | ✅ | - | ✅ | - |
| 会员专享价 | ✅ FIXED_PRICE + 会员条件 | ❌ | ❌ | - | ❌ | 规则数据、会员条件匹配 |
| 省区特色券 | ✅ COUPON_REDEEM | ❌ | ❌ | - | ❌ | 规则数据 |

### 1.5 其他促销

| 促销类型 | 规则引擎 | 数据导入 | 条件匹配 | 状态 | 缺失项 |
|---------|---------|---------|---------|------|--------|
| 节日活动（逐光赛场、中秋团圆礼） | ✅ 复合规则 | ❌ | ⚠️ 日期范围触发 | ❌ | 规则数据 |
| 积分促销（多倍积分） | ✅ POINTS_ACTIVITY | ✅ | ⚠️ 日期触发需P0-3 | ✅ | - |
| LNG/CNG专享 | ✅ GIFT_COUPON | ❌ | ⚠️ 日期触发需P0-3 | ⚠️ | ❌ | 规则数据、券模板 |

---

## 二、条件匹配器检查清单

### 2.1 已实现匹配器

| 匹配器 | 文件 | 状态 | 支持条件 |
|-------|------|------|---------|
| DateConditionMatcher | condition/DateConditionMatcher.java | ✅ | condition_json日期范围、promotion_date_trigger |
| StationTypeConditionMatcher | condition/StationTypeConditionMatcher.java | ✅ | 站点类型 |
| ProvinceConditionMatcher | condition/ProvinceConditionMatcher.java | ✅ | 省份 |
| TimeRangeConditionMatcher | condition/TimeRangeConditionMatcher.java | ✅ | 时间范围（支持跨午夜） |
| MemberConditionMatcher | condition/MemberConditionMatcher.java | 🔄 | 基础会员身份（待接入会员等级） |
| FuelTypeConditionMatcher | condition/FuelTypeConditionMatcher.java | ✅ | 油品类型 |

### 2.2 缺失匹配器

| 匹配器 | 用途 | 优先级 | 缺失项 |
|-------|------|--------|--------|
| StationScopeConditionMatcher | 站点范围校验 | P1 | 新建 |
| ExcludedCategoryConditionMatcher | 排除品类校验 | P0 | 新建 |
| RechargeAmountConditionMatcher | 充值金额条件 | P0 | 新建 |

---

## 三、数据导入检查清单

### 3.1 促销规则数据

| 活动名称 | 规则ID | 规则类型 | 日期触发 | 会员等级 | 状态 | 缺失项 |
|---------|--------|---------|---------|---------|------|--------|
| 逢7-气惠 | abv2-a5-day7 | GIFT_COUPON | ✅ | ❌ | ❌ | 规则数据、券模板 |
| 逢8-CN98立减 | abv2-a5-day8 | FUEL_VOLUME_DISCOUNT | ✅ | - | ❌ | 规则数据 |
| 逢9-便利店促销 | abv2-a5-day9 | PERCENTAGE_DISCOUNT | ✅ | - | ❌ | 规则数据、排除品类 |
| 逢10-超级十惠 | abv2-a5-day10 | GIFT_COUPON | ✅ | ✅ | ❌ | 规则数据、充值条件、券模板 |
| 逐光赛场 | abv2-b3-zgsc | PERCENTAGE_DISCOUNT | ✅ | - | ❌ | 规则数据 |
| 中秋团圆礼 | abv2-b4-zqtyl | AMOUNT_OFF + GIFT_COUPON | ✅ | - | ❌ | 规则数据、券模板 |
| 会员生日券 | abv2-c1-birthday | COUPON_REDEEM | - | - | ❌ | 规则数据、生日月条件 |
| LNG/CNG权益包促销 | abv2-c2-lngcng | GIFT_COUPON | ✅ | - | ❌ | 规则数据、券模板 |

### 3.2 券模板数据

| 券名称 | 面额 | 使用门槛 | 有效期 | 状态 | 缺失项 |
|--------|------|---------|--------|------|--------|
| 10元LNG券 | 10元 | 满500元 | 15天 | ❌ | 券模板 |
| 30元LNG券 | 30元 | 满1000元 | 15天 | ❌ | 券模板 |
| 60元LNG券 | 60元 | 满1500元 | 15天 | ❌ | 券模板 |
| 100元LNG券 | 100元 | 满2000元 | 15天 | ❌ | 券模板 |
| 6元便利店券 | 6元 | 满30元（除香烟） | 60天 | ❌ | 券模板 |
| 12元便利店券 | 12元 | 满50元（除香烟） | 60天 | ❌ | 券模板 |
| 10元洗车券 | 10元 | 满11元 | 30天 | ❌ | 券模板 |
| 12元汽油券 | 12元 | 满200元 | 60天 | ❌ | 券模板 |
| 15元高标号汽油券 | 15元 | 满200元 | 60天 | ❌ | 券模板 |
| 啤酒赠券 | 0元 | 指定啤酒品类 | 60天 | ❌ | 券模板 |
| 生日专属券 | 15元 | 满200元 | 30天 | ❌ | 券模板 |

### 3.3 排除品类数据

| 品类名称 | 说明 | 状态 | 缺失项 |
|---------|------|------|--------|
| 香烟 | 排除全场折扣 | ❌ | 数据导入 |
| 化肥 | 排除全场折扣 | ❌ | 数据导入 |

---

## 四、服务层检查清单

### 4.1 已实现服务

| 服务 | 文件 | 状态 | 功能 |
|------|------|------|------|
| BenefitPackageService | promotion/benefitpackage/ | ✅ | 权益包查询、购买 |
| StationService | station/ | ✅ | 站点查询、自动补齐 |
| MemberService | member/ | ✅ | 会员CRUD、积分变动、券查询 |
| CouponRepository | promotion/coupon/ | ✅ | 券查询、核销、发放 |
| PointsActivityRepository | pointsactivity/ | ✅ | 积分活动查询 |
| PromotionDateTriggerRepository | ruleengine/datetrigger/ | ✅ | 日期触发器查询 |

### 4.2 缺失服务

| 服务 | 用途 | 优先级 | 缺失项 |
|------|------|--------|--------|
| PromotionExcludedCategoryRepository | 排除品类查询 | P0 | 新建 |
| PromotionStationScopeRepository | 站点范围查询 | P1 | 新建 |
| PointsExchangeService | 积分兑换 | P1 | 新建 |
| LotteryActivityService | 积分抽奖 | P1 | 新建 |

---

## 五、结算流程检查清单

### 5.1 已实现流程

| 流程 | 文件 | 状态 | 功能 |
|------|------|------|------|
| 结算计算 | CheckoutApplicationService.calculate() | ✅ | 规则匹配、价格计算 |
| 券核销 | CheckoutApplicationService.confirm() | ✅ | 条件更新防并发 |
| 赠券发放 | CheckoutApplicationService.confirm() | ✅ | 候选促销赠券 |
| 积分计算 | CheckoutApplicationService.confirm() | ✅ | 会员等级倍率、活动倍率 |
| 交易流水 | CheckoutTransactionRepository | ✅ | 写入交易记录 |
| 站点自动补齐 | CheckoutApplicationService.buildStationContext() | ✅ | stationCode→stationType/province/city |

### 5.2 缺失流程

| 流程 | 用途 | 优先级 | 缺失项 |
|------|------|--------|--------|
| 充值场景结算 | 逢10超级十惠充值赠券 | P0 | rechargeAmount字段、充值条件匹配 |
| 新会员自动发券 | 新增会员礼遇 | P0 | MemberService新增触发逻辑 |
| 潜在会员化发券 | 已注册未办卡赠券 | P0 | 新增触发接口 |
| 排除品类校验 | 全场折扣排除香烟/化肥 | P0 | 在价格计算中排除 |
| 站点范围校验 | 促销规则的站点范围限制 | P1 | 在规则匹配中校验 |

---

## 六、API接口检查清单

### 6.1 已实现接口

| 接口 | 路径 | 状态 |
|------|------|------|
| 权益包列表 | GET /api/benefit-packages | ✅ |
| 权益包详情 | GET /api/benefit-packages/{packageCode} | ✅ |
| 权益包购买 | POST /api/benefit-packages/{packageCode}/purchase | ✅ |
| 会员已购权益包 | GET /api/members/{memberCode}/benefit-packages | ✅ |
| 站点列表 | GET /api/stations | ✅ |
| 站点详情 | GET /api/stations/{stationCode} | ✅ |
| 会员识别 | POST /api/members/identify | ✅ |
| 会员详情 | GET /api/members/{memberCode} | ✅ |
| 会员券查询 | GET /api/members/{memberCode}/coupons | ✅ |
| 积分变动 | POST /api/members/{memberCode}/points | ✅ |
| 积分流水 | GET /api/members/{memberCode}/points | ✅ |
| 结算计算 | POST /api/checkout/calculate | ✅ |
| 确认结算 | POST /api/checkout/confirm | ✅ |
| 交易记录 | GET /api/checkout/records | ✅ |

### 6.2 缺失接口

| 接口 | 路径 | 用途 | 优先级 |
|------|------|------|--------|
| 潜在会员化发券 | POST /api/members/{memberCode}/activation-coupons | 已注册未办卡会员发券 | P0 |
| 积分兑换 | POST /api/members/{memberCode}/points/exchange | 积分兑换商品/折扣 | P1 |
| 积分抽奖 | POST /api/members/{memberCode}/points/lottery | 积分抽奖 | P1 |
| 促销活动列表 | GET /api/promotions | 查询当前生效促销活动 | P1 |
| 促销规则导入 | POST /api/promotions/import | 从Excel导入促销规则 | P1 |

---

## 七、数据库表检查清单

### 7.1 已创建表

| 表名 | 状态 | 数据量 |
|------|------|--------|
| station | ✅ | 297条 |
| promotion_date_trigger | ✅ | 6条 |
| points_activity | ✅ | 2条 |
| member_points_change | ✅ | 0条 |
| benefit_package | ✅ | 14条 |
| benefit_package_item | ✅ | 141条 |
| benefit_package_purchase | ✅ | 0条 |
| member | ✅ | 0条 |
| member_level | ✅ | 4条 |
| checkout_transaction | ✅ | 0条 |
| checkout_transaction_item | ✅ | 0条 |
| inventory_alert_record | ✅ | 0条 |
| coupon | ✅ | 0条 |
| coupon_template | ✅ | 0条 |
| promotion_rule | ✅ | 若干 |

### 7.2 缺失数据

| 表名 | 需要的数据 | 优先级 |
|------|-----------|--------|
| promotion_rule | 逢7/8/9/10、逐光赛场、中秋团圆礼、会员生日券等规则 | P0 |
| coupon_template | 12种券模板 | P0 |
| coupon | 发放的券实例 | P0 |
| promotion_excluded_category | 香烟、化肥排除品类 | P0 |
| promotion_station_scope | 促销活动适用站点范围 | P1 |
| member | 测试会员数据 | P1 |

---

## 八、核心缺失功能汇总

### 8.1 P0优先级（必须完成）

| 序号 | 缺失功能 | 影响活动 | 解决方式 |
|------|---------|---------|---------|
| P0-1 | 促销规则数据未导入 | 所有日期触发活动 | 编写Flyway迁移脚本导入规则 |
| P0-2 | 券模板数据未导入 | 所有赠券活动 | 编写Flyway迁移脚本导入券模板 |
| P0-3 | 排除品类校验未实现 | 逢9便利店9折 | 新建ExcludedCategoryConditionMatcher |
| P0-4 | 充值场景未实现 | 逢10超级十惠 | 新增rechargeAmount字段和匹配器 |
| P0-5 | 新会员自动发券 | 新增会员礼遇 | 修改MemberService |
| P0-6 | 潜在会员化发券 | 潜在会员化 | 新增触发接口 |

### 8.2 P1优先级（建议完成）

| 序号 | 缺失功能 | 影响活动 | 解决方式 |
|------|---------|---------|---------|
| P1-1 | 站点范围校验 | 区域促销活动 | 新建StationScopeConditionMatcher |
| P1-2 | 积分兑换闭环 | 逢9积分兑换9折 | 新建PointsExchangeService |
| P1-3 | 积分抽奖 | 逢9积分抽奖 | 新建LotteryActivityService |
| P1-4 | 促销活动列表接口 | 促销管理页面 | 新建PromotionController |

---

## 九、检查步骤

### 9.1 步骤1：规则引擎基础能力检查

```bash
# 检查日期触发
mvn -Dtest=DateConditionMatcherTest test

# 检查会员条件
mvn -Dtest=MemberConditionMatcherTest test

# 检查结算服务
mvn -Dtest=CheckoutApplicationServiceTest test

# 全量测试
mvn test
```

### 9.2 步骤2：数据库数据检查

```bash
# 检查Flyway迁移
mvn -Dtest=FlywayMigrationTest test

# 检查V26数据导入
# 期望：297个站点、6个日期触发器、14个权益包、141个权益包明细
```

### 9.3 步骤3：接口功能检查

```bash
# 启动服务后测试
# 1. GET /api/stations 应返回297个站点
# 2. GET /api/benefit-packages 应返回14个权益包
# 3. POST /api/checkout/calculate 应正确匹配促销规则
# 4. POST /api/checkout/confirm 应完成券核销和赠券发放
```

### 9.4 步骤4：促销活动验证

| 活动 | 测试场景 | 预期结果 |
|------|---------|---------|
| 逢7-气惠 | 7月7日CNG消费500元 | 赠30元LNG券 + 3倍积分 |
| 逢8-CN98立减 | 7月8日加CN98 10升 | 立减8元 |
| 逢9-便利店促销 | 7月9日买零食（非香烟） | 9折 + 3倍积分 |
| 逢9-便利店促销 | 7月9日买香烟 | 不享受折扣 |
| 逢10-超级十惠 | 7月10日充值1000元 | 赠汽油券+便利店券+洗车券 |
| 逢10-超级十惠 | 7月10日金卡会员充值1000元 | 普通券 + 加赠高标号汽油券 |

---

## 十、结论

### 10.1 核心缺失项

促销活动百分百落地需要完成以下核心工作：

1. **导入促销规则数据**（P0-1）：逢7/8/9/10、逐光赛场、中秋团圆礼、会员生日券等规则
2. **导入券模板数据**（P0-2）：12种券模板
3. **实现排除品类校验**（P0-3）：全场折扣排除香烟、化肥
4. **实现充值场景**（P0-4）：逢10超级十惠充值赠券
5. **实现新会员自动发券**（P0-5）：新增会员礼遇
6. **实现潜在会员化发券**（P0-6）：已注册未办卡赠券

### 10.2 完成标准

所有促销活动百分百落地的完成标准：

| 标准 | 说明 |
|------|------|
| 规则引擎 | 日期触发、会员等级、排除品类、充值条件全部接入 |
| 数据完整 | 所有促销规则和券模板已导入数据库 |
| 流程闭环 | 结算→核销→赠券→积分→交易流水完整闭环 |
| 测试通过 | 全量测试通过，端到端测试验证核心活动 |
| 文档齐全 | API文档、数据库文档、操作手册齐全 |

---

**文档结束**
# 功能差距分析与开发路线图

## 文档说明

本文档全面分析活动看板业务需求与当前代码实现的差距，明确未实现功能的根因，并制定详细的开发路线图。

---

## 一、业务需求总览

根据 `data/活动看板.xlsx`，业务需求包含以下核心活动：

| 活动大类 | 活动名称 | 触发条件 | 优惠内容 |
|---------|---------|---------|---------|
| **品牌营销活动** | 逢7-气惠 | 每月7/17/27日 | LNG券+便利店券+CNG/LNG 3倍积分 |
| | 逢8-CN98立减 | 每月8/18/28日 | CN98立减0.8元/升 |
| | 逢9-便利店促销 | 每月9/19/29日 | 便利店9折+积分兑换9折+3倍积分 |
| | 逢10-超级十惠 | 每月10/20/30日 | 充值1000/2000赠券 |
| **会员活动** | 新增会员礼遇 | 首次开卡 | 汽油券+便利店券+洗车券 |
| | 潜在会员化 | 已注册未办卡 | 按燃油偏好赠券 |
| | 会员生日券 | 生日月 | 生日专属券 |
| **节日活动** | 逐光赛场 | 6.12-8.9 | 夜间8.8折+啤酒满66赠券 |
| | 中秋团圆礼 | 9.1-9.30 | 月饼满减+赠券 |
| **其他促销** | 9.9元专区 | 持续 | 固定价9.9元 |
| | 加油换购 | 加油满200/500 | 换购组合包 |
| | LNG/CNG权益包 | 购买权益包 | 权益商品+换购券 |

---

## 二、当前实现状态分析

### 2.1 促销规则类型实现状态

| 规则类型 | 枚举定义 | 计算器实现 | 条件匹配器 | 规则数据 | 状态 |
|---------|---------|-----------|-----------|---------|------|
| FIXED_PRICE | ✅ | ✅ | ✅ | ✅ | ✅ 完整 |
| PERCENTAGE_DISCOUNT | ✅ | ✅ | ❌ 部分 | ⚠️ 缺少日期触发 | 🔄 不完整 |
| AMOUNT_OFF | ✅ | ✅ | ✅ | ✅ | ✅ 完整 |
| EXCHANGE_PURCHASE | ✅ | ✅ | ✅ | ✅ | ✅ 完整 |
| GIFT_ITEM | ✅ | ✅ | ✅ | ✅ | ✅ 完整 |
| GIFT_COUPON | ✅ | ✅ | ❌ 部分 | ⚠️ 缺少充值场景 | 🔄 不完整 |
| BUNDLE_PRICE | ✅ | ✅ | ✅ | ✅ | ✅ 完整 |
| COUPON_REDEEM | ✅ | ✅ | ✅ | ✅ | ✅ 完整 |
| FUEL_VOLUME_DISCOUNT | ✅ | ✅ | ✅ | ✅ | ✅ 完整 |
| COMPOSITE | ✅ | ✅ | ❌ 部分 | ⚠️ 缺少复合规则 | 🔄 不完整 |

### 2.2 条件匹配器实现状态

| 匹配器 | 文件 | 已支持条件 | 缺失条件 | 状态 |
|-------|------|-----------|---------|------|
| DateConditionMatcher | condition/DateConditionMatcher.java | 基础日期范围 | promotion_date_trigger表、每月特定日期 | ❌ |
| MemberConditionMatcher | condition/MemberConditionMatcher.java | 基础会员身份 | member_levels字段、生日月、省份 | ❌ |
| StationTypeConditionMatcher | condition/StationTypeConditionMatcher.java | 站点类型 | station表自动补齐、站点范围校验 | ❌ |
| ProvinceConditionMatcher | condition/ProvinceConditionMatcher.java | 省份 | 站点自动补齐省份 | ❌ |
| TimeRangeConditionMatcher | condition/TimeRangeConditionMatcher.java | 时间范围 | 夜间18:00-02:00跨天 | ⚠️ |

### 2.3 数据库表实现状态

| 表名 | 结构 | 数据 | 服务层 | 状态 |
|------|------|------|--------|------|
| station | ✅ | ✅ 297条 | ✅ | ✅ 完整 |
| promotion_date_trigger | ✅ | ✅ 6条 | ❌ | 🚧 缺服务 |
| points_activity | ✅ | ✅ 2条 | ✅ | ✅ 完整 |
| member_points_change | ✅ | ❌ | ✅ | ✅ 完整 |
| promotion_excluded_category | ✅ | ❌ | ❌ | 🚧 缺服务 |
| promotion_station_scope | ✅ | ❌ | ❌ | 🚧 缺服务 |
| benefit_package | ✅ | ✅ 14条 | ✅ | ✅ 完整 |
| benefit_package_item | ✅ | ✅ 141条 | ✅ | ✅ 完整 |
| benefit_package_purchase | ✅ | ❌ | ✅ | ✅ 完整 |

---

## 三、功能差距详细分析

### 3.1 规则引擎层面差距

#### 差距1：日期触发规则未接入规则引擎

**问题描述**：
- `promotion_date_trigger`表已配置了逢7/8/9/10的触发日期
- 但`DateConditionMatcher`没有查询该表，仍使用promotion_rule的condition_json

**影响活动**：逢7气惠、逢8-CN98立减、逢9便利店促销、逢10超级十惠、逐光赛场、中秋团圆礼

**缺失代码**：
```java
// DateConditionMatcher.java 缺少以下逻辑
List<PromotionDateTrigger> triggers = dateTriggerRepository.findByRuleId(ruleId);
for (PromotionDateTrigger trigger : triggers) {
    if (trigger.isTriggered(currentDate)) {
        return ConditionMatchResult.matched();
    }
}
```

#### 差距2：会员等级条件未接入规则引擎

**问题描述**：
- `promotion_rule.member_levels`字段已预留
- 但`MemberConditionMatcher`没有读取该字段进行匹配

**影响活动**：逢10超级十惠（黄金及以上加赠）、会员生日券、会员专属优惠

**缺失代码**：
```java
// MemberConditionMatcher.java 缺少以下逻辑
List<String> requiredLevels = rule.getMemberLevels();
if (!requiredLevels.isEmpty() && !requiredLevels.contains(member.getLevelCode())) {
    return ConditionMatchResult.notMatched("Member level not in required levels");
}
```

#### 差距3：站点范围校验未实现

**问题描述**：
- `promotion_station_scope`表已创建
- 但没有在规则匹配时校验站点范围

**影响活动**：所有需要站点范围限制的促销活动

**缺失代码**：
```java
// 新增 StationScopeConditionMatcher.java
List<PromotionStationScope> scopes = stationScopeRepository.findByRuleId(ruleId);
for (PromotionStationScope scope : scopes) {
    if (!scope.matches(stationCode, stationType, province, city)) {
        return ConditionMatchResult.notMatched("Station not in scope");
    }
}
```

#### 差距4：排除品类校验未实现

**问题描述**：
- `promotion_excluded_category`表已创建
- 但没有在优惠计算时排除特定品类

**影响活动**：逢7便利店9折（排除香烟、化肥）、逢9便利店9折（排除香烟、化肥）

**缺失代码**：
```java
// 在PriceCalculation中增加排除品类校验
List<PromotionExcludedCategory> excluded = excludedCategoryRepository.findByRuleId(ruleId);
for (CartItem item : cartItems) {
    if (excluded.contains(item.getCategory())) {
        // 该商品不享受此优惠
    }
}
```

### 3.2 业务逻辑层面差距

#### 差距5：充值场景未实现

**问题描述**：
- `CheckoutCalculateRequest`缺少`rechargeAmount`字段
- 规则引擎缺少充值金额条件匹配
- 规则引擎缺少充值赠券动作

**影响活动**：逢10超级十惠（充值1000/2000赠券）

**缺失代码**：
```java
// CheckoutCalculateRequest.java 缺少
BigDecimal rechargeAmount;

// 新增 RechargeConditionMatcher.java
if (request.rechargeAmount().compareTo(rule.getMinRechargeAmount()) >= 0) {
    return ConditionMatchResult.matched();
}
```

#### 差距6：积分兑换闭环未实现

**问题描述**：
- 逢9活动中"积分兑换9折优惠"没有实现
- 缺少积分兑换接口
- 缺少积分兑换交易记录

**影响活动**：逢9全品惠-积分兑换9折

**缺失代码**：
```java
// 新增 PointsExchangeService.java
public PointsExchangeResponse exchangePoints(String memberCode, long points, String productCode) {
    // 校验积分充足
    // 计算兑换金额（积分×0.9折）
    // 扣减积分并写入流水
    // 返回兑换结果
}
```

#### 差距7：会员积分抽奖未实现

**问题描述**：
- 逢9活动中"积分抽奖（500积分抽1次）"没有实现
- 缺少抽奖活动表
- 缺少抽奖逻辑

**影响活动**：逢9全品惠-积分抽奖

**缺失代码**：
```java
// 新增 LotteryActivityService.java
public LotteryResult draw(String memberCode) {
    // 校验是否在抽奖时间
    // 扣减500积分
    // 随机生成中奖结果
    // 发放奖品（券/积分）
}
```

#### 差距8：新会员自动发券未实现

**问题描述**：
- 会员创建/开卡时没有自动发放赠券
- 缺少触发逻辑

**影响活动**：新增会员礼遇、潜在会员化

**缺失代码**：
```java
// MemberServiceImpl.java 缺少
private void issueNewMemberCoupons(String memberCode) {
    // 发放新会员券包
}
```

### 3.3 数据层面差距

#### 差距9：促销规则数据未完整导入

**问题描述**：
- V9/V12/V14/V18-V22导入了部分规则
- 但以下活动规则缺少：
  - 逢7气惠赠券规则
  - 逢9便利店9折规则
  - 逢10超级十惠充值赠券规则
  - 逐光赛场夜间8.8折规则
  - 中秋团圆礼规则
  - 会员生日券规则

**影响活动**：所有日期触发的促销活动

#### 差距10：券模板数据未完整导入

**问题描述**：
- 需要以下券模板但未导入：
  - 10元LNG券（满500可用）
  - 30元LNG券（满1000可用）
  - 60元LNG券（满1500可用）
  - 100元LNG券（满2000可用）
  - 6元便利店券（满30可用）
  - 12元便利店券（满50可用）
  - 10元洗车券（满11可用）
  - 12元汽油券（满200可用）
  - 15元高标号汽油券（满200可用）

**影响活动**：所有赠券活动

---

## 四、功能差距根因分析

### 4.1 架构层面根因

| 根因 | 说明 | 影响范围 |
|------|------|---------|
| 条件匹配器设计缺陷 | 条件匹配器只读取condition_json，没有接入结构化表 | 所有日期触发、会员等级、站点范围、排除品类条件 |
| 规则引擎扩展性不足 | 新增条件类型需要修改DefaultConditionMatcher的硬编码逻辑 | 充值场景、积分兑换等新场景 |
| 缺少规则数据导入工具 | 活动看板数据无法自动转换为规则数据 | 所有促销活动规则 |

### 4.2 开发流程根因

| 根因 | 说明 | 影响范围 |
|------|------|---------|
| 数据库先行，服务滞后 | V26创建了大量表，但服务层实现跟不上 | station、promotion_date_trigger等表 |
| 规则配置与规则引擎脱节 | 规则表有结构化字段，但引擎不读取 | member_levels、days_of_month等字段 |
| 缺少完整的端到端测试 | 没有测试验证完整的促销活动流程 | 所有促销活动 |

### 4.3 业务理解根因

| 根因 | 说明 | 影响范围 |
|------|------|---------|
| 日期触发规则复杂 | 每月特定日期、日期范围、时间范围组合复杂 | 所有日期触发活动 |
| 会员等级差异规则复杂 | 不同等级不同优惠、加赠逻辑复杂 | 逢10超级十惠、会员专属优惠 |
| 促销叠加规则复杂 | 可叠加、互斥、优先级规则多 | 所有促销活动组合 |

---

## 五、开发路线图

### 5.1 第一阶段：规则引擎基础能力补齐（P0）

| 任务 | 优先级 | 预计工时 | 依赖 |
|------|--------|---------|------|
| P0-1：权益包购买闭环 | ✅ 已完成 | 16h | - |
| P0-2：站点服务接入结算 | ✅ 已完成 | 12h | - |
| P0-3：日期触发规则接入引擎 | ✅ 已完成 | 8h | P0-2 |
| P0-4：会员等级条件接入引擎 | 🔄 当前 | 8h | - |

### 5.2 第二阶段：核心促销活动落地（P0）

| 任务 | 优先级 | 预计工时 | 依赖 |
|------|--------|---------|------|
| P0-5：充值场景与逢10超级十惠 | P0 | 16h | P0-3、P0-4 |
| P0-6：新会员/潜在会员化自动发券 | P0 | 8h | - |
| P0-7：排除品类校验 | P0 | 6h | - |
| P0-8：站点范围校验 | P0 | 6h | P0-2 |

### 5.3 第三阶段：促销活动规则数据导入（P1）

| 任务 | 优先级 | 预计工时 | 依赖 |
|------|--------|---------|------|
| P1-1：导入逢7气惠规则 | P1 | 8h | P0-3、P0-4 |
| P1-2：导入逢9便利店9折规则 | P1 | 8h | P0-3、P0-7 |
| P1-3：导入逐光赛场规则 | P1 | 8h | P0-3 |
| P1-4：导入中秋团圆礼规则 | P1 | 8h | P0-3、P0-4 |
| P1-5：导入会员生日券规则 | P1 | 8h | P0-4 |

### 5.4 第四阶段：积分活动与其他功能（P1）

| 任务 | 优先级 | 预计工时 | 依赖 |
|------|--------|---------|------|
| P1-6：积分兑换9折闭环 | P1 | 12h | - |
| P1-7：会员积分抽奖 | P1 | 12h | - |
| P1-8：一卡通销售闭环 | P1 | 8h | P0-2 |
| P1-9：LNG/CNG权益包商品 | P1 | 8h | P0-1 |

---

## 六、当前最紧迫的开发任务

### 6.1 任务优先级排序

```
第一优先级（规则引擎基础）：
├── P0-1：权益包购买闭环 ✅ 已完成
├── P0-2：站点服务接入结算 ✅ 已完成
├── P0-3：日期触发规则接入引擎 🔄 当前
├── P0-4：会员等级条件接入引擎

第二优先级（核心促销活动）：
├── P0-5：充值场景与逢10超级十惠
├── P0-6：新会员/潜在会员化自动发券

第三优先级（规则数据导入）：
├── P1-1：导入逢7气惠规则
├── P1-2：导入逢9便利店9折规则
└── P1-3~P1-5：其他活动规则
```

### 6.2 完成第一优先级后的效果

完成P0-3、P0-4后：

| 活动 | 效果 |
|------|------|
| 逢7气惠 | ✅ 日期触发 + CNG/LNG多倍积分（已有） |
| 逢8-CN98立减 | ✅ 日期触发 + 油品升数立减（已有） |
| 逢9便利店促销 | ✅ 日期触发 + 便利店9折 + 多倍积分（已有） |
| 逢10超级十惠 | ✅ 日期触发 + 会员等级差异（待P0-5） |
| 会员专属优惠 | ✅ 会员等级条件匹配 |

---

## 七、关键代码修改点

### 7.1 DateConditionMatcher.java

**修改内容**：接入`promotion_date_trigger`表

```java
// 新增依赖
private final PromotionDateTriggerRepository dateTriggerRepository;

// 修改match方法
public ConditionMatchResult match(PromotionRule rule, OrderContext context) {
    // 先检查规则自带的日期条件
    ConditionMatchResult result = super.match(rule, context);
    if (!result.matched()) {
        return result;
    }
    
    // 再检查日期触发器
    List<PromotionDateTrigger> triggers = dateTriggerRepository.findByRuleId(rule.getRuleId());
    for (PromotionDateTrigger trigger : triggers) {
        if (!trigger.isTriggered(context.transactionDateTime())) {
            return ConditionMatchResult.notMatched("Date trigger not matched");
        }
    }
    
    return ConditionMatchResult.matched();
}
```

### 7.2 MemberConditionMatcher.java

**修改内容**：接入`promotion_rule.member_levels`字段

```java
public ConditionMatchResult match(PromotionRule rule, OrderContext context) {
    CustomerContext customer = context.customerContext();
    if (customer == null || customer.memberCode() == null) {
        return ConditionMatchResult.notMatched("Not a member");
    }
    
    // 检查会员等级条件
    List<String> requiredLevels = rule.getMemberLevels();
    if (!requiredLevels.isEmpty()) {
        if (!requiredLevels.contains(customer.memberLevel())) {
            return ConditionMatchResult.notMatched("Member level not in required levels");
        }
    }
    
    // 检查生日月条件
    Integer birthMonth = customer.memberBirthMonth();
    if (rule.getBirthMonthRequired() && birthMonth == null) {
        return ConditionMatchResult.notMatched("Birth month not provided");
    }
    
    return ConditionMatchResult.matched();
}
```

### 7.3 CheckoutCalculateRequest.java

**修改内容**：新增充值场景字段

```java
public record CheckoutCalculateRequest(
        // ... 现有字段
        BigDecimal rechargeAmount,           // 新增：充值金额
        String rechargeType                  // 新增：充值类型（PREPAID/CREDIT）
) {
    // 更新构造函数
}
```

### 7.4 CheckoutApplicationService.java

**修改内容**：接入站点查询

```java
// 新增依赖
private final StationRepository stationRepository;

// 修改effectiveOrderContext方法
private OrderContext effectiveOrderContext(CheckoutCalculateRequest request) {
    StationContext stationContext = buildStationContext(request);
    return new OrderContext(
            request.orderContext().cartItems(),
            buildFuelContext(request),
            buildCustomerContext(request),
            stationContext,
            buildTransactionDateTime(request),
            request.stationCode(),
            request.paymentMethod()
    );
}

private StationContext buildStationContext(CheckoutCalculateRequest request) {
    if (request.stationCode() != null && request.stationType() == null) {
        Station station = stationRepository.findByStationCode(request.stationCode())
                .orElseThrow(() -> new StationNotFoundException(request.stationCode()));
        return new StationContext(
                request.stationCode(),
                station.getStationType(),
                station.getProvince(),
                station.getCity(),
                station.getDistrict()
        );
    }
    return new StationContext(
            request.stationCode() != null ? request.stationCode() : "default",
            request.stationType() != null ? request.stationType() : "gas_station",
            request.stationProvince() != null ? request.stationProvince() : "新疆",
            request.stationCity(),
            null
    );
}
```

---

## 八、验证策略

### 8.1 单元测试

每个新功能必须有单元测试：

```bash
# 规则引擎测试
mvn -Dtest=DateConditionMatcherTest,MemberConditionMatcherTest test

# 结算服务测试
mvn -Dtest=CheckoutApplicationServiceTest test

# 站点服务测试
mvn -Dtest=StationServiceTest test

# 全量测试
mvn test
```

### 8.2 集成测试

使用Testcontainers验证：

```bash
# Flyway迁移测试
mvn -Dtest=FlywayMigrationTest test

# 规则引擎集成测试
mvn -Dtest=PromotionEngineIntegrationTest test
```

### 8.3 端到端测试

手动验证核心促销活动：

| 活动 | 测试场景 | 预期结果 |
|------|---------|---------|
| 逢7气惠 | 7月7日CNG消费500元 | 赠LNG券+3倍积分 |
| 逢8-CN98立减 | 7月8日加CN98 10升 | 立减8元 |
| 逢9便利店促销 | 7月9日买便利店商品 | 9折+3倍积分 |
| 逢10超级十惠 | 7月10日充值1000元 | 赠汽油券+便利店券+洗车券 |

---

## 九、总结

### 9.1 功能差距汇总

| 差距类型 | 数量 | 说明 |
|---------|------|------|
| 规则引擎条件匹配器 | 4个 | 日期触发、会员等级、站点范围、排除品类 |
| 业务逻辑场景 | 4个 | 充值、积分兑换、抽奖、新会员发券 |
| 促销规则数据 | 6个活动 | 逢7/8/9/10、逐光赛场、中秋团圆礼 |
| 服务层实现 | 4个表 | station、promotion_date_trigger、promotion_excluded_category、promotion_station_scope |

### 9.2 关键成功因素

1. **先补齐规则引擎基础能力**（P0-2~P0-4）
2. **再实现核心促销活动**（P0-5~P0-6）
3. **最后导入规则数据**（P1-1~P1-5）
4. **每个阶段都要通过测试**

### 9.3 预期完成时间

| 阶段 | 任务数 | 预计工时 | 预计周期 |
|------|--------|---------|---------|
| 第一阶段 | 2个已完成，2个待开发 | 16h | 2-3天 |
| 第二阶段 | 4个 | 36h | 4-5天 |
| 第三阶段 | 5个 | 40h | 5-6天 |
| 第四阶段 | 4个 | 40h | 5-6天 |

---

## 十、最新完成状态（2026-07-14）

### 10.1 P0-2：站点服务接入结算 完成内容

**新增文件**：
- `StationController.java` - REST API控制器
- `StationService.java` - 服务接口
- `DefaultStationService.java` - 服务实现
- `StationRepository.java` - 仓储接口
- `InMemoryStationRepository.java` - 内存仓储实现
- `MybatisStationRepository.java` - MyBatis仓储实现
- `StationNotFoundException.java` - 异常类
- `model/Station.java` - 领域模型
- `model/StationResponse.java` - 响应DTO
- `model/StationQuery.java` - 查询参数
- `persistence/StationEntity.java` - 数据库实体
- `persistence/StationMapper.java` - MyBatis Mapper
- `persistence/StationMapper.xml` - MyBatis SQL映射

**API接口**：
| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/stations | 查询站点列表（支持city、district、stationType、salesScope筛选） |
| GET | /api/stations/{stationCode} | 查询站点详情 |

**修改文件**：
- `CheckoutCalculateRequest.java` - 新增stationCity、stationCode字段
- `CheckoutApplicationService.java` - 接入站点查询，自动补齐站点信息
- `StationContext.java` - 扩展province、city、district字段，保留旧构造器兼容历史代码

### 10.2 验证结果

| 测试项 | 结果 |
|--------|------|
| CheckoutApplicationServiceTest | ✅ 17个测试全部通过 |
| StationServiceTest | ✅ 5个测试全部通过 |
| 全量测试 | ✅ 151个测试，0失败、0错误 |

### 10.3 Docker恢复后需重跑的测试

```powershell
cd "D:\China National Petroleum Corporation\China National Petroleum Corporation\backend"
mvn -Dtest=StationDatabaseTest,FlywayMigrationTest test
```

---

## 十一、当前最紧迫任务：P0-3 日期触发规则接入引擎

### 11.1 任务目标

将`promotion_date_trigger`表的日期触发规则接入`DateConditionMatcher`，使规则引擎能够根据每月特定日期（如7/17/27、8/18/28等）触发促销活动。

### 11.2 待实现内容

| 子任务 | 说明 |
|--------|------|
| 新增PromotionDateTrigger模型 | 创建领域模型和实体类 |
| 新增PromotionDateTriggerRepository | 创建仓储接口和MyBatis实现 |
| 修改DateConditionMatcher | 接入promotion_date_trigger表查询 |
| 新增单元测试 | DateConditionMatcherTest |

### 11.3 验证标准

| 验证项 | 说明 |
|--------|------|
| 7月7日触发逢7活动 | 规则日期触发器匹配成功 |
| 7月8日触发逢8活动 | 规则日期触发器匹配成功 |
| 非活动日不触发 | 规则日期触发器不匹配 |
| 多日期触发器组合 | 支持同时配置多个日期触发 |

---

**文档结束**
# Codex 开发指令：P0-3 日期触发规则接入引擎

## 指令概述

基于当前项目状态，完成 P0-3「日期触发规则接入引擎」任务：
1. 将`promotion_date_trigger`表的日期触发规则接入`DateConditionMatcher`
2. 使规则引擎能够根据每月特定日期（如7/17/27、8/18/28等）触发促销活动

---

## 一、项目上下文

### 1.1 当前状态

| 任务 | 状态 |
|------|------|
| P0-1：权益包购买闭环 | ✅ 已完成 |
| P0-2：站点服务接入结算 | ✅ 已完成 |
| P0-3：日期触发规则接入引擎 | 🔄 待开发 |
| P0-4：会员等级条件接入引擎 | 待开发 |

### 1.2 V26已导入数据

`promotion_date_trigger`表已配置6条日期触发记录：

| 规则ID | 触发类型 | 触发日期 | 活动名称 |
|--------|---------|---------|---------|
| abv2-a5-day7 | days_of_month | [7, 17, 27] | 逢7-气惠 |
| abv2-a5-day8 | days_of_month | [8, 18, 28] | 逢8-CN98立减 |
| abv2-a5-day9 | days_of_month | [9, 19, 29] | 逢9-便利店促销 |
| abv2-a5-day10 | days_of_month | [10, 20, 30] | 逢10-超级十惠 |
| abv2-b3-zgsc | date_range | 2026-06-12 ~ 2026-08-09 | 逐光赛场 |
| abv2-b4-zqtyl | date_range | 2026-09-01 ~ 2026-09-30 | 中秋团圆礼 |

### 1.3 当前问题

`DateConditionMatcher`只读取`promotion_rule`表的`condition_json`字段，没有查询`promotion_date_trigger`表，导致日期触发规则无法生效。

### 1.4 现有相关文件

```
backend/src/main/java/com/cnpc/promoretail/ruleengine/
├── condition/
│   ├── DateConditionMatcher.java            # 需要修改：接入promotion_date_trigger表
│   └── ConditionMatcher.java                # 条件匹配器接口
└── model/
    └── PromotionRule.java                   # 促销规则模型
```

---

## 二、开发任务

### 任务2.1：新增PromotionDateTrigger模块

**目标**：实现日期触发器的领域模型、实体和仓储

**文件结构**：

```
backend/src/main/java/com/cnpc/promoretail/ruleengine/datetrigger/
├── PromotionDateTrigger.java                # 新增：领域模型
├── PromotionDateTriggerRepository.java      # 新增：仓储接口
├── InMemoryPromotionDateTriggerRepository.java  # 新增：内存仓储实现
├── MybatisPromotionDateTriggerRepository.java   # 新增：MyBatis仓储实现
└── persistence/
    ├── PromotionDateTriggerEntity.java      # 新增：数据库实体
    ├── PromotionDateTriggerMapper.java      # 新增：MyBatis Mapper接口
    └── PromotionDateTriggerMapper.xml       # 新增：MyBatis SQL映射
```

**数据库表结构**（V26已创建）：

```sql
-- promotion_date_trigger表结构
trigger_id varchar(64) primary key,
rule_id varchar(64) not null references promotion_rule(rule_id),
trigger_type varchar(64) not null,  -- DAYS_OF_MONTH, DATE_RANGE
days_of_month jsonb,                 -- [7, 17, 27]
start_date date,
end_date date,
enabled boolean not null default true,
created_at timestamptz not null default now(),
updated_at timestamptz not null default now()
```

**领域模型**：

```java
// PromotionDateTrigger.java
public class PromotionDateTrigger {
    private String triggerId;
    private String ruleId;
    private String triggerType;          // DAYS_OF_MONTH, DATE_RANGE
    private List<Integer> daysOfMonth;   // 每月特定日期 [7, 17, 27]
    private LocalDate startDate;         // 日期范围开始
    private LocalDate endDate;           // 日期范围结束
    private boolean enabled;
    
    public boolean isTriggered(LocalDateTime transactionDateTime) {
        LocalDate date = transactionDateTime.toLocalDate();
        if (!enabled) {
            return false;
        }
        switch (triggerType) {
            case "DAYS_OF_MONTH":
                return daysOfMonth != null && daysOfMonth.contains(date.getDayOfMonth());
            case "DATE_RANGE":
                return !date.isBefore(startDate) && !date.isAfter(endDate);
            default:
                return false;
        }
    }
}
```

**仓储接口**：

```java
// PromotionDateTriggerRepository.java
public interface PromotionDateTriggerRepository {
    List<PromotionDateTrigger> findByRuleId(String ruleId);
    List<PromotionDateTrigger> findAllEnabled();
}
```

**MyBatis SQL要求**：

```xml
<!-- PromotionDateTriggerMapper.xml -->
<select id="findByRuleId" resultType="PromotionDateTriggerEntity">
    SELECT * FROM promotion_date_trigger 
    WHERE rule_id = #{ruleId} AND enabled = true
</select>

<select id="findAllEnabled" resultType="PromotionDateTriggerEntity">
    SELECT * FROM promotion_date_trigger WHERE enabled = true
</select>
```

### 任务2.2：修改DateConditionMatcher

**目标**：接入`promotion_date_trigger`表，使日期触发规则生效

**修改文件**：`ruleengine/condition/DateConditionMatcher.java`

**修改内容**：

```java
// 新增依赖
private final PromotionDateTriggerRepository dateTriggerRepository;

// 修改构造函数
public DateConditionMatcher(PromotionDateTriggerRepository dateTriggerRepository) {
    this.dateTriggerRepository = dateTriggerRepository;
}

// 修改match方法
@Override
public ConditionMatchResult match(PromotionRule rule, OrderContext context) {
    // 先检查规则自带的日期条件（condition_json）
    ConditionMatchResult result = matchRuleDateConditions(rule, context);
    if (!result.matched()) {
        return result;
    }
    
    // 再检查日期触发器（promotion_date_trigger表）
    List<PromotionDateTrigger> triggers = dateTriggerRepository.findByRuleId(rule.getRuleId());
    if (!triggers.isEmpty()) {
        boolean anyTriggerMatched = false;
        for (PromotionDateTrigger trigger : triggers) {
            if (trigger.isTriggered(context.transactionDateTime())) {
                anyTriggerMatched = true;
                break;
            }
        }
        if (!anyTriggerMatched) {
            return ConditionMatchResult.notMatched("Date trigger not matched for rule: " + rule.getRuleId());
        }
    }
    
    return ConditionMatchResult.matched();
}

private ConditionMatchResult matchRuleDateConditions(PromotionRule rule, OrderContext context) {
    // 保留原有condition_json的日期条件匹配逻辑
    // ...
}
```

### 任务2.3：接入MyBatis Mapper扫描

**目标**：确保新创建的PromotionDateTriggerMapper被MyBatis扫描到

**检查文件**：`common/persistence/PersistenceConfiguration.java`

**确认扫描路径包含**：`com.cnpc.promoretail.ruleengine.datetrigger.persistence.mapper`

### 任务2.4：新增单元测试

**目标**：验证日期触发器的匹配逻辑

**新增文件**：`DateConditionMatcherTest.java`

**测试场景**：

| 测试方法 | 说明 |
|---------|------|
| testDaysOfMonthTrigger_Matched | 每月7日触发逢7活动 |
| testDaysOfMonthTrigger_NotMatched | 非活动日不触发 |
| testDateRangeTrigger_Matched | 日期范围内触发 |
| testDateRangeTrigger_NotMatched | 日期范围外不触发 |
| testMultipleTriggers_AnyMatched | 多个触发器任一匹配即生效 |
| testNoTriggers_AlwaysMatched | 无触发器规则不影响条件匹配 |

### 任务2.5：集成测试（可选）

**新增文件**：`DateTriggerDatabaseTest.java`

**测试场景**：

| 测试方法 | 说明 |
|---------|------|
| testFindV26ImportedTriggers | 查询V26导入的6条日期触发器 |
| testDateTriggerMatchWithRuleEngine | 规则引擎集成日期触发器 |

---

## 三、测试要求

### 3.1 单元测试

**新增文件**：`DateConditionMatcherTest.java`

```java
public class DateConditionMatcherTest {
    private InMemoryPromotionDateTriggerRepository dateTriggerRepository;
    private DateConditionMatcher matcher;
    
    @BeforeEach
    void setUp() {
        dateTriggerRepository = new InMemoryPromotionDateTriggerRepository();
        matcher = new DateConditionMatcher(dateTriggerRepository);
    }
    
    @Test
    void testDaysOfMonthTrigger_Matched() {
        dateTriggerRepository.save(PromotionDateTrigger.builder()
                .triggerId("test-trigger")
                .ruleId("test-rule")
                .triggerType("DAYS_OF_MONTH")
                .daysOfMonth(List.of(7, 17, 27))
                .enabled(true)
                .build());
        
        OrderContext context = OrderContext.builder()
                .transactionDateTime(LocalDateTime.of(2026, 7, 7, 10, 0))
                .build();
        
        ConditionMatchResult result = matcher.match(createTestRule("test-rule"), context);
        assertTrue(result.matched());
    }
    
    @Test
    void testDaysOfMonthTrigger_NotMatched() {
        dateTriggerRepository.save(PromotionDateTrigger.builder()
                .triggerId("test-trigger")
                .ruleId("test-rule")
                .triggerType("DAYS_OF_MONTH")
                .daysOfMonth(List.of(7, 17, 27))
                .enabled(true)
                .build());
        
        OrderContext context = OrderContext.builder()
                .transactionDateTime(LocalDateTime.of(2026, 7, 8, 10, 0))
                .build();
        
        ConditionMatchResult result = matcher.match(createTestRule("test-rule"), context);
        assertFalse(result.matched());
    }
    
    // ... 其他测试方法
}
```

### 3.2 集成测试

**新增文件**：`DateTriggerDatabaseTest.java`（使用Testcontainers）

---

## 四、验收标准

### 4.1 日期触发规则接入

| 验收项 | 验收方法 |
|--------|---------|
| 逢7活动触发 | 7月7日规则引擎匹配成功 |
| 逢8活动触发 | 7月8日规则引擎匹配成功 |
| 逢9活动触发 | 7月9日规则引擎匹配成功 |
| 逢10活动触发 | 7月10日规则引擎匹配成功 |
| 非活动日不触发 | 7月11日规则引擎不匹配 |

### 4.2 日期范围触发

| 验收项 | 验收方法 |
|--------|---------|
| 逐光赛场触发 | 6月15日规则引擎匹配成功 |
| 逐光赛场不触发 | 8月10日规则引擎不匹配 |
| 中秋团圆礼触发 | 9月15日规则引擎匹配成功 |
| 中秋团圆礼不触发 | 10月1日规则引擎不匹配 |

### 4.3 测试通过

| 测试命令 | 预期结果 |
|---------|---------|
| mvn -Dtest=DateConditionMatcherTest test | ✅ 通过 |
| mvn -Dtest=CheckoutApplicationServiceTest test | ✅ 通过 |
| mvn test | ✅ 通过 |

---

## 五、验证命令

```powershell
cd "D:\China National Petroleum Corporation\China National Petroleum Corporation\backend"

# 日期触发模块测试
mvn -Dtest=DateConditionMatcherTest test

# 结算服务测试（确保修改未破坏现有功能）
mvn -Dtest=CheckoutApplicationServiceTest test

# 全量测试
mvn test

# Flyway迁移验证
mvn -Dtest=FlywayMigrationTest test
```

---

## 六、注意事项

1. **不要回滚历史未跟踪文件**：只处理本轮任务相关文件
2. **不要重复导入旧规则**：V9/V12/V14/V18-V22已覆盖大量规则
3. **保持代码风格一致**：遵循项目现有编码风格，使用Lombok
4. **向后兼容**：保留原有condition_json的日期条件匹配逻辑，日期触发器作为额外校验
5. **测试覆盖**：每个新服务类必须有单元测试
6. **触发器匹配逻辑**：多个触发器时，任一匹配即生效（OR逻辑）

---

## 七、参考文档

| 文档 | 路径 | 用途 |
|------|------|------|
| 功能差距分析与开发路线图 | docs/29-doubao-功能差距分析与开发路线图.md | P0-3任务说明 |
| 数据库设计补全建议 | docs/26-doubao-数据库设计补全建议.md | 日期触发表结构设计 |
| 活动看板促销规则梳理 | docs/25-doubao-活动看板促销规则梳理.md | 促销规则业务背景 |

---

**指令结束**
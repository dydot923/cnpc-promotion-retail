# Codex 开发指令：P0-4 会员等级条件接入引擎

## 指令概述

基于当前项目状态，完成 P0-4「会员等级条件接入引擎」任务：
1. 将`promotion_rule.member_levels`字段接入`MemberConditionMatcher`
2. 使规则引擎能够根据会员等级（普通、银卡、金卡、铂金）匹配促销规则

---

## 一、项目上下文

### 1.1 当前状态

| 任务 | 状态 |
|------|------|
| P0-1：权益包购买闭环 | ✅ 已完成 |
| P0-2：站点服务接入结算 | ✅ 已完成 |
| P0-3：日期触发规则接入引擎 | ✅ 已完成 |
| P0-4：会员等级条件接入引擎 | 🔄 待开发 |

### 1.2 会员等级体系

根据业务需求，会员等级分为四级：

| 等级代码 | 等级名称 | 折扣率 | 积分倍率 | 说明 |
|---------|---------|--------|---------|------|
| ordinary | 普通会员 | 100% | 1倍 | 基础会员 |
| silver | 银卡会员 | 98% | 1.5倍 | 消费达到一定金额 |
| gold | 金卡会员 | 95% | 2倍 | 消费达到较高金额 |
| platinum | 铂金会员 | 92% | 3倍 | 最高等级 |

### 1.3 当前问题

`MemberConditionMatcher`只检查基础会员身份，没有读取`promotion_rule.member_levels`字段进行会员等级条件匹配。

**影响活动**：
- 逢10超级十惠：黄金及以上会员加赠高标号汽油券
- 会员专属优惠：不同等级会员享受不同优惠
- 会员生日券：生日月专属优惠

### 1.4 现有相关文件

```
backend/src/main/java/com/cnpc/promoretail/ruleengine/
├── condition/
│   ├── MemberConditionMatcher.java          # 需要修改：接入member_levels字段
│   └── ConditionMatcher.java                # 条件匹配器接口
├── model/
│   └── PromotionRule.java                   # 促销规则模型（需确认member_levels字段）
└── context/
    ├── CustomerContext.java                 # 客户上下文（包含memberLevel）
    └── OrderContext.java                    # 订单上下文
```

---

## 二、开发任务

### 任务2.1：确认PromotionRule模型

**目标**：确认`PromotionRule`模型中已包含`memberLevels`字段

**检查文件**：`ruleengine/model/PromotionRule.java`

**确认字段**：

```java
public class PromotionRule {
    // ... 现有字段
    private List<String> memberLevels;        // 允许的会员等级列表 ["gold", "platinum"]
    private Boolean birthMonthRequired;       // 是否需要生日月
    private List<String> memberTags;          // 会员标签 ["gasoline_customer", "diesel_customer"]
}
```

### 任务2.2：修改MemberConditionMatcher

**目标**：接入`promotion_rule.member_levels`字段，使会员等级条件生效

**修改文件**：`ruleengine/condition/MemberConditionMatcher.java`

**修改内容**：

```java
@Override
public ConditionMatchResult match(PromotionRule rule, OrderContext context) {
    CustomerContext customer = context.customerContext();
    
    // 检查基础会员身份
    if (customer == null || customer.memberCode() == null) {
        return ConditionMatchResult.notMatched("Not a member");
    }
    
    // 检查会员等级条件
    List<String> requiredLevels = rule.getMemberLevels();
    if (requiredLevels != null && !requiredLevels.isEmpty()) {
        String memberLevel = customer.memberLevel();
        if (memberLevel == null || !requiredLevels.contains(memberLevel.toLowerCase())) {
            return ConditionMatchResult.notMatched("Member level '" + memberLevel + 
                    "' not in required levels: " + requiredLevels);
        }
    }
    
    // 检查生日月条件
    Boolean birthMonthRequired = rule.getBirthMonthRequired();
    if (Boolean.TRUE.equals(birthMonthRequired)) {
        Integer birthMonth = customer.memberBirthMonth();
        if (birthMonth == null) {
            return ConditionMatchResult.notMatched("Birth month not provided");
        }
        int currentMonth = context.transactionDateTime().getMonthValue();
        if (birthMonth != currentMonth) {
            return ConditionMatchResult.notMatched("Not in birth month");
        }
    }
    
    // 检查会员标签条件
    List<String> requiredTags = rule.getMemberTags();
    if (requiredTags != null && !requiredTags.isEmpty()) {
        List<String> memberTags = customer.memberTags();
        if (memberTags == null || memberTags.isEmpty()) {
            return ConditionMatchResult.notMatched("Member has no tags");
        }
        boolean hasMatchingTag = requiredTags.stream()
                .anyMatch(tag -> memberTags.contains(tag.toLowerCase()));
        if (!hasMatchingTag) {
            return ConditionMatchResult.notMatched("Member tags not matching");
        }
    }
    
    return ConditionMatchResult.matched();
}
```

### 任务2.3：扩展CustomerContext

**目标**：确保`CustomerContext`包含会员等级、生日月、会员标签等字段

**修改文件**：`ruleengine/context/CustomerContext.java`

**确认字段**：

```java
public record CustomerContext(
        String memberCode,
        String memberLevel,                   // 会员等级：ordinary/silver/gold/platinum
        Integer memberBirthMonth,             // 生日月：1-12
        List<String> memberTags,              // 会员标签：["gasoline_customer", "diesel_customer"]
        BigDecimal memberPoints,              // 会员积分
        String memberName,
        String phoneNumber
) {
    // 空会员上下文
    public static CustomerContext nonMember() {
        return new CustomerContext(null, null, null, null, null, null, null);
    }
}
```

### 任务2.4：修改CheckoutApplicationService

**目标**：在构建订单上下文时，从会员信息中获取会员等级、生日月、会员标签

**修改文件**：`checkout/CheckoutApplicationService.java`

**修改内容**：

```java
private CustomerContext buildCustomerContext(CheckoutCalculateRequest request) {
    if (Boolean.TRUE.equals(request.isMember()) && request.memberCode() != null) {
        Optional<Member> memberOpt = memberRepository.findByMemberCode(request.memberCode());
        if (memberOpt.isPresent()) {
            Member member = memberOpt.get();
            return new CustomerContext(
                    member.getMemberCode(),
                    member.getLevelCode(),
                    member.getBirthMonth(),
                    member.getMemberTags(),
                    member.getPoints(),
                    member.getMemberName(),
                    member.getPhoneNumber()
            );
        }
    }
    return CustomerContext.nonMember();
}
```

### 任务2.5：新增单元测试

**目标**：验证会员等级条件的匹配逻辑

**新增文件**：`MemberConditionMatcherTest.java`

**测试场景**：

| 测试方法 | 说明 |
|---------|------|
| testMemberLevelMatched | 金卡会员匹配金卡及以上规则 |
| testMemberLevelNotMatched | 银卡会员不匹配金卡及以上规则 |
| testNoMemberLevelCondition | 无会员等级条件的规则对所有会员生效 |
| testBirthMonthMatched | 生日月会员匹配生日券规则 |
| testBirthMonthNotMatched | 非生日月会员不匹配生日券规则 |
| testMemberTagMatched | 汽油客户匹配汽油专属规则 |
| testMemberTagNotMatched | 柴油客户不匹配汽油专属规则 |

### 任务2.6：集成测试（可选）

**新增文件**：`MemberConditionDatabaseTest.java`

---

## 三、测试要求

### 3.1 单元测试

**新增文件**：`MemberConditionMatcherTest.java`

```java
public class MemberConditionMatcherTest {
    private MemberConditionMatcher matcher;
    
    @BeforeEach
    void setUp() {
        matcher = new MemberConditionMatcher();
    }
    
    @Test
    void testMemberLevelMatched() {
        PromotionRule rule = PromotionRule.builder()
                .ruleId("test-rule")
                .memberLevels(List.of("gold", "platinum"))
                .build();
        
        OrderContext context = OrderContext.builder()
                .customerContext(new CustomerContext("M001", "gold", null, null, null, null, null))
                .build();
        
        ConditionMatchResult result = matcher.match(rule, context);
        assertTrue(result.matched());
    }
    
    @Test
    void testMemberLevelNotMatched() {
        PromotionRule rule = PromotionRule.builder()
                .ruleId("test-rule")
                .memberLevels(List.of("gold", "platinum"))
                .build();
        
        OrderContext context = OrderContext.builder()
                .customerContext(new CustomerContext("M001", "silver", null, null, null, null, null))
                .build();
        
        ConditionMatchResult result = matcher.match(rule, context);
        assertFalse(result.matched());
    }
    
    @Test
    void testBirthMonthMatched() {
        PromotionRule rule = PromotionRule.builder()
                .ruleId("test-rule")
                .birthMonthRequired(true)
                .build();
        
        OrderContext context = OrderContext.builder()
                .customerContext(new CustomerContext("M001", "ordinary", 7, null, null, null, null))
                .transactionDateTime(LocalDateTime.of(2026, 7, 15, 10, 0))
                .build();
        
        ConditionMatchResult result = matcher.match(rule, context);
        assertTrue(result.matched());
    }
    
    // ... 其他测试方法
}
```

### 3.2 集成测试

**新增文件**：`MemberConditionDatabaseTest.java`（使用Testcontainers）

---

## 四、验收标准

### 4.1 会员等级条件匹配

| 验收项 | 验收方法 |
|--------|---------|
| 金卡会员匹配金卡规则 | 金卡会员触发逢10超级十惠金卡规则 |
| 银卡会员不匹配金卡规则 | 银卡会员不触发逢10超级十惠金卡规则 |
| 铂金会员匹配金卡规则 | 铂金会员触发逢10超级十惠金卡规则 |
| 无等级条件规则对所有会员生效 | 普通会员触发无等级条件规则 |

### 4.2 生日月条件匹配

| 验收项 | 验收方法 |
|--------|---------|
| 生日月会员匹配生日券规则 | 7月生日会员在7月触发生日券规则 |
| 非生日月会员不匹配生日券规则 | 7月生日会员在8月不触发生日券规则 |

### 4.3 会员标签条件匹配

| 验收项 | 验收方法 |
|--------|---------|
| 汽油客户匹配汽油专属规则 | gasoline_customer标签会员触发汽油专属规则 |
| 柴油客户匹配柴油专属规则 | diesel_customer标签会员触发柴油专属规则 |

### 4.4 测试通过

| 测试命令 | 预期结果 |
|---------|---------|
| mvn -Dtest=MemberConditionMatcherTest test | ✅ 通过 |
| mvn -Dtest=CheckoutApplicationServiceTest test | ✅ 通过 |
| mvn test | ✅ 通过 |

---

## 五、验证命令

```powershell
cd "D:\China National Petroleum Corporation\China National Petroleum Corporation\backend"

# 会员条件模块测试
mvn -Dtest=MemberConditionMatcherTest test

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
4. **向后兼容**：保留原有会员身份检查逻辑，会员等级条件作为额外校验
5. **测试覆盖**：每个新服务类必须有单元测试
6. **会员等级代码**：使用小写（ordinary/silver/gold/platinum）
7. **会员标签**：支持多个标签，任一匹配即生效（OR逻辑）

---

## 七、参考文档

| 文档 | 路径 | 用途 |
|------|------|------|
| 功能差距分析与开发路线图 | docs/history/29-doubao-功能差距分析与开发路线图.md | P0-4任务说明 |
| 数据库设计补全建议 | docs/26-doubao-数据库设计补全建议.md | 会员表结构设计 |
| 活动看板促销规则梳理 | docs/25-doubao-活动看板促销规则梳理.md | 促销规则业务背景 |

---

**指令结束**
# Codex 工程约束：防止屎山代码

## 一、最高优先级原则

本项目的核心是 **促销智能计算引擎**。任何代码生成、重构、修复、扩展都必须优先保护这个核心。

所有参与开发的 Codex agent 必须遵守：

```text
促销逻辑集中在 ruleengine
金额计算使用 BigDecimal
前端不写业务判断
Controller 不写业务判断
规则必须可解释
规则必须可测试
规则必须可版本追溯
```

如果某个实现违反以上原则，即使功能暂时可用，也视为不合格实现。

---

## 二、架构边界约束

### 2.1 后端模块边界

推荐模块结构：

```text
common              通用响应、异常、工具
auth                登录、权限、用户
product             商品主数据
price               价格数据
inventory           库存数据
promotion           活动与促销规则管理
ruleengine          促销智能计算引擎
checkout            结算接口
replenishment       库存预警与补货
poster              AI 海报任务
importcenter        Excel 导入
audit               操作日志
```

必须遵守：

1. `ruleengine` 是促销计算唯一入口。
2. `checkout` 只能调用 `ruleengine`，不能自行判断促销。
3. `promotion` 负责规则管理，不负责订单计算。
4. `importcenter` 负责导入和清洗，不直接生成结算结果。
5. `inventory` 负责库存查询和阈值，不直接决定促销最终价格。
6. `poster` 只能读取结构化促销信息，不得生成价格真相。

### 2.2 禁止行为

禁止以下写法：

```java
// 禁止：Controller 里写促销判断
if (amount.compareTo(new BigDecimal("200")) >= 0) {
    price = new BigDecimal("25");
}
```

```java
// 禁止：Service 里临时硬编码活动日期
if (day == 9 || day == 19 || day == 29) {
    discount = new BigDecimal("0.9");
}
```

```typescript
// 禁止：前端判断促销是否可用
if (cartTotal >= 200 && fuelType === 'gasoline') {
  showExchangePromotion()
}
```

```java
// 禁止：金额用 double
double discount = price * 0.9;
```

---

## 三、促销规则引擎约束

### 3.1 引擎职责

`ruleengine` 必须负责：

1. 标准化订单上下文；
2. 筛选有效规则；
3. 匹配促销条件；
4. 计算优惠动作；
5. 处理互斥与叠加；
6. 排序推荐方案；
7. 生成解释文本；
8. 返回可用和不可用促销。

### 3.2 推荐内部结构

```text
ruleengine/
  context/
    OrderContext.java
    FuelContext.java
    CustomerContext.java
    CartItem.java
  model/
    PromotionRule.java
    PromotionCandidate.java
    BlockedPromotion.java
    CalculationResult.java
  condition/
    ConditionMatcher.java
    DateConditionMatcher.java
    ProductScopeMatcher.java
    FuelConditionMatcher.java
  benefit/
    BenefitCalculator.java
    FixedPriceCalculator.java
    PercentageDiscountCalculator.java
    AmountOffCalculator.java
    ExchangePurchaseCalculator.java
    GiftItemCalculator.java
    GiftCouponCalculator.java
    BundlePriceCalculator.java
  conflict/
    ConflictResolver.java
  ranking/
    CandidateRanker.java
  explanation/
    ExplanationBuilder.java
  PromotionEngine.java
```

### 3.3 规则类型约束

新增促销类型时，必须新增独立计算器，不得在一个大方法中堆 `if else`。

允许的扩展方式：

```java
public interface BenefitCalculator {
    boolean supports(PromotionRuleType type);
    PromotionCandidate calculate(OrderContext context, PromotionRule rule);
}
```

禁止的扩展方式：

```java
// 禁止：一个方法里无限 if else
if (type.equals("fixed_price")) {
    ...
} else if (type.equals("discount")) {
    ...
} else if (type.equals("gift")) {
    ...
}
```

### 3.4 输出约束

促销计算结果必须包含：

- 原价金额；
- 应付金额；
- 优惠金额；
- 命中的规则；
- 推荐方案；
- 可选方案；
- 不可用方案；
- 不可用原因；
- 解释文本；
- 规则版本；
- 库存警告。

不可用促销不能直接丢弃，必须说明原因。

---

## 四、金额与数量约束

### 4.1 金额

必须使用：

```java
BigDecimal
```

禁止使用：

```java
double
float
```

金额比较必须使用：

```java
amount.compareTo(threshold) >= 0
```

禁止使用：

```java
amount == threshold
amount > threshold
```

### 4.2 舍入

所有金额舍入必须显式指定：

```java
setScale(2, RoundingMode.HALF_UP)
```

禁止隐式舍入或随意格式化。

### 4.3 商品编码和条码

必须使用：

```java
String productCode;
String barcode;
```

禁止使用：

```java
Long productCode;
Double barcode;
Integer barcode;
```

原因：Excel 中商品编码和条码可能被错误转换，必须按字符串处理。

---

## 五、Controller 与 Service 约束

### 5.1 Controller

Controller 只做：

1. 参数接收；
2. 参数校验；
3. 调用 Application Service；
4. 返回响应。

Controller 不得：

- 写促销判断；
- 写金额计算；
- 写库存预警逻辑；
- 写 Excel 解析逻辑；
- 写复杂业务流程。

### 5.2 Service

Application Service 负责流程编排，例如：

```text
读取商品
读取价格
读取库存
读取规则
组装 OrderContext
调用 PromotionEngine
保存计算日志
返回结果
```

Service 不得把促销计算写成大段条件判断。

### 5.3 Repository

Repository 只负责数据访问。

禁止在 Repository 中写业务判断，例如：

- 判断是否会员日；
- 判断是否满足换购；
- 判断是否可叠加；
- 计算最终优惠。

---

## 六、前端约束

前端只负责展示和交互。

### 6.1 前端可以做

- 扫码输入；
- 商品列表展示；
- 促销候选方案展示；
- 不可用原因展示；
- 用户选择促销；
- 跳过促销；
- 原价结算；
- 加载状态；
- 错误提示。

### 6.2 前端禁止做

- 判断促销是否可用；
- 计算最终应付金额；
- 判断优惠叠加；
- 判断活动日期；
- 判断油品金额门槛；
- 判断库存是否足够执行促销；
- 根据商品品类自行排除香烟、化肥。

所有这些都必须由后端返回。

---

## 七、Excel 导入约束

Excel 导入必须满足：

1. 生成导入版本；
2. 生成异常报告；
3. 商品编码按字符串读取；
4. 条码按字符串读取；
5. 金额按 BigDecimal 读取；
6. 合并单元格必须处理；
7. 空值继承必须明确；
8. 不可解析规则进入待确认状态；
9. 不得静默丢弃异常行；
10. 不得静默覆盖人工修正规则。

导入结果必须返回：

- 新增数量；
- 更新数量；
- 跳过数量；
- 异常数量；
- 异常明细；
- 导入版本号。

---

## 八、规则版本与审计约束

每条促销规则必须有：

- ruleId；
- activityId；
- ruleType；
- priority；
- exclusiveGroup；
- stackable；
- status；
- version；
- createdAt；
- updatedAt；
- createdBy；
- updatedBy。

每次结算计算必须记录：

- 订单上下文摘要；
- 使用的规则版本；
- 候选方案；
- 推荐方案；
- 用户选择方案；
- 是否跳过促销；
- 操作员；
- 时间。

没有规则版本的结算结果视为不可追溯。

---

## 九、测试约束

### 9.1 必须写测试的内容

以下内容必须有单元测试：

1. 9.9 元固定价；
2. 逢 9 全场 9 折；
3. 香烟、化肥排除；
4. 汽油满额换购；
5. 柴油满额换购；
6. 买赠；
7. 赠券；
8. 组合包；
9. 库存不足；
10. 油品金额不足；
11. 多促销互斥；
12. 原价兜底；
13. 毛利率不足拦截；
14. 不可用原因返回。

### 9.2 测试粒度

优先写 `ruleengine` 单元测试。

其次写：

- checkout 接口测试；
- Excel 导入测试；
- 库存预警测试；
- 前端关键流程测试。

禁止只依赖手工页面测试。

### 9.3 新规则约束

每新增一种促销类型，必须同时新增：

- 规则模型；
- 计算器；
- 测试样例；
- 可用场景；
- 不可用场景；
- 解释文本验证。

---

## 十、命名与代码风格约束

### 10.1 命名必须表达业务含义

推荐：

```java
PromotionCandidate
ExchangePurchaseRule
FuelAmountCondition
BlockedReason
RuleVersion
```

禁止：

```java
Data1
TempRule
CommonUtil2
HandleService
DoSomething
```

### 10.2 方法长度

单个方法建议不超过 50 行。

超过时优先拆分：

- 条件匹配；
- 金额计算；
- 库存判断；
- 解释生成；
- 日志记录。

### 10.3 类职责

一个类只做一类事情。

禁止出现：

```java
PromotionUtil
CheckoutBigService
RuleCommonService
AllInOneProcessor
```

这类名字通常意味着职责已经失控。

---

## 十一、AI 海报模块约束

AI 海报模块必须后置于促销规则数据。

禁止：

- 让 AI 自己编价格；
- 让 AI 自己编活动条件；
- 让 AI 生成的文字覆盖规则库；
- 未审核直接发布海报。

必须：

- 商品名来自商品表；
- 价格来自价格表或促销计算结果；
- 活动条件来自促销规则；
- 提示词和生成结果保存；
- 人工审核后下载或打印。

---

## 十二、Codex 生成代码前检查清单

每次 Codex 准备生成代码前，必须先自查：

1. 这段逻辑是否属于促销计算？
2. 如果属于，是否应该放进 `ruleengine`？
3. 是否用了 `BigDecimal`？
4. 是否需要规则版本？
5. 是否需要解释文本？
6. 是否需要不可用原因？
7. 是否会影响原价兜底？
8. 是否需要测试样例？
9. 是否会把业务逻辑写进前端？
10. 是否会把 Controller 写胖？

如果答案不清楚，先补设计，不要直接写代码。

---

## 十三、Reviewer 拦截标准

出现以下情况，Reviewer 必须拦截：

1. 前端写促销判断；
2. Controller 写促销判断；
3. 金额使用 `double` 或 `float`；
4. 商品编码使用数值类型；
5. 没有原价兜底；
6. 没有规则版本；
7. 没有解释文本；
8. 不可用促销被静默丢弃；
9. Excel 异常行被静默丢弃；
10. 新增规则没有测试；
11. 一个方法里堆大量 `if else`；
12. 出现万能 `Util` 或超大 `Service`；
13. AI 海报覆盖结构化价格；
14. 库存不足仍推荐促销执行；
15. 香烟、化肥参与默认全场折扣。

---

## 十四、最终要求

本项目可以先做少，但不能做乱。

一期目标不是把所有功能一次性堆完，而是先把以下基础打牢：

```text
规则模型清晰
促销引擎独立
金额计算严谨
结果可解释
规则可测试
版本可追溯
前后端边界明确
```

任何为了短期演示而破坏这些基础的代码，都应该被拒绝。

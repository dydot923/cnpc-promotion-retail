# 加油站促销智能零售系统技术选型建议

## 一、推荐结论

本项目后端建议采用 **Java 21 + Spring Boot 3.x** 作为主技术栈。

虽然 Python 在 Excel 解析和 AI 接口调用方面很灵活，但如果团队更倾向 Java，或者项目后续需要企业级稳定性、长期维护、权限体系、审计日志、事务一致性、复杂业务建模和多系统集成，那么 Java 是更适合正式落地的选择。

推荐整体技术组合：

```text
前端：React 18 + TypeScript + Vite + Ant Design
后端：Java 21 + Spring Boot 3.x
数据库：PostgreSQL
ORM：Spring Data JPA 或 MyBatis Plus
数据库迁移：Flyway
缓存：Redis
Excel：EasyExcel
规则引擎：自研轻量规则引擎，必要时再引入 Drools
接口文档：Springdoc OpenAPI
测试：JUnit 5 + AssertJ + Testcontainers
鉴权：Spring Security + JWT
任务调度：Spring Scheduler，后期可升级 XXL-JOB
对象存储：MinIO 或云 OSS
```

最关键的判断是：**一期不要把重点放在技术堆叠复杂度上，而要优先把促销智能计算引擎做好。**

---

## 二、为什么推荐 Java 后端

### 2.1 更适合企业级长期维护

加油站促销结算系统不是一次性工具，而是会持续扩展活动规则、站点、商品、会员、库存、补货、审计和接口集成的业务系统。Java 在企业级项目中的工程规范、类型安全、模块边界、事务控制和长期维护方面优势明显。

### 2.2 更适合复杂业务领域建模

本项目核心是促销计算，不是简单 CRUD。促销规则涉及：

- 全场折扣；
- 固定促销价；
- 满减；
- 买赠；
- 赠券；
- 加油换购；
- 组合包；
- 会员权益；
- 排除品类；
- 互斥叠加；
- 毛利率约束；
- 库存约束。

这些规则适合用 Java 的强类型领域模型表达，例如：

- `OrderContext`
- `CartItem`
- `FuelContext`
- `PromotionRule`
- `PromotionCondition`
- `PromotionBenefit`
- `PromotionCandidate`
- `ConflictResolver`
- `ExplanationBuilder`

强类型模型能降低后期维护风险。

### 2.3 更适合事务和审计

促销结算涉及金额、规则版本、用户选择、跳过促销、补货清单、导入记录等关键数据。Java + Spring 的事务管理、审计日志、权限体系和数据库迁移生态成熟，适合正式系统。

### 2.4 更适合与企业既有系统集成

如果后续要对接 POS、会员系统、电子券系统、ERP、仓储、统一认证平台，Java/Spring Boot 在企业内集成更常见，也更容易被甲方技术团队接受。

---

## 三、后端技术架构建议

### 3.1 架构风格

建议采用 **模块化单体架构**，不要一开始做微服务。

原因：

- 项目早期业务规则尚在沉淀；
- 促销计算引擎需要快速迭代；
- 微服务会增加部署、链路、事务和运维复杂度；
- 模块化单体已经足够支撑试点和一期上线。

推荐后端模块：

```text
cn.project
  common              通用工具、异常、响应模型
  auth                登录、权限、用户
  product             商品主数据
  price               价格数据
  inventory           库存数据
  promotion           活动与促销规则
  ruleengine          促销智能计算引擎
  checkout            结算接口
  replenishment       库存预警与补货
  poster              AI 海报任务
  importcenter        Excel 导入
  audit               操作日志与审计
```

其中 `ruleengine` 必须保持相对独立，不能把促销判断写散在 Controller 或 Service 里。

### 3.2 促销计算引擎优先

一期优先完成：

```text
ruleengine
  context             订单上下文
  condition           条件匹配
  benefit             优惠计算
  conflict            互斥叠加
  ranking             候选排序
  explanation         规则解释
  model               引擎领域模型
```

核心流程：

```text
OrderContext
  ↓
RuleFilter
  ↓
ConditionMatcher
  ↓
BenefitCalculator
  ↓
ConflictResolver
  ↓
CandidateRanker
  ↓
ExplanationBuilder
  ↓
PromotionCandidate[]
```

这部分应优先于页面、报表和 AI 海报。

---

## 四、关键技术选型

### 4.1 Java 版本

推荐：**Java 21**

理由：

- LTS 长期支持版本；
- 性能和语言特性更现代；
- 适合新项目；
- 与 Spring Boot 3.x 兼容良好。

如果部署环境较保守，也可以使用 Java 17，但新项目更推荐 Java 21。

### 4.2 Spring Boot

推荐：**Spring Boot 3.x**

主要用途：

- REST API；
- 事务管理；
- 参数校验；
- 配置管理；
- 安全认证；
- 定时任务；
- 文件上传；
- 数据库访问；
- 监控与健康检查。

### 4.3 ORM 选择

有两个可选方向：

#### 方案 A：Spring Data JPA

适合：

- 领域模型清晰；
- 表结构规范；
- 希望减少基础 CRUD 代码；
- 团队熟悉 JPA。

优点：

- 领域模型表达力强；
- 事务集成自然；
- 适合商品、活动、规则、订单等对象关系。

风险：

- 复杂查询容易出现性能问题；
- 团队需要理解懒加载、事务边界、N+1 查询。

#### 方案 B：MyBatis Plus

适合：

- 团队更熟悉 SQL；
- 需要精确控制查询；
- 报表、导入、复杂筛选较多；
- 甲方 Java 团队偏传统企业开发风格。

优点：

- SQL 可控；
- 学习成本低；
- 复杂查询直观。

风险：

- 领域模型表达弱一些；
- 业务代码容易变成过程式脚本。

推荐选择：

```text
如果团队熟悉 JPA：Spring Data JPA
如果团队更熟悉 SQL：MyBatis Plus
```

对于本项目，我更偏向 **MyBatis Plus + 手写复杂 SQL**，因为活动看板、库存、商品、补货、规则查询会有不少数据筛选和报表场景；促销计算引擎本身则不应依赖 ORM，而应使用领域对象。

### 4.4 数据库

推荐：**PostgreSQL**

理由：

- 支持事务；
- 支持 JSONB；
- 适合存储促销规则配置；
- 查询能力强；
- 后续多站点扩展更稳。

可选：

- 本地演示可用 H2 或 SQLite；
- 正式试点不建议用 SQLite；
- 如果企业已有 MySQL 标准，也可以用 MySQL 8，但 JSON 规则和复杂查询能力不如 PostgreSQL 舒服。

### 4.5 数据库迁移

推荐：**Flyway**

理由：

- 简单稳定；
- 适合 Java 项目；
- 数据库结构可版本化；
- 便于测试、部署和回滚。

### 4.6 Excel 导入

推荐：**Alibaba EasyExcel**

用途：

- 价格表导入；
- 库存表导入；
- 活动看板导入；
- 导入异常报告；
- 补货表导出。

注意：

- 商品编码必须按字符串读取；
- 条码必须按字符串读取；
- 不允许商品编码被转成科学计数法；
- 合并单元格需要单独处理；
- 导入后必须生成异常清单。

### 4.7 缓存

推荐：**Redis**

用途：

- 商品条码查询缓存；
- 促销规则缓存；
- 活动商品池缓存；
- 站点库存快照缓存；
- AI 海报任务状态缓存。

约束：

- Redis 只做加速，不做最终数据真相；
- 促销规则更新后必须刷新缓存；
- 结算结果必须能通过数据库规则版本复现。

### 4.8 规则引擎

推荐：**一期自研轻量规则引擎，不建议一开始上 Drools。**

理由：

- 当前规则虽然复杂，但类型可归纳；
- 需要强业务解释能力；
- Drools 学习和维护成本较高；
- 运营人员未必能直接维护 Drools 规则；
- 项目前期规则模型还在变化，过早引入重型规则引擎会增加复杂度。

一期自研规则引擎支持：

- 固定价；
- 折扣；
- 满减；
- 加油换购；
- 买赠；
- 赠券；
- 组合包价；
- 排除品类；
- 互斥组；
- 优先级；
- 毛利率约束；
- 库存约束；
- 解释文本。

后期如果规则数量极大、规则频繁由运营配置、并且有专门规则运维团队，再评估 Drools。

### 4.9 鉴权与权限

推荐：**Spring Security + JWT**

角色建议：

- 收银员；
- 站长；
- 运营人员；
- 管理员；
- 审计员。

权限控制：

- 收银员只能结算和查看可用促销；
- 站长可看库存预警和补货；
- 运营可导入活动和维护规则；
- 管理员可配置系统；
- 审计员可查看日志。

### 4.10 接口文档

推荐：**Springdoc OpenAPI**

用于生成 Swagger 文档，方便前后端对接。

重点接口：

```text
POST /api/checkout/calculate
POST /api/checkout/confirm
POST /api/import/prices
POST /api/import/inventory
POST /api/import/promotions
GET  /api/replenishment/alerts
POST /api/replenishment/export
POST /api/posters/generate
```

### 4.11 测试

推荐：

- JUnit 5；
- AssertJ；
- Mockito；
- Testcontainers；
- Spring Boot Test。

促销引擎必须有大量单元测试，不要只做接口测试。

规则测试样例至少覆盖：

- 9.9 元专区；
- 逢 9 全场 9 折；
- 加油满额换购；
- 柴油满额换购；
- 香烟和化肥排除；
- 买赠；
- 赠券；
- 组合包；
- 库存不足；
- 油品金额不足；
- 多促销互斥；
- 原价兜底。

---

## 五、前端技术选型

推荐：

```text
React 18
TypeScript
Vite
Ant Design
TanStack Query
Zustand
```

理由：

- 适合后台管理和收银操作台；
- 表格、弹窗、表单、上传、步骤条等组件成熟；
- TypeScript 有利于促销候选方案的数据结构稳定；
- TanStack Query 适合处理结算计算、导入任务、海报任务等异步请求。

前端约束：

- 不写促销判断逻辑；
- 不自行计算最终应付金额；
- 不自行判断活动是否可用；
- 只展示后端返回的候选方案、推荐方案和不可用原因；
- 始终保留原价结算选项。

---

## 六、AI 海报技术选型

AI 海报模块建议后置，不作为一期核心。

后端职责：

- 从促销规则提取商品名、价格、卖点、活动条件；
- 生成提示词；
- 调用图像模型；
- 保存任务记录；
- 保存图片地址；
- 提供下载和审核状态。

约束：

- AI 生成结果不能作为价格真相；
- 价格和活动条件必须来自结构化规则；
- 发布前必须人工审核；
- 敏感品类海报需要额外审核。

---

## 七、部署建议

### 7.1 试点部署

推荐：

```text
Nginx
Spring Boot Jar
PostgreSQL
Redis
MinIO，可选
```

可以部署在站点本地服务器或内网服务器。

### 7.2 后续推广部署

推荐：

```text
Docker
Docker Compose 或 Kubernetes
集中 PostgreSQL
集中 Redis
统一对象存储
统一认证
日志采集
监控告警
```

---

## 八、核心开发约束

1. 促销智能计算引擎必须前期优先完成。
2. 促销逻辑必须集中在 `ruleengine` 模块。
3. Controller 只负责请求响应，不写促销判断。
4. 前端不写促销判断。
5. 金额使用 `BigDecimal`，禁止使用 `double` 或 `float` 计算金额。
6. 商品编码和条码使用 `String`。
7. 每次规则变更必须有版本号。
8. 每个结算结果必须记录规则版本。
9. 每个促销候选方案必须有解释文本。
10. 不可用促销也要返回不可用原因。
11. Excel 导入必须生成异常报告。
12. 原价结算必须始终可用。
13. 香烟、化肥默认排除全场折扣。
14. 库存不足的促销商品不能作为推荐执行方案。
15. AI 海报不能覆盖结构化价格和活动条件。

---

## 九、推荐项目结构

```text
backend/
  pom.xml
  src/main/java/com/company/promotion/
    PromotionApplication.java
    common/
    auth/
    product/
    price/
    inventory/
    promotion/
    ruleengine/
      context/
      condition/
      benefit/
      conflict/
      ranking/
      explanation/
      model/
    checkout/
    replenishment/
    poster/
    importcenter/
    audit/
  src/main/resources/
    application.yml
    db/migration/
  src/test/java/com/company/promotion/
    ruleengine/
    checkout/
    importcenter/

frontend/
  package.json
  src/
    pages/
      checkout/
      promotions/
      inventory/
      replenishment/
      posters/
    services/
    components/
    stores/
    types/
```

---

## 十、最终建议

如果项目团队倾向 Java，建议坚定采用 **Java 21 + Spring Boot 3 + PostgreSQL + EasyExcel + 自研轻量促销规则引擎**。

其中最重要的不是 Spring Boot 本身，而是架构纪律：

- 促销计算引擎独立；
- 金额计算严谨；
- 规则可解释；
- 规则可测试；
- 规则可版本追溯；
- 前端不做业务判断；
- Excel 导入不直接等于最终规则。

只要这些约束守住，Java 技术栈非常适合把该项目做成可长期维护、可扩展、可集成的企业级加油站智能促销结算系统。

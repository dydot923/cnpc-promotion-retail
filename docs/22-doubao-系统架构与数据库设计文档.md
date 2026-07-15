# 系统架构与数据库设计文档

## 文档说明

本文档详细描述加油站智能零售促销系统的技术架构设计和数据库设计，作为开发、部署和维护的技术依据。

---

## 目录

1. [系统架构设计](#1-系统架构设计)
2. [技术选型](#2-技术选型)
3. [模块划分与职责](#3-模块划分与职责)
4. [核心业务流程](#4-核心业务流程)
5. [数据库设计](#5-数据库设计)
6. [API接口设计](#6-api接口设计)
7. [部署架构](#7-部署架构)
8. [安全设计](#8-安全设计)

---

## 1. 系统架构设计

### 1.1 整体架构

系统采用经典的四层架构设计，从下到上依次为：数据存储层、数据访问层、业务逻辑层、展示层。

```
┌─────────────────────────────────────────────────────────────────┐
│                    展示层 (Presentation)                          │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│   │ 智能结算  │ │ 库存管理  │ │ 促销管理  │ │ 数据导入  │          │
│   │   界面    │ │   界面    │ │   界面    │ │   界面    │          │
│   └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│   │ 会员管理  │ │ 券管理    │ │ 报表中心  │ │ 系统设置  │          │
│   │   界面    │ │   界面    │ │   界面    │ │   界面    │          │
│   └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
├─────────────────────────────────────────────────────────────────┤
│                    业务逻辑层 (Business)                          │
│   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│   │  促销规则引擎  │ │  会员服务    │ │  券服务      │            │
│   │  规则匹配/计算 │ │  等级/积分   │ │  发放/核销   │            │
│   └──────────────┘ └──────────────┘ └──────────────┘            │
│   ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │
│   │  交易结算服务  │ │  库存监控服务  │ │  报表统计服务  │            │
│   │  价格计算/记录 │ │  阈值检测/预警 │ │  数据聚合/分析 │            │
│   └──────────────┘ └──────────────┘ └──────────────┘            │
├─────────────────────────────────────────────────────────────────┤
│                    数据访问层 (Data Access)                       │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│   │ 商品数据   │ │ 促销数据   │ │ 库存数据   │ │ 会员数据   │          │
│   │  DAO/ORM  │ │  DAO/ORM  │ │  DAO/ORM  │ │  DAO/ORM  │          │
│   └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│   ┌──────────┐ ┌──────────┐ ┌──────────┐                        │
│   │ 券数据    │ │ 交易数据   │ │ 系统配置   │                        │
│   │  DAO/ORM  │ │  DAO/ORM  │ │  DAO/ORM  │                        │
│   └──────────┘ └──────────┘ └──────────┘                        │
├─────────────────────────────────────────────────────────────────┤
│                    数据存储层 (Storage)                           │
│   ┌────────────────┐  ┌────────────┐  ┌──────────────┐         │
│   │  PostgreSQL    │  │ 文件存储     │  │ 外部API      │         │
│   │  关系型数据库     │  │ 导入文件    │  │ 会员系统     │         │
│   └────────────────┘  └────────────┘  └──────────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 架构特点

| 特点 | 说明 |
|------|------|
| 分层解耦 | 各层职责明确，便于独立开发、测试和维护 |
| 规则引擎独立 | 促销规则引擎作为核心业务组件，支持灵活扩展 |
| 数据驱动 | 通过Excel导入驱动促销规则配置，降低运营成本 |
| 可扩展性 | 支持多站点、多会员体系的扩展 |
| 高可用性 | 核心结算功能支持离线降级 |

---

## 2. 技术选型

### 2.1 后端技术

| 技术 | 版本 | 用途 | 选型理由 |
|------|------|------|---------|
| Java | 21 | 语言基础 | LTS版本，性能稳定，生态成熟 |
| Spring Boot | 3.x | 框架 | 社区成熟，生态完善，便于快速开发 |
| Spring Security | 6.x | 安全 | 身份认证和权限控制 |
| MyBatis Plus | 3.5+ | ORM框架 | 灵活的SQL映射，性能优异 |
| PostgreSQL | 16+ | 数据库 | 支持JSONB，适合存储促销规则，性能好 |
| Flyway | 9.x | 数据库迁移 | 版本化管理数据库变更 |
| EasyExcel | 3.x | Excel处理 | 高性能读写，支持大数据量 |
| Springdoc OpenAPI | 2.x | API文档 | 自动生成OpenAPI文档 |
| Redis | 7.x | 缓存 | 缓存促销规则、会员信息，提高响应速度 |

### 2.2 前端技术

| 技术 | 版本 | 用途 | 选型理由 |
|------|------|------|---------|
| React | 18 | 前端框架 | 组件化开发，虚拟DOM性能好 |
| TypeScript | 5.x | 语言 | 类型安全，代码可维护性高 |
| Ant Design | 5.x | UI组件库 | 企业级组件，设计规范统一 |
| Vite | 6.x | 构建工具 | 快速热更新，构建速度快 |
| Axios | 1.x | HTTP客户端 | Promise API，拦截器支持好 |
| React Router | 6.x | 路由 | 声明式路由，嵌套路由支持 |
| Redux Toolkit | 2.x | 状态管理 | 简化Redux使用，内置异步处理 |

### 2.3 基础设施

| 组件 | 用途 |
|------|------|
| Docker | 容器化部署 |
| Docker Compose | 本地开发环境 |
| Nginx | 反向代理、静态资源服务 |
| Prometheus | 监控指标收集 |
| Grafana | 监控可视化 |

---

## 3. 模块划分与职责

### 3.1 模块总览

| 模块 | 包路径 | 职责 |
|------|--------|------|
| promotion | com.cnpc.promoretail.promotion | 促销规则管理、草稿、版本控制 |
| ruleengine | com.cnpc.promoretail.ruleengine | 促销规则引擎、条件匹配、优惠计算 |
| checkout | com.cnpc.promoretail.checkout | 结算计算、交易记录 |
| member | com.cnpc.promoretail.member | 会员管理、等级、积分 |
| coupon | com.cnpc.promoretail.coupon | 券模板、券发放、券核销 |
| product | com.cnpc.promoretail.product | 商品信息、价格管理 |
| inventory | com.cnpc.promoretail.inventory | 库存快照、库存预警 |
| importcenter | com.cnpc.promoretail.importcenter | 数据导入、Excel解析 |
| replenishment | com.cnpc.promoretail.replenishment | 补货清单生成 |
| audit | com.cnpc.promoretail.audit | 操作审计日志 |
| common | com.cnpc.promoretail.common | 通用配置、工具类 |

### 3.2 核心模块详细设计

#### 3.2.1 促销规则引擎模块 (ruleengine)

**职责**：负责促销规则的匹配、计算、互斥处理和排序推荐。

**核心组件**：

| 组件 | 类名 | 职责 |
|------|------|------|
| 规则引擎 | PromotionEngine | 主入口，协调规则匹配和计算 |
| 条件匹配器 | ConditionMatcher | 判断规则条件是否满足 |
| 优惠计算器 | BenefitCalculator | 计算优惠金额和实付价格 |
| 冲突解决器 | ConflictResolver | 处理规则互斥关系 |
| 候选排序器 | CandidateRanker | 对候选方案进行排序 |
| 解释构建器 | ExplanationBuilder | 生成规则解释文本 |

**规则模型**：

```text
规则 = 触发条件 + 适用范围 + 优惠动作 + 限制条件 + 优先级 + 互斥组
```

**规则类型枚举**：

```java
public enum PromotionRuleType {
    FIXED_PRICE,           // 固定价格
    PERCENTAGE_DISCOUNT,   // 百分比折扣
    AMOUNT_OFF,            // 金额立减
    EXCHANGE_PURCHASE,     // 加油换购
    GIFT_ITEM,             // 买赠
    GIFT_COUPON,           // 赠券
    BUNDLE_PRICE,          // 组合包价格
    COUPON_REDEEM,         // 券核销
    FUEL_VOLUME_DISCOUNT,  // 油品升数立减
    COMPOSITE              // 复合优惠
}
```

#### 3.2.2 结算模块 (checkout)

**职责**：负责购物车管理、促销计算、交易确认和记录。

**核心流程**：

```text
输入订单上下文 → 规则引擎匹配 → 计算候选方案 → 用户选择方案 → 确认结算 → 扣减库存 → 发放赠券 → 记录交易
```

**OrderContext结构**：

| 字段 | 类型 | 说明 |
|------|------|------|
| cartItems | List<CartItem> | 购物车商品列表 |
| fuelContext | FuelContext | 油品消费信息 |
| customerContext | CustomerContext | 顾客信息（会员/非会员） |
| stationContext | StationContext | 站点信息 |
| currentTime | LocalDateTime | 当前时间 |

#### 3.2.3 会员模块 (member)

**职责**：负责会员信息管理、等级配置、积分计算。

**会员等级体系**：

| 等级 | 编码 | 折扣率 | 积分倍率 | 升级条件 |
|------|------|--------|---------|---------|
| 普通会员 | NORMAL | 1.0 | 1 | 注册即成为 |
| 银卡会员 | SILVER | 0.95 | 1.5 | 累计消费≥1000元 |
| 金卡会员 | GOLD | 0.90 | 2.0 | 累计消费≥5000元 |
| 铂金会员 | PLATINUM | 0.85 | 3.0 | 累计消费≥20000元 |

#### 3.2.4 券模块 (coupon)

**职责**：负责券模板管理、券发放和券核销。

**券类型**：

| 类型 | 说明 | 示例 |
|------|------|------|
| FULL_REDUCTION | 满减券 | 满100减20 |
| DISCOUNT | 折扣券 | 9折券 |
| GIFT | 礼品券 | 洗车券 |
| FUEL | 油品券 | LNG加油券 |
| STORE | 便利店券 | 便利店满减券 |

---

## 4. 核心业务流程

### 4.1 智能结算流程

```
收银员扫描商品条码
        │
        ▼
┌─────────────────┐
│  查询商品价格表   │ → 获取商品信息、执行价、会员价
└────────┬────────┘
         ▼
┌─────────────────┐
│  查询库存快照    │ → 获取当前库存、判断是否低库存
└────────┬────────┘
         ▼
┌─────────────────┐
│ 会员身份识别     │ → 扫码或输入手机号
└────────┬────────┘
         ▼
┌─────────────────┐
│ 构建订单上下文   │ → 商品列表、数量、会员信息、油品消费
└────────┬────────┘
         ▼
┌─────────────────┐
│ 规则引擎匹配     │ → 筛选有效规则、计算候选方案、处理互斥叠加
└────────┬────────┘
         ▼
┌─────────────────────────────────┐
│  界面展示所有可选促销方案卡片      │
│  每张卡片显示:                    │
│  - 促销名称与类型标签             │
│  - 规则简述                      │
│  - 原价 → 实付价                 │
│  - 节省金额                      │
│  - 库存预警提示                  │
│  - 会员专享标识                  │
│  - 赠券信息                      │
└────────┬────────────────────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌───────┐ ┌───────┐
│选择方案 │ │ 跳过   │
└───┬───┘ └───┬───┘
    │         │
    ▼         ▼
┌───────────────┐
│ 加入购物车     │ ← 记录: 商品、数量、适用促销、计算后单价
│ 更新小计/总计  │
│ 更新满减累计   │ ← 实时计算满减进度
└───────┬───────┘
        │
        ▼ (所有商品扫完后)
┌───────────────┐
│ 显示交易汇总   │ ← 商品合计、各优惠明细、优惠总额、应付金额
│ 显示赠券提示   │ ← 满额赠券、会员赠券
│ 会员积分计算   │ ← 实付金额 × 积分倍率
└───────┬───────┘
        ▼
┌───────────────┐
│ 确认结算       │ → 扣减库存 → 记录交易流水 → 发放赠券 → 更新积分 → 清空购物车
└───────────────┘
```

### 4.2 数据导入流程

```
选择Excel文件
        │
        ▼
┌─────────────────┐
│ 解析工作表      │ → EasyExcel读取，支持多个工作表
└────────┬────────┘
         ▼
┌─────────────────┐
│ 数据清洗        │ → 合并单元格处理、数据标准化、规则解析
└────────┬────────┘
         ▼
┌─────────────────┐
│ 数据校验        │ → 商品编码存在性校验、金额有效性、日期范围、毛利率、库存
└────────┬────────┘
         ▼
┌─────────────────┐
│ 数据预览        │ → 展示前20条数据供人工确认
└────────┬────────┘
         ▼
┌─────────────────┐
│ 导入确认        │ → 选择全量覆盖或增量更新
└────────┬────────┘
         ▼
┌─────────────────┐
│ 生成规则        │ → 根据清洗后的数据生成促销规则
└────────┬────────┘
         ▼
┌─────────────────┐
│ 更新库存快照    │ → 同步库存数据
└─────────────────┘
```

### 4.3 库存预警流程

```
系统定期扫描库存
        │
        ▼
┌─────────────────┐
│ 获取当前库存    │ → 从inventory_snapshot表查询
└────────┬────────┘
         ▼
┌─────────────────┐
│ 获取安全阈值    │ → 促销商品阈值提升系数 × 基础阈值
└────────┬────────┘
         ▼
┌─────────────────┐
│ 判断预警级别    │ → critical: ≤阈值×20%, warning: ≤阈值, info: ≤阈值×150%
└────────┬────────┘
         ▼
┌─────────────────┐
│ 生成预警记录    │ → 插入inventory_alert表
└────────┬────────┘
         ▼
┌─────────────────┐
│ 发送预警通知    │ → 强制弹窗(critical)、Toast(warning)、列表(info)
└────────┬────────┘
         ▼
┌─────────────────┐
│ 生成补货清单    │ → 建议补货量 = 阈值×2 - 当前库存
└─────────────────┘
```

---

## 5. 数据库设计

### 5.1 核心表结构

#### 5.1.1 商品表（product）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| product_code | varchar(64) | NOT NULL, UNIQUE | 商品编码 |
| product_name | varchar(512) | NOT NULL | 商品名称 |
| barcode | varchar(128) | | 商品条码 |
| category | varchar(128) | | 分类 |
| is_cigarette | boolean | NOT NULL DEFAULT false | 是否香烟 |
| is_fertilizer | boolean | NOT NULL DEFAULT false | 是否化肥 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |
| updated_at | timestamptz | NOT NULL DEFAULT now() | 更新时间 |

**索引**：
```sql
CREATE INDEX idx_product_barcode ON product(barcode);
CREATE INDEX idx_product_category ON product(category);
```

#### 5.1.2 商品价格表（product_price）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| product_code | varchar(64) | NOT NULL | 商品编码 |
| execution_price | numeric(18,2) | NOT NULL | 执行价 |
| member_price | numeric(18,2) | | 会员价 |
| import_version | varchar(64) | NOT NULL | 导入版本 |
| effective_at | timestamptz | NOT NULL DEFAULT now() | 生效时间 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

**索引**：
```sql
CREATE INDEX idx_product_price_code ON product_price(product_code);
CREATE INDEX idx_product_price_effective ON product_price(effective_at);
```

#### 5.1.3 库存快照表（inventory_snapshot）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| station_code | varchar(64) | NOT NULL DEFAULT 'default' | 站点编码 |
| product_code | varchar(64) | NOT NULL | 商品编码 |
| quantity | numeric(18,3) | NOT NULL | 库存数量 |
| safety_threshold | numeric(18,3) | NOT NULL DEFAULT 10 | 安全阈值 |
| import_version | varchar(64) | NOT NULL | 导入版本 |
| snapshot_at | timestamptz | NOT NULL DEFAULT now() | 快照时间 |
| unique (station_code, product_code, import_version) | | | 唯一约束 |

**索引**：
```sql
CREATE INDEX idx_inventory_snapshot_station ON inventory_snapshot(station_code);
CREATE INDEX idx_inventory_snapshot_product ON inventory_snapshot(product_code);
```

#### 5.1.4 促销活动表（promotion_activity）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| activity_code | varchar(64) | NOT NULL, UNIQUE | 活动编码 |
| activity_name | varchar(256) | NOT NULL | 活动名称 |
| source_workbook | varchar(256) | | 来源文件 |
| source_sheet | varchar(256) | | 来源工作表 |
| status | varchar(32) | NOT NULL | 状态 |
| version | varchar(64) | NOT NULL | 版本 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |
| updated_at | timestamptz | NOT NULL DEFAULT now() | 更新时间 |

**状态枚举**：
```sql
-- ACTIVE: 进行中, INACTIVE: 未开始, EXPIRED: 已过期, DISABLED: 已停用
```

#### 5.1.5 促销规则表（promotion_rule）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| rule_id | varchar(64) | NOT NULL, UNIQUE | 规则ID |
| activity_code | varchar(64) | NOT NULL | 关联活动 |
| rule_type | varchar(64) | NOT NULL | 规则类型 |
| priority | int | NOT NULL DEFAULT 0 | 优先级 |
| exclusive_group | varchar(128) | | 互斥组 |
| stackable | boolean | NOT NULL DEFAULT false | 是否可叠加 |
| status | varchar(32) | NOT NULL | 状态 |
| condition_json | jsonb | NOT NULL | 条件JSON |
| benefit_json | jsonb | NOT NULL | 动作JSON |
| version | varchar(64) | NOT NULL | 版本 |
| manual_locked | boolean | NOT NULL DEFAULT false | 是否手动锁定 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |
| updated_at | timestamptz | NOT NULL DEFAULT now() | 更新时间 |

**索引**：
```sql
CREATE INDEX idx_promotion_rule_type_status ON promotion_rule(rule_type, status);
CREATE INDEX idx_promotion_rule_condition_json ON promotion_rule USING GIN(condition_json);
CREATE INDEX idx_promotion_rule_activity ON promotion_rule(activity_code);
```

#### 5.1.6 会员表（member）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| member_code | varchar(64) | NOT NULL, UNIQUE | 会员编号 |
| member_name | varchar(128) | NOT NULL | 会员姓名 |
| phone | varchar(32) | UNIQUE | 手机号 |
| level | varchar(32) | NOT NULL DEFAULT 'NORMAL' | 会员等级编码 |
| total_points | bigint | NOT NULL DEFAULT 0 | 累计积分 |
| available_points | bigint | NOT NULL DEFAULT 0 | 可用积分 |
| birthday | date | | 生日 |
| province | varchar(64) | | 所在省份 |
| status | varchar(32) | NOT NULL DEFAULT 'ACTIVE' | 状态 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |
| updated_at | timestamptz | NOT NULL DEFAULT now() | 更新时间 |

**索引**：
```sql
CREATE INDEX idx_member_phone ON member(phone);
CREATE INDEX idx_member_level ON member(level);
CREATE INDEX idx_member_status ON member(status);
```

#### 5.1.7 会员等级表（member_level）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| level_code | varchar(32) | NOT NULL, UNIQUE | 等级编码 |
| level_name | varchar(128) | NOT NULL | 等级名称 |
| discount_rate | numeric(5,4) | NOT NULL DEFAULT 1.0 | 会员折扣率 |
| points_multiplier | int | NOT NULL DEFAULT 1 | 积分倍率 |
| min_consumption | numeric(18,2) | NOT NULL DEFAULT 0 | 升级最低消费 |
| benefits | jsonb | | 权益列表 |
| priority | int | NOT NULL DEFAULT 0 | 优先级 |

**基础数据**：
```sql
INSERT INTO member_level (level_code, level_name, discount_rate, points_multiplier, min_consumption, benefits, priority) VALUES
('NORMAL', '普通会员', 1.0, 1, 0, '["基础会员价"]', 1),
('SILVER', '银卡会员', 0.95, 15, 1000, '["生日券", "专属活动"]', 2),
('GOLD', '金卡会员', 0.90, 20, 5000, '["生日券", "节日券", "专属客服"]', 3),
('PLATINUM', '铂金会员', 0.85, 30, 20000, '["专属权益包", "优先服务"]', 4);
```

#### 5.1.8 券模板表（coupon_template）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| template_code | varchar(64) | NOT NULL, UNIQUE | 模板编号 |
| coupon_name | varchar(256) | NOT NULL | 券名称 |
| coupon_type | varchar(32) | NOT NULL | 券类型 |
| amount | numeric(18,2) | NOT NULL | 券面额 |
| use_threshold | numeric(18,2) | NOT NULL DEFAULT 0 | 使用门槛 |
| applicable_categories | jsonb | | 适用品类 |
| excluded_categories | jsonb | | 排除品类 |
| applicable_product_codes | jsonb | | 适用商品编码 |
| valid_days | int | NOT NULL DEFAULT 30 | 有效期天数 |
| max_use_count | int | NOT NULL DEFAULT 1 | 最大使用次数 |
| is_member_only | boolean | NOT NULL DEFAULT false | 是否仅限会员 |
| member_levels | jsonb | | 适用会员等级 |
| status | varchar(32) | NOT NULL DEFAULT 'ACTIVE' | 状态 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

**索引**：
```sql
CREATE INDEX idx_coupon_template_type ON coupon_template(coupon_type);
CREATE INDEX idx_coupon_template_status ON coupon_template(status);
```

#### 5.1.9 券实例表（coupon_instance）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| coupon_code | varchar(64) | NOT NULL, UNIQUE | 券码 |
| template_id | bigint | NOT NULL, FOREIGN KEY | 关联券模板 |
| member_id | bigint | FOREIGN KEY | 关联会员 |
| amount | numeric(18,2) | NOT NULL | 面额 |
| use_threshold | numeric(18,2) | NOT NULL DEFAULT 0 | 使用门槛 |
| status | varchar(32) | NOT NULL DEFAULT 'UNUSED' | 状态 |
| valid_until | timestamptz | NOT NULL | 有效期截止 |
| used_at | timestamptz | | 使用时间 |
| used_order_id | varchar(128) | | 使用的订单号 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

**索引**：
```sql
CREATE INDEX idx_coupon_instance_member ON coupon_instance(member_id);
CREATE INDEX idx_coupon_instance_status ON coupon_instance(status);
CREATE INDEX idx_coupon_instance_valid_until ON coupon_instance(valid_until);
```

#### 5.1.10 商品组表（product_group）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| group_code | varchar(64) | NOT NULL, UNIQUE | 组编号 |
| group_name | varchar(256) | NOT NULL | 组名称 |
| group_type | varchar(32) | NOT NULL | 组类型 |
| description | text | | 描述 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

#### 5.1.11 商品组明细表（product_group_item）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| group_id | bigint | NOT NULL, FOREIGN KEY | 关联商品组 |
| product_code | varchar(64) | NOT NULL | 商品编码 |
| quantity | int | NOT NULL DEFAULT 1 | 所需数量 |
| is_primary | boolean | NOT NULL DEFAULT false | 是否主商品 |

#### 5.1.12 组合包表（bundle）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| bundle_code | varchar(64) | NOT NULL, UNIQUE | 包编号 |
| bundle_name | varchar(256) | NOT NULL | 包名称 |
| bundle_price | numeric(18,2) | NOT NULL | 组合价 |
| description | text | | 描述 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

#### 5.1.13 组合包明细表（bundle_item）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| bundle_id | bigint | NOT NULL, FOREIGN KEY | 关联组合包 |
| product_code | varchar(64) | NOT NULL | 商品编码 |
| quantity | int | NOT NULL DEFAULT 1 | 数量 |

#### 5.1.14 交易记录表（transaction）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| txn_no | varchar(64) | NOT NULL, UNIQUE | 交易编号 |
| total_amount | numeric(18,2) | NOT NULL | 商品合计金额 |
| discount_amount | numeric(18,2) | NOT NULL DEFAULT 0 | 优惠总额 |
| payable_amount | numeric(18,2) | NOT NULL | 应付金额 |
| payment_method | varchar(32) | | 支付方式 |
| operator_id | varchar(128) | | 操作员 |
| member_id | bigint | FOREIGN KEY | 会员ID |
| is_member | boolean | NOT NULL DEFAULT false | 是否会员交易 |
| station_code | varchar(64) | NOT NULL | 站点编码 |
| status | varchar(32) | NOT NULL DEFAULT 'PENDING' | 状态 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 交易时间 |

**索引**：
```sql
CREATE INDEX idx_transaction_member ON transaction(member_id);
CREATE INDEX idx_transaction_station ON transaction(station_code);
CREATE INDEX idx_transaction_created ON transaction(created_at);
```

#### 5.1.15 交易明细表（transaction_item）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| transaction_id | bigint | NOT NULL, FOREIGN KEY | 关联交易 |
| product_code | varchar(64) | NOT NULL | 商品编码 |
| product_name | varchar(512) | NOT NULL | 商品名称（冗余） |
| barcode | varchar(128) | | 条码 |
| unit_price | numeric(18,2) | NOT NULL | 原价 |
| actual_price | numeric(18,2) | NOT NULL | 实际单价 |
| quantity | int | NOT NULL | 数量 |
| subtotal | numeric(18,2) | NOT NULL | 小计 |
| applied_promo_id | varchar(64) | | 应用的促销规则ID |
| applied_coupon_code | varchar(64) | | 使用的券码 |

#### 5.1.16 库存预警表（inventory_alert）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| product_code | varchar(64) | NOT NULL | 商品编码 |
| activity_code | varchar(64) | | 关联活动 |
| current_stock | numeric(18,3) | NOT NULL | 当前库存 |
| threshold | numeric(18,3) | NOT NULL | 安全阈值 |
| alert_level | varchar(32) | NOT NULL | 预警级别 |
| suggested_replenish_qty | numeric(18,3) | | 建议补货量 |
| is_promo_related | boolean | NOT NULL DEFAULT false | 是否促销关联 |
| status | varchar(32) | NOT NULL DEFAULT 'PENDING' | 状态 |
| handled_at | timestamptz | | 处理时间 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

**索引**：
```sql
CREATE INDEX idx_inventory_alert_level ON inventory_alert(alert_level);
CREATE INDEX idx_inventory_alert_status ON inventory_alert(status);
CREATE INDEX idx_inventory_alert_promo ON inventory_alert(is_promo_related);
```

### 5.2 表关系图

```text
促销活动 (promotion_activity) 1:N 促销规则 (promotion_rule)
    │                                 │
    │                                 ├── N:1 促销规则草稿 (promotion_rule_draft)
    │                                 └── N:1 促销规则版本 (promotion_rule_version)
    │
    └── 促销规则 (promotion_rule) N:1 组合包 (bundle) 1:N 组合包明细 (bundle_item) N:1 商品 (product)

会员 (member) 1:N 券实例 (coupon_instance) N:1 券模板 (coupon_template)
    │
    ├── 1:N 交易记录 (transaction) 1:N 交易明细 (transaction_item) N:1 商品 (product)
    │
    └── N:1 会员等级 (member_level)

商品 (product)
    ├── 1:N 商品价格 (product_price)
    ├── 1:N 库存快照 (inventory_snapshot)
    ├── 1:N 库存预警 (inventory_alert)
    └── 1:N 商品组明细 (product_group_item) N:1 商品组 (product_group)
```

### 5.3 ER图

```text
                     ┌─────────────────┐
                     │ promotion_rule  │
                     │  促销规则        │
                     └────────┬────────┘
                              │
            ┌─────────────────┼─────────────────┐
            │                 │                 │
    ┌───────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐
    │promotion_activity│  │promotion_draft│  │promotion_version│
    │    促销活动     │  │   规则草稿    │  │   规则版本    │
    └────────────────┘  └──────────────┘  └──────────────┘

                     ┌─────────────────┐
                     │    member       │
                     │     会员        │
                     └────────┬────────┘
                              │
            ┌─────────────────┼─────────────────┐
            │                 │                 │
    ┌───────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐
    │member_level   │  │coupon_instance│  │ transaction  │
    │   会员等级     │  │   券实例      │  │   交易记录    │
    └────────────────┘  └──────┬──────┘  └──────┬──────┘
                               │                 │
                       ┌───────▼───────┐  ┌──────▼───────┐
                       │coupon_template│  │transaction_item│
                       │   券模板      │  │   交易明细    │
                       └────────────────┘  └──────────────┘

                     ┌─────────────────┐
                     │    product      │
                     │     商品        │
                     └────────┬────────┘
                              │
            ┌─────────────────┼─────────────────┐
            │                 │                 │
    ┌───────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐
    │product_price  │  │inventory_snapshot│ │product_group_item│
    │   商品价格     │  │   库存快照     │  │   商品组明细    │
    └────────────────┘  └──────┬──────┘  └──────┬──────┘
                               │                 │
                       ┌───────▼───────┐  ┌──────▼───────┐
                       │inventory_alert│  │product_group │
                       │   库存预警     │  │   商品组      │
                       └────────────────┘  └──────────────┘

                     ┌─────────────────┐
                     │     bundle      │
                     │    组合包       │
                     └────────┬────────┘
                              │
                     ┌────────▼────────┐
                     │  bundle_item    │
                     │   组合包明细     │
                     └─────────────────┘
```

---

## 6. API接口设计

### 6.1 接口总览

| 模块 | 基础路径 | 接口数量 |
|------|---------|---------|
| 促销规则 | /api/promotion-rules | 7 |
| 结算 | /api/checkout | 4 |
| 会员 | /api/members | 7 |
| 券 | /api/coupons, /api/coupon-templates | 8 |
| 商品 | /api/products | 5 |
| 库存 | /api/inventory | 3 |
| 数据导入 | /api/import | 5 |
| 补货 | /api/replenishment | 2 |

### 6.2 接口详细设计

#### 6.2.1 促销规则管理接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/promotion-rules | 查询促销规则列表 |
| GET | /api/promotion-rules/{ruleId} | 查询规则详情 |
| POST | /api/promotion-rules | 创建促销规则 |
| PUT | /api/promotion-rules/{ruleId} | 更新促销规则 |
| DELETE | /api/promotion-rules/{ruleId} | 删除促销规则 |
| POST | /api/promotion-rules/{ruleId}/activate | 启用规则 |
| POST | /api/promotion-rules/{ruleId}/deactivate | 停用规则 |

#### 6.2.2 结算接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| POST | /api/checkout/calculate | 计算促销方案 |
| POST | /api/checkout/confirm | 确认结算 |
| GET | /api/checkout/records | 查询交易记录 |
| GET | /api/checkout/records/{txnNo} | 查询交易详情 |

#### 6.2.3 会员管理接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/members | 查询会员列表 |
| GET | /api/members/{memberCode} | 查询会员详情 |
| POST | /api/members | 创建会员 |
| PUT | /api/members/{memberCode} | 更新会员信息 |
| POST | /api/members/{memberCode}/points | 积分变动 |
| GET | /api/members/{memberCode}/coupons | 查询会员券 |
| POST | /api/members/identify | 会员识别 |

#### 6.2.4 券管理接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/coupon-templates | 查询券模板 |
| GET | /api/coupon-templates/{templateCode} | 查询模板详情 |
| POST | /api/coupon-templates | 创建券模板 |
| PUT | /api/coupon-templates/{templateCode} | 更新券模板 |
| POST | /api/coupons/issue | 发放券 |
| POST | /api/coupons/redeem | 核销券 |
| GET | /api/coupons/{couponCode} | 查询券状态 |
| GET | /api/coupons/stats | 券统计 |

#### 6.2.5 库存预警接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/inventory/alerts | 查询库存预警列表 |
| POST | /api/inventory/alerts/{alertId}/handle | 处理预警 |
| POST | /api/inventory/replenishment/export | 导出补货清单 |

### 6.3 响应格式规范

所有API接口统一返回以下格式：

```json
{
  "code": "number",
  "message": "string",
  "data": {},
  "timestamp": "string (ISO8601)"
}
```

**code值定义**：

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 7. 部署架构

### 7.1 开发环境

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                        │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │  React   │  │  Spring  │  │PostgreSQL│              │
│  │  (3000)  │  │  Boot    │  │  (5432)  │              │
│  │  Frontend│  │  (18082) │  │          │              │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘              │
│       │             │             │                     │
│       └─────────────┼─────────────┘                     │
│                     ▼                                   │
│              ┌──────────┐                               │
│              │  Redis   │                               │
│              │  (6379)  │                               │
│              └──────────┘                               │
└─────────────────────────────────────────────────────────┘
```

### 7.2 生产环境

```
┌─────────────────────────────────────────────────────────────────────┐
│                           负载均衡层                                 │
│                        Nginx / Load Balancer                        │
│                               │                                      │
├───────────────────────────────┼──────────────────────────────────────┤
│                           应用层                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │  React   │  │  Spring  │  │  Spring  │  │  Spring  │             │
│  │  Frontend│  │  Boot    │  │  Boot    │  │  Boot    │             │
│  └──────────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘             │
│                     │             │             │                    │
├─────────────────────┼─────────────┼─────────────┼────────────────────┤
│                           数据层                                    │
│              ┌──────────┐  ┌──────────┐                             │
│              │PostgreSQL│  │   Redis  │                             │
│              │ (主从)   │  │ (集群)   │                             │
│              └──────────┘  └──────────┘                             │
├──────────────────────────────────────────────────────────────────────┤
│                           存储层                                    │
│                     ┌──────────┐                                    │
│                     │  文件存储  │  (导入文件、报表导出)              │
│                     └──────────┘                                    │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.3 端口配置

| 服务 | 端口 | 说明 |
|------|------|------|
| 前端 | 3000 | React开发服务器 |
| 后端 | 18082 | Spring Boot应用 |
| PostgreSQL | 5432 | 数据库 |
| Redis | 6379 | 缓存 |
| Swagger | 18082/swagger-ui.html | API文档 |

---

## 8. 安全设计

### 8.1 认证与授权

| 项目 | 说明 |
|------|------|
| 认证方式 | JWT Token认证 |
| 权限控制 | Spring Security + RBAC |
| 角色定义 | CASHIER, STORE_MANAGER, REGION_ADMIN, SYS_ADMIN |
| 密码策略 | BCrypt加密，最小长度8位 |

### 8.2 数据安全

| 项目 | 说明 |
|------|------|
| 敏感信息脱敏 | 手机号中间4位隐藏 |
| 接口密钥加密 | AES加密存储 |
| 操作日志 | 不可随意删除，保留6个月 |
| SQL注入防护 | MyBatis参数化查询 |
| XSS防护 | 前端输入校验 + 后端过滤 |

### 8.3 接口安全

| 项目 | 说明 |
|------|------|
| 请求频率限制 | 限流中间件，单IP每分钟100次 |
| 请求签名验证 | 对外接口需签名校验 |
| HTTPS | 生产环境强制HTTPS |
| 跨域配置 | 配置允许的Origin |

---

## 附录：数据库迁移脚本清单

| 文件 | 内容 |
|------|------|
| V1__init_core_tables.sql | 创建商品、价格、库存、促销活动、促销规则等核心表 |
| V2__promotion_rule_governance.sql | 促销规则治理表（草稿、版本、审计） |
| V3__audit_checkout_confirmation_replenishment.sql | 审计、结算确认、补货清单表 |
| V4__coupon_redeem.sql | 券核销相关表 |
| V5__bundle_product_group_demo.sql | 组合包和商品组表 |
| V6__coupon_demo_seed.sql | 券模板和实例表 |
| V7__demo_promotion_rules.sql | 示例促销规则数据 |
| V8__coupon_discount_rate.sql | 券折扣率字段 |
| V9__activity_board_structured_import.sql | 活动看板结构化导入表 |
| V10-V22__*.sql | 各活动规则导入数据 |
| V23__member_tables.sql | 会员表、会员等级表 |
| V24__coupon_tables.sql | 券模板表、券实例表 |
| V25__product_group_tables.sql | 商品组表、商品组明细表 |
| V26__bundle_tables.sql | 组合包表、组合包明细表 |
| V27__transaction_tables.sql | 交易记录表、交易明细表 |
| V28__inventory_alert_table.sql | 库存预警表 |
| V29__demo_member_data.sql | 会员等级示例数据 |
| V30__demo_coupon_data.sql | 券模板示例数据 |
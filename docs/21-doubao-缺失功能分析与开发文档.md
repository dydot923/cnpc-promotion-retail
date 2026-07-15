# 缺失功能分析与开发文档

## 文档说明

本文档针对当前系统中尚未实现的核心功能进行详细分析，包括：
1. 用户反馈的未实现功能点梳理
2. 功能缺失的根因分析
3. 详细的功能设计方案
4. 数据库表优化与新增设计
5. API接口契约定义

---

## 目录

1. [未实现功能清单](#1-未实现功能清单)
2. [根因分析](#2-根因分析)
3. [功能详细设计](#3-功能详细设计)
4. [数据库表优化设计](#4-数据库表优化设计)
5. [API接口契约](#5-api接口契约)
6. [开发优先级与排期](#6-开发优先级与排期)

---

## 1. 未实现功能清单

### 1.1 用户反馈的问题

根据用户反馈，当前系统存在以下核心功能缺失：

| 序号 | 功能描述 | 关联场景 | 优先级 |
|------|---------|---------|--------|
| F-01 | 满500送券功能 | 一体化看板首页展示，但促销活动板块无此功能 | P0 |
| F-02 | 不同会员消费优惠方式不同 | 会员等级与促销规则未关联 | P0 |
| F-03 | 买酒满555元送两条烟 | 满额赠商品未实现 | P0 |
| F-04 | 满减功能 | 购物车满X元减Y元 | P0 |
| F-05 | 9.9元商品专区 | 基础功能，需完善 | P1 |
| F-06 | 加油不同升数对应不同福利 | 油品升数门槛未实现 | P0 |

### 1.2 功能矩阵状态

| 促销类型 | 规则引擎 | 数据导入 | 前端展示 | 会员关联 | 状态 |
|---------|---------|---------|---------|---------|------|
| 固定价促销 | ✅ | ✅ | ✅ | ✅ | 已实现 |
| 百分比折扣 | ✅ | ✅ | ✅ | ✅ | 已实现 |
| 金额立减(AMOUNT_OFF) | ✅ | ✅ | ❌ | ✅ | 部分实现 |
| 加油换购 | ✅ | ✅ | ❌ | ✅ | 部分实现 |
| 买赠(GIFT_ITEM) | ✅ | ✅ | ❌ | ✅ | 部分实现 |
| **赠券(GIFT_COUPON)** | ✅ | ❌ | ❌ | ❌ | **未实现** |
| **券核销(COUPON_REDEEM)** | ✅ | ❌ | ❌ | ❌ | **未实现** |
| **会员专享价** | ✅ | ❌ | ❌ | ❌ | **未实现** |
| **满减赠商品** | ❌ | ❌ | ❌ | ❌ | **未实现** |
| **油品升数优惠** | ✅ | ❌ | ❌ | ❌ | **未实现** |

---

## 2. 根因分析

### 2.1 业务逻辑层面

| 问题 | 根因 | 影响 |
|------|------|------|
| 赠券功能缺失 | 促销规则与券发放链路未打通，会员表未接入 | 满额赠券无法执行 |
| 会员专属优惠缺失 | 会员等级体系未与促销规则引擎关联，结算时未识别会员身份 | 不同会员享受相同优惠 |
| 满减赠商品缺失 | GIFT_ITEM规则缺少购物车金额门槛条件 | 无法实现"满X送Y"场景 |
| 油品升数优惠缺失 | FuelContext缺少升数计算，规则引擎未处理minFuelVolume条件 | 无法按升数触发优惠 |

### 2.2 数据模型层面

| 问题 | 根因 | 影响 |
|------|------|------|
| 会员数据缺失 | 缺少member表和member_level表 | 无法存储会员信息和等级配置 |
| 券数据缺失 | 缺少coupon_template和coupon_instance表 | 无法管理券模板和发放记录 |
| 交易数据缺失 | 缺少transaction和transaction_item表 | 无法追踪促销执行和结算记录 |
| 商品组数据缺失 | 缺少product_group和product_group_item表 | 无法管理"红牛（2款通用）"等商品组 |

### 2.3 前端层面

| 问题 | 根因 | 影响 |
|------|------|------|
| 智能结算页缺失 | 未开发扫码输入、促销选择、购物车组件 | 无法完成实际结算流程 |
| 赠券展示缺失 | 未开发券列表和赠券提示组件 | 满额赠券无法展示给用户 |
| 会员识别界面缺失 | 未开发会员扫码和等级展示组件 | 无法识别会员身份 |

---

## 3. 功能详细设计

### 3.1 功能F-01：满额赠券

#### 3.1.1 业务规则

```text
触发条件: 购物车金额 >= 阈值
执行动作: 自动发放对应面额的券到会员账户
数据来源: 一体化营销活动看板 → "逢7气惠满500送LNG券"等规则
```

#### 3.1.2 规则配置示例

```json
{
  "ruleType": "GIFT_COUPON",
  "condition": {
    "minCartAmount": 500,
    "includedCategories": ["便利店"],
    "daysOfMonth": [7, 17, 27],
    "memberRequired": true
  },
  "benefit": {
    "giftCouponTiers": [
      {
        "threshold": 500,
        "couponName": "LNG加油券",
        "amount": 30,
        "useThreshold": 100,
        "validDays": 30,
        "quantity": 1
      },
      {
        "threshold": 1000,
        "couponName": "便利店满减券",
        "amount": 50,
        "useThreshold": 200,
        "validDays": 30,
        "quantity": 2
      }
    ]
  },
  "priority": 50,
  "exclusiveGroup": "gift_coupon",
  "stackable": true
}
```

#### 3.1.3 执行流程

```text
购物车金额达到阈值
        │
        ▼
┌─────────────────────┐
│ 查询匹配的GIFT_COUPON规则 │
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 判断会员身份         │ → 必须是会员才能获得赠券
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 计算应发放券档位     │ → 满500送30元券，满1000送50元券
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 创建券实例          │ → coupon_instance表
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 更新会员券列表       │ → 关联member_id
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 展示赠券成功提示     │ → 前端弹出通知
└─────────────────────┘
```

### 3.2 功能F-02：会员专属优惠

#### 3.2.1 业务规则

```text
不同会员等级享受不同优惠：
- 普通会员：基础会员价
- 银卡会员：95折 + 积分1.5倍
- 金卡会员：9折 + 积分2倍 + 生日券
- 铂金会员：85折 + 积分3倍 + 专属权益包
```

#### 3.2.2 规则配置示例

```json
{
  "ruleType": "PERCENTAGE_DISCOUNT",
  "condition": {
    "memberRequired": true,
    "memberLevels": ["SILVER", "GOLD", "PLATINUM"],
    "applicableCategories": ["非油品"]
  },
  "benefit": {
    "discountRate": 0.95,
    "memberLevelDiscountRates": {
      "SILVER": 0.95,
      "GOLD": 0.90,
      "PLATINUM": 0.85
    }
  },
  "priority": 150,
  "exclusiveGroup": "member_price",
  "stackable": false
}
```

#### 3.2.3 执行流程

```text
识别会员身份（扫码或手机号）
        │
        ▼
┌─────────────────────┐
│ 查询会员等级         │ → member.level → member_level表
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 查询会员专属促销规则 │ → condition.memberLevels包含当前等级
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 按等级计算折扣       │ → 银卡95折，金卡9折，铂金85折
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 与其他促销比较取最优 │ → min(会员价, 促销价)
└─────────────────────┘
```

### 3.3 功能F-03：满额赠商品

#### 3.3.1 业务规则

```text
触发条件: 购物车中特定品类商品金额 >= 阈值
执行动作: 赠送指定商品
数据来源: "买酒满555元送两条烟"
```

#### 3.3.2 规则配置示例

```json
{
  "ruleType": "GIFT_ITEM",
  "condition": {
    "minCartAmount": 555,
    "includedCategories": ["酒类"],
    "memberRequired": false
  },
  "benefit": {
    "giftItems": [
      {
        "itemCode": "70030041",
        "itemName": "黄山香烟",
        "quantity": 2,
        "isOptional": true
      }
    ],
    "maxGiftQuantity": 4
  },
  "priority": 80,
  "exclusiveGroup": "gift_item",
  "stackable": true
}
```

#### 3.3.3 执行流程

```text
购物车添加商品
        │
        ▼
┌─────────────────────┐
│ 实时计算指定品类金额  │ → 累计酒类商品金额
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 判断是否达到满额门槛  │ → >= 555元
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 展示赠商品选项       │ → 可选择黄山香烟x2或其他礼品
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 确认后添加赠品到购物车 │ → 赠品单价为0
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 扣减赠品库存         │ → 库存预警检查
└─────────────────────┘
```

### 3.4 功能F-04：满减

#### 3.4.1 业务规则

```text
触发条件: 购物车金额 >= 阈值
执行动作: 直接减免指定金额
支持多档位: 满100减10, 满200减25, 满500减80
```

#### 3.4.2 规则配置示例

```json
{
  "ruleType": "AMOUNT_OFF",
  "condition": {
    "minCartAmount": 100,
    "includedCategories": ["便利店"],
    "excludedCategories": ["香烟", "化肥"]
  },
  "benefit": {
    "amountOffTiers": [
      {"threshold": 100, "amountOff": 10},
      {"threshold": 200, "amountOff": 25},
      {"threshold": 500, "amountOff": 80}
    ],
    "maxAmountOff": 80
  },
  "priority": 100,
  "exclusiveGroup": "direct_discount",
  "stackable": false
}
```

#### 3.4.3 执行流程

```text
购物车金额变化
        │
        ▼
┌─────────────────────┐
│ 实时计算满减进度     │ → "还差¥50即可减¥25"
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 判断满足的最高档位   │ → 满500减80
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 计算优惠后金额       │ → 原价 - 满减金额
└─────────────────────┘
```

### 3.5 功能F-06：加油升数优惠

#### 3.5.1 业务规则

```text
触发条件: 加油升数达到指定门槛
执行动作: 根据升数档位给予不同优惠
数据来源: 非非促销（统建）→ "CN98满1升立减0.8元/升"
```

#### 3.5.2 规则配置示例

```json
{
  "ruleType": "FUEL_VOLUME_DISCOUNT",
  "condition": {
    "fuelTypes": ["CN98"],
    "minFuelVolume": 1.0,
    "daysOfMonth": [8, 18, 28]
  },
  "benefit": {
    "discountPerUnit": 0.8,
    "volumeTiers": [
      {"minVolume": 1.0, "discountPerUnit": 0.8},
      {"minVolume": 20.0, "discountPerUnit": 1.0},
      {"minVolume": 50.0, "discountPerUnit": 1.5}
    ],
    "maxDiscount": 50.0
  },
  "priority": 200,
  "exclusiveGroup": "fuel_discount",
  "stackable": false
}
```

#### 3.5.3 执行流程

```text
输入油品消费信息
        │
        ▼
┌─────────────────────┐
│ 识别油品类型         │ → CN98/汽油/柴油
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 获取加油升数         │ → 油枪计量或手动输入
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 匹配升数档位         │ → 满1升立减0.8元
└──────────┬──────────┘
           ▼
┌─────────────────────┐
│ 计算油品优惠后金额    │ → 原价 - (升数 × 每升立减)
└─────────────────────┘
```

---

## 4. 数据库表优化设计

### 4.1 新增表结构

#### 4.1.1 会员表（member）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| member_code | varchar(64) | NOT NULL, UNIQUE | 会员编号 |
| member_name | varchar(128) | NOT NULL | 会员姓名 |
| phone | varchar(32) | UNIQUE | 手机号 |
| level | varchar(32) | NOT NULL | 会员等级编码 |
| total_points | bigint | DEFAULT 0 | 累计积分 |
| available_points | bigint | DEFAULT 0 | 可用积分 |
| birthday | date | | 生日 |
| province | varchar(64) | | 所在省份 |
| status | varchar(32) | NOT NULL DEFAULT 'ACTIVE' | 状态 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |
| updated_at | timestamptz | NOT NULL DEFAULT now() | 更新时间 |

**索引**:
```sql
CREATE INDEX idx_member_phone ON member(phone);
CREATE INDEX idx_member_level ON member(level);
CREATE INDEX idx_member_status ON member(status);
```

#### 4.1.2 会员等级表（member_level）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| level_code | varchar(32) | NOT NULL, UNIQUE | 等级编码 |
| level_name | varchar(128) | NOT NULL | 等级名称 |
| discount_rate | numeric(5,4) | NOT NULL DEFAULT 1.0 | 会员折扣率 |
| points_multiplier | int | NOT NULL DEFAULT 1 | 积分倍率 |
| min_consumption | numeric(18,2) | DEFAULT 0 | 升级最低消费 |
| benefits | jsonb | | 权益列表 |
| priority | int | NOT NULL DEFAULT 0 | 优先级 |

**数据示例**:
```sql
INSERT INTO member_level (level_code, level_name, discount_rate, points_multiplier, min_consumption, benefits, priority) VALUES
('NORMAL', '普通会员', 1.0, 1, 0, '["基础会员价"]', 1),
('SILVER', '银卡会员', 0.95, 15, 1000, '["生日券", "专属活动"]', 2),
('GOLD', '金卡会员', 0.90, 20, 5000, '["生日券", "节日券", "专属客服"]', 3),
('PLATINUM', '铂金会员', 0.85, 30, 20000, '["专属权益包", "优先服务"]', 4);
```

#### 4.1.3 券模板表（coupon_template）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| template_code | varchar(64) | NOT NULL, UNIQUE | 模板编号 |
| coupon_name | varchar(256) | NOT NULL | 券名称 |
| coupon_type | varchar(32) | NOT NULL | 券类型 |
| amount | numeric(18,2) | NOT NULL | 券面额 |
| use_threshold | numeric(18,2) | DEFAULT 0 | 使用门槛 |
| applicable_categories | jsonb | | 适用品类 |
| excluded_categories | jsonb | | 排除品类 |
| applicable_product_codes | jsonb | | 适用商品编码 |
| valid_days | int | NOT NULL DEFAULT 30 | 有效期天数 |
| max_use_count | int | DEFAULT 1 | 最大使用次数 |
| is_member_only | boolean | NOT NULL DEFAULT false | 是否仅限会员 |
| member_levels | jsonb | | 适用会员等级 |
| status | varchar(32) | NOT NULL DEFAULT 'ACTIVE' | 状态 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

**索引**:
```sql
CREATE INDEX idx_coupon_template_type ON coupon_template(coupon_type);
CREATE INDEX idx_coupon_template_status ON coupon_template(status);
```

#### 4.1.4 券实例表（coupon_instance）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| coupon_code | varchar(64) | NOT NULL, UNIQUE | 券码 |
| template_id | bigint | NOT NULL, FOREIGN KEY | 关联券模板 |
| member_id | bigint | FOREIGN KEY | 关联会员 |
| amount | numeric(18,2) | NOT NULL | 面额 |
| use_threshold | numeric(18,2) | DEFAULT 0 | 使用门槛 |
| status | varchar(32) | NOT NULL DEFAULT 'UNUSED' | 状态 |
| valid_until | timestamptz | NOT NULL | 有效期截止 |
| used_at | timestamptz | | 使用时间 |
| used_order_id | varchar(128) | | 使用的订单号 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

**索引**:
```sql
CREATE INDEX idx_coupon_instance_member ON coupon_instance(member_id);
CREATE INDEX idx_coupon_instance_status ON coupon_instance(status);
CREATE INDEX idx_coupon_instance_valid_until ON coupon_instance(valid_until);
```

#### 4.1.5 商品组表（product_group）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| group_code | varchar(64) | NOT NULL, UNIQUE | 组编号 |
| group_name | varchar(256) | NOT NULL | 组名称 |
| group_type | varchar(32) | NOT NULL | 组类型 |
| description | text | | 描述 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

#### 4.1.6 商品组明细表（product_group_item）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| group_id | bigint | NOT NULL, FOREIGN KEY | 关联商品组 |
| product_code | varchar(64) | NOT NULL | 商品编码 |
| quantity | int | NOT NULL DEFAULT 1 | 所需数量 |
| is_primary | boolean | NOT NULL DEFAULT false | 是否主商品 |

**数据示例**:
```sql
INSERT INTO product_group (group_code, group_name, group_type, description) VALUES
('RED_BULL', '红牛系列', '通用组', '红牛（2款通用）');

INSERT INTO product_group_item (group_id, product_code, quantity, is_primary) VALUES
(1, '70123456', 1, true),
(1, '70123457', 1, false);
```

#### 4.1.7 组合包表（bundle）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| bundle_code | varchar(64) | NOT NULL, UNIQUE | 包编号 |
| bundle_name | varchar(256) | NOT NULL | 包名称 |
| bundle_price | numeric(18,2) | NOT NULL | 组合价 |
| description | text | | 描述 |
| created_at | timestamptz | NOT NULL DEFAULT now() | 创建时间 |

#### 4.1.8 组合包明细表（bundle_item）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | bigserial | PRIMARY KEY | 主键 |
| bundle_id | bigint | NOT NULL, FOREIGN KEY | 关联组合包 |
| product_code | varchar(64) | NOT NULL | 商品编码 |
| quantity | int | NOT NULL DEFAULT 1 | 数量 |

#### 4.1.9 交易记录表（transaction）

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

**索引**:
```sql
CREATE INDEX idx_transaction_member ON transaction(member_id);
CREATE INDEX idx_transaction_station ON transaction(station_code);
CREATE INDEX idx_transaction_created ON transaction(created_at);
```

#### 4.1.10 交易明细表（transaction_item）

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

#### 4.1.11 库存预警表（inventory_alert）

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

**索引**:
```sql
CREATE INDEX idx_inventory_alert_level ON inventory_alert(alert_level);
CREATE INDEX idx_inventory_alert_status ON inventory_alert(status);
CREATE INDEX idx_inventory_alert_promo ON inventory_alert(is_promo_related);
```

### 4.2 表关系图

```text
促销活动 (promotion_activity)
    │
    ├── 促销规则 (promotion_rule)
    │       ├── 促销规则草稿 (promotion_rule_draft)
    │       └── 促销规则版本 (promotion_rule_version)
    │
    ├── 促销商品范围 → 商品 (product)
    │
    └── 组合包 (bundle) ──→ 组合包明细 (bundle_item) ──→ 商品 (product)

会员 (member) ──→ 会员等级 (member_level)
    │
    ├── 券实例 (coupon_instance) ──→ 券模板 (coupon_template)
    │
    └── 交易记录 (transaction) ──→ 交易明细 (transaction_item) ──→ 商品 (product)

商品 (product)
    ├── 商品价格 (product_price)
    ├── 库存快照 (inventory_snapshot)
    ├── 库存预警 (inventory_alert)
    └── 商品组明细 (product_group_item) ──→ 商品组 (product_group)
```

---

## 5. API接口契约

### 5.1 会员管理接口

| HTTP方法 | 路径 | 请求体 | 响应体 | 说明 |
|----------|------|--------|--------|------|
| GET | /api/members | - | MemberListResponse | 查询会员列表 |
| GET | /api/members/{memberCode} | - | MemberResponse | 查询会员详情 |
| POST | /api/members | MemberCreateRequest | MemberResponse | 创建会员 |
| PUT | /api/members/{memberCode} | MemberUpdateRequest | MemberResponse | 更新会员信息 |
| POST | /api/members/{memberCode}/points | PointsChangeRequest | PointsChangeResponse | 积分变动 |
| GET | /api/members/{memberCode}/coupons | - | MemberCouponListResponse | 查询会员券 |
| POST | /api/members/identify | MemberIdentifyRequest | MemberResponse | 会员识别（扫码/手机号） |

**请求体定义**:

```json
// MemberCreateRequest
{
  "memberName": "string",
  "phone": "string",
  "level": "string",
  "birthday": "string (yyyy-MM-dd)",
  "province": "string"
}

// MemberUpdateRequest
{
  "memberName": "string",
  "level": "string",
  "birthday": "string (yyyy-MM-dd)",
  "province": "string"
}

// PointsChangeRequest
{
  "changeType": "ADD | SUBTRACT",
  "amount": "number",
  "reason": "string"
}

// MemberIdentifyRequest
{
  "identifier": "string",
  "identifyType": "PHONE | BARCODE | MEMBER_CODE"
}
```

**响应体定义**:

```json
// MemberResponse
{
  "memberCode": "string",
  "memberName": "string",
  "phone": "string",
  "level": "string",
  "levelName": "string",
  "totalPoints": "number",
  "availablePoints": "number",
  "birthday": "string",
  "province": "string",
  "status": "string",
  "createdAt": "string (ISO8601)",
  "discountRate": "number",
  "pointsMultiplier": "number",
  "benefits": ["string"]
}

// MemberCouponListResponse
{
  "memberCode": "string",
  "coupons": [
    {
      "couponCode": "string",
      "couponName": "string",
      "couponType": "string",
      "amount": "number",
      "useThreshold": "number",
      "status": "string",
      "validUntil": "string (ISO8601)",
      "applicableCategories": ["string"]
    }
  ]
}
```

### 5.2 券管理接口

| HTTP方法 | 路径 | 请求体 | 响应体 | 说明 |
|----------|------|--------|--------|------|
| GET | /api/coupon-templates | - | CouponTemplateListResponse | 查询券模板 |
| GET | /api/coupon-templates/{templateCode} | - | CouponTemplateResponse | 查询模板详情 |
| POST | /api/coupon-templates | CouponTemplateCreateRequest | CouponTemplateResponse | 创建券模板 |
| PUT | /api/coupon-templates/{templateCode} | CouponTemplateUpdateRequest | CouponTemplateResponse | 更新券模板 |
| POST | /api/coupons/issue | CouponIssueRequest | CouponIssueResponse | 发放券 |
| POST | /api/coupons/redeem | CouponRedeemRequest | CouponRedeemResponse | 核销券 |
| GET | /api/coupons/{couponCode} | - | CouponInstanceResponse | 查询券状态 |
| GET | /api/coupons/stats | - | CouponStatsResponse | 券统计 |

**请求体定义**:

```json
// CouponTemplateCreateRequest
{
  "couponName": "string",
  "couponType": "string",
  "amount": "number",
  "useThreshold": "number",
  "applicableCategories": ["string"],
  "excludedCategories": ["string"],
  "applicableProductCodes": ["string"],
  "validDays": "number",
  "maxUseCount": "number",
  "isMemberOnly": "boolean",
  "memberLevels": ["string"]
}

// CouponIssueRequest
{
  "templateCode": "string",
  "memberCode": "string",
  "quantity": "number",
  "validDays": "number"
}

// CouponRedeemRequest
{
  "couponCode": "string",
  "orderAmount": "number",
  "productCodes": ["string"]
}
```

**响应体定义**:

```json
// CouponTemplateResponse
{
  "templateCode": "string",
  "couponName": "string",
  "couponType": "string",
  "amount": "number",
  "useThreshold": "number",
  "applicableCategories": ["string"],
  "excludedCategories": ["string"],
  "applicableProductCodes": ["string"],
  "validDays": "number",
  "maxUseCount": "number",
  "isMemberOnly": "boolean",
  "memberLevels": ["string"],
  "status": "string"
}

// CouponIssueResponse
{
  "couponCodes": ["string"],
  "issuedCount": "number"
}

// CouponRedeemResponse
{
  "couponCode": "string",
  "redeemedAmount": "number",
  "remainingAmount": "number",
  "status": "string"
}

// CouponStatsResponse
{
  "totalIssued": "number",
  "totalRedeemed": "number",
  "totalExpired": "number",
  "redeemRate": "number"
}
```

### 5.3 结算计算接口

| HTTP方法 | 路径 | 请求体 | 响应体 | 说明 |
|----------|------|--------|--------|------|
| POST | /api/checkout/calculate | CheckoutCalculateRequest | CheckoutCalculateResponse | 计算促销方案 |
| POST | /api/checkout/confirm | CheckoutConfirmRequest | CheckoutConfirmationResponse | 确认结算 |
| GET | /api/checkout/records | - | TransactionListResponse | 查询交易记录 |
| GET | /api/checkout/records/{txnNo} | - | TransactionDetailResponse | 查询交易详情 |

**请求体定义**:

```json
// CheckoutCalculateRequest
{
  "cartItems": [
    {
      "productCode": "string",
      "barcode": "string",
      "quantity": "number",
      "unitPrice": "number"
    }
  ],
  "fuelContext": {
    "fuelType": "string",
    "volume": "number",
    "amount": "number"
  },
  "memberCode": "string",
  "selectedCoupons": ["string"],
  "stationCode": "string"
}

// CheckoutConfirmRequest
{
  "calculationId": "string",
  "selectedPromotionIds": ["string"],
  "paymentMethod": "string",
  "operatorId": "string"
}
```

**响应体定义**:

```json
// CheckoutCalculateResponse
{
  "calculationId": "string",
  "cartItems": [
    {
      "productCode": "string",
      "productName": "string",
      "quantity": "number",
      "unitPrice": "number",
      "actualPrice": "number",
      "subtotal": "number",
      "applicablePromotions": ["string"]
    }
  ],
  "fuelContext": {
    "fuelType": "string",
    "volume": "number",
    "amount": "number",
    "actualAmount": "number",
    "discountAmount": "number"
  },
  "promotionCandidates": [
    {
      "promotionId": "string",
      "promotionName": "string",
      "promotionType": "string",
      "discountAmount": "number",
      "finalPrice": "number",
      "applicable": "boolean",
      "reason": "string",
      "isRecommended": "boolean"
    }
  ],
  "availableCoupons": [
    {
      "couponCode": "string",
      "couponName": "string",
      "amount": "number",
      "useThreshold": "number",
      "applicable": "boolean",
      "reason": "string"
    }
  ],
  "giftCoupons": [
    {
      "couponName": "string",
      "amount": "number",
      "triggerCondition": "string"
    }
  ],
  "totals": {
    "subtotal": "number",
    "promotionDiscount": "number",
    "couponDiscount": "number",
    "totalDiscount": "number",
    "payableAmount": "number"
  },
  "memberInfo": {
    "memberCode": "string",
    "memberName": "string",
    "level": "string",
    "discountRate": "number",
    "pointsEarned": "number"
  }
}

// CheckoutConfirmationResponse
{
  "txnNo": "string",
  "status": "string",
  "payableAmount": "number",
  "issuedCoupons": ["string"],
  "pointsEarned": "number",
  "createdAt": "string (ISO8601)"
}
```

### 5.4 促销规则管理接口

| HTTP方法 | 路径 | 请求体 | 响应体 | 说明 |
|----------|------|--------|--------|------|
| GET | /api/promotion-rules | - | PromotionRuleListResponse | 查询促销规则列表 |
| GET | /api/promotion-rules/{ruleId} | - | PromotionRuleResponse | 查询规则详情 |
| POST | /api/promotion-rules | PromotionRuleCreateRequest | PromotionRuleResponse | 创建促销规则 |
| PUT | /api/promotion-rules/{ruleId} | PromotionRuleUpdateRequest | PromotionRuleResponse | 更新促销规则 |
| DELETE | /api/promotion-rules/{ruleId} | - | - | 删除促销规则 |
| POST | /api/promotion-rules/{ruleId}/activate | - | PromotionRuleResponse | 启用规则 |
| POST | /api/promotion-rules/{ruleId}/deactivate | - | PromotionRuleResponse | 停用规则 |

**请求体定义**:

```json
// PromotionRuleCreateRequest
{
  "activityCode": "string",
  "ruleType": "string",
  "priority": "number",
  "exclusiveGroup": "string",
  "stackable": "boolean",
  "condition": {
    "daysOfMonth": ["number"],
    "dateCondition": {"startDate": "string", "endDate": "string"},
    "timeRangeCondition": {"startTime": "string", "endTime": "string"},
    "fuelTypes": ["string"],
    "minFuelAmount": "number",
    "minFuelVolume": "number",
    "productCodes": ["string"],
    "includedCategories": ["string"],
    "excludedCategories": ["string"],
    "minProductQuantity": "number",
    "minCartAmount": "number",
    "memberRequired": "boolean",
    "memberLevels": ["string"],
    "birthdayMonthRequired": "boolean",
    "stationTypes": ["string"],
    "stationProvinces": ["string"],
    "minInventoryQuantity": "number"
  },
  "benefit": {
    "fixedPrice": "number",
    "discountRate": "number",
    "amountOff": "number",
    "amountOffTiers": [{"threshold": "number", "amountOff": "number"}],
    "exchangePrice": "number",
    "exchangeQuantity": "number",
    "giftItemCode": "string",
    "giftItemName": "string",
    "giftItemQuantity": "number",
    "giftItems": [{"itemCode": "string", "itemName": "string", "quantity": "number"}],
    "giftCouponTiers": [
      {"threshold": "number", "couponName": "string", "amount": "number", 
       "useThreshold": "number", "validDays": "number", "quantity": "number"}
    ],
    "bundleId": "string",
    "bundlePrice": "number",
    "discountPerUnit": "number",
    "volumeTiers": [{"minVolume": "number", "discountPerUnit": "number"}],
    "maxDiscount": "number"
  }
}
```

### 5.5 库存预警接口

| HTTP方法 | 路径 | 请求体 | 响应体 | 说明 |
|----------|------|--------|--------|------|
| GET | /api/inventory/alerts | - | InventoryAlertListResponse | 查询库存预警列表 |
| POST | /api/inventory/alerts/{alertId}/handle | AlertHandleRequest | InventoryAlertResponse | 处理预警 |
| POST | /api/inventory/replenishment/export | ReplenishmentExportRequest | - | 导出补货清单 |

---

## 6. 开发优先级与排期

### 6.1 功能开发优先级

| 优先级 | 功能 | 依赖 | 预计工时 |
|--------|------|------|---------|
| P0 | 会员识别与等级查询 | member表、member_level表 | 8h |
| P0 | 赠券发放（满500送券） | coupon_template、coupon_instance表 | 16h |
| P0 | 券核销 | coupon_instance表、结算接口 | 12h |
| P0 | 满减功能 | AMOUNT_OFF规则引擎扩展 | 12h |
| P0 | 油品升数优惠 | FUEL_VOLUME_DISCOUNT规则引擎扩展 | 10h |
| P0 | 满额赠商品（满555送烟） | GIFT_ITEM + minCartAmount | 10h |
| P1 | 会员专属价计算 | member_level.discount_rate | 6h |
| P1 | 智能结算页面 | 后端结算API | 24h |
| P1 | 促销活动管理页面 | 后端促销规则API | 16h |
| P1 | 会员管理页面 | 后端会员API | 12h |
| P2 | 报表中心 | transaction表 | 20h |
| P2 | 组合包库存计算 | bundle表 | 8h |

### 6.2 数据库迁移顺序

| 顺序 | 迁移文件 | 内容 |
|------|---------|------|
| 1 | V23__member_tables.sql | 创建member、member_level表 |
| 2 | V24__coupon_tables.sql | 创建coupon_template、coupon_instance表 |
| 3 | V25__product_group_tables.sql | 创建product_group、product_group_item表 |
| 4 | V26__bundle_tables.sql | 创建bundle、bundle_item表 |
| 5 | V27__transaction_tables.sql | 创建transaction、transaction_item表 |
| 6 | V28__inventory_alert_table.sql | 创建inventory_alert表 |
| 7 | V29__demo_member_data.sql | 插入会员等级和示例数据 |
| 8 | V30__demo_coupon_data.sql | 插入券模板和示例数据 |

### 6.3 前端页面开发顺序

| 顺序 | 页面 | 依赖 | 说明 |
|------|------|------|------|
| 1 | 智能结算页 | /api/checkout/calculate | 核心收银功能 |
| 2 | 促销活动管理页 | /api/promotion-rules | 管理促销规则 |
| 3 | 会员管理页 | /api/members | 管理会员信息 |
| 4 | 券管理页 | /api/coupon-templates | 管理券模板和发放 |
| 5 | 库存预警页 | /api/inventory/alerts | 查看预警和补货 |
| 6 | 报表中心页 | /api/checkout/records | 统计分析 |

---

## 附录：功能实现状态追踪

### A.1 促销规则引擎状态

| 规则类型 | 条件匹配 | 优惠计算 | 互斥处理 | 会员关联 | 状态 |
|---------|---------|---------|---------|---------|------|
| FIXED_PRICE | ✅ | ✅ | ✅ | ✅ | 已完成 |
| PERCENTAGE_DISCOUNT | ✅ | ✅ | ✅ | ✅ | 已完成 |
| AMOUNT_OFF | ✅ | ✅ | ✅ | ✅ | 已完成 |
| EXCHANGE_PURCHASE | ✅ | ✅ | ✅ | ✅ | 已完成 |
| GIFT_ITEM | ✅ | ✅ | ✅ | ❌ | 待完善 |
| **GIFT_COUPON** | ✅ | ✅ | ❌ | ❌ | **待开发** |
| **COUPON_REDEEM** | ✅ | ✅ | ❌ | ❌ | **待开发** |
| **FUEL_VOLUME_DISCOUNT** | ✅ | ✅ | ✅ | ❌ | **待完善** |

### A.2 前端页面状态

| 页面 | 路由 | 开发状态 | 优先级 |
|------|------|---------|--------|
| 智能结算页 | /settlement | ❌ | P0 |
| 促销活动管理页 | /promotions | ⚠️ 部分 | P0 |
| 会员管理页 | /members | ❌ | P1 |
| 券管理页 | /coupons | ❌ | P0 |
| 库存预警页 | /inventory/alerts | ❌ | P0 |
| 报表中心页 | /reports | ❌ | P2 |

### A.3 API接口状态

| 接口 | 路径 | 开发状态 | 优先级 |
|------|------|---------|--------|
| 会员识别 | POST /api/members/identify | ❌ | P0 |
| 会员券查询 | GET /api/members/{code}/coupons | ❌ | P0 |
| 券发放 | POST /api/coupons/issue | ❌ | P0 |
| 券核销 | POST /api/coupons/redeem | ❌ | P0 |
| 结算计算 | POST /api/checkout/calculate | ⚠️ 部分 | P0 |
| 结算确认 | POST /api/checkout/confirm | ⚠️ 部分 | P0 |
| 库存预警 | GET /api/inventory/alerts | ❌ | P0 |
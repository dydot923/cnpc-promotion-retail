# 促销功能分析与规则引擎设计

## 文档说明

本文档基于 `data/活动看板.xlsx` 中的真实业务数据，全面梳理加油站促销活动的业务规则、数据结构、叠加逻辑和会员关联，为规则引擎开发提供完整的业务分析基础。

---

## 一、数据源分析

### 1.1 数据文件总览

| 文件 | 规模 | 用途 |
|------|------|------|
| `价格.xlsx` | 12756条商品价格记录 | 商品编码、名称、条码、执行价 |
| `库存.xlsx` | 454条本站库存记录 | 商品编码、名称、条码、库存数量 |
| `活动看板.xlsx` | 9个工作表 | 促销规则、会员权益、换购规则等 |

### 1.2 活动看板工作表结构

| 工作表名称 | 行数 | 核心字段 | 促销类型 |
|-----------|------|---------|---------|
| 一体化营销活动看板 | ~39行 | 活动渠道、活动类型、优惠内容、备注 | 总览、赠券、折扣 |
| 会员权益包 | 164行 | 权益包类型、权益定价、券明细、数量 | 会员权益、赠券 |
| 加油换购（统建） | 22行 | 品类、促销政策、促销数量、促销价、毛利率 | 加油换购、组合包 |
| 非非促销（统建） | 43行 | 类型、重点品类、营销事件、活动内容 | 全场折扣、满减、买赠 |
| LNG+CNG | 45行 | 类型、商品编码、售价、对应数量 | LNG/CNG权益包 |
| 参考1-非非促销（个性化促销） | 514行 | 商品编码、促销价、毛利率 | 单品促销、固定价 |
| 参考2-9.9元商品专区 | 197行 | 商品品类、促销金额、毛利率 | 固定价促销 |
| 参考3-会员生日&省区特色专用券范围 | 179行 | 品类、商品编码、可核销券种 | 会员券适用范围 |
| 参考4-一卡通销售站点明细 | 302行 | 分公司、站点名称、经纬度 | 站点范围 |

---

## 二、促销活动类型梳理

### 2.1 促销类型分类体系

基于活动看板数据，梳理出以下促销类型：

| 大类 | 促销类型 | 对应工作表 | 说明 |
|------|---------|-----------|------|
| **油品促销** | 油品立减 | 非非促销（统建） | 根据油品类型和升数立减 |
| | 油品消费赠券 | 一体化营销活动看板 | 加油满额送券 |
| **非油促销** | 固定价促销 | 参考2-9.9元商品专区、参考1 | 特定商品固定价格 |
| | 全场折扣 | 非非促销（统建） | 指定日期全场折扣，排除特定品类 |
| | 满减 | 非非促销（统建） | 购物满X元减Y元 |
| | 买赠 | 非非促销（统建） | 买N件赠M件或赠特定商品 |
| | 整件优惠 | 非非促销（统建） | 整箱购买优惠 |
| | 惊爆价 | 非非促销（统建） | 限时超低价 |
| **加油换购** | 组合包换购 | 加油换购（统建） | 加油满额+加价换购组合包 |
| | 单品换购 | 加油换购（统建） | 加油满额+加价换购单品 |
| **会员促销** | 会员生日券 | 参考3 | 会员生日专享券 |
| | 会员权益包 | 会员权益包 | 购买权益包获得券和服务 |
| | 会员专享价 | 参考1、参考2 | 会员专属促销价格 |
| | 省区特色券 | 参考3 | 特定省份会员专享券 |
| **其他促销** | 节日活动 | 非非促销（统建） | 特定节日促销 |
| | 积分促销 | 会员权益包 | 积分兑换、积分加倍 |
| | LNG/CNG专享 | LNG+CNG | 特定燃料类型专享优惠 |

### 2.2 各类促销规则详解

#### 2.2.1 油品立减

**业务规则**：
- 根据油品类型（汽油、柴油、CN98等）区分
- 根据加油升数或金额触发不同立减金额
- 例如："每月8、18、28日 CN98满1升立减0.8元/升"

**数据结构**：
```json
{
  "fuelTypes": ["CN98"],
  "discountPerUnit": 0.8,
  "daysOfMonth": [8, 18, 28],
  "minFuelVolume": 1.0
}
```

#### 2.2.2 油品消费赠券

**业务规则**：
- 加油满一定金额赠送电子券
- 不同金额档位赠送不同面额和数量的券
- 例如："逢7气惠满500送LNG券，满1000送便利店券"

**数据结构**：
```json
{
  "fuelTypes": ["LNG"],
  "giftCouponTiers": [
    {"threshold": 500, "couponName": "LNG券", "amount": 30, "quantity": 1},
    {"threshold": 1000, "couponName": "便利店券", "amount": 50, "quantity": 2}
  ],
  "daysOfMonth": [7, 17, 27]
}
```

#### 2.2.3 固定价促销（9.9元专区）

**业务规则**：
- 指定商品以固定价格销售
- 常见于9.9元、19.9元等专区
- 需要校验毛利率

**数据结构**：
```json
{
  "productCodes": ["70227684", "70390760"],
  "fixedPrice": 9.9,
  "category": "零食",
  "grossMarginRate": 0.15
}
```

#### 2.2.4 全场折扣

**业务规则**：
- 指定日期全场商品打折
- 排除香烟、化肥等品类
- 根据站点类型区分（加油站、加气站）

**数据结构**：
```json
{
  "discountRate": 0.9,
  "daysOfMonth": [9, 19, 29],
  "stationTypes": ["gas_station"],
  "excludedCategories": ["香烟", "化肥"]
}
```

#### 2.2.5 满减

**业务规则**：
- 购物车满一定金额减固定金额
- 支持跨商品累计
- 例如："满555元送两条烟"

**数据结构**：
```json
{
  "minCartAmount": 555,
  "benefitType": "gift_item",
  "giftItemOptions": [
    [{"itemCode": "70030041", "itemName": "黄山香烟", "quantity": 2}]
  ]
}
```

#### 2.2.6 买赠

**业务规则**：
- 购买一定数量商品赠送其他商品
- 支持多买多赠
- 例如："买2赠1"、"买一赠一"

**数据结构**：
```json
{
  "productCodes": ["70390760"],
  "buyQuantity": 2,
  "giftItemCode": "70390760",
  "giftItemName": "猫耳酥",
  "giftQuantity": 1
}
```

#### 2.2.7 加油换购

**业务规则**：
- 加油满指定金额后可加价换购商品
- 区分油品类型和金额门槛
- 支持组合包和单品换购
- 包含主油承担、非油承担、毛利率计算

**数据结构**：
```json
{
  "fuelTypes": ["gasoline", "diesel"],
  "minFuelAmount": 200,
  "exchangePrice": 25,
  "bundleItems": [
    {"itemCode": "70227684", "itemName": "真龙香烟", "quantity": 1},
    {"itemCode": "70390760", "itemName": "猫耳酥", "quantity": 2}
  ],
  "mainFuel承担": 10,
  "nonFuel承担": 15,
  "grossMarginRate": 0.2
}
```

#### 2.2.8 会员权益包

**业务规则**：
- 会员购买权益包获得多种券和服务
- 权益包类型：十全十美权益包、LNG/CNG权益包等
- 包含多张不同面额的券

**数据结构**：
```json
{
  "packageType": "十全十美权益包",
  "price": 99,
  "coupons": [
    {"couponName": "便利店满减券", "amount": 20, "useThreshold": 100, "validDays": 30},
    {"couponName": "洗车券", "amount": 0, "useThreshold": 0, "validDays": 90}
  ],
  "benefits": ["免费洗车x2", "积分加倍x3"]
}
```

---

## 三、促销叠加与互斥规则

### 3.1 叠加规则

| 促销类型 | 可叠加对象 | 说明 |
|---------|-----------|------|
| 油品立减 | 油品消费赠券 | 立减后仍可获得赠券 |
| 固定价促销 | 会员专享价 | 取最优价，不可叠加 |
| 全场折扣 | 会员权益包 | 折扣后仍可使用权益包内券 |
| 满减 | 赠券 | 满减后可使用赠券 |
| 加油换购 | 油品赠券 | 换购同时可获得油品赠券 |
| 买赠 | 积分加倍 | 买赠商品仍可获得加倍积分 |

### 3.2 互斥规则

| 互斥组 | 包含促销类型 | 说明 |
|--------|------------|------|
| 直接降价组 | 固定价、百分比折扣、金额立减 | 同一商品只能选择一种直接降价 |
| 换购组 | 组合包换购、单品换购 | 同一订单只能选择一种换购方式 |
| 赠券组 | 油品赠券、满赠券、会员券 | 同一类型券不可重复获得 |
| 会员价组 | 会员专享价、普通会员价 | 取最优价 |

### 3.3 优先级排序规则

```text
1. 用户已选择的促销（手动选择优先）
2. 强制规则（如会员生日券必须使用）
3. 到手价最低的方案
4. 优惠金额最大的方案
5. 赠券价值最高的方案
6. 毛利率合规的方案（低于底线的方案禁用）
7. 库存充足的方案（低库存方案降级）
8. 活动优先级高的方案
```

---

## 四、会员体系与促销关联

### 4.1 会员等级体系

| 等级 | 名称 | 权益 |
|------|------|------|
| 普通会员 | 基础会员 | 基础会员价、积分累计 |
| 银卡会员 | 银卡 | 95折会员价、积分1.5倍、生日券 |
| 金卡会员 | 金卡 | 9折会员价、积分2倍、生日券+节日券 |
| 铂金会员 | 铂金 | 85折会员价、积分3倍、专属权益包 |

### 4.2 会员与促销的关联逻辑

#### 4.2.1 会员身份识别
- 扫码识别会员（会员卡或手机号）
- 查询会员等级和可用券
- 自动匹配会员专享促销

#### 4.2.2 会员价与促销价的关系
- 默认取最优价格
- 会员价与促销价互斥（取低价）
- 可配置为叠加模式

#### 4.2.3 会员券核销流程
1. 识别会员身份
2. 查询可用券列表
3. 判断券适用范围和满额门槛
4. 选择使用的券
5. 计算优惠后金额
6. 更新券状态

#### 4.2.4 会员积分规则
- 消费金额 × 积分倍率 = 获得积分
- 不同会员等级倍率不同
- 促销期间可配置额外倍率

---

## 五、数据导入与清洗

### 5.1 导入流程

```text
选择文件 → 解析工作表 → 数据清洗 → 校验 → 预览 → 导入确认 → 生成规则
```

### 5.2 数据清洗规则

#### 5.2.1 合并单元格处理
- 向下填充空值
- 按业务块继承值
- 记录继承来源供人工确认

#### 5.2.2 数据标准化
| 字段类型 | 标准化规则 |
|---------|-----------|
| 商品编码 | 转为字符串，去除".0"后缀 |
| 金额 | 转为BigDecimal，去除逗号和百分号 |
| 日期 | 统一转为LocalDate |
| 油品类型 | 映射为枚举值 |
| 站点类型 | 映射为枚举值 |

#### 5.2.3 规则解析

**自然语言规则 → 结构化规则**：
- "每月7/17/27日" → `daysOfMonth: [7, 17, 27]`
- "满500送30元券" → `minCartAmount: 500, giftCouponAmount: 30`
- "9折" → `discountRate: 0.9`
- "汽油满200元换购" → `fuelTypes: ["gasoline"], minFuelAmount: 200`

### 5.3 校验规则

| 校验项 | 规则 | 处理方式 |
|--------|------|---------|
| 商品编码 | 必须在价格表中存在 | 错误，标记缺失 |
| 条码 | 唯一性校验 | 警告，标记重复 |
| 金额 | 必须大于0 | 错误，标记无效 |
| 日期范围 | 开始日期 ≤ 结束日期 | 错误，标记无效 |
| 毛利率 | 必须 ≥ 最低毛利率 | 警告，标记需审批 |
| 库存 | 促销商品需有库存 | 警告，标记低库存 |

---

## 六、数据库表结构优化

### 6.1 现有表结构问题

| 问题 | 说明 | 影响 |
|------|------|------|
| 缺少会员表 | 无会员等级、积分等信息 | 无法实现会员专属促销 |
| 缺少券表 | 无券模板、券实例管理 | 无法实现赠券和核销 |
| 缺少商品组表 | 无法管理"红牛（2款通用）"等商品组 | 组合促销无法实现 |
| 缺少组合包表 | 无法管理行车包、水饮包等 | 换购组合包无法实现 |
| 缺少交易表 | 无交易记录和结算明细 | 无法追踪促销执行 |
| 库存表缺少阈值 | 无安全库存阈值配置 | 无法实现库存预警 |

### 6.2 新增表设计

#### 6.2.1 会员表（member）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| member_code | varchar(64) | 会员编号，唯一 |
| member_name | varchar(128) | 会员姓名 |
| phone | varchar(32) | 手机号，唯一 |
| level | varchar(32) | 会员等级 |
| total_points | bigint | 累计积分 |
| available_points | bigint | 可用积分 |
| birthday | date | 生日 |
| province | varchar(64) | 所在省份 |
| status | varchar(32) | 状态 |
| created_at | timestamptz | 创建时间 |
| updated_at | timestamptz | 更新时间 |

#### 6.2.2 会员等级表（member_level）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| level_code | varchar(32) | 等级编码 |
| level_name | varchar(128) | 等级名称 |
| discount_rate | numeric(5,4) | 会员折扣率 |
| points_multiplier | int | 积分倍率 |
| min_consumption | numeric(18,2) | 升级最低消费 |
| benefits | jsonb | 权益列表 |
| priority | int | 优先级 |

#### 6.2.3 券模板表（coupon_template）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| template_code | varchar(64) | 模板编号，唯一 |
| coupon_name | varchar(256) | 券名称 |
| coupon_type | varchar(32) | 券类型（满减券、折扣券、礼品券） |
| amount | numeric(18,2) | 券面额 |
| use_threshold | numeric(18,2) | 使用门槛 |
| applicable_categories | jsonb | 适用品类 |
| excluded_categories | jsonb | 排除品类 |
| applicable_product_codes | jsonb | 适用商品编码 |
| valid_days | int | 有效期天数 |
| max_use_count | int | 最大使用次数 |
| is_member_only | boolean | 是否仅限会员 |
| member_levels | jsonb | 适用会员等级 |
| status | varchar(32) | 状态 |
| created_at | timestamptz | 创建时间 |

#### 6.2.4 券实例表（coupon_instance）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| coupon_code | varchar(64) | 券码，唯一 |
| template_id | bigint | 关联券模板 |
| member_id | bigint | 关联会员 |
| amount | numeric(18,2) | 面额 |
| use_threshold | numeric(18,2) | 使用门槛 |
| status | varchar(32) | 状态（未使用、已使用、已过期） |
| valid_until | timestamptz | 有效期截止 |
| used_at | timestamptz | 使用时间 |
| used_order_id | varchar(128) | 使用的订单号 |
| created_at | timestamptz | 创建时间 |

#### 6.2.5 商品组表（product_group）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| group_code | varchar(64) | 组编号，唯一 |
| group_name | varchar(256) | 组名称 |
| group_type | varchar(32) | 组类型（通用组、换购组、赠品类） |
| description | text | 描述 |
| created_at | timestamptz | 创建时间 |

#### 6.2.6 商品组明细表（product_group_item）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| group_id | bigint | 关联商品组 |
| product_code | varchar(64) | 商品编码 |
| quantity | int | 所需数量 |
| is_primary | boolean | 是否主商品 |

#### 6.2.7 组合包表（bundle）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| bundle_code | varchar(64) | 包编号，唯一 |
| bundle_name | varchar(256) | 包名称 |
| bundle_price | numeric(18,2) | 组合价 |
| description | text | 描述 |
| created_at | timestamptz | 创建时间 |

#### 6.2.8 组合包明细表（bundle_item）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| bundle_id | bigint | 关联组合包 |
| product_code | varchar(64) | 商品编码 |
| quantity | int | 数量 |

#### 6.2.9 交易记录表（transaction）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| txn_no | varchar(64) | 交易编号，唯一 |
| total_amount | numeric(18,2) | 商品合计金额 |
| discount_amount | numeric(18,2) | 优惠总额 |
| payable_amount | numeric(18,2) | 应付金额 |
| payment_method | varchar(32) | 支付方式 |
| operator_id | varchar(128) | 操作员 |
| member_id | bigint | 会员ID |
| is_member | boolean | 是否会员交易 |
| station_code | varchar(64) | 站点编码 |
| status | varchar(32) | 状态 |
| created_at | timestamptz | 交易时间 |

#### 6.2.10 交易明细表（transaction_item）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| transaction_id | bigint | 关联交易 |
| product_code | varchar(64) | 商品编码 |
| product_name | varchar(512) | 商品名称（冗余） |
| barcode | varchar(128) | 条码 |
| unit_price | numeric(18,2) | 原价 |
| actual_price | numeric(18,2) | 实际单价 |
| quantity | int | 数量 |
| subtotal | numeric(18,2) | 小计 |
| applied_promo_id | varchar(64) | 应用的促销规则ID |
| applied_coupon_code | varchar(64) | 使用的券码 |

#### 6.2.11 库存预警表（inventory_alert）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigserial | 主键 |
| product_code | varchar(64) | 商品编码 |
| activity_code | varchar(64) | 关联活动 |
| current_stock | numeric(18,3) | 当前库存 |
| threshold | numeric(18,3) | 安全阈值 |
| alert_level | varchar(32) | 预警级别 |
| suggested_replenish_qty | numeric(18,3) | 建议补货量 |
| is_promo_related | boolean | 是否促销关联 |
| status | varchar(32) | 状态 |
| handled_at | timestamptz | 处理时间 |
| created_at | timestamptz | 创建时间 |

### 6.3 表关系图

```text
促销活动 (promotion_activity)
    │
    ├── 促销规则 (promotion_rule)
    │       ├── 促销规则草稿 (promotion_rule_draft)
    │       └── 促销规则版本 (promotion_rule_version)
    │
    ├── 促销商品范围 (promotion_scope) ──→ 商品 (product)
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

## 七、促销规则引擎核心设计

### 7.1 规则模型

```text
规则 = 触发条件 + 适用范围 + 优惠动作 + 限制条件 + 优先级 + 互斥组
```

#### 7.1.1 触发条件（PromotionCondition）

| 条件类型 | 字段 | 说明 |
|---------|------|------|
| 时间条件 | daysOfMonth | 指定日期触发（如每月9日） |
| | dateCondition | 日期范围 |
| | timeRangeCondition | 时间范围 |
| 油品条件 | fuelTypes | 油品类型 |
| | minFuelAmount | 油品金额门槛 |
| | minFuelVolume | 油品升数门槛 |
| 商品条件 | productCodes | 指定商品编码 |
| | includedCategories | 包含品类 |
| | excludedCategories | 排除品类 |
| | minProductQuantity | 最小购买数量 |
| 购物车条件 | minCartAmount | 购物车金额门槛 |
| 会员条件 | memberRequired | 是否需要会员 |
| | memberLevels | 会员等级 |
| | birthdayMonthRequired | 是否生日月 |
| 站点条件 | stationTypes | 站点类型 |
| | stationProvinces | 站点省份 |
| 库存条件 | minInventoryQuantity | 最小库存要求 |

#### 7.1.2 优惠动作（PromotionBenefit）

| 动作类型 | 字段 | 说明 |
|---------|------|------|
| FIXED_PRICE | fixedPrice | 固定价格 |
| PERCENTAGE_DISCOUNT | discountRate | 折扣率 |
| AMOUNT_OFF | amountOff | 金额立减 |
| EXCHANGE_PURCHASE | exchangePrice, exchangeQuantity | 换购价和数量 |
| GIFT_ITEM | giftItemCode, giftItemName, giftItemQuantity | 赠送商品 |
| GIFT_COUPON | giftCouponTiers | 赠送券（多档位） |
| BUNDLE_PRICE | bundleId, bundleItems, bundlePrice | 组合包价格 |
| COUPON_REDEEM | - | 券核销 |
| FUEL_VOLUME_DISCOUNT | discountPerUnit | 油品每升立减 |
| COMPOSITE | compositeComponents | 复合优惠 |

### 7.2 规则计算流程

```text
输入: OrderContext(商品列表, 油品消费, 会员信息, 当前时间, 站点信息)
    │
    ▼
筛选有效规则
    ├── 状态=ACTIVE
    ├── 日期在有效期内
    ├── 站点类型匹配
    ├── 库存充足
    └── 会员条件满足
    │
    ▼
按规则类型分组
    ├── 油品促销组
    ├── 非油促销组
    ├── 换购组
    ├── 会员促销组
    └── 赠券组
    │
    ▼
逐条规则计算优惠
    ├── 计算适用前提条件
    ├── 计算实付价格
    ├── 计算优惠金额
    └── 生成解释文本
    │
    ▼
处理互斥关系
    ├── 同一互斥组仅保留最优方案
    ├── 标记不可用原因
    └── 处理叠加规则
    │
    ▼
排序推荐
    ├── 用户已选择优先
    ├── 到手价最低优先
    ├── 优惠金额最大优先
    └── 赠券价值最高优先
    │
    ▼
输出结果
    ├── 推荐促销方案
    ├── 其他可选方案
    ├── 不可用方案及原因
    └── 原价兜底方案
```

### 7.3 关键算法

#### 7.3.1 促销匹配算法

```text
function matchPromotions(orderContext):
    candidates = []
    for rule in allActiveRules:
        if rule.condition.matches(orderContext):
            benefit = calculateBenefit(rule, orderContext)
            if benefit.isValid():
                candidates.append(Candidate(rule, benefit, explanation))
    return candidates
```

#### 7.3.2 最优方案计算

```text
function findBestCandidate(candidates, orderContext):
    best = null
    for candidate in candidates:
        if candidate is manuallySelected:
            return candidate
        if candidate.price < best.price:
            best = candidate
        elif candidate.price == best.price:
            if candidate.couponValue > best.couponValue:
                best = candidate
    return best
```

---

## 八、API接口设计

### 8.1 促销规则管理接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/promotion-rules | 查询促销规则列表 |
| GET | /api/promotion-rules/{ruleId} | 查询规则详情 |
| POST | /api/promotion-rules | 创建促销规则 |
| PUT | /api/promotion-rules/{ruleId} | 更新促销规则 |
| DELETE | /api/promotion-rules/{ruleId} | 删除促销规则 |
| POST | /api/promotion-rules/{ruleId}/activate | 启用规则 |
| POST | /api/promotion-rules/{ruleId}/deactivate | 停用规则 |

### 8.2 结算计算接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| POST | /api/checkout/calculate | 计算促销方案 |
| POST | /api/checkout/confirm | 确认结算 |
| GET | /api/checkout/records | 查询交易记录 |
| GET | /api/checkout/records/{txnNo} | 查询交易详情 |

### 8.3 会员管理接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/members | 查询会员列表 |
| GET | /api/members/{memberCode} | 查询会员详情 |
| POST | /api/members | 创建会员 |
| PUT | /api/members/{memberCode} | 更新会员信息 |
| POST | /api/members/{memberCode}/points | 积分变动 |
| GET | /api/members/{memberCode}/coupons | 查询会员券 |

### 8.4 券管理接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/coupon-templates | 查询券模板 |
| POST | /api/coupon-templates | 创建券模板 |
| POST | /api/coupons/issue | 发放券 |
| POST | /api/coupons/redeem | 核销券 |
| GET | /api/coupons/{couponCode} | 查询券状态 |

### 8.5 数据导入接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| POST | /api/import/prices | 导入价格表 |
| POST | /api/import/inventory | 导入库存表 |
| POST | /api/import/promotions | 导入促销规则 |
| POST | /api/import/coupons | 导入券数据 |
| GET | /api/import/records | 查询导入记录 |

---

## 九、关键业务场景示例

### 9.1 场景：加油换购

**业务描述**：顾客加油满200元后，可加25元换购行车包

**规则配置**：
```json
{
  "ruleType": "EXCHANGE_PURCHASE",
  "condition": {
    "fuelTypes": ["gasoline", "diesel"],
    "minFuelAmount": 200
  },
  "benefit": {
    "exchangePrice": 25,
    "bundleId": "driving_package",
    "bundleItems": [
      {"itemCode": "70227684", "quantity": 1},
      {"itemCode": "70390760", "quantity": 2}
    ]
  },
  "priority": 100,
  "exclusiveGroup": "exchange_purchase"
}
```

**计算流程**：
1. 顾客加油200元 → 触发换购条件
2. 顾客选择行车包 → 计算换购价25元
3. 检查行车包内商品库存
4. 生成结算方案：油品200元 + 换购25元 = 应付225元
5. 记录交易明细

### 9.2 场景：满减赠券

**业务描述**：购物满500元送30元便利店券

**规则配置**：
```json
{
  "ruleType": "GIFT_COUPON",
  "condition": {
    "minCartAmount": 500,
    "includedCategories": ["便利店"]
  },
  "benefit": {
    "giftCouponTiers": [
      {"threshold": 500, "couponName": "便利店满减券", "amount": 30, "useThreshold": 100, "validDays": 30}
    ]
  },
  "priority": 50,
  "stackable": true
}
```

**计算流程**：
1. 顾客购物车金额达到500元 → 触发赠券条件
2. 系统自动发放30元便利店券到会员账户
3. 券有效期30天，满100元可用
4. 记录赠券事件

### 9.3 场景：会员生日5折

**业务描述**：会员生日当月享受指定商品5折

**规则配置**：
```json
{
  "ruleType": "PERCENTAGE_DISCOUNT",
  "condition": {
    "memberRequired": true,
    "birthdayMonthRequired": true,
    "applicableProductCodes": ["70030041", "70227684"]
  },
  "benefit": {
    "discountRate": 0.5
  },
  "priority": 200,
  "exclusiveGroup": "direct_discount"
}
```

**计算流程**：
1. 识别会员身份和生日
2. 检查当前是否为生日月
3. 匹配适用商品
4. 计算5折后价格
5. 与其他促销比较取最优

---

## 十、总结

### 10.1 促销功能矩阵

| 促销类型 | 规则引擎支持 | 数据导入支持 | 前端展示 | 会员关联 |
|---------|------------|------------|---------|---------|
| 固定价促销 | ✅ | ✅ | ✅ | ✅ |
| 百分比折扣 | ✅ | ✅ | ✅ | ✅ |
| 金额立减 | ✅ | ✅ | ❌ | ✅ |
| 加油换购 | ✅ | ✅ | ❌ | ✅ |
| 买赠 | ✅ | ✅ | ❌ | ✅ |
| 赠券 | ✅ | ✅ | ❌ | ✅ |
| 组合包 | ✅ | ✅ | ❌ | ✅ |
| 券核销 | ✅ | ✅ | ❌ | ✅ |
| 油品立减 | ✅ | ✅ | ❌ | ✅ |
| 会员专享 | ✅ | ✅ | ❌ | ✅ |

### 10.2 待完善功能

1. **前端结算页面**：扫码输入、促销方案选择、购物车管理
2. **赠券展示和发放**：赠券列表、发放记录、券状态管理
3. **会员识别和等级展示**：会员扫码、等级显示、可用权益
4. **组合包库存计算**：组合包可售套数、限制商品提示
5. **规则解释展示**：不可用原因、优惠计算说明
6. **报表中心**：交易统计、促销效果分析、库存周转

### 10.3 数据库优化清单

1. ✅ 添加会员表（member）
2. ✅ 添加会员等级表（member_level）
3. ✅ 添加券模板表（coupon_template）
4. ✅ 添加券实例表（coupon_instance）
5. ✅ 添加商品组表（product_group）
6. ✅ 添加商品组明细表（product_group_item）
7. ✅ 添加组合包表（bundle）
8. ✅ 添加组合包明细表（bundle_item）
9. ✅ 添加交易记录表（transaction）
10. ✅ 添加交易明细表（transaction_item）
11. ✅ 添加库存预警表（inventory_alert）
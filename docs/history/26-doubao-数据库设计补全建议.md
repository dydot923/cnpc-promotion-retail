# 数据库设计补全建议

## 文档说明

本文档基于活动看板促销规则梳理结果，对比现有数据库设计，分析需要补充的表结构、字段和数据配置，确保数据库设计能够完整支持所有促销业务场景。

---

## 一、差距分析总览

### 1.1 促销类型覆盖情况

| 促销类型 | 活动看板 | 现有数据库 | 状态 |
|---------|---------|-----------|------|
| 油品立减 | 逢8-CN98立减 | FUEL_VOLUME_DISCOUNT | ✅ 已支持 |
| 满额赠券 | 逢7气惠、逢10超级十惠 | GIFT_COUPON | ⚠️ 部分支持，缺少档位配置 |
| 固定折扣 | 逢7/9便利店9折 | PERCENTAGE_DISCOUNT | ⚠️ 缺少日期触发和排除品类 |
| 固定价 | 9.9元专区 | FIXED_PRICE | ✅ 已支持 |
| 满减 | 中秋团圆礼 | AMOUNT_OFF | ✅ 已支持 |
| 买赠 | 啤酒赠券、泸州老窖 | GIFT_ITEM | ✅ 已支持 |
| 换购 | 行车包、水饮包、长运包 | EXCHANGE_PURCHASE | ⚠️ 缺少组合包与加油条件关联 |
| 多倍积分 | 逢7/9多倍积分 | - | ❌ 未支持 |
| 会员礼遇 | 新增会员赠券 | - | ❌ 未支持 |

### 1.2 业务规则覆盖情况

| 业务规则 | 活动看板 | 现有数据库 | 状态 |
|---------|---------|-----------|------|
| 日期触发（逢7/8/9/10） | 每月特定日期 | - | ❌ 未支持 |
| 会员等级差异 | 黄金及以上加赠 | member_level | ⚠️ 缺少促销条件配置 |
| 排除品类 | 香烟、化肥不参加 | product.is_cigarette | ⚠️ 缺少促销规则配置 |
| 站点范围限制 | 一卡通销售站点 | - | ❌ 未支持 |
| 券有效期差异化 | 天然气券15天、非油券60天 | coupon_template.valid_days | ✅ 已支持 |
| 赠券档位 | 满500/1000/1500/2000 | condition_json | ⚠️ 缺少结构化配置 |

---

## 二、需要新增的表结构

### 2.1 站点表（station）

**用途**：管理加油站点信息，支持站点范围限制的促销活动

```sql
create table if not exists station (
    id bigserial primary key,
    station_code varchar(64) not null unique,
    station_name varchar(256) not null,
    province varchar(64),
    city varchar(64),
    district varchar(64),
    address varchar(512),
    latitude numeric(12, 8),
    longitude numeric(12, 8),
    station_type varchar(32) not null default 'FUEL',
    status varchar(32) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_station_province on station (province);
create index if not exists idx_station_type on station (station_type);
create index if not exists idx_station_status on station (status);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| station_code | varchar(64) | 站点编码 |
| station_name | varchar(256) | 站点名称 |
| province | varchar(64) | 省份 |
| city | varchar(64) | 城市 |
| district | varchar(64) | 区县 |
| latitude/longitude | numeric(12,8) | 经纬度 |
| station_type | varchar(32) | 站点类型（FUEL/CNG/LNG/ALL） |

### 2.2 促销日期触发配置表（promotion_date_trigger）

**用途**：管理促销活动的日期触发规则（如逢7、逢8等）

```sql
create table if not exists promotion_date_trigger (
    id bigserial primary key,
    rule_id varchar(64) not null references promotion_rule(rule_id),
    trigger_type varchar(32) not null,
    trigger_value jsonb not null,
    start_date date,
    end_date date,
    created_at timestamptz not null default now()
);

create index if not exists idx_promotion_date_trigger_rule on promotion_date_trigger (rule_id);
create index if not exists idx_promotion_date_trigger_type on promotion_date_trigger (trigger_type);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| rule_id | varchar(64) | 关联促销规则 |
| trigger_type | varchar(32) | 触发类型（DAYS_OF_MONTH/WEEKDAYS/DATE_RANGE） |
| trigger_value | jsonb | 触发值（如 [7, 17, 27]） |
| start_date | date | 开始日期 |
| end_date | date | 结束日期 |

**trigger_value示例**：
```json
// 逢7-每月7、17、27日
{"days": [7, 17, 27]}

// 工作日
{"weekdays": [1, 2, 3, 4, 5]}

// 特定日期范围
{"start": "2024-06-12", "end": "2024-08-09"}
```

### 2.3 积分活动配置表（points_activity）

**用途**：管理多倍积分、积分兑换折扣等积分活动

```sql
create table if not exists points_activity (
    id bigserial primary key,
    activity_code varchar(64) not null unique,
    activity_name varchar(256) not null,
    activity_type varchar(32) not null,
    multiplier numeric(5, 2) not null default 1.0,
    discount_rate numeric(5, 4) not null default 1.0,
    condition_json jsonb,
    start_date date not null,
    end_date date not null,
    status varchar(32) not null default 'ACTIVE',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists idx_points_activity_type on points_activity (activity_type);
create index if not exists idx_points_activity_status on points_activity (status);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| activity_type | varchar(32) | 活动类型（MULTIPLIER/EXCHANGE_DISCOUNT） |
| multiplier | numeric(5,2) | 积分倍率（如3.0表示3倍积分） |
| discount_rate | numeric(5,4) | 兑换折扣率（如0.9表示9折兑换） |
| condition_json | jsonb | 触发条件（如油品类型、消费金额等） |

### 2.4 促销排除品类配置表（promotion_excluded_category）

**用途**：管理促销活动的排除品类配置

```sql
create table if not exists promotion_excluded_category (
    id bigserial primary key,
    rule_id varchar(64) not null references promotion_rule(rule_id),
    category_code varchar(64) not null,
    category_name varchar(128),
    exclude_type varchar(32) not null default 'CATEGORY',
    created_at timestamptz not null default now()
);

create index if not exists idx_promotion_excluded_category_rule on promotion_excluded_category (rule_id);
create index if not exists idx_promotion_excluded_category_code on promotion_excluded_category (category_code);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| rule_id | varchar(64) | 关联促销规则 |
| category_code | varchar(64) | 品类编码 |
| exclude_type | varchar(32) | 排除类型（CATEGORY/PRODUCT_TYPE） |

### 2.5 权益包购买记录表（benefit_package_purchase）

**用途**：记录会员购买权益包的历史

```sql
create table if not exists benefit_package_purchase (
    id bigserial primary key,
    purchase_no varchar(64) not null unique,
    member_code varchar(64) not null references member(member_code),
    package_code varchar(64) not null,
    package_name varchar(256),
    package_price numeric(18, 2) not null,
    actual_paid numeric(18, 2) not null,
    purchase_date timestamptz not null default now(),
    status varchar(32) not null default 'COMPLETED',
    created_at timestamptz not null default now()
);

create index if not exists idx_benefit_package_purchase_member on benefit_package_purchase (member_code);
create index if not exists idx_benefit_package_purchase_package on benefit_package_purchase (package_code);
create index if not exists idx_benefit_package_purchase_date on benefit_package_purchase (purchase_date);
```

### 2.6 会员积分变动记录表（member_points_change）

**用途**：记录会员积分的所有变动（增加、减少、过期）

```sql
create table if not exists member_points_change (
    id bigserial primary key,
    member_code varchar(64) not null references member(member_code),
    change_type varchar(32) not null,
    change_amount bigint not null,
    before_points bigint not null,
    after_points bigint not null,
    reason varchar(512),
    related_order_no varchar(128),
    related_activity_code varchar(64),
    created_at timestamptz not null default now()
);

create index if not exists idx_member_points_change_member on member_points_change (member_code);
create index if not exists idx_member_points_change_type on member_points_change (change_type);
create index if not exists idx_member_points_change_created on member_points_change (created_at);
```

| 字段 | 类型 | 说明 |
|------|------|------|
| change_type | varchar(32) | 变动类型（EARN/SPEND/EXPIRE/ADJUST） |
| change_amount | bigint | 变动数量 |
| before_points | bigint | 变动前积分 |
| after_points | bigint | 变动后积分 |
| reason | varchar(512) | 变动原因 |

### 2.7 促销适用站点表（promotion_station_scope）

**用途**：管理促销活动适用的站点范围

```sql
create table if not exists promotion_station_scope (
    id bigserial primary key,
    rule_id varchar(64) not null references promotion_rule(rule_id),
    station_code varchar(64) not null references station(station_code),
    scope_type varchar(32) not null default 'INCLUDED',
    created_at timestamptz not null default now()
);

create index if not exists idx_promotion_station_scope_rule on promotion_station_scope (rule_id);
create index if not exists idx_promotion_station_scope_station on promotion_station_scope (station_code);
```

---

## 三、需要修改的现有表结构

### 3.1 coupon_template 表

**新增字段**：

```sql
alter table coupon_template
    add column if not exists applicable_stations jsonb,
    add column if not exists applicable_fuel_types jsonb,
    add column if not exists min_fuel_volume numeric(18, 3),
    add column if not exists is_sequence_coupon boolean not null default false,
    add column if not exists sequence_order int not null default 0;
```

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| applicable_stations | jsonb | 适用站点列表 |
| applicable_fuel_types | jsonb | 适用油品类型（汽油/柴油/LNG/CNG） |
| min_fuel_volume | numeric(18,3) | 最小加油量（升） |
| is_sequence_coupon | boolean | 是否序列券 |
| sequence_order | int | 序列顺序 |

### 3.2 promotion_rule 表

**新增字段**：

```sql
alter table promotion_rule
    add column if not exists applicable_member_levels jsonb,
    add column if not exists applicable_fuel_types jsonb,
    add column if not exists min_fuel_amount numeric(18, 2),
    add column if not exists min_fuel_volume numeric(18, 3);
```

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| applicable_member_levels | jsonb | 适用会员等级列表 |
| applicable_fuel_types | jsonb | 适用油品类型 |
| min_fuel_amount | numeric(18,2) | 最小加油金额 |
| min_fuel_volume | numeric(18,3) | 最小加油量 |

### 3.3 member 表

**新增字段**：

```sql
alter table member
    add column if not exists total_consumption numeric(18, 2) not null default 0,
    add column if not exists last_purchase_date date,
    add column if not exists preferred_station_code varchar(64);
```

| 新增字段 | 类型 | 说明 |
|---------|------|------|
| total_consumption | numeric(18,2) | 累计消费金额（用于会员升级） |
| last_purchase_date | date | 最后消费日期 |
| preferred_station_code | varchar(64) | 常用站点编码 |

---

## 四、需要补充的数据配置

### 4.1 会员等级升级规则

```sql
insert into member_level (level_code, level_name, discount_rate, points_multiplier, min_consumption, benefits, priority) values
    ('normal', '普通会员', 1.0000, 1.0000, 0.00, '["基础会员价"]'::jsonb, 1),
    ('silver', '银卡会员', 0.9500, 1.5000, 1000.00, '["生日券", "专属活动"]'::jsonb, 2),
    ('gold', '金卡会员', 0.9000, 2.0000, 5000.00, '["生日券", "节日券", "专属客服"]'::jsonb, 3),
    ('platinum', '铂金会员', 0.8500, 3.0000, 20000.00, '["专属权益包", "优先服务"]'::jsonb, 4)
on conflict (level_code) do update set
    level_name = excluded.level_name,
    discount_rate = excluded.discount_rate,
    points_multiplier = excluded.points_multiplier,
    min_consumption = excluded.min_consumption,
    benefits = excluded.benefits,
    priority = excluded.priority;
```

### 4.2 排除品类基础数据

```sql
insert into promotion_excluded_category (rule_id, category_code, category_name, exclude_type) values
    ('ALL', 'CIGARETTE', '香烟', 'PRODUCT_TYPE'),
    ('ALL', 'FERTILIZER', '化肥', 'PRODUCT_TYPE');
```

### 4.3 日期触发规则示例

```sql
-- 逢7气惠 - 每月7、17、27日
insert into promotion_date_trigger (rule_id, trigger_type, trigger_value) values
    ('rule-gas-discount', 'DAYS_OF_MONTH', '{"days": [7, 17, 27]}'::jsonb);

-- 逢8-CN98立减 - 每月8、18、28日
insert into promotion_date_trigger (rule_id, trigger_type, trigger_value) values
    ('rule-cn98-discount', 'DAYS_OF_MONTH', '{"days": [8, 18, 28]}'::jsonb);

-- 逢9便利店促销 - 每月9、19、29日
insert into promotion_date_trigger (rule_id, trigger_type, trigger_value) values
    ('rule-store-discount', 'DAYS_OF_MONTH', '{"days": [9, 19, 29]}'::jsonb);

-- 逢10超级十惠 - 每月10、20、30日
insert into promotion_date_trigger (rule_id, trigger_type, trigger_value) values
    ('rule-super-discount', 'DAYS_OF_MONTH', '{"days": [10, 20, 30]}'::jsonb);

-- 世界杯活动 - 特定日期范围
insert into promotion_date_trigger (rule_id, trigger_type, trigger_value, start_date, end_date) values
    ('rule-world-cup', 'DATE_RANGE', '{}'::jsonb, '2024-06-12', '2024-08-09');
```

### 4.4 积分活动配置示例

```sql
-- 逢7气惠多倍积分 - CNG/LNG消费3倍积分
insert into points_activity (activity_code, activity_name, activity_type, multiplier, condition_json, start_date, end_date) values
    ('points-gas-triple', '逢7气惠多倍积分', 'MULTIPLIER', 3.0, '{"fuelTypes": ["CNG", "LNG"]}'::jsonb, '2024-01-01', '2024-12-31');

-- 逢9全品惠多倍积分 - 便利店消费3倍积分
insert into points_activity (activity_code, activity_name, activity_type, multiplier, condition_json, start_date, end_date) values
    ('points-store-triple', '逢9便利店多倍积分', 'MULTIPLIER', 3.0, '{"category": "STORE"}'::jsonb, '2024-01-01', '2024-12-31');

-- 积分兑换9折优惠
insert into points_activity (activity_code, activity_name, activity_type, discount_rate, condition_json, start_date, end_date) values
    ('points-exchange-discount', '积分兑换9折', 'EXCHANGE_DISCOUNT', 0.9, '{}'::jsonb, '2024-01-01', '2024-12-31');
```

---

## 五、需要新增的API接口

### 5.1 站点管理接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/stations | 查询站点列表 |
| GET | /api/stations/{stationCode} | 查询站点详情 |
| POST | /api/stations | 创建站点 |
| PUT | /api/stations/{stationCode} | 更新站点 |
| DELETE | /api/stations/{stationCode} | 删除站点 |

### 5.2 积分活动接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/points/activities | 查询积分活动列表 |
| GET | /api/points/activities/{activityCode} | 查询积分活动详情 |
| POST | /api/points/activities | 创建积分活动 |
| PUT | /api/points/activities/{activityCode} | 更新积分活动 |
| DELETE | /api/points/activities/{activityCode} | 删除积分活动 |

### 5.3 积分变动记录接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/members/{memberCode}/points/history | 查询会员积分变动历史 |

### 5.4 权益包购买接口

| HTTP方法 | 路径 | 说明 |
|----------|------|------|
| GET | /api/benefit-packages | 查询权益包列表 |
| POST | /api/benefit-packages/purchase | 购买权益包 |
| GET | /api/members/{memberCode}/benefit-packages | 查询会员已购权益包 |

---

## 六、迁移脚本规划

### 6.1 迁移脚本顺序

| 序号 | 迁移文件 | 内容 |
|------|---------|------|
| V26 | V26__station_table.sql | 创建站点表 |
| V27 | V27__promotion_date_trigger.sql | 创建日期触发配置表 |
| V28 | V28__points_activity_tables.sql | 创建积分活动表和积分变动记录表 |
| V29 | V29__promotion_exclusion_tables.sql | 创建促销排除品类表和站点范围表 |
| V30 | V30__benefit_package_purchase.sql | 创建权益包购买记录表 |
| V31 | V31__coupon_template_enhancement.sql | 增强券模板表 |
| V32 | V32__promotion_rule_enhancement.sql | 增强促销规则表 |
| V33 | V33__member_enhancement.sql | 增强会员表 |
| V34 | V34__initial_data_seed.sql | 初始化基础数据 |

### 6.2 关键数据导入

| 数据来源 | 目标表 | 说明 |
|---------|--------|------|
| 活动看板-参考4 | station | 302个站点信息 |
| 活动看板-会员权益包 | benefit_package_purchase | 权益包购买记录（演示数据） |
| 活动看板-一体化营销活动 | promotion_date_trigger | 日期触发规则 |
| 活动看板-非非促销 | points_activity | 多倍积分活动配置 |

---

## 七、总结

### 7.1 新增表清单（7张）

| 表名 | 用途 | 优先级 |
|------|------|--------|
| station | 站点管理 | P0 |
| promotion_date_trigger | 日期触发配置 | P0 |
| points_activity | 积分活动配置 | P1 |
| member_points_change | 积分变动记录 | P1 |
| promotion_excluded_category | 排除品类配置 | P1 |
| promotion_station_scope | 站点范围配置 | P2 |
| benefit_package_purchase | 权益包购买记录 | P2 |

### 7.2 修改表清单（3张）

| 表名 | 新增字段 | 优先级 |
|------|---------|--------|
| coupon_template | applicable_stations, applicable_fuel_types, min_fuel_volume, is_sequence_coupon, sequence_order | P0 |
| promotion_rule | applicable_member_levels, applicable_fuel_types, min_fuel_amount, min_fuel_volume | P0 |
| member | total_consumption, last_purchase_date, preferred_station_code | P1 |

### 7.3 解决的业务问题

| 业务问题 | 解决方式 |
|---------|---------|
| 满500送券 | GIFT_COUPON规则 + 档位配置 + 日期触发 |
| 不同会员优惠不同 | promotion_rule.applicable_member_levels |
| 买酒满555送烟 | GIFT_ITEM规则 + minCartAmount条件 |
| 满减 | AMOUNT_OFF规则 |
| 9.9元商品 | FIXED_PRICE规则 |
| 加油不同升数优惠 | FUEL_VOLUME_DISCOUNT规则 + volumeTiers |
| 多倍积分 | points_activity表 + MULTIPLIER类型 |
| 积分兑换折扣 | points_activity表 + EXCHANGE_DISCOUNT类型 |
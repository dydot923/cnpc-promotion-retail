# 活动看板落地下一步开发计划

## 文档说明

基于 `docs/25-doubao-活动看板促销规则梳理.md`、`docs/26-doubao-数据库设计补全建议.md`、`data/活动看板.xlsx` 和交接文档，制定下一步开发计划。

---

## 一、当前已完成状态

### 1.1 数据库迁移

| 迁移文件 | 内容 | 状态 |
|---------|------|------|
| V1-V25 | 核心表结构（会员、券、交易、库存等） | ✅ 已完成 |
| V26 | 活动看板基础补全（站点、日期触发、积分活动、权益包） | ✅ 已完成 |

### 1.2 V26新增表

| 表名 | 用途 | 数据量 |
|------|------|--------|
| station | 站点信息 | 297个站点 |
| promotion_date_trigger | 日期触发配置 | 6条记录 |
| points_activity | 积分活动配置 | 2条（逢7/9多倍积分） |
| member_points_change | 积分变动记录 | 0条 |
| promotion_excluded_category | 排除品类 | 0条 |
| promotion_station_scope | 站点范围 | 0条 |
| benefit_package | 权益包 | 14个 |
| benefit_package_item | 权益包明细 | 141条 |
| benefit_package_purchase | 权益包购买记录 | 0条 |

### 1.3 已实现功能

| 功能 | 状态 |
|------|------|
| 会员识别与查询 | ✅ |
| 会员积分变动 | ✅ |
| 会员券查询 | ✅ |
| 券核销（条件更新防并发） | ✅ |
| 赠券发放 | ✅ |
| 结算确认事务闭环 | ✅ |
| 交易流水查询 | ✅ |
| 积分活动（多倍积分） | ✅ |

---

## 二、P0任务开发计划

### P0-1：会员权益包购买闭环

#### 2.1.1 目标

实现权益包查询、购买、发放的完整闭环，让V26已导入的14个权益包可以被会员购买和使用。

#### 2.1.2 文件结构

```
backend/src/main/java/com/cnpc/promoretail/promotion/benefitpackage/
├── BenefitPackageController.java             # 新增：REST API控制器
├── BenefitPackageService.java                # 新增：服务接口
├── BenefitPackageServiceImpl.java            # 新增：服务实现
├── BenefitPackageRepository.java             # 新增：仓储接口
├── InMemoryBenefitPackageRepository.java     # 新增：内存仓储实现
├── MybatisBenefitPackageRepository.java      # 新增：MyBatis仓储实现
├── BenefitPackagePurchaseRepository.java     # 新增：购买记录仓储接口
├── InMemoryBenefitPackagePurchaseRepository.java # 新增：内存购买记录仓储
├── MybatisBenefitPackagePurchaseRepository.java  # 新增：MyBatis购买记录仓储
├── model/
│   ├── BenefitPackage.java                   # 新增：权益包模型
│   ├── BenefitPackageItem.java               # 新增：权益包明细模型
│   ├── BenefitPackagePurchase.java           # 新增：购买记录模型
│   ├── BenefitPackageResponse.java           # 新增：查询响应
│   ├── BenefitPackagePurchaseRequest.java    # 新增：购买请求
│   └── BenefitPackagePurchaseResponse.java   # 新增：购买响应
└── persistence/
    ├── BenefitPackageEntity.java             # 新增：数据库实体
    ├── BenefitPackageItemEntity.java         # 新增：数据库实体
    ├── BenefitPackagePurchaseEntity.java     # 新增：数据库实体
    ├── BenefitPackageMapper.java             # 新增：MyBatis Mapper
    ├── BenefitPackageItemMapper.java         # 新增：MyBatis Mapper
    ├── BenefitPackagePurchaseMapper.java     # 新增：MyBatis Mapper
    └── BenefitPackageMapper.xml              # 新增：MyBatis SQL映射
```

#### 2.1.3 API接口

| HTTP方法 | 路径 | 请求体 | 响应体 | 说明 |
|----------|------|--------|--------|------|
| GET | /api/benefit-packages | - | List<BenefitPackageResponse> | 查询权益包列表 |
| GET | /api/benefit-packages/{packageCode} | - | BenefitPackageResponse | 查询权益包详情 |
| POST | /api/benefit-packages/{packageCode}/purchase | BenefitPackagePurchaseRequest | BenefitPackagePurchaseResponse | 购买权益包 |
| GET | /api/members/{memberCode}/benefit-packages | - | List<BenefitPackagePurchaseResponse> | 查询会员已购权益包 |

#### 2.1.4 请求/响应模型

```java
// BenefitPackagePurchaseRequest
public class BenefitPackagePurchaseRequest {
    private String memberCode;
    private String stationCode;
    private BigDecimal paymentAmount;
    private String checkoutTransactionNo;
    private String operatorId;
    private String operatorName;
}

// BenefitPackagePurchaseResponse
public class BenefitPackagePurchaseResponse {
    private String purchaseId;
    private String packageCode;
    private String packageName;
    private BigDecimal salePrice;
    private BigDecimal paymentAmount;
    private String memberCode;
    private String status;
    private List<BenefitPackageItem> entitlementSnapshot;
    private LocalDateTime purchasedAt;
}
```

#### 2.1.5 MyBatis SQL要求

```xml
<!-- BenefitPackageMapper.xml -->
<select id="findAll" resultType="BenefitPackageEntity">
    SELECT * FROM benefit_package WHERE status = 'ACTIVE'
</select>

<select id="findByPackageCode" resultType="BenefitPackageEntity">
    SELECT * FROM benefit_package WHERE package_code = #{packageCode}
</select>

<select id="findItemsByPackageCode" resultType="BenefitPackageItemEntity">
    SELECT * FROM benefit_package_item WHERE package_code = #{packageCode}
</select>
```

#### 2.1.6 验收点

- ✅ 能查询14个权益包
- ✅ 能购买权益包并写入`benefit_package_purchase`
- ✅ 购买快照中包含对应权益包明细
- ✅ 增加service/controller/repository测试

---

### P0-2：站点服务与站点范围接入结算

#### 2.2.1 目标

实现站点查询服务，并在结算时自动根据stationCode补齐站点类型、省份等信息。

#### 2.2.2 文件结构

```
backend/src/main/java/com/cnpc/promoretail/station/
├── StationController.java                    # 新增：REST API控制器
├── StationService.java                       # 新增：服务接口
├── StationServiceImpl.java                   # 新增：服务实现
├── StationRepository.java                    # 新增：仓储接口
├── InMemoryStationRepository.java            # 新增：内存仓储实现
├── MybatisStationRepository.java             # 新增：MyBatis仓储实现
├── model/
│   ├── Station.java                          # 新增：站点模型
│   ├── StationResponse.java                  # 新增：响应DTO
│   └── StationQuery.java                     # 新增：查询参数
└── persistence/
    ├── StationEntity.java                    # 新增：数据库实体
    ├── StationMapper.java                    # 新增：MyBatis Mapper
    └── StationMapper.xml                     # 新增：MyBatis SQL映射
```

#### 2.2.3 API接口

| HTTP方法 | 路径 | 请求参数 | 响应体 | 说明 |
|----------|------|---------|--------|------|
| GET | /api/stations | city, district, stationType, salesScope | List<StationResponse> | 查询站点列表 |
| GET | /api/stations/{stationCode} | - | StationResponse | 查询站点详情 |

#### 2.2.4 结算侧修改

**修改文件**：`CheckoutApplicationService.java`

**修改内容**：在`effectiveOrderContext()`方法中接入站点查询

```java
private OrderContext effectiveOrderContext(CheckoutCalculateRequest request) {
    StationContext stationContext = buildStationContext(request);
    // ... 其他逻辑
}

private StationContext buildStationContext(CheckoutCalculateRequest request) {
    if (request.stationCode() != null && request.stationType() == null) {
        Station station = stationRepository.findByStationCode(request.stationCode())
                .orElseThrow(() -> new StationNotFoundException(request.stationCode()));
        return new StationContext(
                request.stationCode(),
                station.getStationType(),
                station.getProvince(),
                station.getCity()
        );
    }
    return new StationContext(
            request.stationCode(),
            request.stationType(),
            request.stationProvince(),
            request.stationCity()
    );
}
```

#### 2.2.5 验收点

- ✅ 站点列表可查到V26导入的297个站点
- ✅ 收银只传`stationCode`时，规则仍能按站点类型/省份命中
- ✅ 站点查询支持按city、district、stationType筛选

---

### P0-3：逢10超级十惠充值赠券

#### 2.3.1 目标

实现每月10/20/30日充值1000/2000元赠券的完整逻辑，支持普通会员和黄金及以上会员的差异化赠券。

#### 2.3.2 业务规则

**充值1000元**：

| 客户类型 | 赠券内容 |
|---------|---------|
| 普通会员 | 12元汽油券2张（满200可用）+ 12元便利店券3张 + 10元洗车券3张 |
| 黄金及以上 | 普通会员基础上 + 15元汽油券1张（满200可用） |

**充值2000元**：

| 客户类型 | 赠券内容 |
|---------|---------|
| 普通会员 | 12元汽油券5张（满200可用）+ 12元便利店券6张 + 10元洗车券6张 |
| 黄金及以上 | 普通会员基础上 + 15元汽油券2张（满200可用） |

**有效期**：
- 电子券：60天
- 洗车券：30天

#### 2.3.3 实现方案

1. **新增充值场景模型**：在`CheckoutCalculateRequest`中增加`rechargeAmount`字段

2. **新增促销规则**：
   - `abv2-a5-day10-super-1000-normal`：普通会员充值1000赠券
   - `abv2-a5-day10-super-2000-normal`：普通会员充值2000赠券
   - `abv2-a5-day10-super-1000-gold`：黄金及以上会员充值1000赠券
   - `abv2-a5-day10-super-2000-gold`：黄金及以上会员充值2000赠券

3. **规则条件配置**：
   - `daysOfMonth`: [10, 20, 30]
   - `minRechargeAmount`: 1000/2000
   - `memberLevels`: [] / ["gold", "platinum"]

4. **规则动作配置**：
   - `giftCoupons`: 赠券列表（券模板ID、数量）

#### 2.3.4 文件修改

| 文件 | 修改内容 |
|------|---------|
| CheckoutCalculateRequest.java | 新增`rechargeAmount`字段 |
| OrderContext.java | 新增`rechargeAmount`字段 |
| DateConditionMatcher.java | 支持`promotion_date_trigger`表的日期触发 |
| MemberConditionMatcher.java | 支持`promotion_rule.member_levels`字段 |
| GiftCouponBenefitCalculator.java | 支持充值场景的赠券发放 |

#### 2.3.5 验收点

- ✅ 活动日（10/20/30）充值满足门槛可发券
- ✅ 普通会员和黄金及以上会员发券数量不同
- ✅ 非活动日不发券

---

### P0-4：新增会员/潜在会员化自动发券

#### 2.4.1 目标

实现新会员开卡自动赠券和已注册未办卡会员按燃油偏好发券的功能。

#### 2.4.2 业务规则

**新增会员礼遇**：首次开通昆仑e享卡

| 赠券内容 | 数量 | 使用门槛 |
|---------|------|---------|
| 10元汽油券 | 1张 | 满200元可用 |
| 15元高标号汽油券 | 1张 | 满200元可用 |
| 12元便利店商品券 | 1张 | 满50元可用（除香烟） |
| 10元洗车券 | 1张 | 满11元可用 |

**潜在会员化**：已注册未办卡会员

| 客户类型 | 赠券内容 |
|---------|---------|
| 汽油客户 | 10元汽油券1张（满200可用） |
| 柴油客户 | 10元柴油券1张（满200可用） |

#### 2.4.3 实现方案

1. **会员模型扩展**（V26已新增字段）：
   - `e_enjoy_card_no`: 昆仑e享卡号
   - `usual_province`: 常用省份
   - `member_tags`: 会员标签（如"gasoline_customer"、"diesel_customer"）
   - `registered_at`: 注册时间
   - `card_opened_at`: 开卡时间

2. **会员创建/开卡时触发赠券**：

```java
// MemberServiceImpl.java
public MemberResponse createMember(MemberCreateRequest request) {
    Member member = memberRepository.save(new Member(...));
    if (Boolean.TRUE.equals(request.openedCard())) {
        issueNewMemberCoupons(member.getMemberCode());
    }
    return MemberResponse.from(member);
}

private void issueNewMemberCoupons(String memberCode) {
    List<String> couponTemplateIds = Arrays.asList(
            "coupon-template-gas-10",
            "coupon-template-high-octane-15",
            "coupon-template-store-12",
            "coupon-template-wash-10"
    );
    for (String templateId : couponTemplateIds) {
        couponRepository.issueCoupon(templateId, memberCode);
    }
}
```

3. **潜在会员化触发接口**：

```java
// MemberController.java
@PostMapping("/{memberCode}/activation-coupons")
public ApiResponse<Void> issueActivationCoupons(@PathVariable String memberCode) {
    memberService.issueActivationCoupons(memberCode);
    return ApiResponse.success();
}
```

#### 2.4.4 文件修改

| 文件 | 修改内容 |
|------|---------|
| Member.java | 确认V26新增字段已映射 |
| MemberEntity.java | 确认V26新增字段已映射 |
| MemberServiceImpl.java | 新增`issueNewMemberCoupons()`和`issueActivationCoupons()`方法 |
| MemberController.java | 新增`POST /api/members/{memberCode}/activation-coupons`接口 |

#### 2.4.5 验收点

- ✅ 新会员开卡后自动生成对应电子券
- ✅ 已注册未办卡会员可按燃油偏好发券
- ✅ 发券写入审计日志

---

## 三、开发顺序与依赖

### 3.1 任务依赖关系

```
P0-1: 权益包购买闭环（独立，可先开发）
    │
P0-2: 站点服务接入结算（独立，可与P0-1并行）
    │
P0-3: 逢10超级十惠充值赠券（依赖P0-2的站点服务）
    │
P0-4: 新增会员/潜在会员化自动发券（依赖会员模块和券模块）
```

### 3.2 推荐执行顺序

1. **P0-1**：权益包购买闭环（先把V26已建好的表用起来）
2. **P0-2**：站点服务接入结算（让结算支持stationCode自动补齐）
3. **P0-3**：逢10超级十惠充值赠券（利用日期触发和会员条件）
4. **P0-4**：新增会员/潜在会员化自动发券（完善会员生命周期）

### 3.3 验证命令

每个任务完成后执行以下验证：

```powershell
cd "D:\China National Petroleum Corporation\China National Petroleum Corporation\backend"

# 全量测试
mvn test

# 关键测试（每完成一个P0子任务）
mvn -Dtest=FlywayMigrationTest test
mvn -Dtest=CheckoutApplicationServiceTest,MemberServiceTest test

# 新增测试（对应任务）
mvn -Dtest=BenefitPackageServiceTest test      # P0-1
mvn -Dtest=StationServiceTest test             # P0-2
```

---

## 四、注意事项

### 4.1 不要回滚历史未跟踪文件

- 当前工作区有大量历史新增/修改文件，只处理本任务相关文件即可

### 4.2 不要重复导入旧规则

- V9/V12/V14/V18-V22已经覆盖大量活动规则
- 重点补"运行闭环"和"缺失活动"，不要覆盖已确认规则

### 4.3 新表结构扩展

- 所有V26新增表已有完整结构，只需实现对应Repository和Service
- 注意使用`@Transactional`保证事务一致性

### 4.4 日期触发规则

- 已在`promotion_date_trigger`表中配置了逢7/8/9/10的触发日期
- 需要在`DateConditionMatcher`中接入该表查询

### 4.5 会员等级条件

- 已在`promotion_rule.member_levels`字段中预留
- 需要在`MemberConditionMatcher`中接入该字段

---

## 五、P1任务预告

完成P0后，下一步P1任务：

| 任务 | 说明 |
|------|------|
| 积分兑换9折 | 逢9活动中积分兑换9折优惠 |
| 会员积分抽奖 | 每月9-11/19-21/29-31积分抽奖 |
| 新疆旅游一卡通销售 | 一卡通商品购买、赠2张100元汽油券 |
| LNG/CNG权益包商品 | 燃气权益包商品套餐 |
| 规则后台可视化 | 展示结构化触发日、站点范围、排除品类 |

---

## 六、完成标志

所有P0任务完成后，满足以下条件：

1. ✅ `mvn test` 通过
2. ✅ Flyway V1-V26 全量迁移成功
3. ✅ 权益包购买闭环完整（查询→购买→记录→快照）
4. ✅ 站点服务接入结算（stationCode自动补齐）
5. ✅ 逢10超级十惠充值赠券（活动日发券、会员等级差异）
6. ✅ 新增会员/潜在会员化自动发券（开卡赠券、燃油偏好赠券）
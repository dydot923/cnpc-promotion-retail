# Codex 开发指令：P0-2 站点服务与站点范围接入结算

## 指令概述

基于当前项目状态，完成 P0-2「站点服务与站点范围接入结算」任务：
1. 实现站点查询服务（支持按city、district、stationType筛选）
2. 在结算时自动根据stationCode补齐站点类型、省份等信息

---

## 一、项目上下文

### 1.1 当前状态

| 任务 | 状态 |
|------|------|
| P0-1：权益包购买闭环 | ✅ 已完成 |
| P0-2：站点服务接入结算 | 🔄 待开发 |
| P0-3：逢10超级十惠充值赠券 | 待开发 |
| P0-4：新增会员/潜在会员化自动发券 | 待开发 |

### 1.2 V26已导入数据

| 表名 | 数据量 | 说明 |
|------|--------|------|
| station | 297个站点 | 一卡通销售站点信息 |
| promotion_station_scope | 0条 | 待后续接入 |

### 1.3 当前结算问题

结算侧主要依赖请求中显式传入 `stationType`、`stationProvince`，如果收银端只传 `stationCode`，站点类型、省份、城市不会自动补齐，导致规则无法按站点类型/省份命中。

### 1.4 现有相关文件

```
backend/src/main/java/com/cnpc/promoretail/
├── checkout/
│   ├── CheckoutApplicationService.java      # 需要接入站点查询
│   ├── CheckoutCalculateRequest.java        # 需要新增stationCode字段
│   └── model/
│       └── OrderContext.java                # 需要新增stationCode字段
└── ruleengine/context/
    └── StationContext.java                  # 需要扩展字段（region拆分为province/city）
```

---

## 二、开发任务

### 任务2.1：新增站点模块

**目标**：实现站点查询服务和API接口

**文件结构**：

```
backend/src/main/java/com/cnpc/promoretail/station/
├── StationController.java                    # 新增：REST API控制器
├── StationService.java                       # 新增：服务接口
├── StationServiceImpl.java                   # 新增：服务实现（或命名为DefaultStationService）
├── StationRepository.java                    # 新增：仓储接口
├── InMemoryStationRepository.java            # 新增：内存仓储实现（用于测试）
├── MybatisStationRepository.java             # 新增：MyBatis仓储实现
├── StationNotFoundException.java             # 新增：异常类
├── model/
│   ├── Station.java                          # 新增：领域模型
│   ├── StationResponse.java                  # 新增：响应DTO
│   └── StationQuery.java                     # 新增：查询参数
└── persistence/
    ├── StationEntity.java                    # 新增：数据库实体
    ├── StationMapper.java                    # 新增：MyBatis Mapper接口
    └── StationMapper.xml                     # 新增：MyBatis SQL映射
```

**数据库表结构**（V26已创建）：

```sql
-- station表结构
station_code varchar(64) primary key,
hos_code varchar(64),
station_name varchar(256) not null,
branch_company varchar(128),
prefecture varchar(128),
province varchar(64) not null default '新疆',
city varchar(128),
district varchar(128),
address varchar(512),
longitude numeric(12, 6),
latitude numeric(12, 6),
contact_name varchar(128),
contact_phone varchar(64),
station_type varchar(64) not null default 'gas_station',
sales_scope jsonb not null default '[]'::jsonb,
remark varchar(1024),
source_sheet_name varchar(256),
source_row_number integer,
is_demo_data boolean not null default false,
created_at timestamptz not null default now(),
updated_at timestamptz not null default now()
```

**领域模型**：

```java
// Station.java
public class Station {
    private String stationCode;
    private String hosCode;
    private String stationName;
    private String branchCompany;
    private String prefecture;
    private String province;
    private String city;
    private String district;
    private String address;
    private Double longitude;
    private Double latitude;
    private String contactName;
    private String contactPhone;
    private String stationType;
    private List<String> salesScope;
    private String remark;
}
```

**API接口规范**：

| HTTP方法 | 路径 | 请求参数 | 响应体 | 说明 |
|----------|------|---------|--------|------|
| GET | /api/stations | city, district, stationType, salesScope | List<StationResponse> | 查询站点列表 |
| GET | /api/stations/{stationCode} | - | StationResponse | 查询站点详情 |

**MyBatis SQL要求**：

```xml
<!-- StationMapper.xml -->
<select id="findAll" resultType="StationEntity">
    SELECT * FROM station WHERE status != 'DELETED'
</select>

<select id="findByStationCode" resultType="StationEntity">
    SELECT * FROM station WHERE station_code = #{stationCode}
</select>

<select id="findByQuery" resultType="StationEntity">
    SELECT * FROM station
    WHERE status != 'DELETED'
      AND (city = #{city} OR #{city} IS NULL)
      AND (district = #{district} OR #{district} IS NULL)
      AND (station_type = #{stationType} OR #{stationType} IS NULL)
</select>
```

### 任务2.2：扩展StationContext

**目标**：扩展StationContext，支持province和city字段

**修改文件**：`ruleengine/context/StationContext.java`

**修改内容**：

```java
// 修改前
public record StationContext(
        String stationId,
        String stationType,
        String region
) { ... }

// 修改后
public record StationContext(
        String stationId,
        String stationType,
        String province,
        String city,
        String district
) {
    public static StationContext defaultStation() {
        return new StationContext("default", "gas_station", "新疆", null, null);
    }
    
    // 兼容旧版region字段
    public String region() {
        return province;
    }
}
```

### 任务2.3：修改结算请求和订单上下文

**目标**：在结算请求和订单上下文中增加stationCode字段

**修改文件1**：`checkout/CheckoutCalculateRequest.java`

**新增字段**：

```java
public record CheckoutCalculateRequest(
        @Valid @NotNull OrderContext orderContext,
        LocalDate transactionDate,
        LocalTime transactionTime,
        String stationType,
        String stationProvince,
        String stationCity,                    // 新增
        String stationCode,                    // 新增
        Boolean isMember,
        String memberLevel,
        Integer memberBirthMonth,
        String paymentMethod,
        FuelType fuelType,
        BigDecimal fuelAmount,
        BigDecimal fuelVolume,
        List<Coupon> availableCoupons,
        List<String> selectedCouponIds,
        String memberCode
) {
    // 更新所有构造函数，增加stationCity和stationCode参数
}
```

**修改文件2**：`ruleengine/context/OrderContext.java`

**新增字段**：

```java
public record OrderContext(
        List<CartItem> cartItems,
        FuelContext fuelContext,
        CustomerContext customerContext,
        StationContext stationContext,
        LocalDateTime transactionDateTime,
        String stationCode,                    // 新增：站点编码
        String paymentMethod
) { ... }
```

### 任务2.4：在结算服务中接入站点查询

**目标**：在`CheckoutApplicationService.effectiveOrderContext()`中接入站点查询

**修改文件**：`checkout/CheckoutApplicationService.java`

**修改内容**：

1. **新增StationRepository依赖注入**：

```java
private final StationRepository stationRepository;  // 新增

public CheckoutApplicationService(
        PromotionEngine promotionEngine,
        PromotionRuleRepository promotionRuleRepository,
        CheckoutCalculationRecordRepository checkoutCalculationRecordRepository,
        CheckoutConfirmationRepository checkoutConfirmationRepository,
        CheckoutTransactionRepository checkoutTransactionRepository,
        CouponRepository couponRepository,
        MemberRepository memberRepository,
        MemberPointsChangeRepository memberPointsChangeRepository,
        PointsActivityRepository pointsActivityRepository,
        AuditLogService auditLogService,
        StationRepository stationRepository          // 新增
) {
    // ... 其他初始化
    this.stationRepository = stationRepository;
}
```

2. **修改effectiveOrderContext方法**：

```java
private OrderContext effectiveOrderContext(CheckoutCalculateRequest request) {
    StationContext stationContext = buildStationContext(request);
    // ... 其他逻辑
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

### 任务2.5：接入MyBatis Mapper扫描

**目标**：确保新创建的StationMapper被MyBatis扫描到

**检查文件**：`common/persistence/PersistenceConfiguration.java`

**确认扫描路径包含**：`com.cnpc.promoretail.station.persistence.mapper`

### 任务2.6：新增统一异常处理

**目标**：将StationNotFoundException映射为404错误

**修改文件**：`common/api/ApiExceptionHandler.java`

**新增处理**：

```java
@ExceptionHandler(StationNotFoundException.class)
public ApiResponse<Void> handleStationNotFoundException(StationNotFoundException ex) {
    return ApiResponse.error(404, "Station not found: " + ex.getMessage());
}
```

---

## 三、测试要求

### 3.1 单元测试

**新增文件**：`StationServiceTest.java`

**测试场景**：

| 测试方法 | 说明 |
|---------|------|
| testFindByStationCode | 查询站点详情 |
| testFindByCity | 按城市筛选站点 |
| testFindByStationType | 按站点类型筛选站点 |
| testStationNotFound | 查询不存在的站点抛出异常 |

### 3.2 集成测试（可选）

**新增文件**：`StationDatabaseTest.java`

**测试场景**：

| 测试方法 | 说明 |
|---------|------|
| testFindV26ImportedStations | 查询V26导入的297个站点 |
| testStationQueryWithMultipleConditions | 多条件组合查询 |

---

## 四、验收标准

### 4.1 站点查询接口

| 验收项 | 验收方法 |
|--------|---------|
| 查询站点列表 | GET /api/stations 返回297个站点 |
| 查询站点详情 | GET /api/stations/{stationCode} 返回站点信息 |
| 按城市筛选 | GET /api/stations?city=乌鲁木齐 返回乌鲁木齐站点 |
| 按站点类型筛选 | GET /api/stations?stationType=gas_station 返回加油站站点 |
| 站点不存在 | GET /api/stations/invalid 返回404 |

### 4.2 结算侧站点信息自动补齐

| 验收项 | 验收方法 |
|--------|---------|
| 只传stationCode | 结算请求只传stationCode，OrderContext中stationType、province、city自动补齐 |
| 站点类型正确 | 补齐的stationType与数据库一致 |
| 省份正确 | 补齐的province与数据库一致 |

### 4.3 测试通过

| 测试命令 | 预期结果 |
|---------|---------|
| mvn -Dtest=Station* test | ✅ 通过 |
| mvn -Dtest=CheckoutApplicationServiceTest test | ✅ 通过 |
| mvn test | ✅ 通过 |

---

## 五、验证命令

```powershell
cd "D:\China National Petroleum Corporation\China National Petroleum Corporation\backend"

# 站点模块测试
mvn -Dtest=Station* test

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
4. **事务一致性**：站点查询是只读操作，不需要事务
5. **测试覆盖**：每个新服务类必须有单元测试

---

## 七、参考文档

| 文档 | 路径 | 用途 |
|------|------|------|
| 下一步开发计划 | docs/27-doubao-下一步开发计划.md | P0-2任务说明 |
| 数据库设计补全建议 | docs/26-doubao-数据库设计补全建议.md | 站点表结构设计 |
| 活动看板促销规则梳理 | docs/25-doubao-活动看板促销规则梳理.md | 促销规则业务背景 |

---

**指令结束**
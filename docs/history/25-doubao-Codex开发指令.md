# Codex 开发指令：会员-券-交易服务端闭环

## 指令概述

基于当前项目状态和数据库基线，完成「会员-券-交易」服务端闭环开发，实现会员识别、会员券查询、结算服务端校验、交易流水记录等核心功能。

---

## 一、项目上下文

### 1.1 当前数据库状态

已完成 Flyway 迁移 V1-V25，核心表结构：

| 表名 | 用途 | 状态 |
|------|------|------|
| `member_level` | 会员等级配置（normal/silver/gold/platinum） | ✅ V23已创建 |
| `member` | 会员信息（会员编号、姓名、手机号、等级、积分） | ✅ V23已创建 |
| `coupon_template` | 券模板（面额、门槛、有效期） | ✅ V4已创建 |
| `coupon` | 券实例（券码、归属会员、状态、使用时间） | ✅ V4已创建 |
| `checkout_transaction` | 交易记录（交易号、金额、优惠、状态） | ✅ V24已创建 |
| `checkout_transaction_item` | 交易明细（商品、数量、价格、促销） | ✅ V24已创建 |
| `inventory_alert_record` | 库存预警处理记录 | ✅ V25已创建 |
| `bundle` / `bundle_item` | 组合包 | ✅ V5已创建 |
| `product_group` / `product_group_item` | 商品组 | ✅ V5已创建 |

### 1.2 数据库基线ADR

参考文档：docs/24-数据库基线ADR.md

**核心决策**：
- 以 V1-V22 迁移和现有 MyBatis Entity 为事实基线
- 已存在的能力采用「演进现有表」策略，不重复建表
- 券实例表使用现有 `coupon`，不新建 `coupon_instance`
- 交易表使用 `checkout_transaction`，避免保留字 `transaction`
- 后续迁移只从 V26 往后追加，不改写 V1-V25

### 1.3 严重问题整改状态

参考文档：docs/23-严重问题与数据库新增整改计划.md

| 问题 | 状态 | 说明 |
|------|------|------|
| P0-1 一键启动 PostgreSQL | ✅ 已完成 | start-all.bat 和 docker-compose.yml 已修复 |
| P0-2 数据库基线 ADR | ✅ 已完成 | 文档24已采纳 |
| P0-3 会员券服务端闭环 | 🔄 待开发 | 需要实现会员模块、券归属查询、结算服务端校验 |
| P1-1 结算交易流水 | ✅ 已完成 | V24已创建表，需实现查询接口 |
| P1-2 库存预警处理状态 | ✅ 已完成 | V25已创建表，需实现处理接口 |

---

## 二、开发任务清单

### 任务1：会员模块服务层

**目标**：实现会员CRUD服务和API接口

**文件结构**：

```
backend/src/main/java/com/cnpc/promoretail/
├── member/
│   ├── MemberController.java              # 新增：REST API控制器
│   ├── MemberService.java                 # 新增：服务接口
│   ├── MemberServiceImpl.java             # 新增：服务实现
│   ├── MemberRepository.java              # 新增：仓储接口
│   ├── MybatisMemberRepository.java       # 新增：MyBatis仓储实现
│   ├── model/
│   │   ├── Member.java                    # 新增：领域模型
│   │   ├── MemberLevel.java               # 新增：会员等级模型
│   │   ├── MemberCreateRequest.java       # 新增：创建请求
│   │   ├── MemberUpdateRequest.java       # 新增：更新请求
│   │   ├── MemberResponse.java           # 新增：响应DTO
│   │   ├── MemberIdentifyRequest.java     # 新增：识别请求
│   │   ├── PointsChangeRequest.java       # 新增：积分变动请求
│   │   └── PointsChangeResponse.java      # 新增：积分变动响应
│   └── persistence/
│       ├── MemberEntity.java              # 新增：数据库实体
│       ├── MemberLevelEntity.java         # 已存在：会员等级实体
│       ├── MemberMapper.java              # 新增：MyBatis Mapper接口
│       └── MemberMapper.xml               # 新增：MyBatis SQL映射
```

**API接口规范**：

| HTTP方法 | 路径 | 请求体 | 响应体 | 说明 |
|----------|------|--------|--------|------|
| POST | /api/members/identify | MemberIdentifyRequest | MemberResponse | 会员识别（手机号/会员编号） |
| GET | /api/members | - | List<MemberResponse> | 查询会员列表 |
| GET | /api/members/{memberCode} | - | MemberResponse | 查询会员详情 |
| POST | /api/members | MemberCreateRequest | MemberResponse | 创建会员 |
| PUT | /api/members/{memberCode} | MemberUpdateRequest | MemberResponse | 更新会员信息 |
| POST | /api/members/{memberCode}/points | PointsChangeRequest | PointsChangeResponse | 积分变动 |
| GET | /api/members/{memberCode}/coupons | - | List<CouponResponse> | 查询会员可用券 |

**数据模型规范**：

```java
// Member领域模型
public class Member {
    private String memberCode;
    private String memberName;
    private String phone;
    private String levelCode;
    private String levelName;
    private BigDecimal discountRate;
    private BigDecimal pointsMultiplier;
    private long totalPoints;
    private long availablePoints;
    private LocalDate birthday;
    private String province;
    private String status;
    private List<String> benefits;
    private LocalDateTime createdAt;
}

// MemberIdentifyRequest
public class MemberIdentifyRequest {
    private String identifier;           // 手机号或会员编号
    private String identifyType;         // "PHONE" 或 "MEMBER_CODE"
}

// PointsChangeRequest
public class PointsChangeRequest {
    private String changeType;           // "ADD" 或 "SUBTRACT"
    private long amount;
    private String reason;
}
```

**MyBatis SQL要求**：

```xml
<!-- MemberMapper.xml -->
<select id="findByIdentifier" resultType="MemberEntity">
    SELECT m.*, ml.level_name, ml.discount_rate, ml.points_multiplier, ml.benefits
    FROM member m
    LEFT JOIN member_level ml ON m.level_code = ml.level_code
    WHERE (m.phone = #{identifier} OR m.member_code = #{identifier})
      AND m.status = 'ACTIVE'
</select>

<select id="findByMemberCode" resultType="MemberEntity">
    SELECT m.*, ml.level_name, ml.discount_rate, ml.points_multiplier, ml.benefits
    FROM member m
    LEFT JOIN member_level ml ON m.level_code = ml.level_code
    WHERE m.member_code = #{memberCode}
</select>

<update id="updatePoints">
    UPDATE member 
    SET total_points = total_points + #{change},
        available_points = available_points + #{change},
        updated_at = now()
    WHERE member_code = #{memberCode} AND status = 'ACTIVE'
</update>
```

---

### 任务2：券模块服务端闭环

**目标**：完善券的会员归属查询和核销校验

**文件结构**：

```
backend/src/main/java/com/cnpc/promoretail/promotion/coupon/
├── Coupon.java                           # 修改：补全 holderMemberId 映射
├── CouponEntity.java                     # 修改：补全 holder_member_id 字段
├── CouponRepository.java                 # 修改：新增 findAvailableByMemberId()
├── MybatisCouponRepository.java          # 修改：实现新方法
└── persistence/mapper/
    └── CouponMapper.xml                  # 修改：新增会员券查询SQL
```

**核心变更**：

1. **Coupon领域模型** 添加 `holderMemberId` 字段：
   ```java
   public class Coupon {
       private String couponId;
       private String holderMemberId;      // 新增：券归属会员ID
       private BigDecimal faceValue;
       private BigDecimal minSpendAmount;
       private String status;
       private LocalDateTime validUntil;
       // ... 其他字段
   }
   ```

2. **CouponRepository** 新增方法：
   ```java
   List<Coupon> findAvailableByMemberId(String memberId);
   List<Coupon> findAvailableByMemberCode(String memberCode);
   ```

3. **CouponMapper.xml** 新增SQL：
   ```xml
   <select id="findAvailableByMemberCode" resultType="CouponEntity">
       SELECT c.*, ct.coupon_name, ct.coupon_type
       FROM coupon c
       LEFT JOIN coupon_template ct ON c.coupon_template_id = ct.coupon_template_id
       WHERE c.holder_member_id = (SELECT id FROM member WHERE member_code = #{memberCode})
         AND c.status = 'UNUSED'
         AND c.valid_until > now()
   </select>
   ```

4. **Checkout结算服务** 修改：
   - Checkout计算时，后端根据 `memberCode` 自行查询可用券
   - 不再信任前端提交的完整券对象，只接收券ID列表
   - 校验券归属、状态、有效期、适用范围

---

### 任务3：交易流水查询接口

**目标**：实现交易记录查询服务

**文件结构**：

```
backend/src/main/java/com/cnpc/promoretail/checkout/
├── CheckoutTransactionController.java       # 新增：REST API控制器
├── CheckoutTransactionService.java          # 新增：服务接口
├── CheckoutTransactionServiceImpl.java      # 新增：服务实现
├── CheckoutTransactionRepository.java       # 新增：仓储接口
├── MybatisCheckoutTransactionRepository.java # 新增：MyBatis仓储实现
├── model/
│   ├── CheckoutTransaction.java             # 新增：领域模型
│   ├── CheckoutTransactionItem.java         # 新增：交易明细模型
│   ├── TransactionListResponse.java         # 新增：列表响应
│   └── TransactionDetailResponse.java       # 新增：详情响应
└── persistence/
    ├── CheckoutTransactionEntity.java       # 已存在
    ├── CheckoutTransactionItemEntity.java   # 已存在
    ├── CheckoutTransactionMapper.java       # 新增：MyBatis Mapper接口
    └── CheckoutTransactionMapper.xml        # 新增：MyBatis SQL映射
```

**API接口规范**：

| HTTP方法 | 路径 | 请求参数 | 响应体 | 说明 |
|----------|------|---------|--------|------|
| GET | /api/checkout/records | memberCode, stationCode, startDate, endDate | TransactionListResponse | 查询交易记录列表 |
| GET | /api/checkout/records/{txnNo} | - | TransactionDetailResponse | 查询交易详情（含明细） |

**数据模型规范**：

```java
// TransactionDetailResponse
public class TransactionDetailResponse {
    private String txnNo;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private String paymentMethod;
    private String operatorName;
    private String memberCode;
    private String memberName;
    private String stationCode;
    private String status;
    private LocalDateTime createdAt;
    private List<TransactionItemResponse> items;
}

// TransactionItemResponse
public class TransactionItemResponse {
    private String productCode;
    private String productName;
    private String barcode;
    private BigDecimal unitPrice;
    private BigDecimal actualPrice;
    private int quantity;
    private BigDecimal subtotal;
    private String appliedPromoId;
    private String appliedCouponCode;
}
```

**MyBatis SQL要求**：

```xml
<!-- CheckoutTransactionMapper.xml -->
<select id="findByConditions" resultType="CheckoutTransactionEntity">
    SELECT * FROM checkout_transaction
    WHERE (member_code = #{memberCode} OR #{memberCode} IS NULL)
      AND (station_code = #{stationCode} OR #{stationCode} IS NULL)
      AND (created_at >= #{startDate} OR #{startDate} IS NULL)
      AND (created_at <= #{endDate} OR #{endDate} IS NULL)
    ORDER BY created_at DESC
    LIMIT #{limit} OFFSET #{offset}
</select>

<select id="findItemsByTransactionId" resultType="CheckoutTransactionItemEntity">
    SELECT * FROM checkout_transaction_item
    WHERE transaction_id = #{transactionId}
</select>
```

---

### 任务4：确认结算流程完善

**目标**：确认结算时写入交易流水、核销券、更新会员积分

**修改文件**：

```
backend/src/main/java/com/cnpc/promoretail/checkout/
├── CheckoutApplicationService.java         # 修改：完善确认结算流程
└── CheckoutConfirmRequest.java             # 修改：增加必要字段
```

**确认结算流程**：

```text
确认结算请求
        │
        ▼
┌─────────────────────────────────────┐
│ 1. 校验计算记录存在且未已确认         │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│ 2. 查询选中的候选方案                 │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│ 3. 生成交易号 (UUID)                 │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│ 4. 写入 checkout_transaction        │
│    记录交易头信息                    │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│ 5. 写入 checkout_transaction_item   │
│    记录商品明细                     │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│ 6. 核销使用的券                      │
│    更新 coupon.status = 'USED'      │
│    更新 coupon.used_at = now()      │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│ 7. 发放赠券（如有）                   │
│    创建新 coupon 记录                │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│ 8. 更新会员积分                      │
│    实付金额 × 积分倍率               │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│ 9. 更新结算确认记录                   │
│    status = 'CONFIRMED'             │
└────────────────┬────────────────────┘
                 ▼
┌─────────────────────────────────────┐
│ 10. 返回交易详情                     │
└─────────────────────────────────────┘
```

**事务要求**：
- 步骤4-9必须在同一数据库事务中执行
- 任何步骤失败，整个事务回滚
- 使用 `@Transactional` 注解保证事务一致性

---

## 三、技术约束与规范

### 3.1 代码规范

- Java代码遵循项目现有编码风格（参照 `backend/src/main/java/com/cnpc/promoretail/common/`）
- 使用 Lombok `@Data`、`@Builder`、`@NoArgsConstructor`、`@AllArgsConstructor`
- 异常处理使用统一的 `ApiExceptionHandler`
- 返回结果使用统一的 `ApiResponse<T>` 包装

### 3.2 测试规范

- 每个新服务类必须有对应的单元测试
- 使用 Mockito 进行单元测试
- 集成测试使用 Testcontainers 验证数据库迁移和API调用
- 测试类命名：`{Service}Test.java`

### 3.3 数据库规范

- 新增表必须通过 Flyway 迁移脚本创建（从 V26 开始）
- 字段命名使用 snake_case
- 主键使用 `bigserial`
- 索引必须创建在常用查询字段上
- JSONB 字段使用 `JsonbTypeHandler` 进行序列化/反序列化

### 3.4 API规范

- 路径使用 kebab-case（如 `/api/members/{memberCode}/coupons`）
- 请求体字段使用 camelCase
- 响应体字段使用 camelCase
- 统一错误码：200成功、400参数错误、404不存在、500服务器错误

---

## 四、开发顺序与依赖

### 4.1 任务依赖关系

```
任务1: 会员模块服务层
    │
    ├──→ 任务2: 券模块服务端闭环（依赖会员查询）
    │
    └──→ 任务3: 交易流水查询接口
            │
            └──→ 任务4: 确认结算流程完善（依赖交易表写入）
```

### 4.2 执行顺序

1. **任务1**：会员模块服务层（独立，可先开发）
2. **任务2**：券模块服务端闭环（依赖任务1）
3. **任务3**：交易流水查询接口（独立，可与任务1并行）
4. **任务4**：确认结算流程完善（依赖任务2和任务3）

### 4.3 验证顺序

每个任务完成后执行以下验证：

```bash
# 1. 后端编译
mvn compile

# 2. 后端测试
mvn test

# 3. Flyway迁移测试（使用Testcontainers）
# 确保V1-V25迁移成功

# 4. API测试
# 使用 curl 或 Swagger 测试新增接口
```

---

## 五、验收标准

### 5.1 会员模块验收

| 验收项 | 验收方法 |
|--------|---------|
| 会员识别 | POST /api/members/identify，传入手机号返回会员信息 |
| 会员查询 | GET /api/members/{memberCode} 返回完整会员信息（含等级、折扣率、积分倍率） |
| 会员创建 | POST /api/members 创建新会员 |
| 积分变动 | POST /api/members/{memberCode}/points 增减积分 |
| 会员券查询 | GET /api/members/{memberCode}/coupons 返回可用券列表 |

### 5.2 券模块验收

| 验收项 | 验收方法 |
|--------|---------|
| 会员券查询 | 根据会员编号查询可用券，过滤已使用和过期券 |
| 券归属校验 | 非会员无法查询他人的券 |
| 券核销校验 | 结算时后端校验券归属、状态、有效期 |
| 重复核销拦截 | 已核销的券无法再次使用 |

### 5.3 交易流水验收

| 验收项 | 验收方法 |
|--------|---------|
| 交易记录查询 | GET /api/checkout/records 支持按会员、站点、时间筛选 |
| 交易详情查询 | GET /api/checkout/records/{txnNo} 返回交易头+明细 |
| 交易金额一致性 | 交易明细合计 = 交易头 totalAmount |
| 优惠记录 | 交易记录包含 discountAmount 和 appliedPromoId |

### 5.4 结算流程验收

| 验收项 | 验收方法 |
|--------|---------|
| 完整交易流水 | 确认结算后生成 checkout_transaction 和 checkout_transaction_item |
| 券核销 | 确认结算后使用的券状态变为 USED |
| 赠券发放 | 满足赠券条件时自动发放新券到会员账户 |
| 积分累计 | 确认结算后会员积分正确累计 |
| 事务一致性 | 任何步骤失败，数据回滚到确认前状态 |

---

## 六、参考文档

| 文档 | 路径 | 用途 |
|------|------|------|
| 促销功能分析 | docs/19-doubao-促销功能分析与规则引擎设计.md | 促销类型、规则引擎设计 |
| 核心产品需求 | docs/20-doubao-核心产品需求文档.md | 完整产品需求规范 |
| 缺失功能分析 | docs/21-doubao-缺失功能分析与开发文档.md | API接口契约、数据模型 |
| 系统架构设计 | docs/22-doubao-系统架构与数据库设计文档.md | 架构设计、数据库表结构 |
| 整改计划 | docs/23-严重问题与数据库新增整改计划.md | 问题清单和整改建议 |
| 数据库基线ADR | docs/24-数据库基线ADR.md | 数据库新增规则 |

---

## 七、完成标志

所有任务完成后，满足以下条件：

1. ✅ `mvn test` 通过
2. ✅ `npm run build` 通过
3. ✅ Flyway V1-V25 全量迁移成功
4. ✅ 会员识别 → 会员券查询 → 结算计算 → 确认结算 → 交易查询 全链路测试通过
5. ✅ 券核销后不可重复使用
6. ✅ 确认结算后可按交易号查询交易头和明细

---

**指令结束**
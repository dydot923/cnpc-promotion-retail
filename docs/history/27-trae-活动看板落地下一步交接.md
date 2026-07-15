# Trae 开发交接：活动看板落地下一步任务

生成时间：2026-07-13

## 一、本轮 Codex 已完成

本轮基于 `docs/25-doubao-活动看板促销规则梳理.md`、`docs/26-doubao-数据库设计补全建议.md` 和 `data/活动看板.xlsx`，完成了活动看板落地的数据库底座和积分闭环补强。

已落地内容：

1. 新增迁移 `backend/src/main/resources/db/migration/V26__activity_board_foundation_completion.sql`
   - 新增 `station`、`promotion_date_trigger`、`points_activity`、`member_points_change`。
   - 新增 `promotion_excluded_category`、`promotion_station_scope`。
   - 新增 `benefit_package`、`benefit_package_item`、`benefit_package_purchase`。
   - 从活动看板导入 297 个唯一站点、14 个权益包、141 条权益包明细。

2. 补齐结算确认积分闭环
   - `CheckoutApplicationService` 已在确认结算时写入会员积分和积分流水。
   - 支持候选促销自带的 `pointsMultiplier`。
   - 支持 V26 `points_activity` 中配置的逢7 CNG/LNG 3倍积分。
   - 当前策略：会员等级倍率、活动倍率取最高值，避免金卡2倍和活动3倍误乘成6倍。

3. 补齐会员积分流水
   - 新增 `MemberPointsChange` 模型、内存仓储、MyBatis 仓储。
   - 手工调分、结算赠积分都会写入 `member_points_change`。
   - 新增会员积分流水查询接口：`GET /api/members/{memberCode}/points`。

4. 补测试
   - `CheckoutApplicationServiceTest` 覆盖促销候选3倍积分、燃气积分活动3倍积分。
   - `FlywayMigrationTest` 锁定 V26 新表和导入数量。

验证结果：

```powershell
cd "D:\China National Petroleum Corporation\China National Petroleum Corporation\backend"
mvn test
```

结果：后端完整测试通过，Flyway 26 个迁移在 Testcontainers PostgreSQL 中通过。

## 二、下一步建议优先级

### P0-1：会员权益包购买闭环

背景：

V26 已经有 `benefit_package`、`benefit_package_item`、`benefit_package_purchase`，但目前还没有完整购买 API 和权益发放逻辑。

建议实现：

1. 新增权益包查询接口
   - `GET /api/benefit-packages`
   - `GET /api/benefit-packages/{packageCode}`

2. 新增权益包购买接口
   - `POST /api/benefit-packages/{packageCode}/purchase`
   - 入参建议包含：`memberCode`、`stationCode`、`paymentAmount`、`checkoutTransactionNo`、`operatorId`、`operatorName`。

3. 购买成功后写入 `benefit_package_purchase`
   - `entitlement_snapshot` 保存购买时权益明细快照，避免后续权益包配置变更影响历史订单。

4. 后续可接电子券发放
   - 初版可以先只记录购买和权益明细。
   - 第二步再把权益包中的券类明细拆成可发放券模板或直接发券。

建议新增包：

```text
backend/src/main/java/com/cnpc/promoretail/promotion/benefitpackage/
```

验收点：

- 能查询 14 个权益包。
- 能购买权益包并写入 `benefit_package_purchase`。
- 购买快照中包含对应权益包明细。
- 增加 service/controller/repository 测试。

### P0-2：站点服务与站点范围接入结算

背景：

V26 已经导入一卡通销售站点和 `promotion_station_scope`，但结算侧仍主要依赖请求中显式传入 `stationType`、`stationProvince`。如果收银端只传 `stationCode`，站点类型、省份、城市不会自动补齐。

建议实现：

1. 新增站点查询接口
   - `GET /api/stations`
   - 支持按 `city`、`district`、`stationType`、`salesScope` 查询。

2. 新增 `StationRepository`
   - MyBatis 读取 `station` 表。
   - 内存实现用于测试。

3. 在 `CheckoutApplicationService.effectiveOrderContext` 中接入站点查询
   - 如果请求只提供 `stationCode`，自动补齐 `stationType`、`stationProvince`。
   - 后续再扩展 `promotion_station_scope` 的站点级校验。

验收点：

- 站点列表可查到 V26 导入的站点。
- 收银只传 `stationCode` 时，规则仍能按站点类型/省份命中。

### P0-3：逢10超级十惠充值赠券

背景：

25号文档中 `逢10超级十惠` 是明确活动：每月10/20/30，单笔充值1000/2000赠汽油券、便利店券、洗车券，黄金及以上加赠高标号汽油券。当前规则引擎已有赠券能力，但充值场景和该活动规则还未完整闭环。

建议实现：

1. 新增充值场景模型或复用订单上下文增加充值金额字段。
2. 新增规则：
   - `abv2-a5-day10-super-1000`
   - `abv2-a5-day10-super-2000`
   - 黄金及以上加赠券逻辑可拆成独立规则或复合权益。
3. 确认结算时复用现有赠券发放逻辑。

验收点：

- 2026-07-10、2026-07-20、2026-07-30 充值满足门槛可发券。
- 普通会员和黄金及以上会员发券数量不同。
- 非活动日不发券。

### P0-4：新增会员/潜在会员化自动发券

背景：

25号文档中有：

- 3.0会员新增会员：开通昆仑e享卡赠汽油券、高标号汽油券、便利店券、洗车券。
- 潜在会员化：已注册未办卡，按汽油/柴油客户赠券。

建议实现：

1. 在会员创建/开卡接口上触发新增会员赠券。
2. 在会员资料中利用 V26 新增字段：
   - `e_enjoy_card_no`
   - `usual_province`
   - `member_tags`
   - `registered_at`
   - `card_opened_at`
3. 潜在会员化可以先做后台接口触发：
   - `POST /api/members/{memberCode}/activation-coupons`

验收点：

- 新会员开卡后自动生成对应电子券。
- 已注册未办卡会员可按燃油偏好发券。
- 发券写入审计日志。

## 三、P1：继续补活动细节

1. 积分兑换9折
   - 逢9活动中“积分兑换9折优惠”目前尚未形成独立积分兑换交易闭环。
   - 建议新增积分抵扣/兑换接口，并写入 `member_points_change`。

2. 会员积分抽奖
   - 非非促销中提到每月9-11、19-21、29-31或次月1日积分抽奖。
   - 建议先补抽奖活动表和扣500积分一次的流水，再做中奖配置。

3. 新疆旅游一卡通销售
   - 工作簿中已有一卡通销售站点。
   - 下一步应补一卡通商品购买、赠2张100元汽油券的逻辑。

4. LNG/CNG权益包商品
   - `LNG+CNG` sheet 中是会员权益包商品编码与组成明细。
   - 建议并入权益包购买闭环，或独立作为燃气权益包商品套餐。

5. 规则后台可视化
   - V26 已有结构化触发日、站点范围、排除品类表。
   - 后台可以展示这些结构，减少只看 JSON 的维护成本。

## 四、注意事项

1. 不要回滚历史未跟踪文件
   - 当前工作区本身有大量历史新增/修改文件，下一步只处理本任务相关文件即可。

2. 不要重复导入旧规则
   - V9/V12/V14/V18-V22 已经覆盖大量活动规则。
   - 下一步重点补“运行闭环”和“缺失活动”，不要覆盖已确认规则。

3. 新 `.md` 文档分类
   - 核心长期设计文档放 `docs/`。
   - 交接、过程记录、报告类文档放 `docs/history/`。

4. 测试命令

```powershell
cd "D:\China National Petroleum Corporation\China National Petroleum Corporation\backend"
mvn test
```

建议每完成一个 P0 子任务都至少跑：

```powershell
mvn -Dtest=FlywayMigrationTest test
mvn -Dtest=CheckoutApplicationServiceTest,MemberServiceTest test
```

## 五、推荐下一步顺序

1. 先做 P0-1 权益包购买闭环。
2. 再做 P0-2 站点服务接入结算。
3. 再做 P0-3 逢10超级十惠充值赠券。
4. 最后做 P0-4 新增会员/潜在会员化自动发券。

这样顺序最稳：先把 V26 已建好的表用起来，再补活动专用逻辑。

# J 类审计口径诊断报告（最终版）

诊断日期：2026-07-12  
执行标准：`docs/工程约束标准_v2_架构师修订版.md`

## 诊断结果

### inventory_snapshot 表

- 总记录数：484
- 去重商品编码数：462
- `is_demo_data = true` 记录数：30
- `is_demo_data = true` 去重商品编码数：30
- `is_demo_data = false` 记录数：454
- `is_demo_data = false` 去重商品编码数：454

### product 表

- 总记录数：12758
- `is_demo_data = true` 记录数：2
- `is_demo_data = false` 记录数：12756
- 说明：`product` 表已承载 `价格.xlsx` 的全量商品主数据，不等同于本站库存清单，不能作为 J 类审计分母。

### 与库存.xlsx 对比

- Excel 商品编码数：454
- DB `inventory_snapshot` 去重编码数：462
- DB `inventory_snapshot` 非 demo 去重编码数：454
- DB 库存有但 Excel 没有（V9 demo 库存污染）：8 个  
  `70329538`, `70407823`, `70494461`, `70538246`, `70545523`, `70559368`, `demo-cotton-film`, `demo-yingjiu-tea`
- Excel 有但 DB 非 demo 库存没有（导入遗漏）：0 个

### 污染来源

- 来源迁移：`V9__activity_board_structured_import.sql`
- V9 写入了 30 条 `activity-board-v2-demo-inventory` 库存快照。
- 其中 22 个编码同时存在于 `库存.xlsx`，8 个编码不在本站 454 库存内，导致按 `inventory_snapshot` 去重遍历时分母从 454 膨胀为 462。
- V10 已用 `is_demo_data = true` 标记 V9 demo price/inventory 行，因此无需删除原始数据。

### 污染范围判定

- [x] `inventory_snapshot` 口径污染：审计若遍历全部库存快照，会得到 462 个编码。
- [x] `product` 表不适合作为本站库存口径：它是全量商品主数据，不是污染清理对象。
- [x] 不需要 V23 破坏性清理脚本：采用 Excel 权威基数 + demo 过滤即可解决审计口径。

## 修正方案

- 采用方案：方案 B 为主，审计从 `data/库存.xlsx` 读取 454 个本站库存编码作为唯一权威分母。
- 配套修正：`MybatisProductCatalogRepository` 的最新价格和最新库存查询优先读取 `is_demo_data = false`；只有没有真实行时才回退 demo 行。这样重叠 SKU 不会被 V9 demo 覆盖，同时 G6 等显式演示规则仍可读取演示赠品库存。
- 新建迁移脚本：无。

## 验收结果

- `DatabaseContextCoverageAuditTest` 已断言 `库存.xlsx` 权威基数为 454。
- `inventory_snapshot where is_demo_data = false` 去重编码数为 454。
- Excel 有但 DB 非 demo 库存缺失为 0。
- V9 demo 库存污染编码 8 个已完整记录。
- `mvn test`：129 tests，0 failures，0 errors，0 skipped。

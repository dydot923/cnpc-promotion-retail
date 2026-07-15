# 加油站促销智能零售系统演示脚本

## 1. 启动 PostgreSQL

```powershell
docker compose up -d
docker ps
```

确认 `cnpc-promotion-postgres` 为 `healthy`。

## 2. 启动后端

```powershell
cd backend
mvn --% -DskipTests -Dspring-boot.run.profiles=dev-db -Dspring-boot.run.arguments=--server.port=18082 spring-boot:start
```

健康检查：

```powershell
Invoke-RestMethod http://localhost:18082/actuator/health
```

## 3. 导入基础数据

不要修改 `data/` 下原始 Excel，只上传原文件：

```powershell
curl.exe -s -X POST -F "file=@E:/China National Petroleum Corporation/data/价格.xlsx" http://localhost:18082/api/import/prices
curl.exe -s -X POST -F "file=@E:/China National Petroleum Corporation/data/库存.xlsx" http://localhost:18082/api/import/inventory
curl.exe -s -X POST -F "file=@E:/China National Petroleum Corporation/data/活动看板.xlsx" http://localhost:18082/api/import/promotions
```

## 4. 启动前端

```powershell
cd frontend
npm run dev
```

打开：

```text
http://localhost:5173
```

## 5. 规则确认

进入“规则确认”页面：

1. 筛选 `PENDING_CONFIRMATION`；
2. 找到 9.9 专区规则；
3. 点击“确认”；
4. 查看 audit log；
5. 确认页面提示“只有 CONFIRMED 规则参与结算”。

## 6. 收银结算

进入“收银结算”页面：

1. 点击“9.9 固定价”示例；
2. 点击“计算促销”；
3. 核对应付金额、优惠金额、推荐方案、ruleVersionIds；
4. 使用条码输入框查询真实导入商品；
5. 回车后商品自动加入购物车；
6. 重复输入同一条码，购物车数量 +1；
7. 查询失败时输入保留，方便修正。

## 7. 导入异常

进入“导入异常”页面：

1. 查看导入批次；
2. 选择活动看板导入批次；
3. 按 `ERROR` 筛选；
4. 查看 `MISSING_PRODUCT_CODE` 异常行。

## 8. 库存预警

进入“库存预警”页面：

1. 查看 confirmed 促销涉及商品的库存预警；
2. 按 `LOW`、`CRITICAL`、`OUT_OF_STOCK`、`NO_STATION_STOCK` 筛选；
3. 查看关联促销规则、当前库存、阈值、建议补货数量；
4. 对组合包，查看可组装套数不足的限制商品原因；
5. 点击“生成补货清单”。

## 9. 补货清单

进入“补货清单”页面：

1. 点击“从预警生成”；
2. 查看商品编码、条码、当前库存、阈值、建议数量、关联促销和原因；
3. 点击“下载 CSV”；
4. 用 Excel 打开 CSV 检查补货清单。

## 10. 验证命令

```powershell
cd backend
mvn test

cd ../frontend
npm test
npm run build
```

已知：Windows + Docker Desktop named pipe 下，`FlywayMigrationTest` 可能因 Testcontainers 连接方式跳过；Docker Compose dev-db 路径不受影响。

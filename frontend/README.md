# Promotion Retail Frontend

收银前端 MVP 使用 React 18、TypeScript、Vite、Ant Design 和 TanStack Query。

## 启动

先启动后端 dev-db：

```powershell
cd ..\backend
$env:DB_URL="jdbc:postgresql://localhost:5432/cnpc_promotion"
$env:DB_USERNAME="cnpc"
$env:DB_PASSWORD="cnpc"
mvn --% -DskipTests -Dspring-boot.run.profiles=dev-db -Dspring-boot.run.arguments=--server.port=18082 spring-boot:start
```

再启动前端：

```powershell
cd ..\frontend
npm install
npm run dev -- --port 5173
```

访问：

```text
http://localhost:5173
```

Vite 会把 `/api` 代理到 `http://localhost:18082`。

## 边界

前端只负责收集订单上下文、调用 `/api/checkout/calculate`、展示后端返回的推荐方案、可用促销、不可用促销、赠品、赠券、解释文本和规则版本。

前端不得实现促销可用性判断、互斥判断、最终应付金额计算、库存判断、会员判断、油品门槛判断、香烟/化肥排除判断。

## 页面

- `收银结算`：支持演示样例、手工录入、条码查询并加入购物车、重复条码数量 +1。
- `规则确认`：查看 draft、确认、拒绝、停用 confirmed rule、查看 audit log。
- `导入异常`：查看导入批次和异常行，按 severity 筛选。
- `库存预警`：展示后端库存预警，可按 severity 筛选，并生成补货清单。
- `补货清单`：从预警生成清单并下载 CSV。

## 验证

```powershell
npm test
npm run build
```

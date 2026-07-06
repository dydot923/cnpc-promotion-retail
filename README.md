# 加油站促销智能零售系统

当前阶段以后端促销规则引擎、Excel 导入、规则治理和 checkout 计算闭环为核心。

## 默认启动

默认 profile 不依赖数据库，会使用内存版规则仓库和计算记录仓库。

```powershell
cd backend
mvn test
mvn -DskipTests "-Dspring-boot.run.arguments=--server.port=18080" spring-boot:start spring-boot:stop
```

## dev-db 启动

`dev-db` profile 启用 PostgreSQL、Flyway 和 MyBatis Plus 持久化实现。

```powershell
docker compose up -d postgres
cd backend
$env:DB_URL="jdbc:postgresql://localhost:5432/cnpc_promotion"
$env:DB_USERNAME="cnpc"
$env:DB_PASSWORD="cnpc"
mvn -DskipTests spring-boot:run "-Dspring-boot.run.profiles=dev-db"
```

Flyway 会在应用启动时执行 `backend/src/main/resources/db/migration` 下的迁移脚本。

PowerShell 下如果使用 `spring-boot:start`，建议用停止解析符传递 Spring Boot Maven 参数：

```powershell
mvn --% -DskipTests -Dspring-boot.run.profiles=dev-db -Dspring-boot.run.arguments=--server.port=18082 spring-boot:start
mvn -DskipTests spring-boot:stop
```

## dev-db 闭环验证

本地 Docker Compose PostgreSQL 已验证通过：

- Docker PostgreSQL 容器：`cnpc-promotion-postgres`，端口 `localhost:5432`，状态 `healthy`。
- Flyway 已执行：`V1__init_core_tables.sql`、`V2__promotion_rule_governance.sql`。
- `/api/checkout/calculate` 在无 `CONFIRMED` 规则时返回 `original-price` 原价兜底，并写入 `checkout_calculation_record`。
- `/api/import/promotions` 导入 `data/活动看板.xlsx` 后，写入 `import_batch`、`import_error_row`，并生成 9.9 专区 `promotion_rule_draft`。
- 确认 `draft-import-fixed-9_9-70424725` 后，写入 `promotion_rule_version` 和 `promotion_rule_audit_log`，checkout 可从 PostgreSQL 加载 confirmed fixed_price 规则并返回 `ruleVersionIds`。
- 停用 `import-fixed-9_9-70424725` 后，当前 draft 状态变为 `DISABLED`，checkout 不再加载该规则并回到原价兜底。

## 持久化测试

项目包含 Testcontainers/Flyway 迁移测试。当前 Windows + Docker Desktop 环境中，Testcontainers 会识别到 `docker_cli` named pipe，但 Java Docker client 对该 named pipe 返回兼容性错误，因此 `FlywayMigrationTest` 会跳过；这不影响普通 `mvn test`，也不影响上面的 Docker Compose PostgreSQL 真实库验证路径。

```powershell
cd backend
mvn test
```

## 数据文件约束

`data/价格.xlsx`、`data/库存.xlsx`、`data/活动看板.xlsx` 是原始业务数据源。开发和测试只读这些文件，不移动、不删除、不修改。

# 促销引擎性能基线

测试日期：2026-07-12  
测试：`PromotionEnginePerformanceTest`  
环境：Java 21、Spring Boot 3.4.5、PostgreSQL 16 Testcontainers、Flyway V1-V22

## 结果

| 场景 | 商品数 | 规则数 | 预热 | 测量次数 | 平均耗时 | 约束 | 结果 |
|---|---:|---:|---:|---:|---:|---:|---|
| 当前全量 CONFIRMED | 50 | 247 | 3 | 10 | 2 ms | <1000 ms | PASS |
| 500 规则容量基线 | 50 | 500 | 3 | 10 | 4 ms | <1000 ms | PASS |

500 规则场景从 247 条真实反序列化规则循环克隆，保留条件、优惠、互斥组、优先级、叠加属性和积分倍率，仅给测试规则 ID 添加唯一后缀。计时只包含 `PromotionEngine.calculate()`，不包含 Spring 启动、Flyway、Excel 导入和数据库查询。

当前基线远低于 1 秒，无需规则索引优化。生产 SLA 仍应在固定 CI 硬件上持续记录 P50/P95，不能直接使用本机平均值替代。

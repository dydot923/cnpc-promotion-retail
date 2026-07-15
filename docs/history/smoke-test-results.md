# 冒烟测试结果

执行日期：2026-07-12  
依据：`docs/smoke-test-checklist.md`

| 场景 | 结果 | 自动化证据 |
|---|---|---|
| A 扫码结算与确认 | PASS | `CheckoutApplicationServiceTest#calculateLoadsOnlyConfirmedRulesAndReturnsRuleVersionIds`、`confirmStoresSelectedCandidateSnapshotAndAuditLog` |
| B 原价兜底确认 | PASS | `calculateFallsBackToOriginalPriceWhenNoRuleIsConfirmed`、`confirmOriginalPriceFallbackCanBeStoredAsSkippedPromotion` |
| C 候选切换 | PASS | `confirmStoresSelectedCandidateSnapshotAndAuditLog` 验证提交候选快照与追溯一致 |
| D 重复确认拒绝 | PASS | `duplicateConfirmationForSameCalculationIsRejected` |
| E 库存预警与补货 | PASS | `ProductInventoryReplenishmentTest#inventoryAlertsCoverSeverityAndBundleAssembly`、`replenishmentListCanBeGeneratedAndExportedAsCsv`、审计用例 |
| F 导入异常查看与导出 | PASS | `ImportCenterServiceTest#importErrorApplicationServiceFiltersExportsAndWritesAudit` |

补充验证：

- `ImportedPromotionEndToEndTest` 覆盖 9.9、券核销、换购、G6、序列券、商品组。
- `MultiContextAuditTest` 覆盖 8 个业务上下文。
- `ActivityBoardFinalRulesIntegrationTest` 覆盖 A4/G1/G2/A3/G4 共 28 个业务场景。
- `ActivityStatusConfirmationIntegrationTest` 覆盖 A1/E1/E2/F1/H2 的数据库状态和执行。
- `DatabaseContextCoverageAuditTest` 覆盖 454 个真实库存 SKU 的 5 个生产数据库上下文。
- `G5CompositeCandidateTest` 覆盖复合候选 5 个用例。
- `PromotionEnginePerformanceTest` 覆盖 50x247 和 50x500 性能基线。

本轮未启动长驻前后端服务。A-F 的后端状态变化、持久化和审计已由自动化测试重跑；浏览器点击动线仍需在正式演示环境做一次人工预演。已知业务数据限制：水饮包所需 `70655834` 当前库存为 1，少于组合需求 2，系统会返回明确库存不足原因。

最终命令结果：后端 `129 tests, 0 failures, 0 errors, 0 skipped`；前端静态冒烟通过（5173 未启动，代理探测按策略跳过）；生产构建通过，仅有 Vite chunk size warning。

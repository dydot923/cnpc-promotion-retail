# Bug 修复轮次 — 概述

> 高级开发工程师 | 2026-07-12

## 完成内容

基于前一轮代码质量评估发现的问题，对前后端进行了第一轮系统性 Bug 修复。

## 修复清单

### P0 级修复（3 项）

| # | 问题 | 修复内容 | 涉及文件 |
|---|---|---|---|
| 1 | 根目录垃圾文件 `nul)\`` | 已删除进程崩溃产生的临时文件，并在 .gitignore 中添加防护规则 | `.gitignore` |
| 2 | 数据库凭据硬编码 | docker-compose.yml 改为环境变量引用，密码必填；.env.example 补充安全提示和完整变量 | `docker-compose.yml`, `.env.example` |
| 3 | 后端缺少全局异常兜底 | ApiExceptionHandler 新增 5 个异常处理器（参数校验、类型不匹配、非法参数、JSON 解析错误、通用兜底），确保所有异常返回友好 JSON 而非堆栈 | `ApiExceptionHandler.java` |

### P1 级修复（5 项）

| # | 问题 | 修复内容 | 涉及文件 |
|---|---|---|---|
| 4 | 前端硬编码值 | API 超时时间改为环境变量可配；Vite 代理目标改为可配；InventoryAlertPage 硬编码日期改为动态；CheckoutPage 修复 4 处硬编码（inventoryQuantity=999999、V22、247 条、2099-12-31） | `request.ts`, `vite.config.ts`, `InventoryAlertPage.tsx`, `CheckoutPage.tsx` |
| 5 | 前端无 ErrorBoundary | 新建 ErrorBoundary 组件，捕获未处理渲染错误，展示友好错误页+重试按钮，包裹整个 App | `ErrorBoundary.tsx`, `App.tsx` |
| 6 | API 代码大量重复 | 6 个 API 文件（checkout/products/inventory/replenishment/audit/promotionManagement）统一重构为复用 `apiRequest`，消除重复的 fetch 封装，获得统一的超时、错误处理和 BASE_URL 支持 | `api/*.ts`（6 个文件） |
| 7 | 后端导入接口无文件校验 | ImportCenterController 4 个导入接口统一添加文件空值校验和 .xlsx 格式校验 | `ImportCenterController.java` |
| 8 | 后端无 CORS 配置 | 新建 WebMvcConfiguration，配置 /api/** 和 /actuator/** 的 CORS，支持环境变量配置允许的源 | `WebMvcConfiguration.java` |

### P2 级修复（2 项）

| # | 问题 | 修复内容 | 涉及文件 |
|---|---|---|---|
| 9 | Dashboard 假数据兜底 | 移除 `|| 247`、`|| 69`、`|| 254`、`|| 22` 等 4 处假数据兜底，API 返回 0 时展示真实值 | `DashboardPage.tsx` |
| 10 | RuleManagement 假数据兜底 | 移除 `|| 247`、`|| 69`、`|| 3` 等 3 处假数据兜底 | `RuleManagementPage.tsx` |

## 新增文件

| 文件 | 说明 |
|---|---|
| `frontend/src/components/ErrorBoundary.tsx` | React 错误边界组件 |
| `backend/.../common/config/WebMvcConfiguration.java` | Spring MVC + CORS 配置 |

## 修改文件清单（共 16 个）

**后端（4 个）**：ApiExceptionHandler.java、ImportCenterController.java、WebMvcConfiguration.java（新建）、docker-compose.yml

**前端（10 个）**：App.tsx、ErrorBoundary.tsx（新建）、request.ts、vite.config.ts、checkout.ts、products.ts、inventory.ts、replenishment.ts、audit.ts、promotionManagement.ts

**页面（3 个）**：CheckoutPage.tsx、DashboardPage.tsx、InventoryAlertPage.tsx、RuleManagementPage.tsx

**配置（2 个）**：.env.example、.gitignore

## 验证结果

- ✅ TypeScript 类型检查零错误
- ✅ 前端 Vite 构建成功（7.44s）
- ✅ 后端 Maven 编译成功

## 仍需后续处理

| 项目 | 说明 |
|---|---|
| auth 模块 | 仍为空壳，需后续实现 Spring Security + JWT |
| 前端测试 | 仍几乎空白，需补充单元测试和集成测试 |
| CI/CD | ci.yml 已创建但未启用，需推送到 GitHub 激活 |
| 前端 chunk 优化 | index.js 775KB 超阈值，需手动分包 |
| promotionManagement.ts 与 rules.ts | 功能重叠，后续应合并统一 |

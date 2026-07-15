## 变更说明

<!-- 简述本次 PR 做了什么，为什么做 -->

## 变更类型

- [ ] feat: 新功能
- [ ] fix: Bug 修复
- [ ] refactor: 重构
- [ ] test: 测试
- [ ] docs: 文档
- [ ] chore: 构建/工具链
- [ ] perf: 性能优化

## 影响范围

<!-- 影响了哪些模块？是否有 Breaking Change？ -->

## 检查清单

### 通用
- [ ] 代码通过 ESLint / Checkstyle 检查
- [ ] 新增/修改的代码有对应测试
- [ ] mvn test / npm test 通过
- [ ] npm run build 通过
- [ ] 无 console.log / System.out.println 遗留
- [ ] 无硬编码业务值（端口号、版本号、规则数等）

### 后端专项（如涉及）
- [ ] 金额使用 BigDecimal，无 double/float
- [ ] 促销计算逻辑只在 ruleengine
- [ ] Controller 不含业务判断
- [ ] 不可用规则有 BlockedPromotion 原因
- [ ] 原价兜底始终存在
- [ ] 新增规则有测试覆盖（简单 ≥3，复杂 ≥5）
- [ ] Flyway 迁移脚本幂等

### 前端专项（如涉及）
- [ ] 无促销规则判断逻辑
- [ ] 无金额计算逻辑
- [ ] TypeScript 类型完整，无 any
- [ ] useEffect 依赖数组完整
- [ ] 列表渲染有唯一 key
- [ ] 加载/错误/空状态有处理

## 测试方式

<!-- 如何验证本次变更？列出测试步骤或测试用例 -->

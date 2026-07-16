import { readFile } from "node:fs/promises";

const frontendBaseUrl = process.env.FRONTEND_BASE_URL || "http://localhost:5173";
const requireFrontendSmoke = process.env.REQUIRE_FRONTEND_SMOKE === "true";

const requiredCheckoutLabels = [
  "活动看板逐项验收",
  "A1 逢7气惠-LNG满500",
  "A4 逢8 CN98每升立减",
  "A5 超级十惠-充值1000金卡",
  "G3 9.9元零食专区",
  "G6 香烟满200赠品二选一",
  "G6 香烟满555赠伊力特250ML",
  "G6 香烟满888赠伊力特500ML",
  "G6 便利店满额赠品二选一",
  "G6 棉包膜9卷赠完整礼包",
  "G7 单品安全促销价",
  "H1 加油换购驾驶包",
  "验收日期",
  "充值金额",
  "可用促销方案",
  "不可用促销",
  "原价兜底",
  "审计追踪",
  "查询并加入"
];

const requiredNavLabels = ["收银结算", "活动验收", "运营看板", "数据导入", "库存预警", "规则管理", "AI 海报"];

const forbiddenFrontendPatterns = [
  /isPromotionEligible/,
  /calculateFinalPayable/,
  /shouldApplyPromotion/,
  /resolvePromotionConflict/,
  /threshold\s*\*\s*2/,
  /OUT_OF_STOCK.*currentQuantity/,
  /payableAmount\s*=/,
  /discountAmount\s*=/
];

async function main() {
  const files = {
    app: await readFile("src/App.tsx", "utf8"),
    layout: await readFile("src/components/AppLayout.tsx", "utf8"),
    checkout: await readFile("src/pages/CheckoutPage.tsx", "utf8"),
    dashboard: await readFile("src/pages/DashboardPage.tsx", "utf8"),
    importPage: await readFile("src/pages/ImportPage.tsx", "utf8"),
    inventory: await readFile("src/pages/InventoryAlertPage.tsx", "utf8"),
    rules: await readFile("src/pages/RuleManagementPage.tsx", "utf8"),
    poster: await readFile("src/pages/PosterPage.tsx", "utf8"),
    operationCampaigns: await readFile("src/pages/OperationCampaignPage.tsx", "utf8"),
    theme: await readFile("src/theme.ts", "utf8"),
    styles: await readFile("src/styles.css", "utf8"),
    request: await readFile("src/api/request.ts", "utf8"),
    statusHook: await readFile("src/hooks/useBackendStatus.ts", "utf8"),
    price: await readFile("src/components/Price.tsx", "utf8"),
    empty: await readFile("src/components/EmptyState.tsx", "utf8")
  };

  assert(files.app.includes("BrowserRouter") && files.app.includes("lazy(() =>"), "router lazy loading missing");
  assert(files.app.includes("/checkout") && files.app.includes("/dashboard") && files.app.includes("/poster"), "route list missing");

  for (const label of requiredNavLabels) {
    assert(files.layout.includes(label), `AppLayout navigation label missing: ${label}`);
  }
  assert(files.layout.includes("localStorage") && files.layout.includes("cnpc-role"), "role persistence missing");
  assert(files.layout.includes("width={220}") && !files.layout.includes("collapsed"), "fixed 220px sider missing");

  for (const label of requiredCheckoutLabels) {
    assert(files.checkout.includes(label), `CheckoutPage missing label: ${label}`);
  }
  assert(files.checkout.includes("calculateCheckout"), "CheckoutPage must call checkout API wrapper");
  assert(files.checkout.includes("confirmCheckout"), "CheckoutPage must call confirmation API wrapper");
  assert(files.checkout.includes("fetchProductByBarcode"), "CheckoutPage barcode lookup missing");

  assert(files.dashboard.includes("运营看板") && files.dashboard.includes("低库存 TOP 10"), "DashboardPage sections missing");
  assert(files.importPage.includes("Upload.Dragger") && files.importPage.includes("前端不解析文件内容"), "ImportPage upload contract missing");
  assert(files.inventory.includes("生成补货清单") && files.inventory.includes("导出 Excel"), "InventoryAlertPage actions missing");
  assert(files.rules.includes("规则详情") && files.rules.includes("条件 JSON") && files.rules.includes("审计日志"), "RuleManagementPage drawer missing");
  assert(files.poster.includes("AI 海报服务未配置") && files.poster.includes("确认发布"), "PosterPage review flow missing");
  assert(files.operationCampaigns.includes("活动看板验收中心"), "activity acceptance center missing");
  assert(files.operationCampaigns.includes("会员生命周期") && files.operationCampaigns.includes("积分活动") && files.operationCampaigns.includes("权益包"), "activity acceptance tabs missing");

  assert(files.theme.includes("#D71920") && files.theme.includes("#003F88") && files.theme.includes("#FFB81C"), "CNPC theme colors missing");
  assert(files.styles.includes(".header-brand-stripe"), "brand stripe missing");
  assert(files.styles.includes("@media (max-width: 1100px)"), "responsive checkout layout missing");
  assert(!files.styles.includes("useBreakpoint"), "desktop fixed layout should not use breakpoint helpers");

  assert(files.request.includes("VITE_API_BASE_URL") && files.request.includes("10_000"), "request wrapper timeout/base URL missing");
  assert(
    files.statusHook.includes("/api/checkout/capabilities") &&
      files.statusHook.includes("checkout-v2") &&
      files.statusHook.includes("30_000"),
    "checkout-ready backend status hook missing"
  );
  assert(files.price.includes("String(amount)") && !files.price.includes("toFixed"), "Price must display backend amount strings without formatting math");
  assert(files.empty.includes("Empty"), "EmptyState missing Ant Empty wrapper");

  for (const pattern of forbiddenFrontendPatterns) {
    assert(!pattern.test(files.checkout), `Frontend appears to contain promotion calculation logic: ${pattern}`);
  }

  if (await canReachFrontend()) {
    await smokeFrontendRoutes();
  } else if (requireFrontendSmoke) {
    throw new Error(`frontend smoke required but ${frontendBaseUrl} is not reachable`);
  } else {
    console.log(`frontend browser smoke skipped: ${frontendBaseUrl} is not reachable`);
  }

  console.log("frontend smoke tests passed");
}

async function canReachFrontend() {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 1500);
    const response = await fetch(frontendBaseUrl, { signal: controller.signal });
    clearTimeout(timeout);
    return response.ok;
  } catch {
    return false;
  }
}

async function smokeFrontendRoutes() {
  for (const path of ["/checkout", "/operation-campaigns", "/dashboard", "/import", "/inventory", "/rules", "/poster", "/missing-route"]) {
    const response = await fetch(`${frontendBaseUrl}${path}`);
    assert(response.ok, `${path} should return index.html from Vite dev server`);
    const html = await response.text();
    assert(html.includes("<div id=\"root\"></div>"), `${path} did not return app shell`);
  }
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});

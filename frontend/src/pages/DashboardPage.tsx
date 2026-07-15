import { AlertOutlined, ApiOutlined, BarChartOutlined, DatabaseOutlined, TagsOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { Alert, Empty, Progress, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { ReactNode } from "react";
import { useMemo } from "react";
import { fetchInventoryAlerts } from "../api/inventory";
import { fetchImportBatches } from "../api/importCenter";
import { fetchRuleDrafts } from "../api/rules";
import type { ImportBatch, InventoryAlert, PromotionRuleDraft } from "../types";

type ActivityRow = {
  key: string;
  name: string;
  rules: number;
  confirmed: number;
  draft: number;
  status: string;
  ruleTypes: string[];
  source: string;
  sampleRules: string[];
};

const operationRows = [
  { key: "rfm", name: "RFM 客户挽回", endpoint: "/api/operation-campaigns/rfm-recovery/coupons", benefit: "汽油/柴油券 + 商品券" },
  { key: "birthday", name: "生日礼包", endpoint: "/api/operation-campaigns/birthday/coupons", benefit: "汽油券 + 商品券 + 洗车券" },
  { key: "sign-in", name: "签到活动", endpoint: "/api/operation-campaigns/sign-in/coupons", benefit: "3/7/10 天阶梯券包" },
  { key: "group-buy", name: "拼团活动", endpoint: "/api/operation-campaigns/group-buy/coupons", benefit: "2/5/8 人阶梯券包" },
  { key: "industry", name: "行业认证", endpoint: "/api/operation-campaigns/industry-certification/coupons", benefit: "认证人群专属券包" },
  { key: "ecommerce", name: "电商平台", endpoint: "/api/operation-campaigns/ecommerce/coupons", benefit: "平台订单券包" }
];

export default function DashboardPage() {
  const inventoryQuery = useQuery({ queryKey: ["dashboard-inventory-alerts"], queryFn: fetchInventoryAlerts });
  const rulesQuery = useQuery({ queryKey: ["dashboard-rule-drafts"], queryFn: () => fetchRuleDrafts() });
  const importsQuery = useQuery({ queryKey: ["dashboard-import-batches"], queryFn: fetchImportBatches });

  const alerts = inventoryQuery.data || [];
  const drafts = rulesQuery.data || [];
  const batches = importsQuery.data || [];
  const activityRows = useMemo(() => buildActivityRows(drafts), [drafts]);

  const lowInventoryTop = alerts.slice(0, 10);
  const confirmedCount = countRules(drafts, "CONFIRMED");
  const draftCount = drafts.filter((draft) => draft.status !== "CONFIRMED" && draft.rule.status !== "CONFIRMED").length;
  const latestBatch = batches[0];
  const ruleConfirmationRate = drafts.length === 0 ? 0 : Math.round((confirmedCount / drafts.length) * 100);
  const visibleActivityCount = activityRows.length + operationRows.length;

  const activityColumns: ColumnsType<ActivityRow> = [
    {
      title: "活动",
      dataIndex: "name",
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.name}</Typography.Text>
          <Typography.Text type="secondary">{record.source}</Typography.Text>
        </Space>
      )
    },
    { title: "状态", dataIndex: "status", width: 110, render: statusTag },
    { title: "规则数", dataIndex: "rules", width: 90 },
    {
      title: "确认率",
      width: 150,
      render: (_, record) => (
        <Progress
          percent={record.rules === 0 ? 0 : Math.round((record.confirmed / record.rules) * 100)}
          size="small"
        />
      )
    },
    {
      title: "类型",
      dataIndex: "ruleTypes",
      width: 210,
      render: (value: string[]) => (
        <Space wrap size={4}>
          {value.slice(0, 4).map((item) => (
            <Tag key={item}>{ruleTypeLabel(item)}</Tag>
          ))}
        </Space>
      )
    },
    {
      title: "样例",
      dataIndex: "sampleRules",
      render: (value: string[]) => value.join(" / ")
    }
  ];

  const operationColumns = [
    { title: "活动", dataIndex: "name", width: 150 },
    { title: "状态", width: 110, render: () => <Tag color="green">接口已接入</Tag> },
    { title: "发放内容", dataIndex: "benefit", width: 220 },
    { title: "触发接口", dataIndex: "endpoint" }
  ];

  const inventoryColumns: ColumnsType<InventoryAlert> = [
    { title: "预警", dataIndex: "severity", width: 120, render: severityTag },
    { title: "商品", dataIndex: "productName", width: 180 },
    { title: "商品码", dataIndex: "productCode", width: 130 },
    { title: "当前", dataIndex: "currentQuantity", width: 90 },
    { title: "阈值", dataIndex: "threshold", width: 90 },
    { title: "关联活动", dataIndex: "relatedRuleType" }
  ];

  const importColumns: ColumnsType<ImportBatch> = [
    { title: "时间", dataIndex: "createdAt", width: 190 },
    { title: "文件", dataIndex: "sourceFile" },
    { title: "类型", dataIndex: "importType", width: 120 },
    { title: "异常", dataIndex: "invalidCount", width: 90 },
    { title: "警告", dataIndex: "warningCount", width: 90 }
  ];

  return (
    <>
      <div className="page-header">
        <div>
          <Typography.Title level={1}>运营看板</Typography.Title>
          <Typography.Text className="page-subtitle">规则、导入、库存和活动覆盖概览</Typography.Text>
        </div>
        <Tag color="blue">数据版本 activity-board-v2</Tag>
      </div>

      <div className="stat-grid">
        <StatCard icon={<TagsOutlined />} label="CONFIRMED 规则" value={String(confirmedCount)} />
        <StatCard icon={<BarChartOutlined />} label="可见活动" value={String(visibleActivityCount)} detail="规则型 + 接口触发型" />
        <StatCard icon={<AlertOutlined />} label="库存预警" value={String(alerts.length)} detail="缺货/低库存关注" />
        <StatCard icon={<DatabaseOutlined />} label="导入批次" value={String(batches.length)} detail={latestBatch?.sourceFile || "暂无导入记录"} />
      </div>

      <div className="two-column-grid">
        <section className="panel">
          <Space className="panel-toolbar">
            <div>
              <Typography.Title level={3} className="section-title">
                规则型促销活动
              </Typography.Title>
              <Typography.Text type="secondary">从后端已导入规则动态聚合，CONFIRMED 规则会参与收银试算。</Typography.Text>
            </div>
          </Space>
          {rulesQuery.error ? (
            <Alert type="warning" showIcon message="规则列表接口暂不可用" description={rulesQuery.error.message} />
          ) : (
            <Table<ActivityRow>
              rowKey="key"
              size="small"
              loading={rulesQuery.isLoading}
              columns={activityColumns}
              dataSource={activityRows}
              pagination={{ pageSize: 8 }}
              locale={{ emptyText: <Empty description="暂无活动规则" /> }}
              scroll={{ x: 980 }}
            />
          )}
        </section>

        <section className="panel">
          <Typography.Title level={3} className="section-title">
            运营触发型活动
          </Typography.Title>
          <Table
            rowKey="key"
            size="small"
            columns={operationColumns}
            dataSource={operationRows}
            pagination={false}
            scroll={{ x: 720 }}
          />
        </section>
      </div>

      <div className="two-column-grid">
        <section className="panel">
          <Typography.Title level={3} className="section-title">
            促销覆盖统计
          </Typography.Title>
          <Space direction="vertical" size={12} className="full-width">
            <CoverageLine label="规则确认率" percent={ruleConfirmationRate} />
            <CoverageLine label="运营触发接口" percent={100} />
            <CoverageLine label="活动可视化分组" percent={activityRows.length > 0 ? 100 : 0} />
          </Space>
        </section>

        <section className="panel">
          <Typography.Title level={3} className="section-title">
            数据版本信息
          </Typography.Title>
          {importsQuery.error ? (
            <Alert type="warning" showIcon message="导入批次接口暂不可用" description={importsQuery.error.message} />
          ) : (
            <Table<ImportBatch>
              rowKey={(record) => record.importId.value}
              size="small"
              loading={importsQuery.isLoading}
              columns={importColumns}
              dataSource={batches.slice(0, 5)}
              pagination={false}
              locale={{ emptyText: <Empty description="暂无导入批次" /> }}
              scroll={{ x: 820 }}
            />
          )}
        </section>
      </div>

      <section className="panel">
        <Typography.Title level={3} className="section-title">
          低库存 TOP 10
        </Typography.Title>
        {inventoryQuery.error ? (
          <Alert type="warning" showIcon message="库存接口暂不可用" description={inventoryQuery.error.message} />
        ) : (
          <Table<InventoryAlert>
            rowKey="alertId"
            size="small"
            loading={inventoryQuery.isLoading}
            columns={inventoryColumns}
            dataSource={lowInventoryTop}
            pagination={false}
            locale={{ emptyText: <Empty description="暂无库存预警" /> }}
            scroll={{ x: 900 }}
          />
        )}
      </section>
    </>
  );
}

function buildActivityRows(drafts: PromotionRuleDraft[]): ActivityRow[] {
  const groups = new Map<string, ActivityRow>();
  drafts.forEach((draft) => {
    const key = activityKey(draft);
    const current = groups.get(key) || {
      key,
      name: activityName(draft),
      rules: 0,
      confirmed: 0,
      draft: 0,
      status: "DRAFT",
      ruleTypes: [],
      source: draft.sourceImportId || draft.sourceSheetName || "-",
      sampleRules: []
    };
    current.rules += 1;
    if (draft.status === "CONFIRMED" || draft.rule.status === "CONFIRMED") {
      current.confirmed += 1;
    } else {
      current.draft += 1;
    }
    if (!current.ruleTypes.includes(draft.rule.ruleType)) {
      current.ruleTypes.push(draft.rule.ruleType);
    }
    if (current.sampleRules.length < 2) {
      current.sampleRules.push(draft.rule.activityName || draft.rule.ruleId);
    }
    current.status = current.confirmed === current.rules ? "CONFIRMED" : current.confirmed > 0 ? "PARTIAL" : "DRAFT";
    groups.set(key, current);
  });
  return Array.from(groups.values()).sort((left, right) => {
    if (left.status !== right.status) {
      return left.status === "CONFIRMED" ? -1 : 1;
    }
    return right.rules - left.rules;
  });
}

function activityKey(draft: PromotionRuleDraft) {
  const ruleId = draft.rule.ruleId || "";
  const source = draft.sourceImportId || "";
  if (ruleId.startsWith("audit-personalized-fixed-") || source.includes("g7")) return "g7-single-item";
  if (ruleId.includes("bundle-abv2") || ruleId.includes("abv2-h2") || source.includes("exchange-purchase")) return "fuel-exchange";
  if (ruleId.startsWith("fixed-") || source.includes("99-zone")) return "g3-99-zone";
  if (ruleId.startsWith("abv2-a5") || source.includes("a5-recharge")) return "a5-day10-recharge";
  if (source.includes("small-recharge")) return "a6-small-recharge";
  if (ruleId.startsWith("abv2-g6")) return "g6-personalized";
  if (ruleId.startsWith("abv2-g5")) return "g5-composite";
  if (ruleId.startsWith("abv2-g4")) return "g4-event";
  if (ruleId.startsWith("abv2-g1") || ruleId.startsWith("abv2-g2") || ruleId.startsWith("abv2-a3")) return "storewide-days";
  if (ruleId.startsWith("abv2-a4")) return "a4-cn98";
  if (ruleId.startsWith("abv2-e2")) return "e2-case-coupon";
  if (ruleId.startsWith("abv2-a1") || ruleId.startsWith("abv2-e1") || ruleId.startsWith("abv2-f1")) return "fuel-gift";
  return source || draft.sourceSheetName || "other";
}

function activityName(draft: PromotionRuleDraft) {
  const key = activityKey(draft);
  const labels: Record<string, string> = {
    "g7-single-item": "G7 单品促销",
    "fuel-exchange": "加油换购",
    "g3-99-zone": "9.9 元专区",
    "a5-day10-recharge": "逢 10 超级十惠充值",
    "a6-small-recharge": "小额充值 666 赠券",
    "g6-personalized": "个性化赠品/会员价",
    "g5-composite": "组合叠加促销",
    "g4-event": "节庆夜间活动",
    "storewide-days": "逢 7/9/加气站折扣",
    "a4-cn98": "CN98 加油优惠",
    "e2-case-coupon": "整箱购赠券",
    "fuel-gift": "油品消费赠品"
  };
  return labels[key] || draft.sourceImportId || draft.sourceSheetName || draft.rule.activityName || "其他活动";
}

function StatCard({ icon, label, value, detail }: { icon: ReactNode; label: string; value: string; detail?: string }) {
  return (
    <div className="stat-card">
      <Space>
        {icon}
        <Typography.Text>{label}</Typography.Text>
      </Space>
      <div className="stat-value">{value}</div>
      <Typography.Text type="secondary">{detail || "后端接口返回"}</Typography.Text>
    </div>
  );
}

function CoverageLine({ label, percent }: { label: string; percent: number }) {
  return (
    <div>
      <Space className="panel-toolbar">
        <Typography.Text>{label}</Typography.Text>
        <Typography.Text strong>{percent}%</Typography.Text>
      </Space>
      <Progress percent={percent} strokeColor="#003F88" />
    </div>
  );
}

function statusTag(value: string) {
  if (value === "CONFIRMED") return <Tag color="green">CONFIRMED</Tag>;
  if (value === "PARTIAL") return <Tag color="gold">PARTIAL</Tag>;
  return <Tag>{value}</Tag>;
}

function severityTag(value: InventoryAlert["severity"]) {
  const colors: Record<InventoryAlert["severity"], string> = {
    LOW: "gold",
    CRITICAL: "orange",
    OUT_OF_STOCK: "red",
    NO_STATION_STOCK: "default"
  };
  return <Tag color={colors[value]}>{value}</Tag>;
}

function countRules(drafts: PromotionRuleDraft[], status: string) {
  return drafts.filter((draft) => draft.status === status || draft.rule.status === status).length;
}

function ruleTypeLabel(value: string) {
  const labels: Record<string, string> = {
    FIXED_PRICE: "固定价",
    AMOUNT_OFF: "满减",
    GIFT_ITEM: "赠品",
    GIFT_COUPON: "赠券",
    BUNDLE_PRICE: "组合价",
    EXCHANGE_PURCHASE: "换购",
    COUPON_REDEEM: "券核销",
    FUEL_VOLUME_DISCOUNT: "油品优惠",
    PERCENTAGE_DISCOUNT: "折扣",
    COMPOSITE: "组合"
  };
  return labels[value] || value;
}

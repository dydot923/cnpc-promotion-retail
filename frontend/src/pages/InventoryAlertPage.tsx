import {
  CheckCircleOutlined,
  DownloadOutlined,
  InboxOutlined,
  PlusOutlined,
  ReloadOutlined,
  UnorderedListOutlined
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Input, InputNumber, Modal, Select, Space, Table, Tabs, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMemo, useState } from "react";
import type { ReactNode } from "react";
import {
  fetchInventoryAlerts,
  fetchInventoryItems,
  markInventoryAlertHandled,
  replenishInventory
} from "../api/inventory";
import { createReplenishmentList, exportReplenishmentList } from "../api/replenishment";
import type { InventoryAlert, InventoryItem, InventoryStockStatus } from "../types";

const severityOptions = [
  { value: "", label: "全部预警" },
  { value: "OUT_OF_STOCK", label: "红色 缺货" },
  { value: "CRITICAL", label: "橙色 低库存" },
  { value: "LOW", label: "黄色 关注" },
  { value: "NO_STATION_STOCK", label: "灰色 本站无库存" }
];

const activityOptions = [
  { value: "", label: "全部活动" },
  { value: "FIXED_PRICE", label: "9.9 专区" },
  { value: "EXCHANGE_PURCHASE", label: "加油换购" },
  { value: "GIFT_ITEM", label: "买赠活动" },
  { value: "GIFT_COUPON", label: "赠券活动" }
];

const stockStatusOptions = [
  { value: "", label: "全部库存" },
  { value: "OUT_OF_STOCK", label: "缺货" },
  { value: "CRITICAL", label: "紧急补货" },
  { value: "LOW", label: "低库存" },
  { value: "NORMAL", label: "库存正常" }
];

const severityOrder: Record<InventoryAlert["severity"], number> = {
  OUT_OF_STOCK: 1,
  CRITICAL: 2,
  LOW: 3,
  NO_STATION_STOCK: 4
};

export default function InventoryAlertPage() {
  const [activeTab, setActiveTab] = useState("inventory");
  const [inventoryKeyword, setInventoryKeyword] = useState("");
  const [stockStatus, setStockStatus] = useState("");
  const [severity, setSeverity] = useState("");
  const [activityType, setActivityType] = useState("");
  const [keyword, setKeyword] = useState("");
  const [latestListId, setLatestListId] = useState<string>();
  const [replenishTarget, setReplenishTarget] = useState<ReplenishTarget>();
  const [replenishQuantity, setReplenishQuantity] = useState(0);
  const [replenishNote, setReplenishNote] = useState("到货入库");
  const [api, contextHolder] = message.useMessage();
  const queryClient = useQueryClient();

  const inventoryQuery = useQuery({ queryKey: ["inventory-items"], queryFn: fetchInventoryItems });
  const alertsQuery = useQuery({ queryKey: ["inventory-alerts"], queryFn: fetchInventoryAlerts });
  const createMutation = useMutation({
    mutationFn: createReplenishmentList,
    onSuccess: (list) => {
      setLatestListId(list.listId);
      api.success(`已生成补货清单 ${list.listId}`);
    }
  });
  const exportMutation = useMutation({
    mutationFn: async () => {
      const listId = latestListId || (await createReplenishmentList()).listId;
      const blob = await exportReplenishmentList(listId);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `${listId}.csv`;
      anchor.click();
      URL.revokeObjectURL(url);
      return listId;
    },
    onSuccess: (listId) => {
      setLatestListId(listId);
      api.success("补货清单已开始下载");
    }
  });

  const handleMutation = useMutation({
    mutationFn: (alert: InventoryAlert) =>
      markInventoryAlertHandled(alert.alertId, {
        operatorId: "stock-manager",
        note: "handled from inventory alert page"
      }),
    onSuccess: () => {
      api.success("预警已标记处理");
      queryClient.invalidateQueries({ queryKey: ["inventory-alerts"] });
    }
  });

  const replenishMutation = useMutation({
    mutationFn: () => {
      if (!replenishTarget) {
        throw new Error("请选择需要补货的商品");
      }
      return replenishInventory(replenishTarget.productCode, {
        quantity: replenishQuantity,
        operatorId: "stock-manager",
        note: replenishNote
      });
    },
    onSuccess: (result) => {
      api.success(`${result.productName} 已补货 ${formatQuantity(result.replenishedQuantity)}，当前库存 ${formatQuantity(result.quantityAfter)}`);
      setReplenishTarget(undefined);
      queryClient.invalidateQueries({ queryKey: ["inventory-items"] });
      queryClient.invalidateQueries({ queryKey: ["inventory-alerts"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard-inventory-alerts"] });
    },
    onError: (error) => api.error(error instanceof Error ? error.message : "补货失败")
  });

  const inventoryItems = useMemo(() => {
    const lowerKeyword = inventoryKeyword.trim().toLowerCase();
    return (inventoryQuery.data || [])
      .filter((item) => !stockStatus || item.stockStatus === stockStatus)
      .filter((item) => {
        if (!lowerKeyword) {
          return true;
        }
        return [item.productName, item.productCode, item.barcode, item.category]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(lowerKeyword));
      });
  }, [inventoryKeyword, inventoryQuery.data, stockStatus]);

  const inventoryCounts = useMemo(() => {
    const items = inventoryQuery.data || [];
    return {
      total: items.length,
      normal: items.filter((item) => item.stockStatus === "NORMAL").length,
      low: items.filter((item) => item.stockStatus === "LOW").length,
      critical: items.filter((item) => item.stockStatus === "CRITICAL").length,
      out: items.filter((item) => item.stockStatus === "OUT_OF_STOCK").length
    };
  }, [inventoryQuery.data]);

  const alerts = useMemo(() => {
    const lowerKeyword = keyword.trim().toLowerCase();
    return (alertsQuery.data || [])
      .filter((alert) => !severity || alert.severity === severity)
      .filter((alert) => !activityType || alert.relatedRuleType === activityType)
      .filter((alert) => {
        if (!lowerKeyword) {
          return true;
        }
        return [alert.productName, alert.productCode, alert.barcode, alert.category]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(lowerKeyword));
      })
      .sort((a, b) => severityOrder[a.severity] - severityOrder[b.severity]);
  }, [activityType, alertsQuery.data, keyword, severity]);

  const counts = useMemo(
    () => ({
      out: alerts.filter((alert) => alert.severity === "OUT_OF_STOCK").length,
      critical: alerts.filter((alert) => alert.severity === "CRITICAL").length,
      low: alerts.filter((alert) => alert.severity === "LOW").length,
      noStock: alerts.filter((alert) => alert.severity === "NO_STATION_STOCK").length
    }),
    [alerts]
  );

  function openReplenishment(target: ReplenishTarget) {
    setReplenishTarget(target);
    setReplenishQuantity(Math.max(Number(target.suggestedReplenishmentQuantity || 0), 1));
    setReplenishNote("到货入库");
  }

  const inventoryColumns: ColumnsType<InventoryItem> = [
    { title: "库存状态", dataIndex: "stockStatus", width: 110, render: stockStatusTag },
    {
      title: "商品",
      width: 280,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.productName || record.productCode}</Typography.Text>
          <Typography.Text type="secondary">编码 {record.productCode}</Typography.Text>
        </Space>
      )
    },
    { title: "条码", dataIndex: "barcode", width: 150, render: (value) => value || "-" },
    { title: "分类", dataIndex: "category", width: 110, render: (value) => value || "未分类" },
    {
      title: "当前库存",
      dataIndex: "currentQuantity",
      width: 110,
      render: (value) => <strong className={Number(value) < 10 ? "inventory-quantity low" : "inventory-quantity"}>{formatQuantity(value)}</strong>
    },
    { title: "安全库存", dataIndex: "safetyStock", width: 105, render: formatQuantity },
    { title: "建议补货", dataIndex: "suggestedReplenishmentQuantity", width: 110, render: formatQuantity },
    {
      title: "操作",
      width: 100,
      fixed: "right",
      render: (_, record) => (
        <Button type="primary" size="small" icon={<PlusOutlined />} onClick={() => openReplenishment(record)}>
          补货
        </Button>
      )
    }
  ];

  const columns: ColumnsType<InventoryAlert> = [
    { title: "预警", dataIndex: "severity", width: 132, render: severityTag },
    { title: "状态", dataIndex: "status", width: 150, render: statusTag },
    {
      title: "商品名称",
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.productName || record.productCode}</Typography.Text>
          <Typography.Text type="secondary">{record.productCode}</Typography.Text>
        </Space>
      )
    },
    { title: "商品码", dataIndex: "productCode", width: 130 },
    { title: "条码", dataIndex: "barcode", width: 150 },
    { title: "当前", dataIndex: "currentQuantity", width: 90 },
    { title: "阈值", dataIndex: "threshold", width: 90 },
    { title: "建议补货", dataIndex: "suggestedReplenishmentQuantity", width: 110 },
    {
      title: "关联活动",
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text>{record.relatedRuleId}</Typography.Text>
          <Typography.Text type="secondary">{record.relatedRuleType}</Typography.Text>
        </Space>
      )
    },
    { title: "原因", dataIndex: "reason" },
    {
      title: "处理",
      width: 220,
      fixed: "right",
      render: (_, record) => (
        <Space size={6}>
          <Button
            type="primary"
            size="small"
            icon={<PlusOutlined />}
            onClick={() => openReplenishment(record)}
          >
            按建议补货
          </Button>
          <Button
            size="small"
            icon={<CheckCircleOutlined />}
            disabled={record.status === "HANDLED"}
            loading={handleMutation.isPending}
            onClick={() => handleMutation.mutate(record)}
          >
            标记处理
          </Button>
        </Space>
      )
    }
  ];

  return (
    <>
      {contextHolder}
      <div className="page-header">
        <div>
          <Typography.Title level={1}>库存预警</Typography.Title>
          <Typography.Text className="page-subtitle">
            库存表商品、低库存预警与补货入库
          </Typography.Text>
        </div>
        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: ["inventory-items"] });
              queryClient.invalidateQueries({ queryKey: ["inventory-alerts"] });
            }}
          >
            刷新
          </Button>
          <Button className="blue-button" icon={<DownloadOutlined />} loading={exportMutation.isPending} onClick={() => exportMutation.mutate()}>
            导出 Excel
          </Button>
          <Button type="primary" icon={<UnorderedListOutlined />} loading={createMutation.isPending} onClick={() => createMutation.mutate()}>
            生成补货清单
          </Button>
        </Space>
      </div>

      <section className="panel">
        <div className="inventory-summary-strip">
          <InventorySummary label="库存商品" value={inventoryCounts.total} icon={<InboxOutlined />} />
          <InventorySummary label="库存正常" value={inventoryCounts.normal} tone="normal" />
          <InventorySummary label="低库存" value={inventoryCounts.low + inventoryCounts.critical} tone="warning" />
          <InventorySummary label="缺货" value={inventoryCounts.out} tone="danger" />
        </div>
        {latestListId ? (
          <Alert type="success" showIcon message="补货清单已生成" description={latestListId} className="result-alert" />
        ) : null}
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={[
            {
              key: "inventory",
              label: `库存总览 ${inventoryCounts.total}`,
              children: (
                <div className="inventory-tab-content">
                  <Space className="panel-toolbar" wrap>
                    <Space wrap>
                      <Select value={stockStatus} onChange={setStockStatus} options={stockStatusOptions} style={{ width: 160 }} />
                      <Input.Search
                        value={inventoryKeyword}
                        onChange={(event) => setInventoryKeyword(event.target.value)}
                        placeholder="搜索商品名称、编码或条码"
                        style={{ width: 300 }}
                      />
                    </Space>
                    <Typography.Text type="secondary">显示 {inventoryItems.length} 条</Typography.Text>
                  </Space>
                  {inventoryQuery.error ? (
                    <Alert type="warning" showIcon message="库存数据接口暂不可用" description={inventoryQuery.error.message} />
                  ) : (
                    <Table<InventoryItem>
                      rowKey="productCode"
                      size="small"
                      loading={inventoryQuery.isLoading}
                      columns={inventoryColumns}
                      dataSource={inventoryItems}
                      pagination={{ pageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100] }}
                      scroll={{ x: 1100 }}
                      locale={{ emptyText: <Empty description="没有匹配的库存商品" /> }}
                    />
                  )}
                </div>
              )
            },
            {
              key: "alerts",
              label: `促销预警 ${alertsQuery.data?.length || 0}`,
              children: (
                <div className="inventory-tab-content">
                  <Space className="panel-toolbar" wrap>
                    <Space wrap>
                      <Select value={severity} onChange={setSeverity} options={severityOptions} style={{ width: 160 }} />
                      <Select value={activityType} onChange={setActivityType} options={activityOptions} style={{ width: 170 }} />
                      <Input.Search value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索商品/条码" style={{ width: 240 }} />
                    </Space>
                    <Space wrap>
                      <Tag color="red">缺货 {counts.out}</Tag>
                      <Tag color="orange">低库存 {counts.critical}</Tag>
                      <Tag color="gold">关注 {counts.low}</Tag>
                      <Tag>本站无库存 {counts.noStock}</Tag>
                    </Space>
                  </Space>
                  {alertsQuery.error ? (
                    <Alert type="warning" showIcon message="库存预警接口暂不可用" description={alertsQuery.error.message} />
                  ) : (
                    <Table<InventoryAlert>
                      rowKey="alertId"
                      size="small"
                      loading={alertsQuery.isLoading}
                      columns={columns}
                      dataSource={alerts}
                      pagination={{ pageSize: 12 }}
                      scroll={{ x: 1800 }}
                      locale={{ emptyText: <Empty description="暂无库存预警" /> }}
                    />
                  )}
                </div>
              )
            }
          ]}
        />
      </section>

      <Modal
        title="商品补货入库"
        open={Boolean(replenishTarget)}
        okText={`确认补货 ${formatQuantity(replenishQuantity)}`}
        cancelText="取消"
        confirmLoading={replenishMutation.isPending}
        okButtonProps={{ disabled: replenishQuantity <= 0 }}
        onCancel={() => setReplenishTarget(undefined)}
        onOk={() => replenishMutation.mutate()}
      >
        {replenishTarget ? (
          <div className="replenishment-dialog">
            <div className="replenishment-product">
              <strong>{replenishTarget.productName}</strong>
              <span>商品编码 {replenishTarget.productCode}</span>
            </div>
            <div className="replenishment-quantity-preview">
              <span>当前库存<strong>{formatQuantity(replenishTarget.currentQuantity)}</strong></span>
              <span>本次入库<strong>+{formatQuantity(replenishQuantity)}</strong></span>
              <span>补货后<strong>{formatQuantity(replenishTarget.currentQuantity + replenishQuantity)}</strong></span>
            </div>
            <label className="replenishment-field">
              <span>补货数量</span>
              <InputNumber
                min={0.01}
                precision={2}
                value={replenishQuantity}
                onChange={(value) => setReplenishQuantity(Number(value || 0))}
                className="full-width"
              />
            </label>
            <label className="replenishment-field">
              <span>入库备注</span>
              <Input value={replenishNote} onChange={(event) => setReplenishNote(event.target.value)} />
            </label>
          </div>
        ) : null}
      </Modal>
    </>
  );
}

type ReplenishTarget = Pick<
  InventoryItem,
  "productCode" | "productName" | "currentQuantity" | "suggestedReplenishmentQuantity"
>;

function InventorySummary({
  label,
  value,
  icon,
  tone = "default"
}: {
  label: string;
  value: number;
  icon?: ReactNode;
  tone?: "default" | "normal" | "warning" | "danger";
}) {
  return (
    <div className={`inventory-summary-item ${tone}`}>
      {icon ? <span className="inventory-summary-icon">{icon}</span> : null}
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function stockStatusTag(value: InventoryStockStatus) {
  const colors: Record<InventoryStockStatus, string> = {
    NORMAL: "green",
    LOW: "gold",
    CRITICAL: "orange",
    OUT_OF_STOCK: "red"
  };
  const labels: Record<InventoryStockStatus, string> = {
    NORMAL: "库存正常",
    LOW: "低库存",
    CRITICAL: "紧急补货",
    OUT_OF_STOCK: "缺货"
  };
  return <Tag color={colors[value]}>{labels[value]}</Tag>;
}

function formatQuantity(value: number) {
  const quantity = Number(value || 0);
  return Number.isInteger(quantity) ? String(quantity) : quantity.toFixed(2);
}

function severityTag(value: InventoryAlert["severity"]) {
  const colors: Record<InventoryAlert["severity"], string> = {
    LOW: "gold",
    CRITICAL: "orange",
    OUT_OF_STOCK: "red",
    NO_STATION_STOCK: "default"
  };
  const labels: Record<InventoryAlert["severity"], string> = {
    LOW: "黄色关注",
    CRITICAL: "橙色低库存",
    OUT_OF_STOCK: "红色缺货",
    NO_STATION_STOCK: "灰色无库存"
  };
  return <Tag color={colors[value]}>{labels[value]}</Tag>;
}

function statusTag(value: InventoryAlert["status"]) {
  const colors: Record<InventoryAlert["status"], string> = {
    OPEN: "blue",
    REPLENISHMENT_CREATED: "purple",
    HANDLED: "green"
  };
  const labels: Record<InventoryAlert["status"], string> = {
    OPEN: "待处理",
    REPLENISHMENT_CREATED: "已生成清单",
    HANDLED: "已处理"
  };
  return <Tag color={colors[value] || "default"}>{labels[value] || value}</Tag>;
}

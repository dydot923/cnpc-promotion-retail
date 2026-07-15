import { CheckCircleOutlined, DownloadOutlined, ReloadOutlined, UnorderedListOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Input, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMemo, useState } from "react";
import { fetchInventoryAlerts, markInventoryAlertHandled } from "../api/inventory";
import { createReplenishmentList, exportReplenishmentList } from "../api/replenishment";
import type { InventoryAlert } from "../types";

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

const severityOrder: Record<InventoryAlert["severity"], number> = {
  OUT_OF_STOCK: 1,
  CRITICAL: 2,
  LOW: 3,
  NO_STATION_STOCK: 4
};

export default function InventoryAlertPage() {
  const [severity, setSeverity] = useState("");
  const [activityType, setActivityType] = useState("");
  const [keyword, setKeyword] = useState("");
  const [latestListId, setLatestListId] = useState<string>();
  const [api, contextHolder] = message.useMessage();
  const queryClient = useQueryClient();

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
      width: 130,
      fixed: "right",
      render: (_, record) => (
        <Button
          size="small"
          icon={<CheckCircleOutlined />}
          disabled={record.status === "HANDLED"}
          loading={handleMutation.isPending}
          onClick={() => handleMutation.mutate(record)}
        >
          标记处理
        </Button>
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
            数据更新时间: {alertsQuery.dataUpdatedAt ? new Date(alertsQuery.dataUpdatedAt).toLocaleString("zh-CN") : "—"}
          </Typography.Text>
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ["inventory-alerts"] })}>
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
        <Space className="panel-toolbar" wrap>
          <Space wrap>
            <Select value={severity} onChange={setSeverity} options={severityOptions} style={{ width: 160 }} />
            <Select value={activityType} onChange={setActivityType} options={activityOptions} style={{ width: 170 }} />
            <Input.Search value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索商品/条码" style={{ width: 240 }} />
          </Space>
          <Space>
            <Tag color="red">缺货 {counts.out}</Tag>
            <Tag color="orange">低库存 {counts.critical}</Tag>
            <Tag color="gold">关注 {counts.low}</Tag>
            <Tag>本站无库存 {counts.noStock}</Tag>
          </Space>
        </Space>
        {latestListId ? (
          <Alert type="success" showIcon message="补货清单已生成" description={latestListId} className="result-alert" />
        ) : null}
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
            scroll={{ x: 1700 }}
            locale={{ emptyText: <Empty description="暂无库存预警" /> }}
          />
        )}
      </section>
    </>
  );
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

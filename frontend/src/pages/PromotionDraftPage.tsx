import { CheckOutlined, StopOutlined, CloseOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Col, Empty, Row, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useState } from "react";
import {
  confirmDraft,
  disableRule,
  fetchAuditLogs,
  fetchPromotionDrafts,
  rejectDraft
} from "../api/promotionManagement";
import type { PromotionRuleAuditLog, PromotionRuleDraft } from "../types";

const statuses = ["PENDING_CONFIRMATION", "CONFIRMED", "REJECTED", "DISABLED"];

export default function PromotionDraftPage() {
  const [status, setStatus] = useState("PENDING_CONFIRMATION");
  const [selectedRuleId, setSelectedRuleId] = useState<string>();
  const queryClient = useQueryClient();
  const [api, contextHolder] = message.useMessage();

  const draftsQuery = useQuery({
    queryKey: ["promotion-drafts", status],
    queryFn: () => fetchPromotionDrafts(status)
  });

  const auditQuery = useQuery({
    queryKey: ["promotion-audit", selectedRuleId],
    queryFn: () => fetchAuditLogs(selectedRuleId || ""),
    enabled: Boolean(selectedRuleId)
  });

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["promotion-drafts"] });
    await queryClient.invalidateQueries({ queryKey: ["promotion-audit"] });
  };

  const confirmMutation = useMutation({
    mutationFn: confirmDraft,
    onSuccess: async (version) => {
      api.success(`已确认，版本 ${version.versionId}`);
      await refresh();
    }
  });
  const rejectMutation = useMutation({
    mutationFn: rejectDraft,
    onSuccess: async () => {
      api.success("已拒绝");
      await refresh();
    }
  });
  const disableMutation = useMutation({
    mutationFn: disableRule,
    onSuccess: async (version) => {
      api.success(`已停用，版本 ${version.versionId}`);
      await refresh();
    }
  });

  const columns: ColumnsType<PromotionRuleDraft> = [
    {
      title: "规则",
      dataIndex: "draftId",
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.rule.activityName}</Typography.Text>
          <Typography.Text type="secondary">{record.draftId}</Typography.Text>
        </Space>
      )
    },
    { title: "类型", dataIndex: ["rule", "ruleType"], width: 130 },
    {
      title: "商品编码",
      width: 160,
      render: (_, record) => formatList(record.rule.condition.productCodes)
    },
    {
      title: "关键优惠",
      width: 160,
      render: (_, record) => benefitText(record)
    },
    {
      title: "来源",
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <span>{record.sourceImportId}</span>
          <Typography.Text type="secondary">
            {record.sourceSheetName} / 行 {record.sourceRowNumber}
          </Typography.Text>
        </Space>
      )
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 150,
      render: (value: string) => <Tag color={statusColor(value)}>{value}</Tag>
    },
    { title: "规则版本", dataIndex: ["rule", "version"], width: 220 },
    {
      title: "操作",
      width: 260,
      render: (_, record) => (
        <Space wrap>
          <Button size="small" onClick={() => setSelectedRuleId(record.rule.ruleId)}>
            audit
          </Button>
          <Button
            size="small"
            type="primary"
            icon={<CheckOutlined />}
            disabled={record.status !== "PENDING_CONFIRMATION"}
            loading={confirmMutation.isPending}
            onClick={() => confirmMutation.mutate(record.draftId)}
          >
            确认
          </Button>
          <Button
            size="small"
            icon={<CloseOutlined />}
            disabled={record.status !== "PENDING_CONFIRMATION"}
            loading={rejectMutation.isPending}
            onClick={() => rejectMutation.mutate(record.draftId)}
          >
            拒绝
          </Button>
          <Button
            size="small"
            danger
            icon={<StopOutlined />}
            disabled={record.status !== "CONFIRMED"}
            loading={disableMutation.isPending}
            onClick={() => disableMutation.mutate(record.rule.ruleId)}
          >
            停用
          </Button>
        </Space>
      )
    }
  ];

  const auditColumns: ColumnsType<PromotionRuleAuditLog> = [
    { title: "动作", dataIndex: "action", width: 120 },
    { title: "之前", dataIndex: "statusBefore", width: 150 },
    { title: "之后", dataIndex: "statusAfter", width: 150 },
    { title: "操作人", dataIndex: "operatorId", width: 150 },
    { title: "原因", dataIndex: "changeReason" },
    { title: "时间", dataIndex: "createdAt", width: 220 }
  ];

  return (
    <Row gutter={[16, 16]}>
      {contextHolder}
      <Col span={24}>
        <section className="panel">
          <Space className="panel-toolbar">
            <div>
              <Typography.Title level={5}>规则确认</Typography.Title>
              <Typography.Text type="secondary">只有 CONFIRMED 规则参与结算。</Typography.Text>
            </div>
            <Select value={status} onChange={setStatus} options={statuses.map((value) => ({ value, label: value }))} />
          </Space>
          {draftsQuery.error ? (
            <Alert type="error" showIcon message={draftsQuery.error.message} />
          ) : (
            <Table<PromotionRuleDraft>
              rowKey="draftId"
              size="small"
              loading={draftsQuery.isLoading}
              columns={columns}
              dataSource={draftsQuery.data || []}
              pagination={{ pageSize: 8 }}
              scroll={{ x: 1520 }}
              locale={{ emptyText: <Empty description="暂无规则草稿" /> }}
            />
          )}
        </section>
      </Col>
      <Col span={24}>
        <section className="panel">
          <Typography.Title level={5}>Audit Log</Typography.Title>
          {selectedRuleId ? (
            <Table<PromotionRuleAuditLog>
              rowKey="auditId"
              size="small"
              loading={auditQuery.isLoading}
              columns={auditColumns}
              dataSource={auditQuery.data || []}
              pagination={false}
              locale={{ emptyText: "暂无审计记录" }}
            />
          ) : (
            <Empty description="选择一条规则查看 audit log" />
          )}
        </section>
      </Col>
    </Row>
  );
}

function benefitText(record: PromotionRuleDraft) {
  const benefit = record.rule.benefit;
  if (record.rule.ruleType === "FIXED_PRICE") return `固定价 ${benefit.fixedPrice}`;
  if (record.rule.ruleType === "AMOUNT_OFF") return `满减 ${benefit.amountOff}`;
  if (record.rule.ruleType === "GIFT_ITEM") return `${benefit.giftItemName || benefit.giftItemCode} x ${benefit.giftItemQuantity}`;
  if (record.rule.ruleType === "GIFT_COUPON") return `${benefit.giftCouponName} ${benefit.giftCouponAmount}`;
  if (record.rule.ruleType === "BUNDLE_PRICE") return `组合价 ${benefit.bundlePrice}`;
  return record.rule.ruleType;
}

function statusColor(status: string) {
  if (status === "CONFIRMED") return "green";
  if (status === "PENDING_CONFIRMATION") return "gold";
  if (status === "DISABLED") return "red";
  return "default";
}

function formatList(value: unknown) {
  if (Array.isArray(value)) {
    return value.length > 0 ? value.join(", ") : "-";
  }
  if (typeof value === "string" && value.trim()) {
    return value;
  }
  return "-";
}

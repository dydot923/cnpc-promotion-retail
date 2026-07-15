import { CheckOutlined, EyeOutlined, StopOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Drawer, Empty, Input, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMemo, useState } from "react";
import { confirmRuleDraft, deprecateRule, fetchRuleAuditLogs, fetchRuleDrafts } from "../api/rules";
import type { PromotionRuleAuditLog, PromotionRuleDraft } from "../types";

const statusOptions = [
  { value: "", label: "全部状态" },
  { value: "PENDING_CONFIRMATION", label: "DRAFT" },
  { value: "CONFIRMED", label: "CONFIRMED" },
  { value: "DISABLED", label: "DEPRECATED" }
];

const ruleTypeOptions = [
  { value: "", label: "全部类型" },
  { value: "FIXED_PRICE", label: "固定价" },
  { value: "AMOUNT_OFF", label: "满减" },
  { value: "GIFT_ITEM", label: "买赠" },
  { value: "GIFT_COUPON", label: "赠券" },
  { value: "BUNDLE_PRICE", label: "组合包" },
  { value: "EXCHANGE_PURCHASE", label: "换购" }
];

export default function RuleManagementPage() {
  const [status, setStatus] = useState("");
  const [ruleType, setRuleType] = useState("");
  const [keyword, setKeyword] = useState("");
  const [selectedDraft, setSelectedDraft] = useState<PromotionRuleDraft>();
  const [api, contextHolder] = message.useMessage();
  const queryClient = useQueryClient();

  const draftsQuery = useQuery({
    queryKey: ["rule-management-drafts", status],
    queryFn: () => fetchRuleDrafts(status || undefined)
  });

  const auditQuery = useQuery({
    queryKey: ["rule-management-audit", selectedDraft?.rule.ruleId],
    queryFn: () => fetchRuleAuditLogs(selectedDraft?.rule.ruleId || ""),
    enabled: Boolean(selectedDraft?.rule.ruleId)
  });

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ["rule-management-drafts"] });
    await queryClient.invalidateQueries({ queryKey: ["rule-management-audit"] });
  };

  const confirmMutation = useMutation({
    mutationFn: confirmRuleDraft,
    onSuccess: async () => {
      api.success("规则已确认");
      await refresh();
    }
  });

  const deprecateMutation = useMutation({
    mutationFn: deprecateRule,
    onSuccess: async () => {
      api.success("规则已停用");
      await refresh();
    }
  });

  const rows = useMemo(() => {
    const lowerKeyword = keyword.trim().toLowerCase();
    return (draftsQuery.data || [])
      .filter((draft) => !ruleType || draft.rule.ruleType === ruleType)
      .filter((draft) => {
        if (!lowerKeyword) {
          return true;
        }
        return [draft.draftId, draft.rule.ruleId, draft.rule.activityName, draft.sourceImportId]
          .filter(Boolean)
          .some((value) => String(value).toLowerCase().includes(lowerKeyword));
      });
  }, [draftsQuery.data, keyword, ruleType]);

  const stats = useMemo(
    () => ({
      confirmed: rows.filter((row) => row.status === "CONFIRMED" || row.rule.status === "CONFIRMED").length,
      draft: rows.filter((row) => row.status === "PENDING_CONFIRMATION").length,
      deprecated: rows.filter((row) => row.status === "DISABLED" || row.rule.status === "DEPRECATED").length
    }),
    [rows]
  );

  const columns: ColumnsType<PromotionRuleDraft> = [
    {
      title: "规则ID",
      dataIndex: ["rule", "ruleId"],
      width: 170
    },
    {
      title: "规则名称",
      width: 260,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.rule.activityName}</Typography.Text>
          <Typography.Text type="secondary">{record.draftId}</Typography.Text>
        </Space>
      )
    },
    { title: "类型", dataIndex: ["rule", "ruleType"], width: 150 },
    { title: "状态", dataIndex: "status", width: 150, render: statusTag },
    { title: "优先级", dataIndex: ["rule", "priority"], width: 100 },
    { title: "互斥组", dataIndex: ["rule", "exclusiveGroup"], width: 140 },
    {
      title: "操作",
      width: 260,
      render: (_, record) => (
        <Space>
          <Button icon={<EyeOutlined />} onClick={() => setSelectedDraft(record)}>
            查看
          </Button>
          <Button
            type="primary"
            icon={<CheckOutlined />}
            disabled={record.status !== "PENDING_CONFIRMATION"}
            loading={confirmMutation.isPending}
            onClick={() => confirmMutation.mutate(record.draftId)}
          >
            确认
          </Button>
          <Button
            danger
            icon={<StopOutlined />}
            disabled={record.status !== "CONFIRMED" && record.rule.status !== "CONFIRMED"}
            loading={deprecateMutation.isPending}
            onClick={() => deprecateMutation.mutate(record.rule.ruleId)}
          >
            停用
          </Button>
        </Space>
      )
    }
  ];

  const auditColumns: ColumnsType<PromotionRuleAuditLog> = [
    { title: "动作", dataIndex: "action", width: 130 },
    { title: "之前", dataIndex: "statusBefore", width: 130 },
    { title: "之后", dataIndex: "statusAfter", width: 130 },
    { title: "操作人", dataIndex: "operatorId", width: 140 },
    { title: "原因", dataIndex: "changeReason" },
    { title: "时间", dataIndex: "createdAt", width: 190 }
  ];

  return (
    <>
      {contextHolder}
      <div className="page-header">
        <div>
          <Typography.Title level={1}>规则管理</Typography.Title>
          <Typography.Text className="page-subtitle">仅 CONFIRMED 规则参与结算；前端不直接编辑规则 JSON</Typography.Text>
        </div>
        <Space>
          <Tag color="blue">CONFIRMED {stats.confirmed}</Tag>
          <Tag color="orange">DRAFT {stats.draft}</Tag>
          <Tag>DEPRECATED {stats.deprecated}</Tag>
        </Space>
      </div>

      <section className="panel">
        <Space className="panel-toolbar" wrap>
          <Space wrap>
            <Select value={status} onChange={setStatus} options={statusOptions} style={{ width: 180 }} />
            <Select value={ruleType} onChange={setRuleType} options={ruleTypeOptions} style={{ width: 160 }} />
            <Input.Search value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索规则/活动/导入批次" style={{ width: 260 }} />
          </Space>
          <Typography.Text type="secondary">规则条件和动作只读展示，生命周期操作调用后端接口。</Typography.Text>
        </Space>
        {draftsQuery.error ? (
          <Alert type="warning" showIcon message="规则列表接口暂不可用" description={draftsQuery.error.message} />
        ) : (
          <Table<PromotionRuleDraft>
            rowKey="draftId"
            size="small"
            loading={draftsQuery.isLoading}
            columns={columns}
            dataSource={rows}
            pagination={{ pageSize: 12 }}
            scroll={{ x: 1230 }}
            locale={{ emptyText: <Empty description="暂无规则" /> }}
          />
        )}
      </section>

      <Drawer
        title="规则详情"
        open={Boolean(selectedDraft)}
        width={720}
        onClose={() => setSelectedDraft(undefined)}
        extra={
          selectedDraft ? (
            <Space>
              <Button
                type="primary"
                disabled={selectedDraft.status !== "PENDING_CONFIRMATION"}
                loading={confirmMutation.isPending}
                onClick={() => confirmMutation.mutate(selectedDraft.draftId)}
              >
                确认
              </Button>
              <Button
                danger
                disabled={selectedDraft.status !== "CONFIRMED" && selectedDraft.rule.status !== "CONFIRMED"}
                loading={deprecateMutation.isPending}
                onClick={() => deprecateMutation.mutate(selectedDraft.rule.ruleId)}
              >
                停用
              </Button>
            </Space>
          ) : null
        }
      >
        {selectedDraft ? (
          <Space direction="vertical" size={14} className="full-width">
            <Space wrap>
              <Tag color="blue">{selectedDraft.rule.ruleId}</Tag>
              {statusTag(selectedDraft.status)}
              <Tag color="gold">优先级 {selectedDraft.rule.priority}</Tag>
              <Tag>{selectedDraft.rule.exclusiveGroup || "无互斥组"}</Tag>
              <Tag>{selectedDraft.rule.stackable ? "可叠加" : "不可叠加"}</Tag>
            </Space>
            <div>
              <Typography.Title level={3} className="section-title">
                基础信息
              </Typography.Title>
              <Typography.Paragraph>
                {selectedDraft.rule.activityName} / {selectedDraft.rule.ruleType} / 版本 {selectedDraft.rule.version}
              </Typography.Paragraph>
              <Typography.Text type="secondary">
                来源：{selectedDraft.sourceImportId} / {selectedDraft.sourceSheetName} / 行 {selectedDraft.sourceRowNumber}
              </Typography.Text>
            </div>
            <div>
              <Typography.Title level={3} className="section-title">
                条件 JSON
              </Typography.Title>
              <pre className="json-view">{JSON.stringify(selectedDraft.rule.condition, null, 2)}</pre>
            </div>
            <div>
              <Typography.Title level={3} className="section-title">
                动作描述
              </Typography.Title>
              <pre className="json-view">{JSON.stringify(selectedDraft.rule.benefit, null, 2)}</pre>
            </div>
            <div>
              <Typography.Title level={3} className="section-title">
                审计日志
              </Typography.Title>
              <Table<PromotionRuleAuditLog>
                rowKey="auditId"
                size="small"
                loading={auditQuery.isLoading}
                columns={auditColumns}
                dataSource={auditQuery.data || []}
                pagination={false}
                locale={{ emptyText: "暂无审计记录" }}
              />
            </div>
          </Space>
        ) : null}
      </Drawer>
    </>
  );
}

function statusTag(value: string) {
  if (value === "CONFIRMED") {
    return <Tag color="blue">CONFIRMED</Tag>;
  }
  if (value === "PENDING_CONFIRMATION") {
    return <Tag color="orange">DRAFT</Tag>;
  }
  if (value === "DISABLED" || value === "DEPRECATED") {
    return <Tag>DEPRECATED</Tag>;
  }
  return <Tag>{value}</Tag>;
}

import { SearchOutlined } from "@ant-design/icons";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button, Col, Empty, Form, Input, InputNumber, Row, Select, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMemo, useState } from "react";
import { fetchAuditLogs, type AuditLogQuery } from "../api/audit";
import type { AuditLog } from "../types";

type QueryForm = {
  actionType?: string;
  entityType?: string;
  entityId?: string;
  operatorId?: string;
  limit?: number;
};

const actionOptions = [
  "CHECKOUT_CONFIRM",
  "PROMOTION_RULE_IMPORT",
  "PROMOTION_RULE_CONFIRM",
  "PROMOTION_RULE_REJECT",
  "PROMOTION_RULE_REVISE",
  "PROMOTION_RULE_DISABLE",
  "REPLENISHMENT_GENERATE",
  "REPLENISHMENT_EXPORT",
  "IMPORT_ERRORS_EXPORT"
].map((value) => ({ value, label: value }));

const entityOptions = [
  "CHECKOUT_CONFIRMATION",
  "PROMOTION_RULE",
  "REPLENISHMENT_LIST",
  "IMPORT_BATCH"
].map((value) => ({ value, label: value }));

export default function AuditLogPage() {
  const [form] = Form.useForm<QueryForm>();
  const [query, setQuery] = useState<AuditLogQuery>({ limit: 100 });
  const logsQuery = useQuery({
    queryKey: ["audit-logs", query],
    queryFn: () => fetchAuditLogs(query)
  });

  const columns = useMemo<ColumnsType<AuditLog>>(
    () => [
      { title: "时间", dataIndex: "operatedAt", width: 220 },
      { title: "动作", dataIndex: "actionType", width: 220, render: (value: string) => <Tag color="blue">{value}</Tag> },
      { title: "对象类型", dataIndex: "entityType", width: 190 },
      { title: "对象 ID", dataIndex: "entityId", width: 280 },
      { title: "操作人", width: 180, render: (_, record) => `${record.operatorName || "-"} / ${record.operatorId}` },
      { title: "原因", dataIndex: "reason", width: 260 }
    ],
    []
  );

  function search(values: QueryForm) {
    setQuery({
      actionType: values.actionType || undefined,
      entityType: values.entityType || undefined,
      entityId: values.entityId?.trim() || undefined,
      operatorId: values.operatorId?.trim() || undefined,
      limit: values.limit || 100
    });
  }

  return (
    <Row gutter={[16, 16]}>
      <Col span={24}>
        <section className="panel">
          <Space className="panel-toolbar" wrap>
            <div>
              <Typography.Title level={5}>通用审计日志</Typography.Title>
              <Typography.Text type="secondary">
                查询关键操作的后端审计记录，包括结算确认、规则治理、补货和导入异常导出。
              </Typography.Text>
            </div>
          </Space>
          <Form<QueryForm> form={form} layout="inline" initialValues={{ limit: 100 }} onFinish={search}>
            <Form.Item name="actionType">
              <Select allowClear className="audit-filter" placeholder="actionType" options={actionOptions} />
            </Form.Item>
            <Form.Item name="entityType">
              <Select allowClear className="audit-filter" placeholder="entityType" options={entityOptions} />
            </Form.Item>
            <Form.Item name="entityId">
              <Input className="audit-filter" placeholder="entityId" allowClear />
            </Form.Item>
            <Form.Item name="operatorId">
              <Input className="audit-filter" placeholder="operatorId" allowClear />
            </Form.Item>
            <Form.Item name="limit">
              <InputNumber min={1} max={500} precision={0} />
            </Form.Item>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={logsQuery.isFetching}>
              查询
            </Button>
          </Form>
        </section>
      </Col>

      <Col span={24}>
        <section className="panel">
          {logsQuery.error ? (
            <Alert type="error" showIcon message="审计日志查询失败" description={logsQuery.error.message} />
          ) : (
            <Table<AuditLog>
              rowKey="auditId"
              size="small"
              loading={logsQuery.isLoading || logsQuery.isFetching}
              columns={columns}
              dataSource={logsQuery.data || []}
              pagination={{ pageSize: 10 }}
              scroll={{ x: 1350 }}
              expandable={{
                expandedRowRender: (record) => (
                  <Row gutter={12}>
                    <Col xs={24} lg={12}>
                      <Typography.Text strong>beforeSnapshot</Typography.Text>
                      <pre className="json-view">{formatJson(record.beforeSnapshot)}</pre>
                    </Col>
                    <Col xs={24} lg={12}>
                      <Typography.Text strong>afterSnapshot</Typography.Text>
                      <pre className="json-view">{formatJson(record.afterSnapshot)}</pre>
                    </Col>
                  </Row>
                )
              }}
              locale={{ emptyText: <Empty description="暂无审计日志" /> }}
            />
          )}
        </section>
      </Col>
    </Row>
  );
}

function formatJson(value: unknown) {
  if (value == null) {
    return "-";
  }
  return JSON.stringify(value, null, 2);
}

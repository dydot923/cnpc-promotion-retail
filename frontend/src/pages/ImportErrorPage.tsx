import { DownloadOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Col, Empty, Row, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import { exportImportErrors, fetchImportBatches, fetchImportErrors } from "../api/promotionManagement";
import type { ImportBatch, ImportErrorRow } from "../types";

const severityOptions = [
  { value: "", label: "全部" },
  { value: "ERROR", label: "ERROR" },
  { value: "WARNING", label: "WARNING" },
  { value: "BLOCKER", label: "BLOCKER" }
];

export default function ImportErrorPage() {
  const [selectedImportId, setSelectedImportId] = useState<string>();
  const [severity, setSeverity] = useState("");
  const [sheetName, setSheetName] = useState("");
  const [errorCode, setErrorCode] = useState("");
  const [api, contextHolder] = message.useMessage();
  const batchesQuery = useQuery({ queryKey: ["import-batches"], queryFn: fetchImportBatches });

  const batchOptions = useMemo(
    () =>
      (batchesQuery.data || []).map((batch) => ({
        value: batch.importId.value,
        label: `${batch.importType} / ${batch.importId.value}`
      })),
    [batchesQuery.data]
  );

  useEffect(() => {
    if (!selectedImportId && batchOptions.length > 0) {
      setSelectedImportId(batchOptions[0].value);
    }
  }, [batchOptions, selectedImportId]);

  const errorsQuery = useQuery({
    queryKey: ["import-errors", selectedImportId, severity, sheetName, errorCode],
    queryFn: () =>
      fetchImportErrors(selectedImportId || "", severity || undefined, sheetName || undefined, errorCode || undefined),
    enabled: Boolean(selectedImportId)
  });
  const exportMutation = useMutation({
    mutationFn: async () => {
      if (!selectedImportId) {
        return;
      }
      const blob = await exportImportErrors(
        selectedImportId,
        severity || undefined,
        sheetName || undefined,
        errorCode || undefined
      );
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `${selectedImportId}-errors.csv`;
      anchor.click();
      URL.revokeObjectURL(url);
    },
    onSuccess: () => api.success("异常行 CSV 已开始下载")
  });

  const sheetOptions = useMemo(() => distinctOptions(errorsQuery.data?.map((row) => row.sheetName)), [errorsQuery.data]);
  const errorCodeOptions = useMemo(
    () => distinctOptions(errorsQuery.data?.map((row) => row.errorCode)),
    [errorsQuery.data]
  );

  const batchColumns: ColumnsType<ImportBatch> = [
    { title: "importId", dataIndex: ["importId", "value"], width: 260 },
    { title: "类型", dataIndex: "importType", width: 120 },
    { title: "新增", dataIndex: "insertedCount", width: 90 },
    { title: "跳过", dataIndex: "skippedCount", width: 90 },
    { title: "异常", dataIndex: "invalidCount", width: 90 },
    { title: "警告", dataIndex: "warningCount", width: 90 },
    { title: "文件", dataIndex: "sourceFile" }
  ];

  const errorColumns: ColumnsType<ImportErrorRow> = [
    { title: "severity", dataIndex: "severity", width: 110, render: severityTag },
    { title: "sheet", dataIndex: "sheetName", width: 220 },
    { title: "行号", dataIndex: "rowNumber", width: 90 },
    { title: "列", dataIndex: "columnName", width: 150 },
    { title: "rawValue", dataIndex: "rawValue", width: 160 },
    { title: "errorCode", dataIndex: "errorCode", width: 180 },
    { title: "message", dataIndex: "errorMessage" }
  ];

  return (
    <Row gutter={[16, 16]}>
      {contextHolder}
      <Col span={24}>
        <section className="panel">
          <Space className="panel-toolbar">
            <div>
              <Typography.Title level={5}>导入批次</Typography.Title>
              <Typography.Text type="secondary">这里只查看异常，不在页面中修正规则。</Typography.Text>
            </div>
          </Space>
          {batchesQuery.error ? (
            <Alert type="error" showIcon message={batchesQuery.error.message} />
          ) : (
            <Table<ImportBatch>
              rowKey={(record) => record.importId.value}
              size="small"
              loading={batchesQuery.isLoading}
              columns={batchColumns}
              dataSource={batchesQuery.data || []}
              pagination={{ pageSize: 5 }}
              scroll={{ x: 980 }}
              locale={{ emptyText: <Empty description="暂无导入批次" /> }}
            />
          )}
        </section>
      </Col>
      <Col span={24}>
        <section className="panel">
          <Space className="panel-toolbar" wrap>
            <Typography.Title level={5}>异常行</Typography.Title>
            <Space wrap>
              <Select
                className="import-select"
                placeholder="选择导入批次"
                value={selectedImportId}
                onChange={setSelectedImportId}
                options={batchOptions}
              />
              <Select value={severity} onChange={setSeverity} options={severityOptions} />
              <Select
                allowClear
                className="import-select"
                placeholder="sheet"
                value={sheetName || undefined}
                onChange={(value) => setSheetName(value || "")}
                options={sheetOptions}
              />
              <Select
                allowClear
                className="import-select"
                placeholder="errorCode"
                value={errorCode || undefined}
                onChange={(value) => setErrorCode(value || "")}
                options={errorCodeOptions}
              />
              <Button
                icon={<DownloadOutlined />}
                disabled={!selectedImportId}
                loading={exportMutation.isPending}
                onClick={() => exportMutation.mutate()}
              >
                导出 CSV
              </Button>
            </Space>
          </Space>
          {exportMutation.error ? (
            <Alert type="error" showIcon message="导出失败" description={exportMutation.error.message} />
          ) : null}
          {errorsQuery.error ? (
            <Alert type="error" showIcon message={errorsQuery.error.message} />
          ) : (
            <Table<ImportErrorRow>
              rowKey={(record) => `${record.importId.value}-${record.sheetName}-${record.rowNumber}-${record.errorCode}`}
              size="small"
              loading={errorsQuery.isLoading}
              columns={errorColumns}
              dataSource={errorsQuery.data || []}
              pagination={{ pageSize: 10 }}
              scroll={{ x: 1080 }}
              locale={{ emptyText: <Empty description="暂无异常行" /> }}
            />
          )}
        </section>
      </Col>
    </Row>
  );
}

function severityTag(value: string) {
  const color = value === "ERROR" ? "red" : value === "WARNING" ? "gold" : "purple";
  return <Tag color={color}>{value}</Tag>;
}

function distinctOptions(values?: string[]) {
  return Array.from(new Set((values || []).filter(Boolean))).map((value) => ({ value, label: value }));
}

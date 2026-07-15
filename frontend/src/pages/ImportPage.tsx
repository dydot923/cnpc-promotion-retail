import { CloudUploadOutlined, DownloadOutlined, ReloadOutlined } from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Empty, Progress, Radio, Space, Table, Tag, Typography, Upload, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { UploadProps } from "antd";
import { useEffect, useMemo, useState } from "react";
import { fetchImportBatches, fetchImportErrors, uploadImportFile } from "../api/importCenter";
import type { ImportBatch, ImportErrorRow } from "../types";

const importTypeOptions = [
  { value: "prices", label: "价格表" },
  { value: "inventory", label: "库存表" },
  { value: "promotions", label: "活动看板" },
  { value: "coupons", label: "券模板" }
] as const;

type ImportType = (typeof importTypeOptions)[number]["value"];

export default function ImportPage() {
  const [importType, setImportType] = useState<ImportType>("promotions");
  const [file, setFile] = useState<File>();
  const [progress, setProgress] = useState(0);
  const [selectedImportId, setSelectedImportId] = useState<string>();
  const [api, contextHolder] = message.useMessage();
  const queryClient = useQueryClient();

  const batchesQuery = useQuery({ queryKey: ["import-batches"], queryFn: fetchImportBatches });
  const errorsQuery = useQuery({
    queryKey: ["import-errors", selectedImportId],
    queryFn: () => fetchImportErrors(selectedImportId || ""),
    enabled: Boolean(selectedImportId)
  });

  const uploadMutation = useMutation({
    mutationFn: async () => {
      if (!file) {
        throw new Error("请选择 Excel 文件");
      }
      return uploadImportFile(importType, file);
    },
    onSuccess: async () => {
      setProgress(100);
      setFile(undefined);
      api.success("导入完成，历史记录已刷新");
      await queryClient.invalidateQueries({ queryKey: ["import-batches"] });
    }
  });

  useEffect(() => {
    if (!uploadMutation.isPending) {
      return;
    }
    setProgress(12);
    const timer = window.setInterval(() => setProgress((value) => Math.min(value + 11, 92)), 400);
    return () => window.clearInterval(timer);
  }, [uploadMutation.isPending]);

  useEffect(() => {
    const first = batchesQuery.data?.[0]?.importId.value;
    if (!selectedImportId && first) {
      setSelectedImportId(first);
    }
  }, [batchesQuery.data, selectedImportId]);

  const uploadProps: UploadProps = {
    accept: ".xlsx,.xls",
    maxCount: 1,
    beforeUpload: (nextFile) => {
      setFile(nextFile);
      setProgress(0);
      return false;
    },
    onRemove: () => {
      setFile(undefined);
      setProgress(0);
    },
    fileList: file
      ? [
          {
            uid: file.name,
            name: file.name,
            status: "done"
          }
        ]
      : []
  };

  const batchColumns: ColumnsType<ImportBatch> = [
    { title: "时间", dataIndex: "createdAt", width: 190 },
    { title: "文件名", dataIndex: "sourceFile" },
    { title: "类型", dataIndex: "importType", width: 120 },
    { title: "状态", width: 110, render: (_, record) => <Tag color={record.invalidCount > 0 ? "orange" : "green"}>{record.invalidCount > 0 ? "有异常" : "成功"}</Tag> },
    { title: "新增", dataIndex: "insertedCount", width: 90 },
    { title: "更新", dataIndex: "updatedCount", width: 90 },
    { title: "异常", dataIndex: "invalidCount", width: 90 }
  ];

  const errorColumns: ColumnsType<ImportErrorRow> = [
    { title: "行号", dataIndex: "rowNumber", width: 90 },
    { title: "Sheet", dataIndex: "sheetName", width: 160 },
    { title: "列名", dataIndex: "columnName", width: 140 },
    { title: "原值", dataIndex: "rawValue", width: 160 },
    { title: "错误码", dataIndex: "errorCode", width: 180 },
    { title: "错误信息", dataIndex: "errorMessage" }
  ];

  const currentErrors = useMemo(() => errorsQuery.data || [], [errorsQuery.data]);

  return (
    <>
      {contextHolder}
      <div className="page-header">
        <div>
          <Typography.Title level={1}>数据导入</Typography.Title>
          <Typography.Text className="page-subtitle">前端只上传 Excel 文件，解析、校验和入库由后端完成</Typography.Text>
        </div>
        <Button icon={<ReloadOutlined />} onClick={() => queryClient.invalidateQueries({ queryKey: ["import-batches"] })}>
          刷新历史
        </Button>
      </div>

      <section className="panel">
        <Space direction="vertical" size={14} className="full-width">
          <Radio.Group
            optionType="button"
            buttonStyle="solid"
            value={importType}
            onChange={(event) => setImportType(event.target.value)}
            options={[...importTypeOptions]}
          />
          <Upload.Dragger {...uploadProps}>
            <p className="ant-upload-drag-icon">
              <CloudUploadOutlined />
            </p>
            <p className="ant-upload-text">拖拽或点击选择 Excel 文件</p>
            <p className="ant-upload-hint">仅支持 .xlsx / .xls，前端不解析文件内容</p>
          </Upload.Dragger>
          {uploadMutation.isPending ? <Progress percent={progress} status="active" /> : null}
          {uploadMutation.error ? (
            <Alert type="error" showIcon message="导入失败" description={uploadMutation.error.message} />
          ) : null}
          <Button
            size="large"
            type="primary"
            icon={<CloudUploadOutlined />}
            disabled={!file}
            loading={uploadMutation.isPending}
            onClick={() => uploadMutation.mutate()}
          >
            开始导入
          </Button>
        </Space>
      </section>

      <section className="panel">
        <Typography.Title level={3} className="section-title">
          导入历史
        </Typography.Title>
        {batchesQuery.error ? (
          <Alert type="warning" showIcon message="导入历史接口暂不可用" description={batchesQuery.error.message} />
        ) : (
          <Table<ImportBatch>
            rowKey={(record) => record.importId.value}
            size="small"
            loading={batchesQuery.isLoading}
            columns={batchColumns}
            dataSource={batchesQuery.data || []}
            pagination={{ pageSize: 6 }}
            rowSelection={{
              type: "radio",
              selectedRowKeys: selectedImportId ? [selectedImportId] : [],
              onChange: (keys) => setSelectedImportId(String(keys[0]))
            }}
            scroll={{ x: 1080 }}
            locale={{ emptyText: <Empty description="暂无导入历史" /> }}
          />
        )}
      </section>

      <section className="panel">
        <Space className="panel-toolbar">
          <Typography.Title level={3} className="section-title">
            异常明细
          </Typography.Title>
          <Button className="blue-button" icon={<DownloadOutlined />} disabled={currentErrors.length === 0}>
            导出异常
          </Button>
        </Space>
        {errorsQuery.error ? (
          <Alert type="warning" showIcon message="异常明细接口暂不可用" description={errorsQuery.error.message} />
        ) : (
          <Table<ImportErrorRow>
            rowKey={(record) => `${record.importId.value}-${record.sheetName}-${record.rowNumber}-${record.errorCode}`}
            size="small"
            loading={errorsQuery.isLoading}
            columns={errorColumns}
            dataSource={currentErrors}
            pagination={{ pageSize: 8 }}
            scroll={{ x: 980 }}
            locale={{ emptyText: <Empty description="暂无异常明细" /> }}
          />
        )}
      </section>
    </>
  );
}

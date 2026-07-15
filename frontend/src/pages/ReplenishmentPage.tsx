import { DownloadOutlined, UnorderedListOutlined } from "@ant-design/icons";
import { useMutation } from "@tanstack/react-query";
import { Alert, Button, Empty, Space, Table, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { createReplenishmentList, exportReplenishmentList } from "../api/replenishment";
import type { ReplenishmentItem, ReplenishmentList } from "../types";

export default function ReplenishmentPage() {
  const [api, contextHolder] = message.useMessage();

  const createMutation = useMutation({
    mutationFn: createReplenishmentList,
    onSuccess: (list) => api.success(`已生成补货清单 ${list.listId}`)
  });
  const exportMutation = useMutation({
    mutationFn: async (list: ReplenishmentList) => {
      const blob = await exportReplenishmentList(list.listId);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = `${list.listId}.csv`;
      anchor.click();
      URL.revokeObjectURL(url);
    },
    onSuccess: () => api.success("CSV 已开始下载")
  });

  const list = createMutation.data;

  const columns: ColumnsType<ReplenishmentItem> = [
    {
      title: "商品",
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.productName || record.productCode}</Typography.Text>
          <Typography.Text type="secondary">{record.productCode}</Typography.Text>
        </Space>
      )
    },
    { title: "条码", dataIndex: "barcode", width: 140 },
    { title: "品类", dataIndex: "category", width: 120 },
    { title: "当前库存", dataIndex: "currentQuantity", width: 110 },
    { title: "阈值", dataIndex: "threshold", width: 90 },
    { title: "建议数量", dataIndex: "suggestedQuantity", width: 110 },
    { title: "关联促销", dataIndex: "relatedPromotion", width: 220 },
    { title: "原因", dataIndex: "reason", width: 300 }
  ];

  return (
    <section className="panel">
      {contextHolder}
      <Space className="panel-toolbar" wrap>
        <div>
          <Typography.Title level={5}>补货清单</Typography.Title>
          <Typography.Text type="secondary">第一版从当前库存预警生成清单，并支持 CSV 导出。</Typography.Text>
        </div>
        <Space wrap>
          <Button
            type="primary"
            icon={<UnorderedListOutlined />}
            loading={createMutation.isPending}
            onClick={() => createMutation.mutate()}
          >
            从预警生成
          </Button>
          <Button
            icon={<DownloadOutlined />}
            disabled={!list}
            loading={exportMutation.isPending}
            onClick={() => list && exportMutation.mutate(list)}
          >
            下载 CSV
          </Button>
        </Space>
      </Space>
      {createMutation.error ? (
        <Alert type="error" showIcon message="生成补货清单失败" description={createMutation.error.message} />
      ) : null}
      {exportMutation.error ? (
        <Alert type="error" showIcon message="导出失败" description={exportMutation.error.message} />
      ) : null}
      {list ? (
        <>
          <Alert
            type="info"
            showIcon
            message={`清单 ${list.listId}`}
            description={`状态 ${list.status} / ${list.createdAt}`}
            className="result-alert"
          />
          <Table<ReplenishmentItem>
            rowKey={(record) => `${list.listId}-${record.productCode}-${record.relatedPromotion}`}
            size="small"
            columns={columns}
            dataSource={list.items}
            pagination={{ pageSize: 10 }}
            scroll={{ x: 1320 }}
            locale={{ emptyText: <Empty description="清单中暂无商品" /> }}
          />
        </>
      ) : (
        <Empty description="点击从预警生成补货清单" />
      )}
    </section>
  );
}

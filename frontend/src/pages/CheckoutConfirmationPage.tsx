import { SearchOutlined } from "@ant-design/icons";
import { useMutation } from "@tanstack/react-query";
import { Alert, Button, Col, Descriptions, Empty, Form, Input, Row, Space, Table, Tag, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import {
  fetchCheckoutConfirmation,
  fetchCheckoutConfirmationsByCalculationId
} from "../api/checkout";
import type { CartItem, CheckoutConfirmationResponse, GiftCoupon, GiftItem } from "../types";

type QueryForm = {
  confirmationId?: string;
  calculationId?: string;
};

const money = (value?: number | null) => `¥${Number(value || 0).toFixed(2)}`;

export default function CheckoutConfirmationPage() {
  const [form] = Form.useForm<QueryForm>();
  const confirmationMutation = useMutation({ mutationFn: fetchCheckoutConfirmation });
  const calculationMutation = useMutation({ mutationFn: fetchCheckoutConfirmationsByCalculationId });
  const confirmation = confirmationMutation.data;
  const confirmations = calculationMutation.data || [];

  const cartColumns: ColumnsType<CartItem> = [
    { title: "商品编码", dataIndex: "productCode", width: 130 },
    { title: "条码", dataIndex: "barcode", width: 150 },
    { title: "商品名称", dataIndex: "name", width: 220 },
    { title: "品类", dataIndex: "category", width: 120 },
    { title: "数量", dataIndex: "quantity", width: 90 },
    { title: "单价", dataIndex: "unitPrice", width: 100, render: money }
  ];

  const confirmationColumns: ColumnsType<CheckoutConfirmationResponse> = [
    { title: "confirmationId", dataIndex: "confirmationId", width: 280 },
    { title: "calculationId", dataIndex: "calculationId", width: 280 },
    {
      title: "已选方案",
      width: 220,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.selectedCandidateSnapshot.title}</Typography.Text>
          <Typography.Text type="secondary">{record.selectedCandidateSnapshot.ruleType}</Typography.Text>
        </Space>
      )
    },
    {
      title: "应付/优惠",
      width: 160,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text>{money(record.selectedCandidateSnapshot.payableAmount)}</Typography.Text>
          <Typography.Text type="secondary">优惠 {money(record.selectedCandidateSnapshot.discountAmount)}</Typography.Text>
        </Space>
      )
    },
    { title: "操作人", width: 160, render: (_, record) => `${record.operatorName || "-"} / ${record.operatorId}` },
    { title: "跳过促销", dataIndex: "skipped", width: 110, render: (value: boolean) => <Tag color={value ? "orange" : "green"}>{value ? "是" : "否"}</Tag> },
    { title: "确认时间", dataIndex: "confirmedAt", width: 220 }
  ];

  async function query(values: QueryForm) {
    confirmationMutation.reset();
    calculationMutation.reset();
    const confirmationId = values.confirmationId?.trim();
    const calculationId = values.calculationId?.trim();
    if (confirmationId) {
      confirmationMutation.mutate(confirmationId);
      return;
    }
    if (calculationId) {
      calculationMutation.mutate(calculationId);
    }
  }

  return (
    <Row gutter={[16, 16]}>
      <Col span={24}>
        <section className="panel">
          <Space className="panel-toolbar" wrap>
            <div>
              <Typography.Title level={5}>确认记录追溯</Typography.Title>
              <Typography.Text type="secondary">用于演示结算确认后的审计追溯，只展示后端快照。</Typography.Text>
            </div>
          </Space>
          <Form<QueryForm> form={form} layout="inline" onFinish={query}>
            <Form.Item name="confirmationId">
              <Input className="trace-input" placeholder="confirmationId 精确查询" allowClear />
            </Form.Item>
            <Form.Item name="calculationId">
              <Input className="trace-input" placeholder="calculationId 查询" allowClear />
            </Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              icon={<SearchOutlined />}
              loading={confirmationMutation.isPending || calculationMutation.isPending}
            >
              查询
            </Button>
          </Form>
        </section>
      </Col>

      {confirmationMutation.error ? (
        <Col span={24}>
          <Alert type="error" showIcon message="确认记录查询失败" description={confirmationMutation.error.message} />
        </Col>
      ) : null}
      {calculationMutation.error ? (
        <Col span={24}>
          <Alert type="error" showIcon message="计算记录关联查询失败" description={calculationMutation.error.message} />
        </Col>
      ) : null}

      {confirmation ? (
        <Col span={24}>
          <ConfirmationDetail confirmation={confirmation} cartColumns={cartColumns} />
        </Col>
      ) : null}

      {calculationMutation.data ? (
        <Col span={24}>
          <section className="panel">
            <Typography.Title level={5}>关联确认记录</Typography.Title>
            <Table<CheckoutConfirmationResponse>
              rowKey="confirmationId"
              size="small"
              columns={confirmationColumns}
              dataSource={confirmations}
              pagination={false}
              scroll={{ x: 1430 }}
              locale={{ emptyText: <Empty description="该 calculationId 暂无确认记录" /> }}
            />
          </section>
          {confirmations.map((item) => (
            <ConfirmationDetail key={item.confirmationId} confirmation={item} cartColumns={cartColumns} />
          ))}
        </Col>
      ) : null}
    </Row>
  );
}

function ConfirmationDetail({
  confirmation,
  cartColumns
}: {
  confirmation: CheckoutConfirmationResponse;
  cartColumns: ColumnsType<CartItem>;
}) {
  const candidate = confirmation.selectedCandidateSnapshot;
  return (
    <section className="panel">
      <Descriptions size="small" bordered column={{ xs: 1, md: 2, xl: 3 }}>
        <Descriptions.Item label="confirmationId">{confirmation.confirmationId}</Descriptions.Item>
        <Descriptions.Item label="calculationId">{confirmation.calculationId}</Descriptions.Item>
        <Descriptions.Item label="确认时间">{confirmation.confirmedAt}</Descriptions.Item>
        <Descriptions.Item label="方案名称">{candidate.title}</Descriptions.Item>
        <Descriptions.Item label="方案类型">{candidate.ruleType}</Descriptions.Item>
        <Descriptions.Item label="命中规则">{candidate.ruleId}</Descriptions.Item>
        <Descriptions.Item label="原价金额">{money(candidate.originalAmount)}</Descriptions.Item>
        <Descriptions.Item label="应付金额">{money(candidate.payableAmount)}</Descriptions.Item>
        <Descriptions.Item label="优惠金额">{money(candidate.discountAmount)}</Descriptions.Item>
        <Descriptions.Item label="操作人">{confirmation.operatorName || "-"}</Descriptions.Item>
        <Descriptions.Item label="操作人 ID">{confirmation.operatorId}</Descriptions.Item>
        <Descriptions.Item label="跳过促销">
          <Tag color={confirmation.skipped ? "orange" : "green"}>{confirmation.skipped ? "是" : "否"}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="规则版本">{candidate.ruleVersion}</Descriptions.Item>
        <Descriptions.Item label="涉及商品编码">{candidate.consumedProductCodes.join(", ") || "-"}</Descriptions.Item>
        <Descriptions.Item label="说明">{candidate.explanation}</Descriptions.Item>
      </Descriptions>

      <Typography.Title level={5} className="trace-section-title">赠品</Typography.Title>
      <GiftList gifts={candidate.gifts} />

      <Typography.Title level={5} className="trace-section-title">赠券</Typography.Title>
      <CouponList coupons={candidate.coupons} />

      <Typography.Title level={5} className="trace-section-title">购物车快照</Typography.Title>
      <Table<CartItem>
        rowKey="lineId"
        size="small"
        columns={cartColumns}
        dataSource={confirmation.cartItems || []}
        pagination={false}
        scroll={{ x: 810 }}
        locale={{ emptyText: <Empty description="暂无购物车快照" /> }}
      />
    </section>
  );
}

function GiftList({ gifts }: { gifts: GiftItem[] }) {
  if (!gifts.length) {
    return <Typography.Text type="secondary">无</Typography.Text>;
  }
  return (
    <Space wrap>
      {gifts.map((gift) => (
        <Tag color="green" key={gift.productCode}>
          {gift.name} x {gift.quantity}
        </Tag>
      ))}
    </Space>
  );
}

function CouponList({ coupons }: { coupons: GiftCoupon[] }) {
  if (!coupons.length) {
    return <Typography.Text type="secondary">无</Typography.Text>;
  }
  return (
    <Space wrap>
      {coupons.map((coupon) => (
        <Tag color="blue" key={coupon.couponName}>
          {coupon.couponName} {money(coupon.amount)} x {coupon.quantity}
        </Tag>
      ))}
    </Space>
  );
}

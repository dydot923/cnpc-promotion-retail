import { GiftOutlined, SendOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Form, Input, InputNumber, Select, Space, Table, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useMemo, useState } from "react";
import {
  fetchOperationCampaigns,
  issueOperationCampaignCoupon,
  type OperationCampaignPayload
} from "../api/operationCampaigns";
import EmptyState from "../components/EmptyState";
import Price from "../components/Price";
import type { OperationCampaignDefinition, OperationCouponIssueResponse } from "../types";

type CampaignForm = {
  campaignCode: string;
  memberCodes: string;
  businessDate?: string;
  operatorId?: string;
  operatorName?: string;
  customerType?: "GASOLINE" | "DIESEL";
  signInDays?: number;
  groupId?: string;
  groupSize?: number;
  memberRole?: "NEW_MEMBER" | "OLD_MEMBER";
  qualificationType?: string;
  rewardCode?: "STORE_6" | "STORE_12" | "GASOLINE_10";
  quantity?: number;
  eventKey?: string;
};

type IssueRow = {
  key: string;
  memberCode: string;
  campaignName: string;
  eventKey?: string;
  couponCount: number;
  totalFaceValue: number;
  couponIds: string[];
  status: "SUCCESS" | "FAILED";
  message?: string;
};

const defaultCampaignCode = "rfm-recovery";

export default function OperationCampaignPage() {
  const [form] = Form.useForm<CampaignForm>();
  const [rows, setRows] = useState<IssueRow[]>([]);
  const [api, contextHolder] = message.useMessage();

  const campaignsQuery = useQuery({
    queryKey: ["operation-campaigns"],
    queryFn: fetchOperationCampaigns
  });

  const campaignCode = Form.useWatch("campaignCode", form) || defaultCampaignCode;
  const selectedCampaign = useMemo(
    () => campaignsQuery.data?.find((campaign) => campaign.campaignCode === campaignCode),
    [campaignCode, campaignsQuery.data]
  );

  const issueMutation = useMutation({
    mutationFn: async (values: CampaignForm) => {
      const campaign = campaignsQuery.data?.find((item) => item.campaignCode === values.campaignCode);
      if (!campaign) {
        throw new Error("未找到运营活动配置");
      }
      const memberCodes = parseMemberCodes(values.memberCodes);
      if (memberCodes.length === 0) {
        throw new Error("请输入至少一个会员编号");
      }

      const issuedRows: IssueRow[] = [];
      for (const memberCode of memberCodes) {
        try {
          const response = await issueOperationCampaignCoupon(campaign.endpoint, payloadForCampaign(campaign, memberCode, values));
          issuedRows.push(successRow(campaign, response));
        } catch (error) {
          issuedRows.push({
            key: `${campaign.campaignCode}-${memberCode}-${issuedRows.length}`,
            memberCode,
            campaignName: campaign.campaignName,
            couponCount: 0,
            totalFaceValue: 0,
            couponIds: [],
            status: "FAILED",
            message: error instanceof Error ? error.message : "发券失败"
          });
        }
      }
      return issuedRows;
    },
    onSuccess: (issuedRows) => {
      setRows(issuedRows);
      const successCount = issuedRows.filter((row) => row.status === "SUCCESS").length;
      if (successCount > 0) {
        api.success(`已完成 ${successCount} 个会员发券`);
      }
      if (successCount < issuedRows.length) {
        api.warning(`${issuedRows.length - successCount} 个会员发券失败，请查看结果表`);
      }
    },
    onError: (error) => api.error(error instanceof Error ? error.message : "发券失败")
  });

  const columns: ColumnsType<IssueRow> = [
    {
      title: "会员",
      dataIndex: "memberCode",
      width: 140,
      render: (value, record) => (
        <Space>
          <Typography.Text strong>{value}</Typography.Text>
          <Tag color={record.status === "SUCCESS" ? "green" : "red"}>
            {record.status === "SUCCESS" ? "已发券" : "失败"}
          </Tag>
        </Space>
      )
    },
    { title: "活动", dataIndex: "campaignName", width: 180 },
    { title: "事件键", dataIndex: "eventKey", width: 180, render: (value) => value || "-" },
    { title: "券数", dataIndex: "couponCount", width: 80 },
    {
      title: "券号/错误",
      dataIndex: "couponIds",
      render: (_, record) =>
        record.status === "SUCCESS" ? (
          <Space wrap>{record.couponIds.map((couponId) => <Tag key={couponId}>{couponId}</Tag>)}</Space>
        ) : (
          <Typography.Text type="danger">{record.message}</Typography.Text>
        )
    }
  ];

  return (
    <>
      {contextHolder}
      <div className="page-header">
        <div>
          <Typography.Title level={1}>运营发券</Typography.Title>
          <Typography.Text className="page-subtitle">按活动规则向会员发放真实券实例</Typography.Text>
        </div>
        <Space>
          <Tag color="blue">{campaignsQuery.data?.length || 0} 个活动</Tag>
          <Tag color="green">写入 coupon</Tag>
        </Space>
      </div>

      {campaignsQuery.error ? (
        <Alert type="error" showIcon message="活动配置加载失败" description={campaignsQuery.error.message} className="result-alert" />
      ) : null}

      <div className="operation-grid">
        <section className="panel operation-form-panel">
          <Space className="panel-toolbar" align="start">
            <div>
              <Typography.Title level={3} className="section-title">
                发券任务
              </Typography.Title>
              <Typography.Text type="secondary">
                选择活动后输入会员编号，提交后会调用后端发券接口并生成审计记录。
              </Typography.Text>
            </div>
            <GiftOutlined className="panel-icon" />
          </Space>

          <Form<CampaignForm>
            form={form}
            layout="vertical"
            initialValues={{
              campaignCode: defaultCampaignCode,
              memberCodes: "member-001",
              businessDate: currentDate(),
              operatorId: "operator-001",
              operatorName: "运营员",
              customerType: "GASOLINE",
              signInDays: 7,
              groupId: `group-${Date.now()}`,
              groupSize: 5,
              memberRole: "NEW_MEMBER",
              qualificationType: "TRUCK_DRIVER",
              rewardCode: "STORE_12",
              quantity: 1
            }}
            onFinish={(values) => issueMutation.mutate(values)}
          >
            <Form.Item label="活动" name="campaignCode" rules={[{ required: true, message: "请选择活动" }]}>
              <Select
                loading={campaignsQuery.isLoading}
                options={(campaignsQuery.data || []).map((campaign) => ({
                  value: campaign.campaignCode,
                  label: campaign.campaignName
                }))}
              />
            </Form.Item>

            {selectedCampaign ? (
              <div className="campaign-summary">
                <Typography.Text strong>{selectedCampaign.benefitSummary}</Typography.Text>
                <Space wrap>
                  {selectedCampaign.requiredFields.map((field) => <Tag color="red" key={field}>{field}</Tag>)}
                  {selectedCampaign.optionalFields.map((field) => <Tag key={field}>{field}</Tag>)}
                </Space>
              </div>
            ) : null}

            <Form.Item label="会员编号" name="memberCodes" rules={[{ required: true, message: "请输入会员编号" }]}>
              <Input.TextArea rows={4} placeholder="多个会员可用换行、逗号或空格分隔" />
            </Form.Item>

            <div className="compact-field-grid">
              <Form.Item label="营业日期" name="businessDate">
                <Input placeholder="YYYY-MM-DD" />
              </Form.Item>
              <Form.Item label="操作员工号" name="operatorId">
                <Input />
              </Form.Item>
              <Form.Item label="操作员姓名" name="operatorName">
                <Input />
              </Form.Item>
            </div>

            <CampaignExtraFields campaignCode={campaignCode} />

            <Button
              type="primary"
              size="large"
              icon={<SendOutlined />}
              htmlType="submit"
              loading={issueMutation.isPending}
              disabled={!selectedCampaign}
            >
              发放券包
            </Button>
          </Form>
        </section>

        <section className="panel">
          <Space className="panel-toolbar" align="center">
            <div>
              <Typography.Title level={3} className="section-title">
                发券结果
              </Typography.Title>
              <Typography.Text type="secondary">成功后可在收银台输入会员编号，系统会自动读取可用券。</Typography.Text>
            </div>
            <Tag>{rows.length} 条</Tag>
          </Space>

          {rows.length === 0 ? (
            <EmptyState description="暂无发券结果" />
          ) : (
            <>
              <div className="operation-stats">
                <div>
                  <span>成功会员</span>
                  <strong>{rows.filter((row) => row.status === "SUCCESS").length}</strong>
                </div>
                <div>
                  <span>发放券数</span>
                  <strong>{rows.reduce((total, row) => total + row.couponCount, 0)}</strong>
                </div>
                <div>
                  <span>券面额合计</span>
                  <Price amount={rows.reduce((total, row) => total + row.totalFaceValue, 0)} size="small" />
                </div>
              </div>
              <Table<IssueRow>
                rowKey="key"
                columns={columns}
                dataSource={rows}
                pagination={false}
                size="small"
                scroll={{ x: 760 }}
              />
            </>
          )}
        </section>
      </div>
    </>
  );
}

function CampaignExtraFields({ campaignCode }: { campaignCode: string }) {
  if (campaignCode === "rfm-recovery") {
    return (
      <Form.Item label="客群类型" name="customerType">
        <Select options={[{ value: "GASOLINE", label: "汽油客户" }, { value: "DIESEL", label: "柴油客户" }]} />
      </Form.Item>
    );
  }
  if (campaignCode === "sign-in") {
    return (
      <Form.Item label="连续签到天数" name="signInDays" rules={[{ required: true, message: "请输入签到天数" }]}>
        <InputNumber min={1} precision={0} className="full-width" />
      </Form.Item>
    );
  }
  if (campaignCode === "group-buy") {
    return (
      <div className="compact-field-grid">
        <Form.Item label="拼团 ID" name="groupId" rules={[{ required: true, message: "请输入拼团 ID" }]}>
          <Input />
        </Form.Item>
        <Form.Item label="成团人数" name="groupSize" rules={[{ required: true, message: "请输入成团人数" }]}>
          <InputNumber min={2} precision={0} className="full-width" />
        </Form.Item>
        <Form.Item label="会员角色" name="memberRole">
          <Select options={[{ value: "NEW_MEMBER", label: "新会员" }, { value: "OLD_MEMBER", label: "老会员" }]} />
        </Form.Item>
      </div>
    );
  }
  if (campaignCode === "industry-certification") {
    return (
      <Form.Item label="认证类型" name="qualificationType">
        <Input placeholder="TRUCK_DRIVER / TAXI_DRIVER / FLEET" />
      </Form.Item>
    );
  }
  if (campaignCode === "ecommerce") {
    return (
      <div className="compact-field-grid">
        <Form.Item label="奖励编码" name="rewardCode">
          <Select
            options={[
              { value: "STORE_6", label: "6 元商品券" },
              { value: "STORE_12", label: "12 元商品券" },
              { value: "GASOLINE_10", label: "10 元汽油券" }
            ]}
          />
        </Form.Item>
        <Form.Item label="数量" name="quantity">
          <InputNumber min={1} max={20} precision={0} className="full-width" />
        </Form.Item>
        <Form.Item label="事件键" name="eventKey">
          <Input placeholder="订单号或平台活动批次" />
        </Form.Item>
      </div>
    );
  }
  return null;
}

function payloadForCampaign(
  campaign: OperationCampaignDefinition,
  memberCode: string,
  values: CampaignForm
): OperationCampaignPayload {
  const base: OperationCampaignPayload = {
    memberCode,
    businessDate: values.businessDate,
    operatorId: values.operatorId,
    operatorName: values.operatorName
  };
  if (campaign.campaignCode === "rfm-recovery") {
    return { ...base, customerType: values.customerType };
  }
  if (campaign.campaignCode === "sign-in") {
    return { ...base, signInDays: values.signInDays };
  }
  if (campaign.campaignCode === "group-buy") {
    return { ...base, groupId: values.groupId, groupSize: values.groupSize, memberRole: values.memberRole };
  }
  if (campaign.campaignCode === "industry-certification") {
    return { ...base, qualificationType: values.qualificationType };
  }
  if (campaign.campaignCode === "ecommerce") {
    return { ...base, rewardCode: values.rewardCode, quantity: values.quantity, eventKey: values.eventKey };
  }
  return base;
}

function successRow(campaign: OperationCampaignDefinition, response: OperationCouponIssueResponse): IssueRow {
  return {
    key: `${response.activityCode}-${response.memberCode}-${response.eventKey}`,
    memberCode: response.memberCode,
    campaignName: campaign.campaignName,
    eventKey: response.eventKey,
    couponCount: response.coupons.length,
    totalFaceValue: response.coupons.reduce((total, coupon) => total + Number(coupon.faceValue || 0), 0),
    couponIds: response.coupons.map((coupon) => coupon.couponId),
    status: "SUCCESS"
  };
}

function parseMemberCodes(value: string) {
  return value
    .split(/[\s,，;；]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function currentDate() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

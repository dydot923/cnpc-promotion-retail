import { GiftOutlined, SendOutlined, ShoppingCartOutlined, StarOutlined, TeamOutlined } from "@ant-design/icons";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Alert, Button, Form, Input, InputNumber, Select, Space, Table, Tabs, Tag, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { useEffect, useMemo, useState } from "react";
import {
  drawPointsLottery,
  exchangePoints,
  fetchBenefitPackages,
  purchaseBenefitPackage
} from "../api/activityAcceptance";
import { changeMemberPoints, createMember, fetchMemberCoupons, issueActivationCoupons } from "../api/members";
import {
  fetchOperationCampaigns,
  issueOperationCampaignCoupon,
  type OperationCampaignPayload
} from "../api/operationCampaigns";
import { fetchRuleDrafts } from "../api/rules";
import EmptyState from "../components/EmptyState";
import Price from "../components/Price";
import type {
  BenefitPackage,
  BenefitPackagePurchaseResponse,
  Coupon,
  MemberCouponListResponse,
  OperationCampaignDefinition,
  OperationCouponIssueResponse,
  PromotionRuleDraft,
  PointsExchangeResponse,
  PointsLotteryDrawResponse
} from "../types";

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
  coupons: Coupon[];
  status: "SUCCESS" | "FAILED";
  message?: string;
};

const defaultCampaignCode = "rfm-recovery";

const operationPresets: { key: string; label: string; values: Partial<CampaignForm> }[] = [
  { key: "rfm-gasoline", label: "RFM汽油客户", values: { campaignCode: "rfm-recovery", customerType: "GASOLINE" } },
  { key: "rfm-diesel", label: "RFM柴油客户", values: { campaignCode: "rfm-recovery", customerType: "DIESEL" } },
  { key: "birthday", label: "生日礼包", values: { campaignCode: "birthday", memberCodes: "member-001", businessDate: "2026-07-08" } },
  { key: "sign-3", label: "签到3天", values: { campaignCode: "sign-in", signInDays: 3 } },
  { key: "sign-7", label: "签到7天", values: { campaignCode: "sign-in", signInDays: 7 } },
  { key: "sign-10", label: "签到10天", values: { campaignCode: "sign-in", signInDays: 10 } },
  { key: "group-2", label: "2人拼团", values: { campaignCode: "group-buy", groupSize: 2, memberRole: "NEW_MEMBER" } },
  { key: "group-5", label: "5人拼团", values: { campaignCode: "group-buy", groupSize: 5, memberRole: "NEW_MEMBER" } },
  { key: "group-8", label: "8人拼团", values: { campaignCode: "group-buy", groupSize: 8, memberRole: "NEW_MEMBER" } },
  { key: "industry", label: "行业认证", values: { campaignCode: "industry-certification", qualificationType: "TEACHER" } },
  { key: "ecommerce", label: "电商奖励", values: { campaignCode: "ecommerce", rewardCode: "STORE_12", quantity: 1 } }
];

const checkoutScenarioGroups = [
  {
    title: "品牌日与充值活动",
    scenarios: [
      ["a1-500", "逢7气惠-LNG满500"], ["a1-1000", "逢7气惠-LNG满1000"],
      ["a1-1500", "逢7气惠-LNG满1500"], ["a1-2000", "逢7气惠-LNG满2000"],
      ["a3", "加气站便利店9折"], ["a4", "逢8 CN98每升立减"],
      ["g1", "逢7加气站全场9折"], ["g2", "逢9油站9折+3倍积分"],
      ["a5-1000-normal", "十惠1000普通会员"], ["a5-1000-gold", "十惠1000金卡"],
      ["a5-2000-normal", "十惠2000普通会员"], ["a5-2000-gold", "十惠2000金卡"],
      ["a6", "小额充值666赠券"]
    ]
  },
  {
    title: "油非与气非互动",
    scenarios: [
      ["e1", "汽油满230赠非油券"], ["e1-diesel", "柴油满280赠非油券"],
      ["e2", "伊力特250整件赠油券"], ["e2-ilite-500-jia", "佳藏500整件赠油券"],
      ["e2-ilite-500-li", "礼藏500整件赠油券"], ["f1", "CNG满50赠2瓶水"],
      ["e2-wing-card", "翼卡通399购卡赠油券"], ["f1-lng", "LNG满1000赠4瓶水"]
    ]
  },
  {
    title: "便利店与纯非促销",
    scenarios: [
      ["g3", "9.9元零食专区"], ["g4", "赛事啤酒赠券+夜折"], ["g5", "中秋满减+赠券"],
      ["g6-ilite-250", "伊力特250会员价"], ["g6-ilite-500-jia", "佳藏500会员价"],
      ["g6-ilite-500-li", "礼藏500会员价"], ["g6-cigarette-200", "香烟满200二选一"],
      ["g6-cigarette-555", "香烟满555赠伊力特250"], ["g6-cigarette-888", "香烟满888赠伊力特500"],
      ["g6-store-gift", "便利店满额赠品"], ["g6-cotton-film", "棉包膜9卷礼包"],
      ["g7", "单品安全促销价"]
    ]
  },
  {
    title: "加油换购",
    scenarios: [["h1", "加油换购驾驶包"], ["h2", "汽油满180换购红牛"]]
  }
] as const;

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
            coupons: [],
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
      title: "实际发放权益",
      dataIndex: "couponIds",
      render: (_, record) =>
        record.status === "SUCCESS" ? (
          <Space wrap>{record.coupons.map((coupon) => (
            <Tag color="blue" key={coupon.couponId}>
              {coupon.couponName} ¥{coupon.faceValue} / 满{coupon.minSpendAmount}
            </Tag>
          ))}</Space>
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
          <Typography.Title level={1}>活动看板验收中心</Typography.Title>
          <Typography.Text className="page-subtitle">逐项装载业务条件，调用真实规则、发券、积分与权益包接口</Typography.Text>
        </div>
        <Space>
          <Tag color="blue">收银促销 + 会员运营</Tag>
          <Tag color="green">真实写库</Tag>
        </Space>
      </div>

      {campaignsQuery.error ? (
        <Alert type="error" showIcon message="活动配置加载失败" description={campaignsQuery.error.message} className="result-alert" />
      ) : null}

      <Tabs
        defaultActiveKey="checkout"
        items={[
          {
            key: "checkout",
            label: "收银促销",
            children: <CheckoutAcceptanceCatalog />
          },
          {
            key: "coupon",
            label: "运营发券",
            children: (
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
            <div className="acceptance-button-grid">
              {operationPresets.map((preset) => (
                <Button
                  key={preset.key}
                  onClick={() => form.setFieldsValue({
                    ...preset.values,
                    groupId: preset.values.campaignCode === "group-buy"
                      ? `group-${Date.now()}`
                      : form.getFieldValue("groupId")
                  })}
                >
                  {preset.label}
                </Button>
              ))}
            </div>
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
            )
          },
          {
            key: "lifecycle",
            label: "会员生命周期",
            children: <LifecycleAcceptance />
          },
          {
            key: "points",
            label: "积分活动",
            children: <PointsAcceptance />
          },
          {
            key: "packages",
            label: "权益包",
            children: <BenefitPackageAcceptance />
          }
        ]}
      />
    </>
  );
}

function CheckoutAcceptanceCatalog() {
  const [zoneSearch, setZoneSearch] = useState("");
  const rulesQuery = useQuery({
    queryKey: ["priority-activity-rules"],
    queryFn: () => fetchRuleDrafts("CONFIRMED"),
    staleTime: 30_000
  });
  const focusedRules = useMemo(() => {
    const all = rulesQuery.data || [];
    const zone = all
      .filter(({ rule }) => rule.ruleId.startsWith("abv2-99-zone-"))
      .sort(bySourceRow);
    const exchange = all
      .filter(({ rule, sourceSheetName }) => sourceSheetName === "加油换购（统建）"
        || rule.ruleId.startsWith("abv2-h2-")
        || rule.ruleId.startsWith("abv2-bundle-abv2-"))
      .sort(bySourceRow);
    const nonOil = all
      .filter(({ rule, sourceSheetName }) => sourceSheetName === "非非促销（统建）"
        || /^(abv2-(g1|g2|g4|g5|g6|e2)-)/.test(rule.ruleId))
      .filter(({ rule }) => !rule.ruleId.startsWith("abv2-99-zone-"))
      .sort(bySourceRow);
    return { zone, exchange, nonOil };
  }, [rulesQuery.data]);
  const visibleZoneRules = useMemo(() => {
    const keyword = zoneSearch.trim().toLowerCase();
    if (!keyword) return focusedRules.zone;
    return focusedRules.zone.filter(({ rule }) => {
      const productCode = rule.condition.productCodes[0] || "";
      return productCode.toLowerCase().includes(keyword) || rule.activityName.toLowerCase().includes(keyword);
    });
  }, [focusedRules.zone, zoneSearch]);

  const activityColumns: ColumnsType<PromotionRuleDraft> = [
    {
      title: "活动",
      dataIndex: ["rule", "activityName"],
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.rule.activityName}</Typography.Text>
          <Typography.Text type="secondary">源表第 {record.sourceRowNumber} 行</Typography.Text>
        </Space>
      )
    },
    { title: "规则类型", width: 124, render: (_, record) => <Tag>{ruleTypeLabel(record.rule.ruleType)}</Tag> },
    { title: "执行条件", width: 220, render: (_, record) => conditionText(record) },
    { title: "优惠结果", width: 220, render: (_, record) => benefitText(record) },
    {
      title: "验收",
      width: 112,
      render: (_, record) => {
        const demo = demoForRule(record.rule.ruleId);
        return demo ? <Button type="primary" href={`/checkout?demo=${demo}`}>去实操</Button> : <Tag color="green">已发布</Tag>;
      }
    }
  ];

  const zoneColumns: ColumnsType<PromotionRuleDraft> = [
    {
      title: "商品",
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Typography.Text strong>{record.rule.activityName.replace(/^9\.9元商品专区-/, "")}</Typography.Text>
          <Typography.Text type="secondary">{record.rule.condition.productCodes[0]}</Typography.Text>
        </Space>
      )
    },
    {
      title: "促销包价",
      width: 140,
      render: (_, record) => {
        const quantity = Math.max(record.rule.condition.minProductQuantity || 1, 1);
        return <Typography.Text strong>{quantity > 1 ? `${quantity}件 / 9.9元` : "1件 / 9.9元"}</Typography.Text>;
      }
    },
    { title: "状态", width: 90, render: () => <Tag color="green">可结算</Tag> },
    {
      title: "验收",
      width: 112,
      render: (_, record) => {
        const quantity = Math.max(record.rule.condition.minProductQuantity || 1, 1);
        return (
          <Button type="primary" href={`/checkout?product=${encodeURIComponent(record.rule.condition.productCodes[0])}&quantity=${quantity}`}>
            装载{quantity}件
          </Button>
        );
      }
    }
  ];

  return (
    <section className="panel acceptance-catalog">
      <Space className="panel-toolbar" align="start">
        <div>
          <Typography.Title level={3} className="section-title">重点促销验收</Typography.Title>
          <Typography.Text type="secondary">仅聚焦加油换购、非非促销和9.9元专区，数据来自已确认结算规则。</Typography.Text>
        </div>
        <ShoppingCartOutlined className="panel-icon" />
      </Space>
      <Space wrap className="focus-rule-summary">
        <Tag color="blue">加油换购 {focusedRules.exchange.length} 条</Tag>
        <Tag color="green">非非促销 {focusedRules.nonOil.length} 条</Tag>
        <Tag color="gold">9.9专区 {focusedRules.zone.length} 个商品</Tag>
      </Space>
      {rulesQuery.error ? <Alert type="error" showIcon message="重点规则加载失败" description={rulesQuery.error.message} /> : null}
      <Tabs
        defaultActiveKey="exchange"
        items={[
          {
            key: "exchange",
            label: "加油换购",
            children: (
              <Space direction="vertical" size={16} className="full-width">
                <Alert
                  type="info"
                  showIcon
                  message="先录油品金额，再选组合包或单品"
                  description="组合包和单品换购已合并到同一清单；汽油180/200元、柴油300/500元门槛会自动判断。"
                  action={<Button type="primary" href="/checkout?mode=exchange&fuelType=GASOLINE&fuelAmount=500">打开完整换购清单</Button>}
                />
                <Table rowKey="draftId" size="small" loading={rulesQuery.isLoading} columns={activityColumns}
                  dataSource={focusedRules.exchange} pagination={{ pageSize: 12, showSizeChanger: false }} scroll={{ x: 920 }} />
              </Space>
            )
          },
          {
            key: "non-oil",
            label: "非非促销",
            children: (
              <Table rowKey="draftId" size="small" loading={rulesQuery.isLoading} columns={activityColumns}
                dataSource={focusedRules.nonOil} pagination={{ pageSize: 12, showSizeChanger: false }} scroll={{ x: 920 }} />
            )
          },
          {
            key: "zone99",
            label: "9.9元专区",
            children: (
              <Space direction="vertical" size={12} className="full-width">
                <Alert
                  type="warning"
                  showIcon
                  message={`${focusedRules.zone.length}个专区条目已可搜索、装载和结算`}
                  description="源表最后4行没有商品编码，系统已保留原活动名称，并映射到价格主档中的真实商品编码；验收表中可按源表行号追溯。"
                />
                <Input.Search value={zoneSearch} onChange={(event) => setZoneSearch(event.target.value)}
                  allowClear placeholder="搜索商品名称或商品编码" />
                <Table rowKey="draftId" size="small" loading={rulesQuery.isLoading} columns={zoneColumns}
                  dataSource={visibleZoneRules} pagination={{ pageSize: 15, showSizeChanger: false, showTotal: (total) => `共 ${total} 个商品` }} />
              </Space>
            )
          }
        ]}
      />

      <div className="acceptance-group">
        <Typography.Title level={4}>常用实操场景</Typography.Title>
        <div className="acceptance-button-grid">
          {checkoutScenarioGroups.slice(1).map((group) => group.scenarios.map(([key, label]) => (
            <Button key={key} href={`/checkout?demo=${key}`}>{label}</Button>
          )))}
        </div>
      </div>
    </section>
  );
}

function bySourceRow(left: PromotionRuleDraft, right: PromotionRuleDraft) {
  return left.sourceRowNumber - right.sourceRowNumber || left.rule.activityName.localeCompare(right.rule.activityName, "zh-CN");
}

function ruleTypeLabel(type: string) {
  return ({
    FIXED_PRICE: "固定价",
    PERCENTAGE_DISCOUNT: "折扣",
    GIFT_ITEM: "买赠",
    GIFT_COUPON: "赠券",
    EXCHANGE_PURCHASE: "单品换购",
    BUNDLE_PRICE: "组合换购",
    COMPOSITE: "组合优惠"
  } as Record<string, string>)[type] || type;
}

function conditionText({ rule }: PromotionRuleDraft) {
  const parts: string[] = [];
  if (Number(rule.condition.minFuelAmount || 0) > 0) parts.push(`油品满${rule.condition.minFuelAmount}元`);
  if (Number(rule.condition.minCartAmount || 0) > 0) parts.push(`商品满${rule.condition.minCartAmount}元`);
  if (Number(rule.condition.minProductQuantity || 0) > 0) parts.push(`满${rule.condition.minProductQuantity}件`);
  if (rule.condition.memberRequired) parts.push("会员");
  if (rule.condition.daysOfMonth?.length) parts.push(`每月${rule.condition.daysOfMonth.join("/")}日`);
  return parts.join("，") || "购买指定商品";
}

function benefitText({ rule }: PromotionRuleDraft) {
  const benefit = rule.benefit;
  switch (rule.ruleType) {
    case "FIXED_PRICE": {
      const packageQuantity = Math.max(rule.condition.minProductQuantity || 1, 1);
      return packageQuantity > 1
        ? `满${packageQuantity}件${benefit.fixedPrice}元/组`
        : `每件${benefit.fixedPrice}元`;
    }
    case "PERCENTAGE_DISCOUNT": return `${Number(benefit.discountRate || 0) * 10}折`;
    case "EXCHANGE_PURCHASE": return `${benefit.exchangePrice}元换购${benefit.exchangeQuantity || 1}件`;
    case "BUNDLE_PRICE": return "组合包25元换购";
    case "GIFT_COUPON": return `${benefit.giftCouponAmount || "多档"}元券 x ${benefit.giftCouponQuantity || 1}`;
    case "GIFT_ITEM": return benefit.giftItemName || "赠指定商品/可选赠品";
    default: return rule.activityName;
  }
}

function demoForRule(ruleId: string) {
  const pairs: [RegExp, string][] = [
    [/g1-/, "g1"], [/g2-/, "g2"], [/g4-/, "g4"], [/g5-/, "g5"],
    [/g6-ilite-250/, "g6-ilite-250"], [/g6-ilite-500-jia/, "g6-ilite-500-jia"],
    [/g6-ilite-500-li/, "g6-ilite-500-li"], [/g6-cigarette-200/, "g6-cigarette-200"],
    [/g6-cigarette-555/, "g6-cigarette-555"], [/g6-cigarette-888/, "g6-cigarette-888"],
    [/g6-store-/, "g6-store-gift"], [/g6-cotton-/, "g6-cotton-film"],
    [/e2-wing-card/, "e2-wing-card"], [/e2-ilite-250/, "e2"],
    [/e2-ilite-500-jia/, "e2-ilite-500-jia"], [/e2-ilite-500-li/, "e2-ilite-500-li"],
    [/bundle-abv2-driving/, "h1"], [/h2-/, "h2"]
  ];
  return pairs.find(([pattern]) => pattern.test(ruleId))?.[1];
}

type LifecycleForm = {
  mode: "NEW_MEMBER" | "POTENTIAL_GASOLINE" | "POTENTIAL_DIESEL";
};

function LifecycleAcceptance() {
  const [form] = Form.useForm<LifecycleForm>();
  const mutation = useMutation<MemberCouponListResponse, Error, LifecycleForm>({
    mutationFn: async ({ mode }) => {
      const suffix = `${Date.now()}-${Math.floor(Math.random() * 1000)}`;
      const memberCode = `accept-${mode.toLowerCase()}-${suffix}`;
      const isNewMember = mode === "NEW_MEMBER";
      const fuelTag = mode === "POTENTIAL_DIESEL" ? "diesel_customer" : "gasoline_customer";
      await createMember({
        memberCode,
        memberName: isNewMember ? "验收新会员" : "验收潜在会员",
        levelCode: "normal",
        totalPoints: 2000,
        availablePoints: 2000,
        birthday: "1990-07-16",
        province: "新疆",
        usualProvince: "新疆",
        eEnjoyCardNo: isNewMember ? `EJOY-${suffix}` : undefined,
        status: "ACTIVE",
        memberTags: [fuelTag],
        openedCard: isNewMember
      });
      return isNewMember ? fetchMemberCoupons(memberCode) : issueActivationCoupons(memberCode);
    }
  });

  return (
    <div className="operation-grid">
      <section className="panel operation-form-panel">
        <Space className="panel-toolbar" align="start">
          <div>
            <Typography.Title level={3} className="section-title">新增会员与潜在会员</Typography.Title>
            <Typography.Text type="secondary">创建独立验收会员并实际写入会员表和券实例，可重复验收。</Typography.Text>
          </div>
          <TeamOutlined className="panel-icon" />
        </Space>
        <Form form={form} layout="vertical" initialValues={{ mode: "NEW_MEMBER" }} onFinish={(values) => mutation.mutate(values)}>
          <Form.Item label="验收活动" name="mode">
            <Select options={[
              { value: "NEW_MEMBER", label: "首次开通昆仑e享卡-4类券" },
              { value: "POTENTIAL_GASOLINE", label: "潜在汽油会员-12元汽油券3张" },
              { value: "POTENTIAL_DIESEL", label: "潜在柴油会员-20元柴油券3张" }
            ]} />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={mutation.isPending}>创建会员并发放券包</Button>
        </Form>
      </section>
      <section className="panel">
        <Typography.Title level={3} className="section-title">实际发放结果</Typography.Title>
        {mutation.error ? <Alert type="error" showIcon message="验收失败" description={mutation.error.message} /> : null}
        {mutation.data ? (
          <>
            <Alert type="success" showIcon message={`验收通过：${mutation.data.memberCode}`} description={`已实际写入 ${mutation.data.coupons.length} 张券。`} />
            <CouponResult coupons={mutation.data.coupons} />
          </>
        ) : <EmptyState description="选择活动后执行验收" />}
      </section>
    </div>
  );
}

type PointsForm = {
  memberCode: string;
  businessDate: string;
  pointsUsed: number;
};

type PointsAction = "TOP_UP" | "EXCHANGE" | "LOTTERY";
type PointsActionResult = { title: string; detail: string; coupon?: Coupon | null };

function PointsAcceptance() {
  const [form] = Form.useForm<PointsForm>();
  const mutation = useMutation<PointsActionResult, Error, { action: PointsAction; values: PointsForm }>({
    mutationFn: async ({ action, values }) => {
      if (action === "TOP_UP") {
        const response = await changeMemberPoints(values.memberCode, { changeType: "ADD", amount: 2000, reason: "活动看板验收补充积分" });
        return { title: "积分已补充", detail: `当前可用积分 ${response.availablePoints}` };
      }
      if (action === "EXCHANGE") {
        const response: PointsExchangeResponse = await exchangePoints(values.memberCode, {
          pointsUsed: values.pointsUsed,
          businessDate: values.businessDate,
          stationCode: "1-A6501-C001-S001",
          operatorId: "acceptance",
          operatorName: "验收员"
        });
        return { title: "积分兑换9折券成功", detail: `扣除 ${response.pointsUsed} 积分，剩余 ${response.availablePointsAfter}`, coupon: response.coupon };
      }
      const response: PointsLotteryDrawResponse = await drawPointsLottery(values.memberCode, {
        businessDate: values.businessDate,
        stationCode: "1-A6501-C001-S001",
        operatorId: "acceptance",
        operatorName: "验收员"
      });
      return { title: "500积分抽奖完成", detail: `${response.resultLabel}，剩余 ${response.availablePointsAfter} 积分`, coupon: response.prizeCoupon };
    }
  });

  const run = async (action: PointsAction) => {
    const values = await form.validateFields();
    mutation.mutate({ action, values });
  };

  return (
    <div className="operation-grid">
      <section className="panel operation-form-panel">
        <Space className="panel-toolbar" align="start">
          <div>
            <Typography.Title level={3} className="section-title">积分兑换与积分抽奖</Typography.Title>
            <Typography.Text type="secondary">逢9积分兑换9折券；9-11、19-21、29-31及次月1日可用500积分抽奖。</Typography.Text>
          </div>
          <StarOutlined className="panel-icon" />
        </Space>
        <Form form={form} layout="vertical" initialValues={{ memberCode: "member-001", businessDate: "2026-07-19", pointsUsed: 100 }}>
          <Form.Item label="会员编号" name="memberCode" rules={[{ required: true }]}><Input /></Form.Item>
          <div className="compact-field-grid">
            <Form.Item label="营业日期" name="businessDate" rules={[{ required: true }]}><Input type="date" /></Form.Item>
            <Form.Item label="兑换积分" name="pointsUsed" rules={[{ required: true }]}><InputNumber min={1} precision={0} className="full-width" /></Form.Item>
          </div>
          <Space wrap>
            <Button onClick={() => run("TOP_UP")}>补充2000积分</Button>
            <Button type="primary" onClick={() => run("EXCHANGE")}>兑换9折券</Button>
            <Button onClick={() => run("LOTTERY")}>500积分抽奖</Button>
          </Space>
        </Form>
      </section>
      <section className="panel">
        <Typography.Title level={3} className="section-title">积分业务结果</Typography.Title>
        {mutation.error ? <Alert type="error" showIcon message="操作失败" description={mutation.error.message} /> : null}
        {mutation.data ? (
          <>
            <Alert type="success" showIcon message={mutation.data.title} description={mutation.data.detail} />
            {mutation.data.coupon ? <CouponResult coupons={[mutation.data.coupon]} /> : null}
          </>
        ) : <EmptyState description="选择积分操作进行验收" />}
      </section>
    </div>
  );
}

type PackageForm = { packageCode: string; memberCode: string; paymentAmount: number };

function BenefitPackageAcceptance() {
  const [form] = Form.useForm<PackageForm>();
  const packagesQuery = useQuery({ queryKey: ["benefit-packages"], queryFn: fetchBenefitPackages });
  const packageCode = Form.useWatch("packageCode", form);
  const selectedPackage = useMemo(
    () => packagesQuery.data?.find((item) => item.packageCode === packageCode),
    [packageCode, packagesQuery.data]
  );
  useEffect(() => {
    const first = packagesQuery.data?.[0];
    if (first && !form.getFieldValue("packageCode")) {
      form.setFieldsValue({ packageCode: first.packageCode, memberCode: "member-001", paymentAmount: Number(first.salePrice) });
    }
  }, [form, packagesQuery.data]);
  const mutation = useMutation<BenefitPackagePurchaseResponse, Error, PackageForm>({
    mutationFn: (values) => purchaseBenefitPackage(values.packageCode, {
      memberCode: values.memberCode,
      stationCode: "1-A6501-C001-S001",
      paymentAmount: values.paymentAmount,
      checkoutTransactionNo: `accept-package-${Date.now()}`,
      operatorId: "acceptance",
      operatorName: "验收员"
    })
  });

  return (
    <div className="operation-grid">
      <section className="panel operation-form-panel">
        <Typography.Title level={3} className="section-title">十全十美与LNG/CNG权益包</Typography.Title>
        <Form form={form} layout="vertical" onFinish={(values) => mutation.mutate(values)}>
          <Form.Item label="权益包" name="packageCode" rules={[{ required: true }]}>
            <Select
              loading={packagesQuery.isLoading}
              options={(packagesQuery.data || []).map((item) => ({ value: item.packageCode, label: `${item.packageName} / ¥${item.salePrice}` }))}
              onChange={(value) => {
                const item = packagesQuery.data?.find((candidate) => candidate.packageCode === value);
                if (item) form.setFieldValue("paymentAmount", Number(item.salePrice));
              }}
            />
          </Form.Item>
          <div className="compact-field-grid">
            <Form.Item label="会员编号" name="memberCode" rules={[{ required: true }]}><Input /></Form.Item>
            <Form.Item label="实付金额" name="paymentAmount" rules={[{ required: true }]}><InputNumber min={0} precision={2} className="full-width" /></Form.Item>
          </div>
          <Button type="primary" htmlType="submit" loading={mutation.isPending} disabled={!selectedPackage}>购买并激活权益包</Button>
        </Form>
        {mutation.error ? <Alert className="result-alert" type="error" showIcon message="购买失败" description={mutation.error.message} /> : null}
        {mutation.data ? <Alert className="result-alert" type="success" showIcon message={`购买成功：${mutation.data.packageName}`} description={`权益快照 ${mutation.data.entitlementSnapshot.length} 项，单号 ${mutation.data.purchaseId}`} /> : null}
      </section>
      <section className="panel">
        <Typography.Title level={3} className="section-title">权益明细</Typography.Title>
        {selectedPackage ? <BenefitPackageItems benefitPackage={selectedPackage} /> : <EmptyState description="请选择权益包" />}
      </section>
    </div>
  );
}

function CouponResult({ coupons }: { coupons: Coupon[] }) {
  return (
    <div className="coupon-result-list">
      {coupons.map((coupon) => (
        <div key={coupon.couponId}>
          <strong>{coupon.couponName}</strong>
          <span>¥{coupon.faceValue} / 满{coupon.minSpendAmount} / {coupon.validUntil || "按规则有效"}</span>
        </div>
      ))}
    </div>
  );
}

function BenefitPackageItems({ benefitPackage }: { benefitPackage: BenefitPackage }) {
  return (
    <>
      <Alert type="info" showIcon message={`${benefitPackage.packageName} / ¥${benefitPackage.salePrice}`} description={`${benefitPackage.salesChannel}，共 ${benefitPackage.items.length} 项权益。`} />
      <Table
        rowKey={(record) => `${record.sourceRowNumber}-${record.itemName}`}
        dataSource={benefitPackage.items}
        pagination={{ pageSize: 8, hideOnSinglePage: true }}
        size="small"
        columns={[
          { title: "权益", dataIndex: "itemName" },
          { title: "数量", dataIndex: "quantity", width: 90 },
          { title: "说明", dataIndex: "remark" }
        ]}
      />
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
    coupons: response.coupons,
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

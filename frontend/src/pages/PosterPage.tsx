import { DownloadOutlined, PrinterOutlined, ReloadOutlined, SafetyCertificateOutlined } from "@ant-design/icons";
import { Alert, Button, Form, Select, Space, Spin, Tag, Typography, message } from "antd";
import { useMemo, useState } from "react";
import {
  buildPosterPrompt,
  createPosterTask,
  posterActivities,
  posterProducts,
  type PosterTask
} from "../api/poster";

type PosterForm = {
  activityId: string;
  productCodes: string[];
  size: string;
  template: string;
};

export default function PosterPage() {
  const [form] = Form.useForm<PosterForm>();
  const [generating, setGenerating] = useState(false);
  const [task, setTask] = useState<PosterTask>();
  const [api, contextHolder] = message.useMessage();
  const activityId = Form.useWatch("activityId", form) || posterActivities[0].activityId;
  const availableProducts = posterProducts[activityId] || [];
  const productCodes = Form.useWatch("productCodes", form) || availableProducts.map((product) => product.productCode);

  const selectedActivity = useMemo(
    () => posterActivities.find((activity) => activity.activityId === activityId) || posterActivities[0],
    [activityId]
  );
  const selectedProducts = availableProducts.filter((product) => productCodes.includes(product.productCode));
  const prompt = buildPosterPrompt(selectedActivity, selectedProducts);

  async function generatePoster() {
    setGenerating(true);
    setTask(undefined);
    window.setTimeout(() => {
      setTask(createPosterTask(selectedActivity, selectedProducts));
      setGenerating(false);
    }, 900);
  }

  function approvePoster() {
    if (!task) {
      return;
    }
    setTask({ ...task, status: "APPROVED" });
    api.success("海报已确认发布");
  }

  return (
    <>
      {contextHolder}
      <div className="page-header">
        <div>
          <Typography.Title level={1}>AI 海报</Typography.Title>
          <Typography.Text className="page-subtitle">提示词、价格和活动条件由后端生成，前端负责配置与审核展示</Typography.Text>
        </div>
        <Tag color="orange">演示数据：AI 服务未配置时的前端占位</Tag>
      </div>

      <Alert
        type="warning"
        showIcon
        message="AI 海报服务未配置，请联系管理员"
        description="当前页面使用后端响应模板格式的演示数据展示完整审核流程。"
        className="result-alert"
      />

      <div className="poster-grid">
        <section className="panel">
          <Typography.Title level={3} className="section-title">
            海报配置
          </Typography.Title>
          <Form<PosterForm>
            form={form}
            layout="vertical"
            initialValues={{
              activityId: posterActivities[0].activityId,
              productCodes: posterProducts[posterActivities[0].activityId].map((product) => product.productCode),
              size: "A4",
              template: "中石油红蓝黄标准版"
            }}
          >
            <Form.Item label="活动选择" name="activityId">
              <Select
                options={posterActivities.map((activity) => ({
                  value: activity.activityId,
                  label: activity.activityName
                }))}
                onChange={(value) => {
                  form.setFieldValue(
                    "productCodes",
                    posterProducts[value]?.map((product) => product.productCode) || []
                  );
                }}
              />
            </Form.Item>
            <Form.Item label="商品选择" name="productCodes">
              <Select
                mode="multiple"
                options={availableProducts.map((product) => ({
                  value: product.productCode,
                  label: product.productName
                }))}
              />
            </Form.Item>
            <Form.Item label="海报尺寸" name="size">
              <Select
                options={[
                  { value: "A4", label: "A4 竖版" },
                  { value: "A3", label: "A3 竖版" },
                  { value: "screen", label: "电子屏 16:9" }
                ]}
              />
            </Form.Item>
            <Form.Item label="风格模板" name="template">
              <Select
                options={[
                  { value: "中石油红蓝黄标准版", label: "中石油红蓝黄标准版" },
                  { value: "收银台促销版", label: "收银台促销版" },
                  { value: "加油岛提示版", label: "加油岛提示版" }
                ]}
              />
            </Form.Item>
            <Button size="large" type="primary" className="full-width" loading={generating} onClick={generatePoster}>
              生成海报
            </Button>
          </Form>

          <Typography.Title level={3} className="section-title" style={{ marginTop: 18 }}>
            提示词预览
          </Typography.Title>
          <pre className="json-view">{prompt}</pre>
        </section>

        <section className="panel">
          <Space className="panel-toolbar">
            <Typography.Title level={3} className="section-title">
              海报预览
            </Typography.Title>
            <Space>
              <Button icon={<ReloadOutlined />} disabled={!task} onClick={generatePoster}>
                换一张
              </Button>
              <Button className="blue-button" icon={<DownloadOutlined />} disabled={!task}>
                下载
              </Button>
              <Button className="blue-button" icon={<PrinterOutlined />} disabled={!task}>
                打印
              </Button>
            </Space>
          </Space>
          <Spin spinning={generating} tip="AI 正在创作海报，预计 10-30 秒...">
            <div className="poster-preview">
              <div className="poster-paper">
                <Typography.Title style={{ color: "#FFFFFF", marginBottom: 8 }} level={2}>
                  {selectedActivity.activityName}
                </Typography.Title>
                <Typography.Text style={{ color: "#E8F0FA" }}>{selectedActivity.description}</Typography.Text>
                <div className="poster-price">
                  {selectedProducts[0] ? `¥${selectedProducts[0].displayPrice}` : "特惠"}
                </div>
                <Space direction="vertical" size={6} style={{ marginTop: 24 }}>
                  {selectedProducts.map((product) => (
                    <Typography.Text key={product.productCode} style={{ color: "#FFFFFF" }}>
                      {product.productName} 到手价 ¥{product.displayPrice}
                    </Typography.Text>
                  ))}
                </Space>
              </div>
            </div>
          </Spin>
          <Space className="panel-toolbar" style={{ marginTop: 14 }}>
            <span>
              审核状态：
              <Tag color={task?.status === "APPROVED" ? "green" : "orange"}>
                {task?.status === "APPROVED" ? "已确认" : "待确认"}
              </Tag>
            </span>
            <Space>
              <Button
                type="primary"
                icon={<SafetyCertificateOutlined />}
                disabled={!task || task.status === "APPROVED"}
                onClick={approvePoster}
              >
                确认发布
              </Button>
              <Button disabled={!task} onClick={generatePoster}>
                重新生成
              </Button>
            </Space>
          </Space>
        </section>
      </div>
    </>
  );
}

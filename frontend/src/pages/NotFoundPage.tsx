import { Button, Result } from "antd";
import { useNavigate } from "react-router-dom";

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <Result
      status="404"
      title="404"
      subTitle="页面不存在，请从左侧导航进入系统功能。"
      extra={
        <Button type="primary" onClick={() => navigate("/checkout")}>
          返回收银结算
        </Button>
      }
    />
  );
}

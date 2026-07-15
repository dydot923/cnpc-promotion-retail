import {
  AlertOutlined,
  CloudUploadOutlined,
  DashboardOutlined,
  GiftOutlined,
  PictureOutlined,
  SettingOutlined,
  ShoppingCartOutlined
} from "@ant-design/icons";
import { Layout, Menu, Select, Space, Typography } from "antd";
import type { MenuProps } from "antd";
import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import useBackendStatus from "../hooks/useBackendStatus";

const { Header, Sider, Content, Footer } = Layout;

type Role = "cashier" | "station_manager" | "operator" | "admin";

type NavItem = {
  key: string;
  label: string;
  path: string;
  icon: ReactNode;
  roles: Role[];
};

const roleOptions: { value: Role; label: string }[] = [
  { value: "cashier", label: "收银员" },
  { value: "station_manager", label: "站长" },
  { value: "operator", label: "运营" },
  { value: "admin", label: "管理员" }
];

const navItems: NavItem[] = [
  {
    key: "checkout",
    label: "收银结算",
    path: "/checkout",
    icon: <ShoppingCartOutlined />,
    roles: ["cashier", "station_manager", "operator", "admin"]
  },
  {
    key: "dashboard",
    label: "运营看板",
    path: "/dashboard",
    icon: <DashboardOutlined />,
    roles: ["station_manager", "operator", "admin"]
  },
  {
    key: "operation-campaigns",
    label: "运营发券",
    path: "/operation-campaigns",
    icon: <GiftOutlined />,
    roles: ["operator", "admin"]
  },
  {
    key: "import",
    label: "数据导入",
    path: "/import",
    icon: <CloudUploadOutlined />,
    roles: ["operator", "admin"]
  },
  {
    key: "inventory",
    label: "库存预警",
    path: "/inventory",
    icon: <AlertOutlined />,
    roles: ["station_manager", "operator", "admin"]
  },
  {
    key: "rules",
    label: "规则管理",
    path: "/rules",
    icon: <SettingOutlined />,
    roles: ["operator", "admin"]
  },
  {
    key: "poster",
    label: "AI 海报",
    path: "/poster",
    icon: <PictureOutlined />,
    roles: ["station_manager", "operator", "admin"]
  }
];

export default function AppLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const backendStatus = useBackendStatus();
  const [role, setRole] = useState<Role>(() => (localStorage.getItem("cnpc-role") as Role) || "cashier");
  const [now, setNow] = useState(() => new Date());
  const [lastCalculationMs, setLastCalculationMs] = useState(() => localStorage.getItem("lastCalculationMs") || "2");

  const allowedItems = useMemo(() => navItems.filter((item) => item.roles.includes(role)), [role]);
  const selectedKey = navItems.find((item) => location.pathname.startsWith(item.path))?.key;

  useEffect(() => {
    localStorage.setItem("cnpc-role", role);
    const isAllowed = allowedItems.some((item) => location.pathname.startsWith(item.path));
    if (!isAllowed && location.pathname !== "/") {
      navigate(allowedItems[0]?.path || "/checkout", { replace: true });
    }
  }, [allowedItems, location.pathname, navigate, role]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 1_000);
    const onCalculation = () => setLastCalculationMs(localStorage.getItem("lastCalculationMs") || "2");
    window.addEventListener("checkout-calculation-finished", onCalculation);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener("checkout-calculation-finished", onCalculation);
    };
  }, []);

  const menuItems: MenuProps["items"] = allowedItems.map((item) => ({
    key: item.key,
    icon: item.icon,
    label: item.label,
    onClick: () => navigate(item.path)
  }));

  return (
    <Layout className="app-shell">
      <Sider width={220} className="app-sider">
        <div className="sider-title">CNPC</div>
        <Menu theme="dark" mode="inline" selectedKeys={selectedKey ? [selectedKey] : []} items={menuItems} />
      </Sider>
      <Layout className="main-shell">
        <Header className="app-header">
          <div className="brand-block">
            <span className="brand-red-bar" />
            <div>
              <Typography.Title level={3}>中石油加油站智能零售系统</Typography.Title>
              <Typography.Text>CNPC Smart Retail</Typography.Text>
            </div>
          </div>
          <Space size={18} className="header-actions">
            <Typography.Text className="current-time">{formatDateTime(now)}</Typography.Text>
            <Select<Role> className="role-select" value={role} onChange={setRole} options={roleOptions} />
            <span className="status-pill">
              <span className={backendStatus.connected ? "status-dot online" : "status-dot offline"} />
              {backendStatus.connected ? "已连接" : "未连接"}
            </span>
          </Space>
          <div className="header-brand-stripe" />
        </Header>
        <Content className="app-content">
          <Outlet />
        </Content>
        <Footer className="app-footer">
          <span>
            <span className={backendStatus.connected ? "status-dot online" : "status-dot offline"} />
            {backendStatus.connected ? `已连接${backendStatus.latency ? ` ${backendStatus.latency}ms` : ""}` : "未连接"}
          </span>
          <span>规则版本: V22, 247 条 CONFIRMED</span>
          <span>上次计算: {lastCalculationMs}ms</span>
        </Footer>
      </Layout>
    </Layout>
  );
}

function formatDateTime(value: Date) {
  const parts = new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  }).formatToParts(value);
  const pick = (type: string) => parts.find((part) => part.type === type)?.value || "";
  return `${pick("year")}年${pick("month")}${pick("day")}日 ${pick("weekday")} ${pick("hour")}:${pick("minute")}`;
}

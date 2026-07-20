import {
  AlertOutlined,
  CalendarOutlined,
  CloudUploadOutlined,
  DashboardOutlined,
  GiftOutlined,
  PictureOutlined,
  SettingOutlined,
  ShoppingCartOutlined
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, Input, Layout, Menu, Popover, Select, Space, Tag, Typography } from "antd";
import type { MenuProps } from "antd";
import type { ReactNode } from "react";
import { useEffect, useMemo, useState } from "react";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { fetchBusinessClock, resetBusinessClock, updateBusinessClock } from "../api/businessClock";
import useBackendStatus from "../hooks/useBackendStatus";
import type { BusinessClock } from "../types";

const { Header, Sider, Content, Footer } = Layout;

type Role = "cashier" | "station_manager" | "operator" | "admin";

export type AppOutletContext = {
  businessDate: string;
};

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

const promotionTestDays = [7, 9, 17, 19, 27, 29];

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
    label: "活动验收",
    path: "/operation-campaigns",
    icon: <GiftOutlined />,
    roles: ["cashier", "station_manager", "operator", "admin"]
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
  const queryClient = useQueryClient();
  const { message } = App.useApp();
  const [role, setRole] = useState<Role>(() => (localStorage.getItem("cnpc-role") as Role) || "cashier");
  const [now, setNow] = useState(() => new Date());
  const [lastCalculationMs, setLastCalculationMs] = useState(() => localStorage.getItem("lastCalculationMs") || "2");
  const [businessClockOpen, setBusinessClockOpen] = useState(false);
  const [businessDateDraft, setBusinessDateDraft] = useState(() => formatIsoDate(new Date()));

  const businessClockQuery = useQuery({
    queryKey: ["business-clock"],
    queryFn: fetchBusinessClock,
    staleTime: 30_000,
    refetchInterval: 60_000
  });
  const businessClock = businessClockQuery.data;
  const effectiveBusinessDate = businessClock?.businessDate || formatIsoDate(now);

  const saveBusinessDateMutation = useMutation({
    mutationFn: updateBusinessClock,
    onSuccess: (updatedClock) => {
      queryClient.setQueryData<BusinessClock>(["business-clock"], updatedClock);
      setBusinessClockOpen(false);
      message.success(`业务日期已切换为 ${formatBusinessDate(updatedClock.businessDate)}`);
    },
    onError: (error) => message.error(error instanceof Error ? error.message : "业务日期保存失败")
  });
  const resetBusinessDateMutation = useMutation({
    mutationFn: resetBusinessClock,
    onSuccess: (updatedClock) => {
      queryClient.setQueryData<BusinessClock>(["business-clock"], updatedClock);
      setBusinessClockOpen(false);
      message.success("已恢复系统日期");
    },
    onError: (error) => message.error(error instanceof Error ? error.message : "恢复系统日期失败")
  });

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

  useEffect(() => {
    if (!businessClockOpen) {
      setBusinessDateDraft(effectiveBusinessDate);
    }
  }, [businessClockOpen, effectiveBusinessDate]);

  const menuItems: MenuProps["items"] = allowedItems.map((item) => ({
    key: item.key,
    icon: item.icon,
    label: item.label,
    onClick: () => navigate(item.path)
  }));

  const businessClockPending = saveBusinessDateMutation.isPending || resetBusinessDateMutation.isPending;
  const businessClockPanel = (
    <div className="business-clock-panel">
      <div className="business-clock-panel-header">
        <div>
          <strong>业务日期</strong>
          <small>促销规则判定日期</small>
        </div>
        <Tag color={businessClock?.overrideEnabled ? "orange" : "green"}>
          {businessClock?.overrideEnabled ? "测试模式" : "系统日期"}
        </Tag>
      </div>
      <label className="business-clock-field">
        <span>选择日期</span>
        <Input
          aria-label="业务测试日期"
          type="date"
          value={businessDateDraft}
          onChange={(event) => setBusinessDateDraft(event.target.value)}
        />
      </label>
      <div className="business-clock-quick-days">
        <span>逢 7 / 逢 9</span>
        <Space size={4} wrap>
          {promotionTestDays.map((day) => (
            <Button key={day} size="small" onClick={() => setBusinessDateDraft(dateWithDay(businessDateDraft, day))}>
              {day}日
            </Button>
          ))}
        </Space>
      </div>
      <div className="business-clock-actions">
        <Button
          disabled={!businessClock?.overrideEnabled}
          loading={resetBusinessDateMutation.isPending}
          onClick={() => resetBusinessDateMutation.mutate()}
        >
          恢复系统日期
        </Button>
        <Button
          type="primary"
          loading={saveBusinessDateMutation.isPending}
          disabled={!isIsoDate(businessDateDraft)}
          onClick={() => saveBusinessDateMutation.mutate(businessDateDraft)}
        >
          保存并应用
        </Button>
      </div>
    </div>
  );

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
            <Popover
              content={businessClockPanel}
              open={businessClockOpen}
              placement="bottomRight"
              trigger="click"
              onOpenChange={(open) => {
                setBusinessClockOpen(open);
                if (open) {
                  setBusinessDateDraft(effectiveBusinessDate);
                }
              }}
            >
              <Button
                type="text"
                className={`business-clock-trigger${businessClock?.overrideEnabled ? " overridden" : ""}`}
                icon={<CalendarOutlined />}
                loading={businessClockQuery.isFetching && !businessClock}
                disabled={businessClockPending}
              >
                <span className="business-clock-copy">
                  <strong>{formatBusinessDateTime(effectiveBusinessDate, now)}</strong>
                  <small>{businessClock?.overrideEnabled ? "测试日期，点击调整" : "业务日期，点击调整"}</small>
                </span>
              </Button>
            </Popover>
            <Select<Role> className="role-select" value={role} onChange={setRole} options={roleOptions} />
            <span className="status-pill">
              <span className={backendStatus.connected ? "status-dot online" : "status-dot offline"} />
              {backendStatus.connected ? "已连接" : "未连接"}
            </span>
          </Space>
          <div className="header-brand-stripe" />
        </Header>
        <Content className="app-content">
          <Outlet context={{ businessDate: effectiveBusinessDate } satisfies AppOutletContext} />
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

function formatBusinessDateTime(businessDate: string, now: Date) {
  const date = parseIsoDate(businessDate) || now;
  const parts = new Intl.DateTimeFormat("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
    weekday: "long"
  }).formatToParts(date);
  const timeParts = new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: false
  }).formatToParts(now);
  const pick = (type: string) => parts.find((part) => part.type === type)?.value || "";
  const pickTime = (type: string) => timeParts.find((part) => part.type === type)?.value || "";
  return `${pick("year")}年${pick("month")}月${pick("day")}日 ${pick("weekday")} ${pickTime("hour")}:${pickTime("minute")}`;
}

function formatBusinessDate(value: string) {
  const date = parseIsoDate(value);
  if (!date) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", { year: "numeric", month: "long", day: "numeric" }).format(date);
}

function formatIsoDate(value: Date) {
  const pad = (part: number) => String(part).padStart(2, "0");
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
}

function parseIsoDate(value: string) {
  if (!isIsoDate(value)) {
    return undefined;
  }
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function isIsoDate(value: string) {
  return /^\d{4}-\d{2}-\d{2}$/.test(value);
}

function dateWithDay(value: string, day: number) {
  const base = parseIsoDate(value) || new Date();
  const target = new Date(base.getFullYear(), base.getMonth(), day);
  return formatIsoDate(target);
}

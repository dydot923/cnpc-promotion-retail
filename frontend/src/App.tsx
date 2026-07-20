import { Suspense, lazy } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { Spin } from "antd";
import AppLayout from "./components/AppLayout";
import ErrorBoundary from "./components/ErrorBoundary";

const CheckoutPage = lazy(() => import("./pages/CheckoutPage"));
const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const ImportPage = lazy(() => import("./pages/ImportPage"));
const InventoryAlertPage = lazy(() => import("./pages/InventoryAlertPage"));
const OperationCampaignPage = lazy(() => import("./pages/OperationCampaignPage"));
const RuleManagementPage = lazy(() => import("./pages/RuleManagementPage"));
const PosterPage = lazy(() => import("./pages/PosterPage"));
const NotFoundPage = lazy(() => import("./pages/NotFoundPage"));

export default function App() {
  return (
    <ErrorBoundary>
      <BrowserRouter basename="/">
        <Suspense
          fallback={
            <div className="page-loading">
              <Spin aria-label="正在加载页面" />
            </div>
          }
        >
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/" element={<Navigate to="/checkout" replace />} />
              <Route path="/checkout" element={<CheckoutPage />} />
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/operation-campaigns" element={<OperationCampaignPage />} />
              <Route path="/import" element={<ImportPage />} />
              <Route path="/inventory" element={<InventoryAlertPage />} />
              <Route path="/rules" element={<RuleManagementPage />} />
              <Route path="/poster" element={<PosterPage />} />
              <Route path="*" element={<NotFoundPage />} />
            </Route>
          </Routes>
        </Suspense>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

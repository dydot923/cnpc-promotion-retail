import { apiRequest, ApiRequestError } from "./request";
import type {
  ImportBatch,
  ImportErrorRow,
  PromotionRuleAuditLog,
  PromotionRuleDraft,
  PromotionRuleVersion
} from "../types";

const actionBody = JSON.stringify({
  operatorId: "frontend-mvp",
  changeReason: "前端规则管理操作"
});

export function fetchPromotionDrafts(status: string): Promise<PromotionRuleDraft[]> {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return apiRequest<PromotionRuleDraft[]>(`/promotion-drafts${query}`);
}

export function confirmDraft(draftId: string): Promise<PromotionRuleVersion> {
  return apiRequest<PromotionRuleVersion>(`/promotion-drafts/${encodeURIComponent(draftId)}/confirm`, {
    method: "POST",
    body: actionBody
  });
}

export function rejectDraft(draftId: string): Promise<PromotionRuleDraft> {
  return apiRequest<PromotionRuleDraft>(`/promotion-drafts/${encodeURIComponent(draftId)}/reject`, {
    method: "POST",
    body: actionBody
  });
}

export function disableRule(ruleId: string): Promise<PromotionRuleVersion> {
  return apiRequest<PromotionRuleVersion>(`/promotion-rules/${encodeURIComponent(ruleId)}/disable`, {
    method: "POST",
    body: actionBody
  });
}

export function fetchAuditLogs(ruleId: string): Promise<PromotionRuleAuditLog[]> {
  return apiRequest<PromotionRuleAuditLog[]>(`/promotion-rules/${encodeURIComponent(ruleId)}/audit-logs`);
}

export function fetchImportBatches(): Promise<ImportBatch[]> {
  return apiRequest<ImportBatch[]>("/import-batches");
}

export function fetchImportErrors(
  importId: string,
  severity?: string,
  sheetName?: string,
  errorCode?: string
): Promise<ImportErrorRow[]> {
  const params = new URLSearchParams();
  if (severity) params.set("severity", severity);
  if (sheetName) params.set("sheetName", sheetName);
  if (errorCode) params.set("errorCode", errorCode);
  const query = params.toString() ? `?${params.toString()}` : "";
  return apiRequest<ImportErrorRow[]>(`/import-batches/${encodeURIComponent(importId)}/errors${query}`);
}

export async function exportImportErrors(
  importId: string,
  severity?: string,
  sheetName?: string,
  errorCode?: string
): Promise<Blob> {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";
  const params = new URLSearchParams();
  if (severity) params.set("severity", severity);
  if (sheetName) params.set("sheetName", sheetName);
  if (errorCode) params.set("errorCode", errorCode);
  params.set("operatorId", "frontend-mvp");
  const response = await fetch(`${API_BASE_URL}/import-batches/${encodeURIComponent(importId)}/errors/export?${params.toString()}`);
  if (!response.ok) {
    throw new ApiRequestError(`导出失败：${response.status}`, response.status);
  }
  return response.blob();
}

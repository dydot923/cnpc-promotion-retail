import { apiRequest } from "./request";
import type { PromotionRuleAuditLog, PromotionRuleDraft, PromotionRuleVersion } from "../types";

const actionBody = JSON.stringify({
  operatorId: "frontend-ui",
  changeReason: "前端规则管理操作"
});

export function fetchRuleDrafts(status?: string) {
  const query = status ? `?status=${encodeURIComponent(status)}` : "";
  return apiRequest<PromotionRuleDraft[]>(`/promotion-drafts${query}`);
}

export function confirmRuleDraft(draftId: string) {
  return apiRequest<PromotionRuleVersion>(`/promotion-drafts/${encodeURIComponent(draftId)}/confirm`, {
    method: "POST",
    body: actionBody
  });
}

export function deprecateRule(ruleId: string) {
  return apiRequest<PromotionRuleVersion>(`/promotion-rules/${encodeURIComponent(ruleId)}/disable`, {
    method: "POST",
    body: actionBody
  });
}

export function fetchRuleAuditLogs(ruleId: string) {
  return apiRequest<PromotionRuleAuditLog[]>(`/promotion-rules/${encodeURIComponent(ruleId)}/audit-logs`);
}

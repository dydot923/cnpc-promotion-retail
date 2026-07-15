import { apiRequest } from "./request";
import type { AuditLog } from "../types";

export type AuditLogQuery = {
  actionType?: string;
  entityType?: string;
  entityId?: string;
  operatorId?: string;
  limit?: number;
};

export function fetchAuditLogs(query: AuditLogQuery): Promise<AuditLog[]> {
  const params = new URLSearchParams();
  if (query.actionType) params.set("actionType", query.actionType);
  if (query.entityType) params.set("entityType", query.entityType);
  if (query.entityId) params.set("entityId", query.entityId);
  if (query.operatorId) params.set("operatorId", query.operatorId);
  if (query.limit) params.set("limit", String(query.limit));
  const queryStr = params.toString();
  return apiRequest<AuditLog[]>(`/audit-logs${queryStr ? `?${queryStr}` : ""}`);
}

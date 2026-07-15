import { apiRequest } from "./request";
import type { InventoryAlert, InventoryAlertHandleRequest } from "../types";

export function fetchInventoryAlerts(): Promise<InventoryAlert[]> {
  return apiRequest<InventoryAlert[]>("/inventory/alerts");
}

export function markInventoryAlertHandled(
  alertId: string,
  request: InventoryAlertHandleRequest = {}
): Promise<InventoryAlert> {
  return apiRequest<InventoryAlert>(`/inventory/alerts/${encodeURIComponent(alertId)}/handled`, {
    method: "PATCH",
    body: JSON.stringify(request)
  });
}

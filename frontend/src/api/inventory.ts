import { apiRequest } from "./request";
import type {
  InventoryAlert,
  InventoryAlertHandleRequest,
  InventoryItem,
  InventoryReplenishmentRequest,
  InventoryReplenishmentResponse
} from "../types";

export function fetchInventoryItems(): Promise<InventoryItem[]> {
  return apiRequest<InventoryItem[]>("/inventory/items");
}

export function replenishInventory(
  productCode: string,
  request: InventoryReplenishmentRequest
): Promise<InventoryReplenishmentResponse> {
  return apiRequest<InventoryReplenishmentResponse>(`/inventory/items/${encodeURIComponent(productCode)}/replenish`, {
    method: "POST",
    body: JSON.stringify(request)
  });
}

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

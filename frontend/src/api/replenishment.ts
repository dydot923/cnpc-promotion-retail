import { apiRequest, ApiRequestError } from "./request";
import type { ReplenishmentList } from "../types";

export function createReplenishmentList(): Promise<ReplenishmentList> {
  return apiRequest<ReplenishmentList>("/replenishment/lists", { method: "POST" });
}

export function fetchReplenishmentList(listId: string): Promise<ReplenishmentList> {
  return apiRequest<ReplenishmentList>(`/replenishment/lists/${encodeURIComponent(listId)}`);
}

export async function exportReplenishmentList(listId: string): Promise<Blob> {
  const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";
  const url = `${API_BASE_URL}/replenishment/lists/${encodeURIComponent(listId)}/export`;
  const response = await fetch(url);
  if (!response.ok) {
    throw new ApiRequestError(`导出失败：${response.status}`, response.status);
  }
  return response.blob();
}

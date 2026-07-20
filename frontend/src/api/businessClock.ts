import type { BusinessClock } from "../types";
import { apiRequest } from "./request";

export function fetchBusinessClock(): Promise<BusinessClock> {
  return apiRequest<BusinessClock>("/system/business-clock");
}

export function updateBusinessClock(businessDate: string): Promise<BusinessClock> {
  return apiRequest<BusinessClock>("/system/business-clock", {
    method: "PUT",
    body: JSON.stringify({ businessDate })
  });
}

export function resetBusinessClock(): Promise<BusinessClock> {
  return apiRequest<BusinessClock>("/system/business-clock", {
    method: "DELETE"
  });
}

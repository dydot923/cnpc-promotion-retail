import { apiRequest } from "./request";
import type { OperationCampaignDefinition, OperationCouponIssueResponse } from "../types";

export type OperationCampaignPayload = Record<string, string | number | boolean | undefined>;

export function fetchOperationCampaigns(): Promise<OperationCampaignDefinition[]> {
  return apiRequest<OperationCampaignDefinition[]>("/operation-campaigns");
}

export function issueOperationCampaignCoupon(
  endpoint: string,
  payload: OperationCampaignPayload
): Promise<OperationCouponIssueResponse> {
  return apiRequest<OperationCouponIssueResponse>(endpoint, {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

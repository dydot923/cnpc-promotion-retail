import { apiRequest } from "./request";
import type {
  BenefitPackage,
  BenefitPackagePurchaseResponse,
  PointsExchangeResponse,
  PointsLotteryDrawResponse
} from "../types";

export function exchangePoints(memberCode: string, request: {
  pointsUsed: number;
  businessDate: string;
  stationCode?: string;
  operatorId?: string;
  operatorName?: string;
}): Promise<PointsExchangeResponse> {
  return apiRequest<PointsExchangeResponse>(`/members/${encodeURIComponent(memberCode)}/points/exchange-discount`, {
    method: "POST",
    body: JSON.stringify(request)
  });
}

export function drawPointsLottery(memberCode: string, request: {
  businessDate: string;
  stationCode?: string;
  operatorId?: string;
  operatorName?: string;
}): Promise<PointsLotteryDrawResponse> {
  return apiRequest<PointsLotteryDrawResponse>(`/members/${encodeURIComponent(memberCode)}/points/lottery-draws`, {
    method: "POST",
    body: JSON.stringify(request)
  });
}

export function fetchBenefitPackages(): Promise<BenefitPackage[]> {
  return apiRequest<BenefitPackage[]>("/benefit-packages");
}

export function purchaseBenefitPackage(packageCode: string, request: {
  memberCode: string;
  stationCode?: string;
  paymentAmount: number;
  checkoutTransactionNo?: string;
  operatorId?: string;
  operatorName?: string;
}): Promise<BenefitPackagePurchaseResponse> {
  return apiRequest<BenefitPackagePurchaseResponse>(`/benefit-packages/${encodeURIComponent(packageCode)}/purchase`, {
    method: "POST",
    body: JSON.stringify(request)
  });
}

import { apiRequest } from "./request";
import type {
  CheckoutCalculateRequest,
  CheckoutCalculateResponse,
  CheckoutExchangeOffer,
  CheckoutConfirmRequest,
  CheckoutConfirmationResponse,
  CheckoutTransactionResponse
} from "../types";

export function calculateCheckout(request: CheckoutCalculateRequest): Promise<CheckoutCalculateResponse> {
  return apiRequest<CheckoutCalculateResponse>("/checkout/calculate", {
    method: "POST",
    body: JSON.stringify(request)
  });
}

export function fetchExchangeOffers(params: {
  fuelType?: string;
  fuelAmount?: number;
  businessDate?: string;
  stationType?: string;
  stationProvince?: string;
}): Promise<CheckoutExchangeOffer[]> {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== "") {
      search.set(key, String(value));
    }
  });
  return apiRequest<CheckoutExchangeOffer[]>(`/checkout/exchange-offers?${search.toString()}`);
}

export function confirmCheckout(request: CheckoutConfirmRequest): Promise<CheckoutConfirmationResponse> {
  return apiRequest<CheckoutConfirmationResponse>("/checkout/confirm", {
    method: "POST",
    body: JSON.stringify(request)
  });
}

export function fetchCheckoutConfirmation(confirmationId: string): Promise<CheckoutConfirmationResponse> {
  return apiRequest<CheckoutConfirmationResponse>(`/checkout/confirmations/${encodeURIComponent(confirmationId)}`);
}

export function fetchCheckoutConfirmationsByCalculationId(
  calculationId: string
): Promise<CheckoutConfirmationResponse[]> {
  return apiRequest<CheckoutConfirmationResponse[]>(`/checkout/confirmations?calculationId=${encodeURIComponent(calculationId)}`);
}

export function fetchCheckoutTransaction(txnNo: string): Promise<CheckoutTransactionResponse> {
  return apiRequest<CheckoutTransactionResponse>(`/checkout/transactions/${encodeURIComponent(txnNo)}`);
}

export function fetchCheckoutTransactions(limit = 50): Promise<CheckoutTransactionResponse[]> {
  return apiRequest<CheckoutTransactionResponse[]>(`/checkout/transactions?limit=${encodeURIComponent(String(limit))}`);
}

export function fetchCheckoutRecord(txnNo: string): Promise<CheckoutTransactionResponse> {
  return apiRequest<CheckoutTransactionResponse>(`/checkout/records/${encodeURIComponent(txnNo)}`);
}

export function fetchCheckoutRecords(params: {
  memberCode?: string;
  stationCode?: string;
  startDate?: string;
  endDate?: string;
  limit?: number;
} = {}): Promise<CheckoutTransactionResponse[]> {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== "") {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  return apiRequest<CheckoutTransactionResponse[]>(`/checkout/records${query ? `?${query}` : ""}`);
}

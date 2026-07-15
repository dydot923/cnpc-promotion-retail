import type { ApiResponse } from "../types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "/api";
const REQUEST_TIMEOUT_MS = Number(import.meta.env.VITE_API_TIMEOUT) || 10_000;

export class ApiRequestError extends Error {
  constructor(
    message: string,
    public status?: number
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}

type RequestOptions = RequestInit & {
  unwrap?: boolean;
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  const url = path.startsWith("http") ? path : `${API_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`;
  const isFormData = options.body instanceof FormData;

  try {
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
      headers: {
        ...(isFormData ? {} : { "Content-Type": "application/json" }),
        ...(options.headers || {})
      }
    });

    if (!response.ok) {
      const payload = (await response.json().catch(() => null)) as Partial<ApiResponse<T>> | null;
      throw new ApiRequestError(payload?.message || `接口请求失败：${response.status}`, response.status);
    }

    if (options.unwrap === false) {
      return (await response.json()) as T;
    }

    const payload = (await response.json()) as ApiResponse<T>;
    if (!payload.success) {
      throw new ApiRequestError(payload.message || "接口返回失败", response.status);
    }
    return payload.data;
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") {
      throw new ApiRequestError("网络连接超时，请检查后端服务");
    }
    if (error instanceof TypeError) {
      throw new ApiRequestError("网络连接失败，请检查后端服务");
    }
    throw error;
  } finally {
    window.clearTimeout(timeout);
  }
}

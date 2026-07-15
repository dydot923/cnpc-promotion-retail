import { apiRequest } from "./request";
import type { ProductCatalogItem } from "../types";

export function searchProducts(keyword: string): Promise<ProductCatalogItem[]> {
  return apiRequest<ProductCatalogItem[]>(`/products/search?keyword=${encodeURIComponent(keyword)}`);
}

export function fetchProductByBarcode(barcode: string): Promise<ProductCatalogItem> {
  return apiRequest<ProductCatalogItem>(`/products/by-barcode/${encodeURIComponent(barcode)}`);
}

export function fetchProductByCode(productCode: string): Promise<ProductCatalogItem> {
  return apiRequest<ProductCatalogItem>(`/products/${encodeURIComponent(productCode)}`);
}

import { apiRequest } from "./request";
import type { ImportBatch, ImportErrorRow } from "../types";

export type ImportUploadType = "prices" | "inventory" | "promotions" | "coupons";

export function uploadImportFile(type: ImportUploadType, file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return apiRequest<ImportBatch | { importId?: string; insertedCount?: number; invalidCount?: number }>(`/import/${type}`, {
    method: "POST",
    body: formData
  });
}

export function fetchImportBatches() {
  return apiRequest<ImportBatch[]>("/import-batches");
}

export function fetchImportErrors(importId: string) {
  return apiRequest<ImportErrorRow[]>(`/import-batches/${encodeURIComponent(importId)}/errors`);
}

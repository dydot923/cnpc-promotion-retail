import { apiRequest } from "./request";
import type { Station } from "../types";

export function fetchStations(): Promise<Station[]> {
  return apiRequest<Station[]>("/stations");
}

import { apiClient } from "./client";
import type { AreaResponse } from "../types/area";

export const areaApi = {
  async list(signal?: AbortSignal): Promise<AreaResponse[]> {
    const response = await apiClient.get<AreaResponse[]>(
      "/api/areas",
      { signal },
    );

    return response.data;
  },
};
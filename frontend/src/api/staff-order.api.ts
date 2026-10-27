import { apiClient } from "./client";

import type { PageResponse } from "../types/api";

import type {
  StaffOrderFilters,
  StaffOrderSummaryResponse,
  StaffOrderDetailResponse,
} from "../types/staff-order";

interface StaffOrderListParams extends StaffOrderFilters {
  page: number;
  size: number;
}

export const staffOrderApi = {
  async list(
    params: StaffOrderListParams,
    signal?: AbortSignal,
  ): Promise<PageResponse<StaffOrderSummaryResponse>> {
    const { data } = await apiClient.get<
      PageResponse<StaffOrderSummaryResponse>
    >("/api/staff/orders", {
      signal,

      params: {
        search: params.search.trim() || undefined,
        date: params.date || undefined,
        status: params.status || undefined,
        page: params.page,
        size: params.size,
      },
    });

    return data;
  },

  async getDetail(
    id: number,
    signal?: AbortSignal,
  ): Promise<StaffOrderDetailResponse> {
    const { data } =
      await apiClient.get<StaffOrderDetailResponse>(
        `/api/staff/orders/${id}`,
        { signal },
      );

    return data;
  },
};
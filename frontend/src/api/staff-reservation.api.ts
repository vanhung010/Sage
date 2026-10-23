import { apiClient } from "./client";

import type { PageResponse } from "../types/api";
import type {
  StaffReservationFilterForm,
  StaffReservationItem,
  StaffReservationStatistics,
} from "../types/staff-reservation";

function toParams(filters: StaffReservationFilterForm) {
  return {
    date: filters.date,
    search: filters.search.trim() || undefined,
    status: filters.status || undefined,
    areaId: filters.areaId
      ? Number(filters.areaId)
      : undefined,
  };
}

export const staffReservationApi = {
  async list(
    filters: StaffReservationFilterForm,
    page: number,
    size: number,
    signal?: AbortSignal,
  ): Promise<PageResponse<StaffReservationItem>> {
    const response = await apiClient.get<
      PageResponse<StaffReservationItem>
    >("/api/staff/reservations", {
      params: {
        ...toParams(filters),
        page,
        size,
      },
      signal,
    });

    return response.data;
  },

  async summary(
    filters: StaffReservationFilterForm,
    signal?: AbortSignal,
  ): Promise<StaffReservationStatistics> {
    const response =
      await apiClient.get<StaffReservationStatistics>(
        "/api/staff/reservations/summary",
        {
          params: toParams(filters),
          signal,
        },
      );

    return response.data;
  },
};
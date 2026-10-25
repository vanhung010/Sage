import { apiClient } from "./client";

import type { PageResponse } from "../types/api";
import type {
  StaffReservationFilterForm,
  StaffReservationDetail,
  StaffReservationItem,
  StaffReservationStatistics,
} from "../types/staff-reservation";


import type {
  CreateGuestReservationRequest,
  CreateGuestReservationResponse,
} from "../types/staff-reservation";

import type {
  StaffReservationCommand,
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
  async getDetail(
    id: number,
    signal?: AbortSignal,
  ): Promise<StaffReservationDetail> {
    const response = await apiClient.get<StaffReservationDetail>(
      `/api/staff/reservations/${id}`,
      { signal },
    );

    return response.data;
  },

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

  async createGuest(
  request: CreateGuestReservationRequest,): Promise<CreateGuestReservationResponse> {
  const response =
    await apiClient.post<CreateGuestReservationResponse>(
      "/api/staff/reservations/guest",
      request,
    );

  return response.data;
},

  async changeStatus(
    id: number,
    command: StaffReservationCommand,
  ): Promise<StaffReservationDetail> {
    const body =
      "reason" in command
        ? { reason: command.reason.trim() }
        : undefined;

    const response = await apiClient.post<StaffReservationDetail>(
      `/api/staff/reservations/${id}/${command.action}`,
      body,
    );

    return response.data;
  },
};

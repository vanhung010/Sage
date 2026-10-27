import { apiClient } from "./client";

import type { PageResponse } from "../types/api";
import type {
  StaffReservationFilterForm,
  StaffReservationDetail,
  StaffReservationItem,
  StaffReservationStatistics,
} from "../types/staff-reservation";

import type {
  MoveTableOptionsResponse,
  MoveTableRequest,
} from "../types/staff-move-table";

import type {
  CreateGuestReservationRequest,
  CreateGuestReservationResponse,
} from "../types/staff-reservation";

import type {
  StaffDurationChanged,
  StaffDurationChangeRequest,
  StaffDurationPreview,
} from "../types/staff-reservation-duration";

import type { StaffReservationCommand, } from "../types/staff-reservation";

import type { ReservationTimelineData, } from "../components/ui/ReservationTimeline";

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
  async previewDuration(
  id: number,
  endTime: string,
  signal?: AbortSignal,
): Promise<StaffDurationPreview> {
  const response = await apiClient.post<StaffDurationPreview>(
    `/api/staff/reservations/${id}/change-duration/preview`,
    { endTime },
    { signal },
  );

  return response.data;
},

async changeDuration(
  id: number,
  request: StaffDurationChangeRequest,
): Promise<StaffDurationChanged> {
  const response = await apiClient.post<StaffDurationChanged>(
    `/api/staff/reservations/${id}/change-duration`,
    request,
  );

  return response.data;
},
async getMoveTableOptions(
  id: number,
  areaId: number | null,
  signal?: AbortSignal,
): Promise<MoveTableOptionsResponse> {
  const response = await apiClient.get<MoveTableOptionsResponse>(
    `/api/staff/reservations/${id}/move-table/options`,
    {
      params: {
        areaId: areaId ?? undefined,
      },
      signal,
    },
  );

  return response.data;
},

async moveTable(
  id: number,
  request: MoveTableRequest,
): Promise<StaffReservationDetail> {
  const response = await apiClient.post<StaffReservationDetail>(
    `/api/staff/reservations/${id}/move-table`,
    request,
  );

  return response.data;
},
async timeline(
  date: string,
  areaId: number | null,
  signal?: AbortSignal,
): Promise<ReservationTimelineData> {
  const response = await apiClient.get<ReservationTimelineData>(
    "/api/staff/reservations/timeline",
    {
      params: {
        date,
        areaId: areaId ?? undefined,
      },
      signal,
    },
  );

  return response.data;
},
};

import { apiClient } from "./client";

import type {
  AvailabilityParams,
  AvailabilityResponse,
  CreateReservationRequest,
  RescheduleReservationRequest,
  ReservationResponse,
} from "../types/reservation";

import type { PageResponse } from "../types/api";
import type { ReservationStatus } from "../types/reservation";

export type * from "../types/reservation";

const CUSTOMER_RESERVATIONS = "/api/customer/reservations";

export const reservationApi = {
  async getDetail(
    id: number,
    signal?: AbortSignal,
  ): Promise<ReservationResponse> {
    const response = await apiClient.get<ReservationResponse>(
      `${CUSTOMER_RESERVATIONS}/${id}`,
      { signal },
    );

    return response.data;
  },

  async getRescheduleAvailability(
    id: number,
    params: AvailabilityParams,
    signal?: AbortSignal,
  ): Promise<AvailabilityResponse> {
    const response = await apiClient.get<AvailabilityResponse>(
      `${CUSTOMER_RESERVATIONS}/${id}/availability`,
      { params, signal },
    );

    return response.data;
  },

  async reschedule(
    id: number,
    request: RescheduleReservationRequest,
  ): Promise<ReservationResponse> {
    const response = await apiClient.post<ReservationResponse>(
      `${CUSTOMER_RESERVATIONS}/${id}/reschedule`,
      request,
    );

    return response.data;
  },

  async getAvailability(
    params: AvailabilityParams,
    signal?: AbortSignal,
  ): Promise<AvailabilityResponse> {
    const response = await apiClient.get<AvailabilityResponse>(
      "/api/availability",
      { params, signal },
    );

    return response.data;
  },

  async create(
    request: CreateReservationRequest,
  ): Promise<ReservationResponse> {
    const response = await apiClient.post<ReservationResponse>(
      CUSTOMER_RESERVATIONS,
      request,
    );

    return response.data;
  },
  async cancel(id: number): Promise<ReservationResponse> {
  const response = await apiClient.patch<ReservationResponse>(
    `${CUSTOMER_RESERVATIONS}/${id}/cancel`,
  );

  return response.data;
},

async listMine(
  params: {
    scope: "all" | "upcoming" | "history";
    status?: ReservationStatus;
    page?: number;
    size?: number;
  },
  signal?: AbortSignal,
): Promise<PageResponse<ReservationResponse>> {
  const response = await apiClient.get<PageResponse<ReservationResponse>>(
    CUSTOMER_RESERVATIONS,
    { params, signal },
  );
  return response.data;
},
};
import { apiClient } from "./client";

import type {
  AvailabilityParams,
  AvailabilityResponse,
  CreateReservationRequest,
  RescheduleReservationRequest,
  ReservationResponse,
} from "../types/reservation";

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
};
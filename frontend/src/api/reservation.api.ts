import { apiClient } from "./client";

export interface AvailabilitySlot {
  startTime: string;
  endTime: string;
}

export interface AvailabilityResponse {
  date: string;
  partySize: number;
  durationMinutes: number;
  areaId: number | null;
  slots: AvailabilitySlot[];
  suggestions: AvailabilitySlot[];
}

export interface AvailabilityParams {
  date: string;
  partySize: number;
  durationMinutes: number;
  areaId?: number;
}

export interface CreateReservationRequest {
  startTime: string;
  endTime: string;
  partySize: number;
  requestedAreaId: number | null;
  specialNote: string | null;
}

export interface ReservationResponse {
  id: number;
  code: string;
  status: "PENDING" | "CONFIRMED";
  startTime: string;
  endTime: string;
}

export const reservationApi = {
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
      "/api/reservations",
      request,
    );

    return response.data;
  },
};
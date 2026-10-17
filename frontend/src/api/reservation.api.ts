import { apiClient } from "./client";

export type ReservationStatus =
  | "PENDING"
  | "CONFIRMED"
  | "ARRIVED"
  | "COMPLETED"
  | "NO_SHOW"
  | "CANCELLED"
  | "REJECTED";

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
  partySize: number;
  startTime: string;
  endTime: string;
  status: ReservationStatus;
  requestedArea: {
    id: number;
    name: string;
    description: string | null;
  } | null;
  specialNote: string | null;
  rescheduleCount: number;
  rejectionReason: string | null;
  cancellationReason: string | null;
  createdAt: string;
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
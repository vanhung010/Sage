import type { ReservationStatus } from "./reservation";

export interface StaffDurationConflict {
  code: string;
  message: string;
  blockedFrom: string;
  blockedUntil: string;
}

export interface StaffDurationPreview {
  reservationId: number;
  version: number;
  status: ReservationStatus;
  startTime: string;
  currentEndTime: string;
  endTime: string;
  bufferMinutes: number;
  currentDurationMinutes: number;
  durationMinutes: number;
  differenceMinutes: number;
  occupiedUntil: string;
  allowed: boolean;
  conflicts: StaffDurationConflict[];
  suggestedEndTime: string | null;
}

export interface StaffDurationChangeRequest {
  endTime: string;
  expectedVersion: number;
  reason?: string;
}

export interface StaffDurationChanged {
  reservationId: number;
  reservationCode: string;
  version: number;
  status: ReservationStatus;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  bufferMinutes: number;
  occupiedUntil: string;
  updatedAt: string;
}

export interface StaffDurationNavigationState {
  durationChanged?: StaffDurationChanged;
}
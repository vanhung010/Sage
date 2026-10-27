import type { ReservationStatus } from "../../../types/reservation";

export type TimelineState = "default" | "loading" | "empty" | "error";

export type TimelineReservationStatus = Extract<
  ReservationStatus,
  "PENDING" | "CONFIRMED" | "ARRIVED" | "COMPLETED"
>;

export interface TimelineReservation {
  id: number;
  code: string;
  customerName: string | null;
  partySize: number;
  status: TimelineReservationStatus;
  startTime: string;
  endTime: string;
  bufferMinutes: number;
  occupiedUntil: string;
}

export interface TimelineSuspension {
  id: number;
  startTime: string;
  endTime: string;
  reason: string | null;
}

export interface TimelineTable {
  id: number;
  code: string;
  active: boolean;

  area: {
    id: number;
    name: string;
    active: boolean;
  };

  minCapacity: number;
  maxCapacity: number;

  reservations: TimelineReservation[];
  suspensions: TimelineSuspension[];
}

export interface ReservationTimelineData {
  date: string;
  timezone: "Asia/Ho_Chi_Minh";

  openingHours: {
    closed: boolean;
    openTime: string | null;
    closeTime: string | null;
    closedReason: string | null;
  };

  gridStepMinutes: number;
  tables: TimelineTable[];
}
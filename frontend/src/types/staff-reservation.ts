import type { ReservationStatus } from "./reservation";

export interface StaffReservationItem {
  id: number;
  code: string;

  customerName: string | null;
  customerPhone: string | null;

  startTime: string;
  endTime: string;
  partySize: number;

  tableId: number | null;
  tableCode: string | null;

  areaId: number | null;
  areaName: string | null;

  status: ReservationStatus;
}

export interface StaffReservationStatistics {
  pendingCount: number;
}

/** Giá trị của các control trên giao diện. */
export interface StaffReservationFilterForm {
  date: string;
  search: string;
  status: ReservationStatus | "";
  areaId: string;
}

export type StaffReservationFilterKey =
  keyof StaffReservationFilterForm;
import type { ReservationStatus } from "./reservation";
import type { AccountRole } from "./auth";

export interface StaffReservationArea {
  id: number;
  name: string;
}

export interface StaffReservationLog {
  id: number;
  eventType:
    | "CREATED"
    | "STATUS_CHANGED"
    | "TABLE_CHANGED"
    | "TIME_CHANGED"
    | "RESCHEDULED";
  changedAt: string;
  actorName: string | null;
  actorRole: AccountRole | null;
  oldValue: Record<string, unknown> | null;
  newValue: Record<string, unknown> | null;
  reason: string | null;
}

/** Contract của StaffReservationDetailResponse ở backend. */
export interface StaffReservationDetail {
  id: number;
  code: string;
  status: ReservationStatus;
  source: "ONLINE" | "STAFF_GUEST";
  customerName: string | null;
  customerPhone: string | null;
  partySize: number;
  startTime: string;
  endTime: string;
  table: {
    id: number;
    code: string;
    area: StaffReservationArea;
  };
  requestedArea: StaffReservationArea | null;
  specialNote: string | null;
  bufferMinutes: number;
  rescheduleCount: number;
  cancellationReason: string | null;
  rejectionReason: string | null;
  createdAt: string;
  updatedAt: string;
  approvalDeadline: string | null;
  noShowAllowedAt: string | null;
  logs: StaffReservationLog[];
}

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

  export interface CreateGuestReservationRequest {
  name: string;
  phone: string;
  date: string;
  timeStart: string;
  timeEnd: string;
  partySize: number;
  areaId?: number | null;
  description?: string | null;
}

export interface CreateGuestReservationResponse {
  reservationCode: string;
  name: string;
  tableCode: string;
  partySize: number;
  startTime: string;
  endTime: string;
}

/** Dữ liệu chuyển từ trang tạo đơn về danh sách. */
export interface StaffReservationNavigationState {
  guestCreated?: CreateGuestReservationResponse;
}
export type StaffReservationAction =
  | "approve"
  | "reject"
  | "cancel"
  | "arrive"
  | "no-show"
  | "complete";

// TypeScript bắt buộc có reason khi từ chối hoặc hủy.
export type StaffReservationCommand =
  | {
      action: "reject" | "cancel";
      reason: string;
    }
  | {
      action: "approve" | "arrive" | "no-show" | "complete";
    };
import type {
  AvailabilitySlot,
  ReservationResponse,
} from "../types/reservation";

export const RESERVATION_ZONE = "Asia/Ho_Chi_Minh";

// Các giá trị hiển thị theo quy tắc hiện tại.
// Backend vẫn kiểm tra cấu hình và điều kiện cuối cùng.
export const MAX_RESCHEDULES = 2;
export const CHANGE_DEADLINE_HOURS = 2;

export function dateInputValue(
  value: string | number = Date.now(),
): string {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: RESERVATION_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date(value));

  const part = (type: string) =>
    parts.find((item) => item.type === type)?.value;

  return `${part("year")}-${part("month")}-${part("day")}`;
}

export function addDays(value: string, days: number): string {
  const [year, month, day] = value.split("-").map(Number);

  return new Date(Date.UTC(year, month - 1, day + days))
    .toISOString()
    .slice(0, 10);
}

export function displayDate(value: string): string {
  if (!value) return "—";

  const [year, month, day] = value.slice(0, 10).split("-");

  return `${day}/${month}/${year}`;
}

export function displayTime(value: string): string {
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: RESERVATION_ZONE,
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(new Date(value));
}

export function timeRange(value: AvailabilitySlot): string {
  return `${displayTime(value.startTime)} – ${displayTime(value.endTime)}`;
}

export function reservationDeadline(
  reservation: ReservationResponse,
): number {
  return (
    Date.parse(reservation.startTime) -
    CHANGE_DEADLINE_HOURS * 3_600_000
  );
}

export function remainingReschedules(
  reservation: ReservationResponse,
): number {
  return Math.max(
    0,
    MAX_RESCHEDULES - reservation.rescheduleCount,
  );
}

export function rescheduleIneligibility(
  reservation: ReservationResponse,
  now = Date.now(),
): string {
  if (
    reservation.status !== "PENDING" &&
    reservation.status !== "CONFIRMED"
  ) {
    return "Trạng thái hiện tại không cho phép đổi lịch đơn đặt bàn này.";
  }

  if (remainingReschedules(reservation) === 0) {
    return "Đơn đã sử dụng hết 2 lần đổi lịch.";
  }

  // Đúng thời hạn vẫn được phép đổi.
  if (now > reservationDeadline(reservation)) {
    return "Đã quá thời hạn đổi lịch. Bạn cần đổi ít nhất 2 giờ trước lịch hẹn hiện tại.";
  }

  return "";
}

export function sameSlot(
  a: AvailabilitySlot,
  b: AvailabilitySlot,
): boolean {
  return (
    Date.parse(a.startTime) === Date.parse(b.startTime) &&
    Date.parse(a.endTime) === Date.parse(b.endTime)
  );
}

// Phát hiện đơn bị cập nhật từ một request/tab khác.
export function reservationFingerprint(
  reservation: ReservationResponse,
): string {
  return JSON.stringify([
    Date.parse(reservation.startTime),
    Date.parse(reservation.endTime),
    reservation.partySize,
    reservation.requestedArea?.id ?? null,
    reservation.status,
    reservation.rescheduleCount,
  ]);
}
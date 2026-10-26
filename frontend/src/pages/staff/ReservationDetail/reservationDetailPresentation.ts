import type { ActivityTimelineEvent } from "../../../components/ui/ActivityTimeline";
import type { ButtonVariant } from "../../../components/ui/Button";
import type { ReservationStatus } from "../../../types/reservation";
import type { StaffReservationDetail, StaffReservationLog } from "../../../types/staff-reservation";
import { dateInputValue, displayDate, displayTime } from "../../../utils/reservation";
import { RESERVATION_STATUS_META } from "../Reservations/reservationPresentation";
import type {
  StaffReservationAction,
} from "../../../types/staff-reservation";

export function detailDate(value: string) {
  return Number.isFinite(Date.parse(value)) ? displayDate(dateInputValue(value)) : "—";
}

export function detailTime(value: string) {
  return Number.isFinite(Date.parse(value)) ? displayTime(value) : "—";
}

export function detailDateTime(value: string) {
  return `${detailTime(value)} ngày ${detailDate(value)}`;
}

export function cleanupEndsAt(reservation: StaffReservationDetail) {
  const end = Date.parse(reservation.endTime) + reservation.bufferMinutes * 60_000;
  return Number.isFinite(end) ? new Date(end).toISOString() : "";
}

interface DetailAction {
  label: string;
  variant: ButtonVariant;
  action?: StaffReservationAction;
   navigation?: "change-duration";
}

export const DETAIL_ACTIONS: Record<
  ReservationStatus,
  readonly DetailAction[]
> = {
  PENDING: [
    { label: "Từ chối", variant: "destructive", action: "reject" },
    { label: "Chuyển bàn", variant: "outline" },
    { label: "Hủy thay khách", variant: "ghost", action: "cancel" },
    { label: "Duyệt đơn", variant: "primary", action: "approve" },
  ],
  CONFIRMED: [
    { label: "Chuyển bàn", variant: "outline" },
    {
  label: "Đổi thời lượng",
  variant: "outline",
  navigation: "change-duration",
},
    { label: "Hủy thay khách", variant: "ghost", action: "cancel" },
    { label: "Khách đã đến", variant: "primary", action: "arrive" },
  ],
  ARRIVED: [
    {
  label: "Đổi thời lượng",
  variant: "outline",
  navigation: "change-duration",
},
    { label: "Hoàn thành", variant: "primary", action: "complete" },
  ],
  COMPLETED: [],
  CANCELLED: [],
  REJECTED: [],
  NO_SHOW: [],
};

function isStatus(value: unknown): value is ReservationStatus {
  return typeof value === "string" && Object.hasOwn(RESERVATION_STATUS_META, value);
}

export function newestLogs(logs: readonly StaffReservationLog[]) {
  return [...logs].sort((a, b) =>
    Date.parse(b.changedAt) - Date.parse(a.changedAt) || b.id - a.id,
  );
}

function statusLog(reservation: StaffReservationDetail, status: ReservationStatus) {
  return newestLogs(reservation.logs).find((log) =>
    log.eventType === "STATUS_CHANGED" && log.newValue?.status === status,
  );
}

function reasonDescription(reason: string | null, released: string) {
  const text = reason?.trim();
  if (!text) return released;
  return `Lý do: ${text}${/[.!?]$/.test(text) ? "" : "."} ${released}`;
}

export function reservationNotice(reservation: StaffReservationDetail): {
  tone: "info" | "warning" | "success";
  title: string;
  description: string;
} {
  const table = reservation.table?.code ? `Bàn ${reservation.table.code}` : "Bàn";
  const released = `${table} đã được giải phóng khỏi lượt đặt này.`;

  switch (reservation.status) {
    case "PENDING":
      return {
        tone: "warning",
        title: reservation.approvalDeadline
          ? `Duyệt trước ${detailDateTime(reservation.approvalDeadline)}`
          : "Đơn đặt bàn đang chờ duyệt",
        description: `Đơn đặt bàn cần nhân viên duyệt. ${table} đang được giữ trong thời gian chờ.`,
      };
    case "CONFIRMED":
      return {
        tone: "info",
        title: "Bàn đã sẵn sàng cho lượt đặt",
        description: "Có thể đánh dấu khách đã đến trong ngày hẹn." +
          (reservation.noShowAllowedAt
            ? ` Chỉ ghi nhận không đến từ ${detailDateTime(reservation.noShowAllowedAt)}.`
            : ""),
      };
    case "ARRIVED":
      return {
        tone: "info",
        title: "Khách đang dùng bữa",
        description: "Sau khi hoàn thành, bàn tiếp tục được giữ đến hết thời gian đã cam kết và dọn bàn.",
      };
    case "COMPLETED":
      return {
        tone: "success",
        title: "Lượt đặt bàn đã hoàn thành",
        description: `Đã ghi nhận khách dùng bữa xong. ${table} được giữ đến ${detailDateTime(cleanupEndsAt(reservation))} để bảo toàn thời gian đã cam kết và dọn bàn.`,
      };
    case "CANCELLED": {
      const actor = statusLog(reservation, "CANCELLED")?.actorRole;
      return {
        tone: "info",
        title: actor === "STAFF" || actor === "ADMIN"
          ? "Đơn đã được hủy thay khách"
          : "Đơn đặt bàn đã được hủy",
        description: reasonDescription(reservation.cancellationReason, released),
      };
    }
    case "REJECTED":
      return {
        tone: "warning",
        title: "Đơn đặt bàn đã bị từ chối",
        description: reasonDescription(reservation.rejectionReason, released),
      };
    case "NO_SHOW": {
      const event = statusLog(reservation, "NO_SHOW");
      return {
        tone: "warning",
        title: "Đã ghi nhận khách không đến",
        description: `${event ? `Xác nhận lúc ${detailDateTime(event.changedAt)}. ` : ""}${released}`,
      };
    }
  }
}

const STATUS_EVENT_TITLES: Record<ReservationStatus, string> = {
  PENDING: "Đơn chuyển về chờ duyệt",
  CONFIRMED: "Đã xác nhận đặt bàn",
  ARRIVED: "Khách đã đến",
  COMPLETED: "Lượt đặt bàn đã hoàn thành",
  CANCELLED: "Đơn đặt bàn đã được hủy",
  REJECTED: "Đơn đặt bàn đã bị từ chối",
  NO_SHOW: "Đã ghi nhận khách không đến",
};

const EVENT_TITLES: Record<StaffReservationLog["eventType"], string> = {
  CREATED: "Đã tạo đơn đặt bàn",
  STATUS_CHANGED: "Đã cập nhật trạng thái",
  TABLE_CHANGED: "Đã chuyển bàn",
  TIME_CHANGED: "Đã đổi thời lượng",
  RESCHEDULED: "Đã đổi lịch đặt bàn",
};

const SNAPSHOT_FIELDS = [
  ["status", "Trạng thái"],
  ["startTime", "Bắt đầu"],
  ["endTime", "Kết thúc"],
  ["partySize", "Số khách"],
  ["tableId", "Bàn"],
  ["requestedAreaId", "Khu vực yêu cầu"],
  ["bufferMinutes", "Dọn bàn"],
] as const;

function snapshotValue(key: string, value: unknown, reservation: StaffReservationDetail): string | null {
  if (key === "requestedAreaId" && value === null) return "Không yêu cầu";
  if (value === null || value === undefined) return null;
  if (key === "status") return isStatus(value) ? RESERVATION_STATUS_META[value].label : null;
  if (key === "startTime" || key === "endTime") {
    return typeof value === "string" && Number.isFinite(Date.parse(value))
      ? `${detailDate(value)} · ${detailTime(value)}` : null;
  }
  if (typeof value !== "number" || !Number.isFinite(value)) return null;
  if (key === "partySize") return `${value} khách`;
  if (key === "bufferMinutes") return `${value} phút`;
  if (key === "tableId") return value === reservation.table?.id ? reservation.table.code : `#${value}`;
  if (key === "requestedAreaId") return value === reservation.requestedArea?.id ? reservation.requestedArea.name : `#${value}`;
  return null;
}

function createdSummary(log: StaffReservationLog, reservation: StaffReservationDetail) {
  const value = log.newValue;
  if (!value) return null;

  const parts = ["status", "partySize", "tableId"].map((key) =>
    snapshotValue(key, value[key], reservation),
  );
  if (typeof value.startTime === "string" && typeof value.endTime === "string" &&
      Number.isFinite(Date.parse(value.startTime)) && Number.isFinite(Date.parse(value.endTime))) {
    const startDate = detailDate(value.startTime);
    const endDate = detailDate(value.endTime);
    const startLabel = startDate !== detailDate(log.changedAt) ? `${startDate} · ` : "";
    const endLabel = endDate !== startDate ? `${endDate} · ` : "";
    parts.push(`${startLabel}${detailTime(value.startTime)}–${endLabel}${detailTime(value.endTime)}`);
  }
  return parts.filter(Boolean).join(" • ") || null;
}

export function reservationTimeline(reservation: StaffReservationDetail): ActivityTimelineEvent[] {
  return newestLogs(reservation.logs).map((log) => {
    const status = log.newValue?.status;
    let title = EVENT_TITLES[log.eventType] ?? "Đã cập nhật đơn đặt bàn";

    if (log.eventType === "STATUS_CHANGED" && isStatus(status)) {
      title = STATUS_EVENT_TITLES[status];
      if (status === "CANCELLED" && (log.actorRole === "STAFF" || log.actorRole === "ADMIN")) {
        title = "Đơn đã được hủy thay khách";
      }
    }

    // Chỉ đọc snapshot của sự kiện, không lấy giờ/số khách hiện tại để điền lịch sử.
    const changes = SNAPSHOT_FIELDS.flatMap(([key, label]) => {
      if (!log.newValue || !Object.hasOwn(log.newValue, key)) return [];
      const before = snapshotValue(key, log.oldValue?.[key], reservation);
      const after = snapshotValue(key, log.newValue[key], reservation);
      if (after === null || before === after) return [];
      const change = before === null ? after : `${before} → ${after}`;
      return [key === "status" ? change : `${label}: ${change}`];
    });

    return {
      id: log.id,
      title,
      occurredAt: log.changedAt,
      actorName: log.actorName?.trim() || "Hệ thống",
      actorRole: log.actorRole,
      description: log.eventType === "CREATED"
        ? createdSummary(log, reservation)
        : changes.join(" • ") || null,
      reason: log.reason,
      tone: "info",
    };
  });
}

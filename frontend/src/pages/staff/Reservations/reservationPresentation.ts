import type { BadgeVariant } from "../../../components/ui/Badge";
import type { ReservationStatus } from "../../../types/reservation";

export const RESERVATION_STATUS_META: Record<
  ReservationStatus,
  {
    label: string;
    variant: BadgeVariant;
  }
> = {
  PENDING: {
    label: "Chờ duyệt",
    variant: "warning",
  },
  CONFIRMED: {
    label: "Đã xác nhận",
    variant: "success",
  },
  ARRIVED: {
    label: "Khách đã đến",
    variant: "info",
  },
  COMPLETED: {
    label: "Hoàn thành",
    variant: "neutral",
  },
  CANCELLED: {
    label: "Đã hủy",
    variant: "error",
  },
  REJECTED: {
    label: "Bị từ chối",
    variant: "error",
  },
  NO_SHOW: {
    label: "Không đến",
    variant: "warning",
  },
};
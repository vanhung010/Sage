import type { BadgeVariant } from
  "../../../components/ui/Badge";

import type { ReservationStatus } from
  "../../../types/reservation";

export const RESERVATION_STATUS_META: Record<
  ReservationStatus,
  {
    label: string;
    variant: BadgeVariant;
  }
> = {
  PENDING: {
    label: "Chờ duyệt",
    variant: "pending",
  },
  CONFIRMED: {
    label: "Đã xác nhận",
    variant: "confirmed",
  },
  ARRIVED: {
    label: "Khách đã đến",
    variant: "arrived",
  },
  COMPLETED: {
    label: "Hoàn thành",
    variant: "completed",
  },
  CANCELLED: {
    label: "Đã hủy",
    variant: "cancelled",
  },
  REJECTED: {
    label: "Bị từ chối",
    variant: "rejected",
  },
  NO_SHOW: {
    label: "Không đến",
    variant: "no-show",
  },
};
import {
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom";

import { useReservationDetail } from "../../hooks/useReservationDetail";
import type { ReservationResponse } from "../../types/reservation";

import { ReservationDetailPage } from "./ReservationDetailPage";
import { ReservationPageState } from "./ReservationPageState";

export function ReservationDetailRoute() {
  const { id } = useParams();

  const navigate = useNavigate();
  const location = useLocation();

  const {
    reservation,
    loading,
    error,
    reload,
  } = useReservationDetail(Number(id));

  const state = location.state as {
    rescheduled?: ReservationResponse;
  } | null;

  if (loading) {
    return (
      <ReservationPageState
        message="Đang tải chi tiết đơn đặt bàn…"
        loading
      />
    );
  }

  if (error || !reservation) {
    return (
      <ReservationPageState
        message={error || "Không tìm thấy đơn đặt bàn"}
        onRetry={reload}
      />
    );
  }

  return (
    <ReservationDetailPage
      reservation={reservation}
      notice={
        state?.rescheduled?.id === reservation.id
          ? "Đổi lịch thành công. Thông tin đơn đã được cập nhật."
          : undefined
      }
      onReschedule={() =>
        navigate(
          `/reservations/${reservation.id}/reschedule`,
        )
      }
    />
  );
}
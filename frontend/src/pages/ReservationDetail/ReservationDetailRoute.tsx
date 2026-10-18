import { useRef, useState } from "react";

import {
  useLocation,
  useNavigate,
  useParams,
} from "react-router-dom";

import { reservationApi } from "../../api/reservation.api";
import { getApiError } from "../../api/error";

import { AppFeedbackModal } from "../../components/common/AppFeedbackModal";
import { useReservationDetail } from "../../hooks/useReservationDetail";

import type { ReservationResponse } from "../../types/reservation";
import { reservationDeadline } from "../../utils/reservation";

import { ReservationDetailPage } from "./ReservationDetailPage";
import { ReservationPageState } from "./ReservationPageState";

export function ReservationDetailRoute() {
  const { id } = useParams();
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
    <ReservationDetailContent
      key={reservation.id}
      initialReservation={reservation}
      notice={
        state?.rescheduled?.id === reservation.id
          ? "Đổi lịch thành công. Thông tin đơn đã được cập nhật."
          : undefined
      }
    />
  );
}

interface ReservationDetailContentProps {
  initialReservation: ReservationResponse;
  notice?: string;
}

function ReservationDetailContent({
  initialReservation,
  notice,
}: ReservationDetailContentProps) {
  const navigate = useNavigate();

  const [reservation, setReservation] =
    useState(initialReservation);

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [uncertain, setUncertain] = useState(false);

  const [cancelFeedback, setCancelFeedback] = useState<{
    type: "success" | "error";
    message: string;
  } | null>(null);

  // Chặn hai lần gửi trước khi React kịp render loading.
  const cancellingRef = useRef(false);

  function openCancel() {
    if (cancellingRef.current || uncertain) return;

    setCancelFeedback(null);
    setConfirmOpen(true);
  }

  async function confirmCancel() {
    if (cancellingRef.current || uncertain) return;

    const active =
      reservation.status === "PENDING" ||
      reservation.status === "CONFIRMED";

    const withinDeadline =
      Date.now() <= reservationDeadline(reservation);

    // Kiểm tra lại vì khách có thể mở modal rồi để lâu.
    if (!active || !withinDeadline) {
      setConfirmOpen(false);

      setCancelFeedback({
        type: "error",
        message: "Đơn hiện không còn đủ điều kiện hủy.",
      });

      return;
    }

    cancellingRef.current = true;
    setIsCancelling(true);

    try {
      const updated = await reservationApi.cancel(
        reservation.id,
      );

      // Cập nhật bằng kết quả backend:
      // badge chuyển sang Đã hủy, nút hủy/đổi lịch bị khóa.
      setReservation(updated);

      setCancelFeedback({
        type: "success",
        message: "Hủy đơn đặt bàn thành công.",
      });
    } catch (error) {
      const problem = getApiError(error);

      if (
        problem.status === 0 ||
        problem.status >= 500
      ) {
        // Mất kết nối có thể xảy ra sau khi server đã lưu.
        setUncertain(true);

        setCancelFeedback({
          type: "error",
          message:
            "Chưa xác định được kết quả hủy. Vui lòng tải lại trang để kiểm tra trước khi thử lại.",
        });
      } else {
        setCancelFeedback({
          type: "error",
          message: problem.message,
        });
      }
    } finally {
      cancellingRef.current = false;

      setIsCancelling(false);
      setConfirmOpen(false);
    }
  }

  return (
    <>
      <ReservationDetailPage
        reservation={reservation}
        notice={cancelFeedback ? undefined : notice}
        isCancelling={isCancelling}
        cancelFeedback={cancelFeedback}
        onCancel={uncertain ? undefined : openCancel}
        onReschedule={
          uncertain
            ? undefined
            : () =>
                navigate(
                  `/reservations/${reservation.id}/reschedule`,
                )
        }
      />

      <AppFeedbackModal
        open={confirmOpen}
        title="Xác nhận hủy đơn đặt bàn?"
        primaryLabel={
          isCancelling
            ? "Đang hủy…"
            : "Xác nhận hủy đơn"
        }
        secondaryLabel="Giữ đơn đặt bàn"
        busy={isCancelling}
        onConfirm={() => void confirmCancel()}
        onClose={() => {
          if (!cancellingRef.current) {
            setConfirmOpen(false);
          }
        }}
      >
        <p>Bạn muốn hủy đơn {reservation.code}?</p>

        <p>
          Lịch đặt bàn sẽ được giải phóng sau khi hủy
          thành công.
        </p>
      </AppFeedbackModal>
    </>
  );
}
import { Link, useParams } from "react-router-dom";

import { ActivityTimeline } from "../../../components/ui/ActivityTimeline";
import { Badge } from "../../../components/ui/Badge";
import { Button } from "../../../components/ui/Button";
import type { StaffReservationDetail } from "../../../types/staff-reservation";
import backIcon from "../../../assets/staff-reservation-detail/back.svg";
import { RESERVATION_STATUS_META } from "../Reservations/reservationPresentation";
import {
  cleanupEndsAt,
  DETAIL_ACTIONS,
  detailDate,
  detailDateTime,
  detailTime,
  reservationNotice,
  reservationTimeline,
} from "./reservationDetailPresentation";
import { useStaffReservationDetail } from "./useStaffReservationDetail";

import "./StaffReservationDetailPage.css";
import { AppFeedbackModal } from "../../../components/ui/AppFeedbackModal";
import { useStaffReservationStatusActions } from "./useStaffReservationStatusActions";

const UNAVAILABLE_ACTION = "Thao tác này hiện chưa khả dụng.";

function BackToReservations() {
  return (
    <Link className="staff-reservation-detail__back" to="/staff/reservations">
      <img src={backIcon} width={16} height={16} alt="" />
      Danh sách đặt bàn
    </Link>
  );
}

function ReservationInformation({ reservation }: { reservation: StaffReservationDetail }) {
  const showCleanupEnd = reservation.status === "ARRIVED" || reservation.status === "COMPLETED";

  return (
    <section className="staff-reservation-detail__card" aria-labelledby="reservation-information-title">
      <header className="staff-reservation-detail__card-header">
        <h2 id="reservation-information-title">Thông tin đặt bàn</h2>
        <p>Thông tin liên hệ, thời gian và bàn đã được giữ.</p>
      </header>

      <dl className="staff-reservation-detail__fields">
        <div>
          <dt>Khách hàng</dt>
          <dd>{reservation.customerName?.trim() || "Chưa có tên khách"}</dd>
        </div>
        <div>
          <dt>Số điện thoại</dt>
          <dd>{reservation.customerPhone?.trim() || "Chưa có số điện thoại"}</dd>
        </div>
        <div>
          <dt>Ngày và giờ hẹn</dt>
          <dd>
            {detailDate(reservation.startTime)} · {detailTime(reservation.startTime)} – {" "}
            {detailDate(reservation.startTime) !== detailDate(reservation.endTime)
              ? `${detailDate(reservation.endTime)} · ` : ""}
            {detailTime(reservation.endTime)}
          </dd>
        </div>
        <div>
          <dt>Số khách</dt>
          <dd>{reservation.partySize} khách</dd>
        </div>
        <div>
          <dt>Bàn được xếp</dt>
          <dd>{reservation.table
            ? `${reservation.table.code} · ${reservation.table.area.name}`
            : "Chưa xếp bàn"}</dd>
        </div>
        <div>
          <dt>Khu vực khách yêu cầu</dt>
          <dd>{reservation.requestedArea?.name || "Không yêu cầu khu vực"}</dd>
        </div>
        <div className="staff-reservation-detail__note">
          <dt>Ghi chú của khách</dt>
          <dd>{reservation.specialNote?.trim() || "Không có ghi chú"}</dd>
        </div>
      </dl>

      <p className="staff-reservation-detail__cleanup">
        {showCleanupEnd
          ? `Dọn bàn đến ${detailDateTime(cleanupEndsAt(reservation))} (${reservation.bufferMinutes} phút sau giờ kết thúc).`
          : `Thời gian dọn bàn: ${reservation.bufferMinutes} phút sau giờ kết thúc.`}
      </p>
    </section>
  );
}

export function StaffReservationDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { reservation, loading, error, reload, updateReservation } = useStaffReservationDetail(id);

  if (!reservation) {
    const title = loading
      ? "Đang tải chi tiết đặt bàn"
      : error?.status === 404
        ? "Không tìm thấy đơn đặt bàn"
        : error?.status === 403
          ? "Không có quyền xem đơn đặt bàn"
          : "Không thể mở đơn đặt bàn";
    const canRetry = error && (error.status === 0 || error.status >= 500);

    return (
      <div className="staff-reservation-detail" aria-busy={loading}>
        <BackToReservations />
        <section className="staff-reservation-detail__card staff-reservation-detail__state">
          <h1>{title}</h1>
          <p role={loading ? "status" : "alert"}>
            {loading ? "Vui lòng chờ trong giây lát." : error?.message || "Vui lòng thử lại sau."}
          </p>
          {canRetry && <Button type="button" variant="outline" onClick={reload}>Thử lại</Button>}
        </section>
      </div>
    );
  }

  return (
  <StaffReservationDetailView
    key={reservation.id}
    reservation={reservation}
    onUpdated={updateReservation}
  />
);
}

interface StaffReservationDetailViewProps {
  reservation: StaffReservationDetail;
  onUpdated: (updated: StaffReservationDetail) => void;
}

export function StaffReservationDetailView({ reservation, onUpdated }: StaffReservationDetailViewProps) {

    const mutation = useStaffReservationStatusActions(
    reservation,
    onUpdated,
  );

  const status = RESERVATION_STATUS_META[reservation.status];
  const notice = reservationNotice(reservation);
  const actions = DETAIL_ACTIONS[reservation.status];

  return (
    <div className="staff-reservation-detail">
      <header className="staff-reservation-detail__header">
        <div className="staff-reservation-detail__heading">
          <BackToReservations />
          <h1>{reservation.code}</h1>
          <p>Chi tiết đơn đặt bàn • {detailDate(reservation.startTime)}</p>
        </div>

        {actions.length > 0 && (
  <div
    className="staff-reservation-detail__actions"
    role="group"
    aria-label="Xử lý đơn đặt bàn"
  >
    {actions.map((item) => {
      const action = item.action;
      const disabledMessage = action
        ? mutation.disabledReason(action)
        : UNAVAILABLE_ACTION;

      return (
        <span
          key={item.label}
          title={disabledMessage || undefined}
        >
          <Button
            type="button"
            variant={item.variant}
            disabled={Boolean(disabledMessage)}
            loading={
              action === "arrive" &&
              mutation.pending === "arrive"
            }
            aria-description={disabledMessage || undefined}
            onClick={() => {
              if (!action) return;

              if (action === "arrive") {
                void mutation.execute(action);
              } else {
                mutation.openDialog(action);
              }
            }}
          >
            {item.label}
          </Button>
        </span>
      );
    })}
  </div>
)}
      </header>

      {mutation.error && !mutation.dialog && (
  <section
    className={
      "staff-reservation-detail__notice " +
      "staff-reservation-detail__notice--warning"
    }
    role="alert"
  >
    <h2>Chưa thể hoàn tất thao tác</h2>
    <p>{mutation.error.message}</p>
  </section>
)}

      <div className="staff-reservation-detail__status">
        <Badge variant={status.variant}>{status.label}</Badge>
        <p>
          {reservation.source === "STAFF_GUEST" ? "Nhân viên đặt hộ" : "Đặt trực tuyến"}
          {" • "}Tạo ngày {detailDate(reservation.createdAt)}
        </p>
      </div>

      <section className={`staff-reservation-detail__notice staff-reservation-detail__notice--${notice.tone}`} aria-labelledby="reservation-status-title">
        <h2 id="reservation-status-title">{notice.title}</h2>
        <p>{notice.description}</p>
      </section>

      <ReservationInformation reservation={reservation} />

      {reservation.status === "CONFIRMED" && (
  <div className="staff-reservation-detail__no-show">
    <span
      title={mutation.disabledReason("no-show") || undefined}
    >
      <Button
        type="button"
        variant="outline"
        disabled={Boolean(mutation.disabledReason("no-show"))}
        aria-description={
          mutation.disabledReason("no-show") || undefined
        }
        onClick={() => mutation.openDialog("no-show")}
      >
        Đánh dấu không đến
      </Button>
    </span>

    <p>
      {mutation.disabledReason("no-show") ||
        "Đã đủ thời gian chờ để ghi nhận khách không đến."}
    </p>
  </div>
)}

      <ActivityTimeline
        title="Nhật ký thay đổi"
        description={`${reservation.code} • Mới nhất trước`}
        events={reservationTimeline(reservation)}
      />

      {mutation.dialogProps && (
  <AppFeedbackModal
    key={`${reservation.id}:${mutation.dialog}`}
    {...mutation.dialogProps}
  >
    {mutation.modalError && (
      <p role="alert">{mutation.modalError.message}</p>
    )}

    {mutation.blocked && (
      <p role="status">{mutation.blocked}</p>
    )}
  </AppFeedbackModal>
)}
    </div>
  );
}

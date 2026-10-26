import type { ReactNode } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import {
  CheckCircle2,
  Clock,
  Info,
  XCircle,
} from "lucide-react";

import { Button } from "../../../components/ui/Button";
import { FormField } from "../../../components/ui/FormField";
import { Select } from "../../../components/ui/Select";
import { Textarea } from "../../../components/ui/Textarea";
import { useToast } from "../../../components/ui/Toast";

import backIcon from "../../../assets/staff-reservation-detail/back.svg";

import type { StaffReservationDetail } from "../../../types/staff-reservation";
import type { StaffDurationNavigationState } from "../../../types/staff-reservation-duration";

import { RESERVATION_STATUS_META } from "../Reservations/reservationPresentation";
import {
  detailDate,
  detailDateTime,
  detailTime,
} from "../ReservationDetail/reservationDetailPresentation";
import { useStaffReservationDetail } from "../ReservationDetail/useStaffReservationDetail";
import { useStaffReservationDuration } from "./useStaffReservationDuration";

import "./StaffReservationDurationPage.css";

const TIME_OPTIONS = Array.from({ length: 96 }, (_, index) => {
  const hour = String(Math.floor(index / 4)).padStart(2, "0");
  const minute = String((index % 4) * 15).padStart(2, "0");
  const value = `${hour}:${minute}`;

  return { value, label: value };
});

interface NoticeProps {
  tone: "success" | "error" | "info";
  title: string;
  children: ReactNode;
}

function Notice({ tone, title, children }: NoticeProps) {
  const Icon =
    tone === "success"
      ? CheckCircle2
      : tone === "error"
        ? XCircle
        : Info;

  return (
    <section
      className={`staff-duration__notice staff-duration__notice--${tone}`}
      role={tone === "error" ? "alert" : "status"}
    >
      <Icon size={20} strokeWidth={1.8} aria-hidden="true" />

      <div>
        <strong>{title}</strong>
        {children}
      </div>
    </section>
  );
}

export function StaffReservationDurationPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const {
    reservation,
    loading,
    error,
    reload,
    updateReservation,
  } = useStaffReservationDetail(id);

  if (!reservation) {
    const canRetry =
      error && (error.status === 0 || error.status >= 500);

    return (
      <div className="staff-duration" aria-busy={loading}>
        <section className="staff-duration__card">
          <h1>
            {loading
              ? "Đang tải đơn đặt bàn"
              : error?.status === 404
                ? "Không tìm thấy đơn đặt bàn"
                : error?.status === 403
                  ? "Bạn không có quyền truy cập"
                  : "Không thể mở đơn đặt bàn"}
          </h1>

          <p role={loading ? "status" : "alert"}>
            {loading ? "Vui lòng chờ trong giây lát." : error?.message}
          </p>

          <div className="staff-duration__inline-actions">
            {canRetry && (
              <Button
                type="button"
                variant="outline"
                onClick={reload}
              >
                Thử lại
              </Button>
            )}

            <Button
              type="button"
              variant="outline"
              onClick={() => void navigate("/staff/reservations")}
            >
              Danh sách đặt bàn
            </Button>
          </div>
        </section>
      </div>
    );
  }

  return (
    <DurationForm
      key={reservation.id}
      reservation={reservation}
      onRefreshed={updateReservation}
    />
  );
}

interface DurationFormProps {
  reservation: StaffReservationDetail;
  onRefreshed: (reservation: StaffReservationDetail) => void;
}

function DurationForm({
  reservation,
  onRefreshed,
}: DurationFormProps) {
  const navigate = useNavigate();
  const showToast = useToast();
  const detailPath = `/staff/reservations/${reservation.id}`;

  const editor = useStaffReservationDuration({
    reservation,
    onRefreshed,
    onSaved: (result) => {
      showToast({
        title: "Đã đổi thời lượng",
        message:
          `Dùng bữa đến ${detailTime(result.endTime)}, ` +
          `dọn bàn đến ${detailTime(result.occupiedUntil)}.`,
      });

      const state: StaffDurationNavigationState = {
        durationChanged: result,
      };

      // Trang chi tiết sẽ tải lại GET detail, bao gồm TIME_CHANGED.
      void navigate(detailPath, { replace: true, state });
    },
  });

  const currentDuration =
    (Date.parse(reservation.endTime) -
      Date.parse(reservation.startTime)) /
    60_000;

  const proposedEnd = Date.parse(editor.endIso);
  const hasProposedEnd = Number.isFinite(proposedEnd);

  const duration = editor.preview?.durationMinutes ??
    (hasProposedEnd
      ? (proposedEnd - Date.parse(reservation.startTime)) / 60_000
      : null);

  const difference = duration === null
    ? null
    : duration - currentDuration;

  const occupiedUntil = editor.preview?.occupiedUntil ??
    (hasProposedEnd
      ? new Date(
          proposedEnd + reservation.bufferMinutes * 60_000,
        ).toISOString()
      : null);

  const preview =
    !editor.inputError && !editor.reviewRequired
      ? editor.preview
      : null;

  const hasConflict = Boolean(preview && !preview.allowed);
  const suggestedEnd = preview?.suggestedEndTime ?? null;

  const tableConflict = preview?.conflicts.some(
    (item) =>
      item.code === "TABLE_TIME_CONFLICT" ||
      item.code === "TABLE_SUSPENDED",
  );

  const timeError =
    editor.inputError || editor.previewError?.message || "";

  const options = TIME_OPTIONS.map((option) => ({
    ...option,
    disabled: option.value <= detailTime(reservation.startTime),
  }));

  return (
    <div className="staff-duration">
      <header className="staff-duration__heading">
        <Link
          className="staff-duration__back"
          to={detailPath}
          aria-disabled={editor.busy || undefined}
          onClick={(event) => {
            if (editor.busy) event.preventDefault();
          }}
        >
          <img src={backIcon} width={16} height={16} alt="" />
          Chi tiết đặt bàn
        </Link>

        <h1>Đổi thời lượng</h1>

        <p>
          {reservation.code} · {reservation.table.code} ·{" "}
          {RESERVATION_STATUS_META[reservation.status].label}
        </p>
      </header>

      {!editor.eligible ? (
        <>
          <Notice tone="info" title="Không thể đổi thời lượng">
            <p>
              Chỉ áp dụng cho đơn đã xác nhận hoặc khách đang dùng bữa.
            </p>
          </Notice>

          <Button
            type="button"
            variant="outline"
            onClick={() => void navigate(detailPath)}
          >
            Về chi tiết đặt bàn
          </Button>
        </>
      ) : (
        <form
          className="staff-duration__form"
          noValidate
          onSubmit={(event) => {
            event.preventDefault();
            void editor.save();
          }}
        >
          <section
            className="staff-duration__card"
            aria-labelledby="duration-form-title"
          >
            <header className="staff-duration__card-heading">
              <h2 id="duration-form-title">
                Điều chỉnh giờ kết thúc
              </h2>
              <p>
                Giờ bắt đầu giữ nguyên. Chọn giờ mới để gia hạn
                hoặc rút ngắn.
              </p>
            </header>

            <dl className="staff-duration__grid">
              <div className="staff-duration__value">
                <dt>Giờ bắt đầu</dt>
                <dd>
                  {detailDate(reservation.startTime)} ·{" "}
                  {detailTime(reservation.startTime)}
                </dd>
              </div>

              <div className="staff-duration__value">
                <dt>Giờ kết thúc hiện tại</dt>
                <dd>
                  {detailTime(reservation.endTime)} · Thời lượng{" "}
                  {currentDuration} phút
                </dd>
              </div>
            </dl>

            <div className="staff-duration__grid">
              <FormField
                label="Giờ kết thúc mới"
                required
                htmlFor="duration-end-time"
                error={timeError || undefined}
              >
                <Select
                  id="duration-end-time"
                  name="endTime"
                  aria-label="Giờ kết thúc mới"
                  value={editor.endTime}
                  options={options}
                  leadingIcon={
                    <Clock
                      size={24}
                      strokeWidth={1.8}
                      aria-hidden="true"
                    />
                  }
                  error={Boolean(timeError || hasConflict)}
                  disabled={editor.busy}
                  onValueChange={editor.changeTime}
                />
              </FormField>

              <dl className="staff-duration__value">
                <dt>Thời lượng mới &amp; khoảng giữ bàn</dt>
                <dd>
                  {duration !== null && duration > 0 && occupiedUntil
                    ? (
                      <>
                        {duration} phút
                        {difference !== null && difference !== 0 && (
                          <>
                            {" "}({difference > 0 ? "+" : ""}
                            {difference} phút)
                          </>
                        )}
                        {" "}· Giữ bàn{" "}
                        {detailTime(reservation.startTime)}–
                        {detailTime(occupiedUntil)}
                      </>
                    )
                    : "Chọn giờ kết thúc hợp lệ."}
                </dd>
              </dl>
            </div>

            {hasConflict && preview && (
              <>
                <Notice
                  tone="error"
                  title={`Không thể cập nhật đến ${editor.endTime}`}
                >
                  {preview.conflicts.map((conflict, index) => (
                    <p key={`${conflict.code}:${index}`}>
                      {conflict.message}. Khoảng bị chặn:{" "}
                      {detailDateTime(conflict.blockedFrom)} –{" "}
                      {detailDateTime(conflict.blockedUntil)}.
                    </p>
                  ))}
                </Notice>

                <div className="staff-duration__inline-actions">
                  {suggestedEnd && (
                    <Button
                      type="button"
                      variant="outline"
                      disabled={editor.busy}
                      onClick={() =>
                        editor.changeTime(detailTime(suggestedEnd))
                      }
                    >
                      Chọn {detailTime(suggestedEnd)}
                    </Button>
                  )}

                  {tableConflict && (
                    <span title="Chức năng chuyển bàn hiện chưa khả dụng.">
                      <Button type="button" disabled>
                        Tìm bàn khác
                      </Button>
                    </span>
                  )}
                </div>
              </>
            )}

            <p className="staff-duration__hint">
              {suggestedEnd
                ? (
                  <>
                    Có thể kết thúc lúc {detailTime(suggestedEnd)}{" "}
                    để dọn bàn đến{" "}
                    {detailTime(
                      new Date(
                        Date.parse(suggestedEnd) +
                          reservation.bufferMinutes * 60_000,
                      ).toISOString(),
                    )}.
                  </>
                )
                : (
                  <>
                    Khoảng giữ bàn bao gồm{" "}
                    {reservation.bufferMinutes} phút dọn bàn.
                    Giờ hiển thị theo Việt Nam (UTC+7).
                  </>
                )}
            </p>

            {editor.previewError && !editor.reviewRequired && (
              <div className="staff-duration__inline-actions">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  disabled={editor.busy}
                  onClick={editor.retryPreview}
                >
                  Kiểm tra lại thời gian
                </Button>
              </div>
            )}

            <FormField
              label="Lý do điều chỉnh (không bắt buộc)"
              htmlFor="duration-reason"
              supportingText={`${editor.reason.length}/1000 ký tự`}
            >
              <Textarea
                id="duration-reason"
                name="reason"
                className="staff-duration__reason"
                value={editor.reason}
                maxLength={1000}
                disabled={editor.busy}
                placeholder="Ví dụ: Khách muốn kéo dài thời gian dùng bữa."
                onChange={(event) =>
                  editor.setReason(event.target.value)
                }
              />
            </FormField>
          </section>

          {editor.checking && (
            <Notice tone="info" title="Đang kiểm tra thời lượng">
              <p>
                Đang kiểm tra lịch đặt bàn và thời gian dọn bàn.
              </p>
            </Notice>
          )}

          {preview?.allowed && preview.differenceMinutes !== 0 && (
            <Notice tone="success" title="Có thể cập nhật thời lượng">
              <p>
                Dùng bữa đến {detailTime(preview.endTime)}, dọn bàn
                đến {detailTime(preview.occupiedUntil)}. Không có
                xung đột trong lần kiểm tra này.
              </p>
            </Notice>
          )}

          {editor.saveError && (
            <Notice tone="error" title="Chưa thể hoàn tất thay đổi">
              <p>{editor.saveError.message}</p>
              <p>
                Nội dung bạn nhập vẫn được giữ. Kiểm tra lại đơn
                để xác nhận thời lượng hiện tại trước khi lưu tiếp.
              </p>

              <Button
                type="button"
                variant="outline"
                size="sm"
                loading={editor.refreshing}
                disabled={editor.busy}
                onClick={() => void editor.review()}
              >
                Kiểm tra lại đơn
              </Button>
            </Notice>
          )}

          <footer className="staff-duration__actions">
            <p>
              {editor.reviewRequired
                ? "Kiểm tra lại đơn trước khi tiếp tục."
                : hasConflict
                  ? "Chọn thời gian phù hợp trước khi lưu."
                  : preview?.differenceMinutes === 0
                    ? "Chọn giờ kết thúc khác giờ hiện tại."
                    : "Thay đổi được lưu vào lịch sử đặt bàn."}
            </p>

            <div className="staff-duration__buttons">
              <Button
                type="submit"
                loading={editor.saving}
                disabled={!editor.canSave}
              >
                Lưu thời lượng
              </Button>

              <Button
                type="button"
                variant="outline"
                className="staff-duration__back-button"
                disabled={editor.busy}
                onClick={() => void navigate(detailPath)}
              >
                Quay lại
              </Button>
            </div>
          </footer>
        </form>
      )}
    </div>
  );
}
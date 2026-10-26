import { useEffect, useRef, useState } from "react";

import { getApiError } from "../../../api/error";
import { staffReservationApi } from "../../../api/staff-reservation.api";
import type { ApiErrorResponse } from "../../../types/api";
import type { StaffReservationDetail } from "../../../types/staff-reservation";
import type {
  StaffDurationChanged,
  StaffDurationPreview,
} from "../../../types/staff-reservation-duration";
import {
  dateInputValue,
  displayTime,
} from "../../../utils/reservation";

interface PreviewSnapshot {
  key: string;
  data: StaffDurationPreview | null;
  error: ApiErrorResponse | null;
}

interface Options {
  reservation: StaffReservationDetail;
  onRefreshed: (reservation: StaffReservationDetail) => void;
  onSaved: (result: StaffDurationChanged) => void;
}

const TIME_PATTERN = /^([01]\d|2[0-3]):(00|15|30|45)$/;

export function useStaffReservationDuration({
  reservation,
  onRefreshed,
  onSaved,
}: Options) {
  const [endTime, setEndTime] = useState(() =>
    displayTime(reservation.endTime),
  );
  const [reason, setReason] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [snapshot, setSnapshot] = useState<PreviewSnapshot | null>(null);
  const [saving, setSaving] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [reviewRequired, setReviewRequired] = useState(false);
  const [saveError, setSaveError] = useState<ApiErrorResponse | null>(null);
  const [now, setNow] = useState(() => Date.now());

  const mounted = useRef(false);
  const locked = useRef(false);

  const id = reservation.id;
  const eligible =
    reservation.status === "CONFIRMED" ||
    reservation.status === "ARRIVED";

  // Luôn ghép với ngày tại nhà hàng và offset UTC+7.
  const endIso = TIME_PATTERN.test(endTime)
    ? `${dateInputValue(reservation.startTime)}T${endTime}:00+07:00`
    : "";

  const inputError = !endIso
    ? "Vui lòng chọn giờ kết thúc theo bước 15 phút."
    : Date.parse(endIso) <= Date.parse(reservation.startTime)
      ? "Giờ kết thúc phải sau giờ bắt đầu."
      : Date.parse(endIso) <= now
        ? "Giờ kết thúc mới phải sau thời điểm hiện tại."
        : "";

  // Preview phải tương ứng với thông tin đang hiển thị trên form.
  const baseline = JSON.stringify([
    Date.parse(reservation.startTime),
    Date.parse(reservation.endTime),
    reservation.status,
    reservation.bufferMinutes,
  ]);

  const key = JSON.stringify([
    id,
    reservation.updatedAt,
    baseline,
    endIso,
    attempt,
  ]);

  const current = snapshot?.key === key ? snapshot : null;
  const preview = current?.data ?? null;
  const previewError = current?.error ?? null;
  const busy = saving || refreshing;

  const checking =
    eligible &&
    !inputError &&
    !reviewRequired &&
    !busy &&
    !current;

  const canSave = Boolean(
    eligible &&
      !busy &&
      !reviewRequired &&
      !inputError &&
      reason.length <= 1000 &&
      preview?.allowed &&
      preview.differenceMinutes !== 0,
  );

  useEffect(() => {
    mounted.current = true;

    const timer = window.setInterval(
      () => setNow(Date.now()),
      30_000,
    );

    return () => {
      mounted.current = false;
      window.clearInterval(timer);
    };
  }, []);

  useEffect(() => {
    if (!eligible || inputError || reviewRequired || busy) return;

    const controller = new AbortController();

    const timer = window.setTimeout(() => {
      void staffReservationApi
        .previewDuration(id, endIso, controller.signal)
        .then(
          (data) => {
            if (controller.signal.aborted) return;

            const receivedBaseline = JSON.stringify([
              Date.parse(data.startTime),
              Date.parse(data.currentEndTime),
              data.status,
              data.bufferMinutes,
            ]);

            if (receivedBaseline !== baseline) {
              setSaveError({
                status: 409,
                code: "RESERVATION_CHANGED",
                message:
                  "Đơn đặt bàn đã thay đổi. Kiểm tra lại đơn trước khi tiếp tục.",
              });
              setReviewRequired(true);
              return;
            }

            setSnapshot({ key, data, error: null });
          },
          (cause: unknown) => {
            if (controller.signal.aborted) return;

            setSnapshot({
              key,
              data: null,
              error: getApiError(cause),
            });
          },
        );
    }, 300);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [
    id,
    key,
    baseline,
    endIso,
    eligible,
    inputError,
    reviewRequired,
    busy,
  ]);

  function changeTime(value: string) {
    if (locked.current) return;

    setEndTime(value);
    setSnapshot(null);

    if (!reviewRequired) setSaveError(null);
  }

  function retryPreview() {
    if (locked.current || reviewRequired) return;

    setSnapshot(null);
    setAttempt((value) => value + 1);
  }

  async function review() {
    if (locked.current) return;

    locked.current = true;
    setRefreshing(true);

    try {
      const updated = await staffReservationApi.getDetail(id);

      if (!mounted.current) return;

      // Cập nhật thông tin hiện tại nhưng giữ giờ và lý do đang nhập.
      onRefreshed(updated);
      setSnapshot(null);
      setSaveError(null);
      setReviewRequired(false);
      setAttempt((value) => value + 1);
    } catch (cause: unknown) {
      if (mounted.current) setSaveError(getApiError(cause));
    } finally {
      locked.current = false;

      if (mounted.current) setRefreshing(false);
    }
  }

  async function save() {
    if (locked.current || !canSave || !preview) return;

    // Kiểm tra lại thời gian ngay lúc bấm lưu.
    if (Date.parse(endIso) <= Date.now()) {
      setNow(Date.now());
      return;
    }

    locked.current = true;
    setSaving(true);
    setSaveError(null);

    try {
      const result = await staffReservationApi.changeDuration(id, {
        endTime: endIso,
        expectedVersion: preview.version,
        reason: reason.trim() || undefined,
      });

      if (!mounted.current) return;

      setReviewRequired(true);
      onSaved(result);
    } catch (cause: unknown) {
      if (!mounted.current) return;

      setSaveError(getApiError(cause));

      // Không tự gửi lại POST: request lỗi mạng có thể đã được commit.
      // Cần đọc lại đơn, sau đó preview lại trước lần lưu tiếp theo.
      setReviewRequired(true);
      setSnapshot(null);
    } finally {
      locked.current = false;

      if (mounted.current) setSaving(false);
    }
  }

  return {
    eligible,
    endTime,
    endIso,
    reason,
    setReason,
    inputError,
    preview,
    previewError,
    checking,
    saving,
    refreshing,
    busy,
    reviewRequired,
    saveError,
    canSave,
    changeTime,
    retryPreview,
    review,
    save,
  };
}
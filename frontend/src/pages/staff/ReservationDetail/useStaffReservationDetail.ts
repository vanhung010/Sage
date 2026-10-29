import { useEffect, useState, useCallback } from "react";
import { useRealtimeRefresh } from "../../../realtime/useRealtimeRefresh";
import { getApiError } from "../../../api/error";
import { staffReservationApi } from "../../../api/staff-reservation.api";
import type { ApiErrorResponse } from "../../../types/api";
import type { StaffReservationDetail } from "../../../types/staff-reservation";

interface DetailSnapshot {
  key: string;
  reservation: StaffReservationDetail | null;
  error: ApiErrorResponse | null;
}

export function useStaffReservationDetail(routeId: string | undefined) {
  const id = Number(routeId);
  const valid = /^\d+$/.test(routeId ?? "") && Number.isSafeInteger(id) && id > 0;
  const [attempt, setAttempt] = useState(0);
  const [snapshot, setSnapshot] = useState<DetailSnapshot | null>(null);
  const key = `${routeId}:${attempt}`;
  const reload = useCallback(() => {
    setAttempt((value) => value + 1);
  }, []);

  useRealtimeRefresh(
    (signal) =>
      valid &&
      (
        signal.type === "SYNC" ||
        (
          signal.type.startsWith("RESERVATION_") &&
          signal.resourceId === id
        )
      ),
    reload,
  );

  useEffect(() => {
    if (!valid) return;

    const controller = new AbortController();

    void staffReservationApi.getDetail(id, controller.signal).then(
      (reservation) => {
        if (!controller.signal.aborted) {
          setSnapshot({ key, reservation, error: null });
        }
      },
      (cause: unknown) => {
        if (!controller.signal.aborted) {
          setSnapshot({ key, reservation: null, error: getApiError(cause) });
        }
      },
    );

    return () => controller.abort();
  }, [id, key, valid]);

  // Không hiển thị đơn cũ khi URL đổi hoặc đang thử tải lại.
  const current = snapshot?.key === key ? snapshot : null;

  function updateReservation(updated: StaffReservationDetail) {
  if (updated.id !== id) return;

  setSnapshot((current) => {
    // Không ghi response của đơn cũ lên một lần tải khác.
    if (!current || current.key !== key) return current;

    return {
      key,
      reservation: updated,
      error: null,
    };
  });
}

  return {
  reservation: current?.reservation ?? null,
  loading: valid && !current,
  error: valid
    ? current?.error ?? null
    : {
        status: 400,
        code: "INVALID_RESERVATION_ID",
        message: "ID đơn đặt bàn không hợp lệ.",
      },
  reload,
  updateReservation,
};
}

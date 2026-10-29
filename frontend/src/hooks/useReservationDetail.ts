import { useEffect, useState,useCallback } from "react";

import { getApiError } from "../api/error";
import { reservationApi } from "../api/reservation.api";
import type { ReservationResponse } from "../types/reservation";
import { useRealtimeRefresh } from "../realtime/useRealtimeRefresh";
interface DetailResult {
  key: string;
  reservation: ReservationResponse | null;
  error: string;
}

export function useReservationDetail(id: number) {
  const [result, setResult] = useState<DetailResult | null>(null);
  const [attempt, setAttempt] = useState(0);
  const reload = useCallback(() => {
  setAttempt((value) => value + 1);
}, []);

useRealtimeRefresh(
  (signal) => valid && (
    signal.type === "SYNC"
    || (signal.type.startsWith("RESERVATION_") && signal.resourceId === id)
  ),
  reload,
);

  const valid = Number.isSafeInteger(id) && id > 0;
  const key = `${id}:${attempt}`;

  useEffect(() => {
    if (!valid) return;

    const controller = new AbortController();

    reservationApi
      .getDetail(id, controller.signal)
      .then((reservation) => {
        if (!controller.signal.aborted) {
          setResult({
            key,
            reservation,
            error: "",
          });
        }
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setResult({
            key,
            reservation: null,
            error: getApiError(cause).message,
          });
        }
      });

    return () => controller.abort();
  }, [id, key, valid]);

  const current = result?.key === key ? result : null;

  return {
    reservation: current?.reservation ?? null,
    loading: valid && !current,

    error: valid
      ? current?.error ?? ""
      : "ID đơn đặt bàn không hợp lệ.",

    reload,
  };
}
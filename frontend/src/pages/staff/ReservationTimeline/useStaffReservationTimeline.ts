import {
  useCallback,
  useEffect,
  useState,
} from "react";
import { useRealtimeRefresh } from "../../../realtime/useRealtimeRefresh";
import { areaApi } from "../../../api/area.api";
import { getApiError } from "../../../api/error";
import { staffReservationApi } from "../../../api/staff-reservation.api";

import type { AreaResponse } from "../../../types/area";

import type {
  ReservationTimelineData,
} from "../../../components/ui/ReservationTimeline";

type AreaOption = Pick<AreaResponse, "id" | "name">;

interface TimelineSnapshot {
  requestKey: string;
  data: ReservationTimelineData | null;
  error: string | null;
  loading: boolean;
}

function mergeAreas(
  current: readonly AreaOption[],
  incoming: readonly AreaOption[],
): AreaOption[] {
  const result = new Map(
    current.map((area) => [area.id, area]),
  );

  for (const area of incoming) {
    result.set(area.id, {
      id: area.id,
      name: area.name,
    });
  }

  return Array.from(result.values()).sort(
    (a, b) => a.name.localeCompare(b.name, "vi"),
  );
}

function timelineErrorMessage(error: unknown): string {
  const apiError = getApiError(error);

  if (apiError.status === 401) {
    return "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.";
  }

  if (apiError.status === 403) {
    return "Bạn không có quyền xem lịch đặt bàn.";
  }

  return (
    apiError.message ||
    "Không tải được lịch đặt bàn. Vui lòng thử lại."
  );
}

export function useStaffReservationTimeline(
  date: string,
  areaId: number | null,
) {
  const [attempt, setAttempt] = useState(0);
  const [areasAttempt, setAreasAttempt] = useState(0);

  const [snapshot, setSnapshot] =
    useState<TimelineSnapshot | null>(null);

  const [areas, setAreas] = useState<AreaOption[]>([]);
  const [areasLoading, setAreasLoading] = useState(true);
  const [areasError, setAreasError] = useState<string | null>(null);

  const requestKey = `${date}|${areaId ?? "all"}|${attempt}`;

  useEffect(() => {
    const controller = new AbortController();

    setSnapshot({
      requestKey,
      data: null,
      error: null,
      loading: true,
    });

    void staffReservationApi
      .timeline(date, areaId, controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;

        if (data.date !== date) {
          throw new Error("Ngày trả về không khớp với ngày yêu cầu.");
        }

        // Giữ cả khu vực có dữ liệu lịch dù khu vực đó đã ngừng
        // hoạt động và không còn xuất hiện trong API public.
        setAreas((current) =>
          mergeAreas(
            current,
            data.tables.map((table) => table.area),
          ),
        );

        setSnapshot({
          requestKey,
          data,
          error: null,
          loading: false,
        });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;

        setSnapshot({
          requestKey,
          data: null,
          error: timelineErrorMessage(error),
          loading: false,
        });
      });

    return () => controller.abort();
  }, [date, areaId, requestKey]);

  useEffect(() => {
    const controller = new AbortController();

    setAreasLoading(true);
    setAreasError(null);

    void areaApi
      .list(controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;

        setAreas((current) => mergeAreas(current, data));
        setAreasLoading(false);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted) return;

        setAreasError(
          getApiError(error).message ||
            "Không tải được danh sách khu vực.",
        );

        setAreasLoading(false);
      });

    return () => controller.abort();
  }, [areasAttempt]);

  // Không dùng response của bộ lọc hoặc lần tải trước.
  const current =
    snapshot?.requestKey === requestKey
      ? snapshot
      : null;

  const refresh = useCallback(() => {
    setAttempt((value) => value + 1);
    setAreasAttempt((value) => value + 1);
  }, []);

  const retryAreas = useCallback(() => {
    setAreasAttempt((value) => value + 1);
  }, []);

  const reloadTimeline = useCallback(() => {
  setAttempt((value) => value + 1);
}, []);

useRealtimeRefresh(
  (signal) =>
    signal.type === "SYNC" ||
    signal.type.startsWith("RESERVATION_"),
  reloadTimeline,
);

  return {
    data: current?.data ?? null,
    error: current?.error ?? null,
    loading: current?.loading ?? true,

    areas,
    areasLoading,
    areasError,

    refresh,
    retryAreas,
  };
}
import {
  useCallback,
  useEffect,
  useState,
} from "react";

import { staffReservationApi } from "../../../api/staff-reservation.api";
import { areaApi } from "../../../api/area.api";
import { getApiError } from "../../../api/error";

import type {
  StaffReservationFilterForm,
} from "../../../types/staff-reservation";

type Loader<T> = (signal: AbortSignal) => Promise<T>;

interface Snapshot<T> {
  loader: Loader<T>;
  attempt: number;

  data: T | null;
  error: string | null;
  loading: boolean;
}

function useResource<T>(loader: Loader<T>) {
  const [attempt, setAttempt] = useState(0);

  const [snapshot, setSnapshot] =
    useState<Snapshot<T> | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    setSnapshot({
      loader,
      attempt,
      data: null,
      error: null,
      loading: true,
    });

    void loader(controller.signal).then(
      (data) => {
        if (controller.signal.aborted) return;

        setSnapshot({
          loader,
          attempt,
          data,
          error: null,
          loading: false,
        });
      },
      (error: unknown) => {
        if (controller.signal.aborted) return;

        setSnapshot({
          loader,
          attempt,
          data: null,
          error:
            getApiError(error).message ||
            "Không thể tải dữ liệu. Vui lòng thử lại.",
          loading: false,
        });
      },
    );

    return () => controller.abort();
  }, [loader, attempt]);

  const current =
    snapshot &&
    snapshot.loader === loader &&
    snapshot.attempt === attempt
      ? snapshot
      : null;

  const retry = useCallback(() => {
    setAttempt((value) => value + 1);
  }, []);

  return {
    data: current?.data ?? null,
    error: current?.error ?? null,
    loading: current?.loading ?? true,
    retry,
  };
}

export function useStaffReservations(
  filters: StaffReservationFilterForm,
  page: number,
) {
  // 7 hàng/trang theo màn hình Figma.
  const pageSize = 7;

  const loadList = useCallback(
    (signal: AbortSignal) =>
      staffReservationApi.list(
        filters,
        page,
        pageSize,
        signal,
      ),
    [filters, page],
  );

  const loadSummary = useCallback(
    (signal: AbortSignal) =>
      staffReservationApi.summary(filters, signal),
    [filters],
  );

  const loadAreas = useCallback(
    (signal: AbortSignal) => areaApi.list(signal),
    [],
  );

  const list = useResource(loadList);
  const summary = useResource(loadSummary);
  const areas = useResource(loadAreas);

  return {
    list,
    summary,
    areas,
    pageSize,
  };
}
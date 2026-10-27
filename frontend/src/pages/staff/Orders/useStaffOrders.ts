import {
  useCallback,
  useEffect,
  useMemo,
  useState,
} from "react";

import { staffOrderApi } from "../../../api/staff-order.api";
import { getApiError } from "../../../api/error";

import type { PageResponse } from "../../../types/api";

import type {
  StaffOrderFilters,
  StaffOrderSummaryResponse,
} from "../../../types/staff-order";

export const STAFF_ORDER_PAGE_SIZE = 7;

export function useStaffOrders(
  filters: StaffOrderFilters,
  page: number,
) {
  const [attempt, setAttempt] = useState(0);

  const request = useMemo(
    () => ({ filters, page, attempt }),
    [filters, page, attempt],
  );

  const [snapshot, setSnapshot] = useState<{
    request: typeof request;
    data: PageResponse<StaffOrderSummaryResponse> | null;
    error: string | null;
    loading: boolean;
  } | null>(null);

  useEffect(() => {
    const controller = new AbortController();

    setSnapshot({
      request,
      data: null,
      error: null,
      loading: true,
    });

    void staffOrderApi.list(
      {
        ...request.filters,
        page: request.page,
        size: STAFF_ORDER_PAGE_SIZE,
      },
      controller.signal,
    ).then(
      (data) => {
        if (controller.signal.aborted) return;

        setSnapshot({
          request,
          data,
          error: null,
          loading: false,
        });
      },
      (error: unknown) => {
        if (controller.signal.aborted) return;

        setSnapshot({
          request,
          data: null,
          error:
            getApiError(error).message ||
            "Kiểm tra kết nối và thử lại để xem các đơn đặt món.",
          loading: false,
        });
      },
    );

    return () => controller.abort();
  }, [request]);

  const retry = useCallback(() => {
    setAttempt((current) => current + 1);
  }, []);

  const current =
    snapshot?.request === request ? snapshot : null;

  return {
    data: current?.data ?? null,
    error: current?.error ?? null,
    loading: current?.loading ?? true,
    retry,
    pageSize: STAFF_ORDER_PAGE_SIZE,
  };
}
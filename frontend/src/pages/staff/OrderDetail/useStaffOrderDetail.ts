import {
  useCallback,
  useEffect,
  useState,
} from "react";

import axios from "axios";

import { getApiError } from "../../../api/error";
import { staffOrderApi } from "../../../api/staff-order.api";

import type { ApiErrorResponse } from "../../../types/api";

import type {
  StaffOrderDetailResponse,
} from "../../../types/staff-order";

interface DetailSnapshot {
  key: string;
  order: StaffOrderDetailResponse | null;
  error: ApiErrorResponse | null;
}

export function useStaffOrderDetail(
  routeId: string | undefined,
) {
  const id = Number(routeId);

  const valid =
    /^[1-9]\d*$/.test(routeId ?? "") &&
    Number.isSafeInteger(id);

  const [attempt, setAttempt] = useState(0);

  const [snapshot, setSnapshot] =
    useState<DetailSnapshot | null>(null);

  const key = `${routeId}:${attempt}`;

  useEffect(() => {
    if (!valid) return;

    const controller = new AbortController();

    void staffOrderApi.getDetail(
      id,
      controller.signal,
    ).then(
      (order) => {
        if (controller.signal.aborted) return;

        setSnapshot({
          key,
          order,
          error: null,
        });
      },

      (cause: unknown) => {
        if (controller.signal.aborted) return;

        const parsed = getApiError(cause);

        // Ưu tiên HTTP status thực tế để phân biệt 403/404/500.
        const status = axios.isAxiosError(cause)
          ? cause.response?.status ?? 0
          : parsed.status ?? 0;

        setSnapshot({
          key,
          order: null,
          error: {
            status,
            code: parsed.code || "ORDER_DETAIL_REQUEST_FAILED",
            message:
              parsed.message ||
              "Không thể tải chi tiết đơn đặt món.",
          },
        });
      },
    );

    return () => controller.abort();
  }, [id, key, valid]);

  const reload = useCallback(() => {
    setAttempt((current) => current + 1);
  }, []);

  // Không hiển thị đơn cũ khi URL đổi hoặc đang tải lại.
  const current =
    snapshot?.key === key ? snapshot : null;

  const error: ApiErrorResponse | null = valid
    ? current?.error ?? null
    : {
        status: 400,
        code: "INVALID_ORDER_ID",
        message: "Mã đơn đặt món không hợp lệ.",
      };

  return {
    order: valid ? current?.order ?? null : null,
    loading: valid && current === null,
    error,
    reload,
  };
}
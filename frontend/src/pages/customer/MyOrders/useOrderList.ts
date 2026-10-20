import { useEffect, useState } from "react";
import type { PageResponse } from "../../../types/api";
import { getApiError } from "../../../api/error";
export interface OrderListState<T> {
  data: PageResponse<T> | null;
  loading: boolean;
  error: string;
  retry: () => void;
}
// Each filter/page renders a keyed results component. The old request is aborted
// on unmount, so an older response can never replace the current selection.
export function useOrderList<T>(load: (signal: AbortSignal) => Promise<PageResponse<T>>): OrderListState<T> {
  const [data, setData] = useState<PageResponse<T> | null>(null);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();
    load(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setData(result);
      })
      .catch((cause: unknown) => {
        if (!controller.signal.aborted) {
          setError(getApiError(cause).message || "Không thể tải danh sách đơn. Vui lòng thử lại.");
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [load, attempt]);

  function retry() {
    setError("");
    setData(null);
    setLoading(true);
    setAttempt((value) => value + 1);
  }

  return { data, loading, error, retry };
}



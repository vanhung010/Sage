import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import { orderApi } from "../../../api/order.api";
import { getApiError } from "../../../api/error";
import type { OrderDetailResponse } from "../../../types/order";
import { OrderDetailView } from "./OrderDetailView";
export function OrderDetailPage() {
  const { id } = useParams();

  // Đổi ID trên URL sẽ khởi tạo lại state của trang.
  return <OrderDetailContent key={id} rawId={id ?? ""} />;
}

function OrderDetailContent({ rawId }: { rawId: string }) {
  const id = Number(rawId);
  const validId =
    /^[1-9]\d*$/.test(rawId) &&
    Number.isSafeInteger(id);

  const [order, setOrder] =
    useState<OrderDetailResponse | null>(null);

  const [loading, setLoading] = useState(validId);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [uncertain, setUncertain] = useState(false);

  const [feedback, setFeedback] = useState<{
    error: boolean;
    message: string;
  } | null>(null);

  const cancellingRef = useRef(false);
  const mountedRef = useRef(false);

  useEffect(() => {
    mountedRef.current = true;

    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    if (!validId) return;

    const controller = new AbortController();

    orderApi
      .getDetail(id, controller.signal)
      .then((data) => {
        if (controller.signal.aborted) return;

        setOrder(data);
        setUncertain(false);
      })
      .catch((cause: unknown) => {
        if (controller.signal.aborted) return;

        const problem = getApiError(cause);

        setOrder(null);
        setError(
          problem.status === 404 || problem.status === 403
            ? "Không tìm thấy đơn hoặc bạn không có quyền xem đơn này."
            : problem.message ||
                "Không thể tải chi tiết đơn đặt món.",
        );
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      });

    return () => controller.abort();
  }, [id, validId, attempt]);

  async function confirmCancel() {
    if (
      !order ||
      order.foodOrderStatus !== "PLACED" ||
      cancellingRef.current ||
      uncertain
    ) {
      return;
    }

    cancellingRef.current = true;
    setCancelling(true);
    setFeedback(null);

    try {
      const updated = await orderApi.cancel(order.id);

      if (!mountedRef.current) return;

      // Cập nhật bằng dữ liệu backend trả về.
      setOrder(updated);
      setFeedback({
        error: false,
        message: "Đơn đặt món đã được hủy.",
      });
    } catch (cause) {
      if (!mountedRef.current) return;

      const problem = getApiError(cause);

      // Mất kết nối hoặc lỗi server có thể xảy ra sau khi đã lưu.
      const unknownResult =
        problem.status === 0 || problem.status >= 500;

      setUncertain(unknownResult);

      setFeedback({
        error: true,
        message: unknownResult
          ? "Chưa xác định được kết quả hủy. Hãy kiểm tra trạng thái trước khi thử lại."
          : problem.message || "Không thể hủy đơn đặt món.",
      });
    } finally {
      cancellingRef.current = false;

      if (mountedRef.current) {
        setCancelling(false);
        setConfirmOpen(false);
      }
    }
  }

  const pageError = !validId
    ? "Mã đơn đặt món không hợp lệ."
    : error;

  function reload() {
    if (!validId) return;
    setLoading(true);
    setError("");
    setFeedback(null);
    setAttempt((value) => value + 1);
  }

  return (
    <OrderDetailView
      order={order}
      loading={loading}
      error={pageError}
      cancelling={cancelling}
      uncertain={uncertain}
      confirmOpen={confirmOpen}
      feedback={feedback}
      onRetry={reload}
      onOpenCancel={() => setConfirmOpen(true)}
      onConfirmCancel={() => void confirmCancel()}
      onCloseCancel={() => {
        if (!cancellingRef.current) setConfirmOpen(false);
      }}
    />
  );
}

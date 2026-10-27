import {
  Link,
  useNavigate,
  useParams,
} from "react-router-dom";

import {
  ChevronLeft,
  CircleAlert,
  ShoppingCart,
} from "lucide-react";

import { Button } from "../../../components/ui/Button";

import {
  DataTableState,
} from "../../../components/ui/DataTable";

import {
  DeliveryInfoCard,
  FoodOrderItems,
  FoodOrderStatusCard,
  FoodOrderSummaryCard,
} from "../../../components/common/FoodOrderDetail/FoodOrderDetailCards";

import type {
  ApiErrorResponse,
} from "../../../types/api";

import {
  dateInputValue,
  displayDate,
  displayTime,
} from "../../../utils/reservation";

import { useStaffOrderDetail } from "./useStaffOrderDetail";

import "./StaffOrderDetailPage.css";

const ORDER_LIST_PATH = "/staff/orders";

function DetailSkeleton() {
  const widths = ["64%", "94%", "80%", "54%"];

  return (
    <>
      <span
        className="staff-order-detail__sr-only"
        role="status"
      >
        Đang tải chi tiết đơn đặt món…
      </span>

      <div
        className="staff-order-detail__grid"
        aria-hidden="true"
      >
        {[0, 1].map((column) => (
          <div
            key={column}
            className={
              "food-order-card " +
              "staff-order-detail__loading-card"
            }
          >
            {widths.map((width) => (
              <span
                key={width}
                className="sage-data-table__skeleton"
                style={{ width }}
              />
            ))}
          </div>
        ))}
      </div>
    </>
  );
}

interface DetailErrorProps {
  error: ApiErrorResponse | null;
  onRetry: () => void;
  onBack: () => void;
}

function DetailError({
  error,
  onRetry,
  onBack,
}: DetailErrorProps) {
  const status = error?.status ?? 0;

  const notFound = status === 404;
  const forbidden = status === 403;
  const invalidId = status === 400;
  const unauthorized = status === 401;

  const canRetry =
    status === 0 ||
    status === 408 ||
    status === 429 ||
    status >= 500;

  const title = notFound
    ? "Không tìm thấy đơn đặt món"
    : forbidden
      ? "Không có quyền xem đơn đặt món"
      : invalidId
        ? "Mã đơn đặt món không hợp lệ"
        : unauthorized
          ? "Phiên đăng nhập đã hết hạn"
          : "Không thể tải chi tiết đơn";

  const description = notFound
    ? "Đơn không tồn tại hoặc không còn truy cập được."
    : forbidden
      ? "Tài khoản của bạn không có quyền xem đơn này."
      : invalidId
        ? "Vui lòng mở lại đơn từ danh sách đơn đặt món."
        : unauthorized
          ? "Vui lòng đăng nhập lại để tiếp tục."
          : "Kiểm tra kết nối và thử lại để xem thông tin đơn.";

  return (
    <div className="food-order-card">
      <div className="staff-order-detail__state">
        <DataTableState
          variant={notFound ? "empty" : "error"}
          title={title}
          description={description}
          icon={
            notFound ? (
              <ShoppingCart size={32} />
            ) : (
              <CircleAlert size={32} />
            )
          }
        />

        <div className="staff-order-detail__state-actions">
          {canRetry && (
            <Button
              type="button"
              variant="primary"
              size="md"
              onClick={onRetry}
            >
              Thử lại
            </Button>
          )}

          <Button
            type="button"
            variant="outline"
            size="md"
            onClick={onBack}
          >
            Quay lại danh sách
          </Button>
        </div>
      </div>
    </div>
  );
}

export function StaffOrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const {
    order,
    loading,
    error,
    reload,
  } = useStaffOrderDetail(id);

  const description = order
    ? (
        `Đặt lúc ${displayDate(dateInputValue(order.createdAt))}` +
        ` · ${displayTime(order.createdAt)}` +
        ` · Khách hàng: ${order.customerName || "—"}`
      )
    : "Xem thông tin đơn đặt món của khách hàng.";

  return (
    <section
      className="staff-order-detail"
      aria-labelledby="staff-order-detail-title"
      aria-busy={loading}
    >
      <header className="staff-order-detail__header">
        <div className="staff-order-detail__heading">
          <Link
            className="staff-order-detail__back"
            to={ORDER_LIST_PATH}
          >
            <ChevronLeft size={16} aria-hidden="true" />
            Quay lại danh sách
          </Link>

          <h1 id="staff-order-detail-title">
            {order
              ? `Đơn đặt món #${order.id}`
              : "Chi tiết đơn đặt món"}
          </h1>

          <p>{description}</p>
        </div>

        <Button
          type="button"
          variant="outline"
          size="md"
          className="staff-order-detail__refresh"
          disabled={loading}
          onClick={reload}
        >
          Làm mới
        </Button>
      </header>

      {loading ? (
        <DetailSkeleton />
      ) : order ? (
        <div className="staff-order-detail__grid">
          <div className="staff-order-detail__column">
            <FoodOrderItems order={order} />
            <DeliveryInfoCard order={order} />
          </div>

          <aside
            className="staff-order-detail__column"
            aria-label="Trạng thái và tóm tắt đơn hàng"
          >
            <FoodOrderStatusCard order={order} />
            <FoodOrderSummaryCard order={order} />
          </aside>
        </div>
      ) : (
        <DetailError
          error={error}
          onRetry={reload}
          onBack={() => void navigate(ORDER_LIST_PATH)}
        />
      )}
    </section>
  );
}
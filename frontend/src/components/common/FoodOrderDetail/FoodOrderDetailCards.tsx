import {
  useId,
  type ReactNode,
} from "react";

import {
  Clock,
  MapPin,
  Phone,
} from "lucide-react";

import { Badge } from "../../ui/Badge";

import {
  DataTable,
  type DataTableColumn,
} from "../../ui/DataTable";

import type {
  StaffOrderDetailItem,
  StaffOrderDetailResponse,
} from "../../../types/staff-order";

import {
  dateInputValue,
  displayDate,
  displayTime,
} from "../../../utils/reservation";

import "./FoodOrderDetailCards.css";

const numberFormatter = new Intl.NumberFormat("vi-VN");

function money(value: number): string {
  return `${numberFormatter.format(value)}đ`;
}

function eventTime(value: string | null): string {
  if (!value) return "—";

  return (
    `${displayDate(dateInputValue(value))} · ` +
    displayTime(value)
  );
}

interface OrderCardProps {
  title: string;
  description?: ReactNode;
  trailing?: ReactNode;
  children: ReactNode;
  className?: string;
}

function OrderCard({
  title,
  description,
  trailing,
  children,
  className = "",
}: OrderCardProps) {
  const titleId = useId();

  return (
    <section
      className={`food-order-card ${className}`.trim()}
      aria-labelledby={titleId}
    >
      <header className="food-order-card__header">
        <div className="food-order-card__title-row">
          <h2 id={titleId}>{title}</h2>
          {trailing}
        </div>

        {description != null && (
          <p className="food-order-card__description">
            {description}
          </p>
        )}
      </header>

      {children}
    </section>
  );
}

interface OrderProps {
  order: StaffOrderDetailResponse;
}

const itemColumns: DataTableColumn<StaffOrderDetailItem>[] = [
  {
    id: "dishName",
    header: "Món ăn",
    width: 240,
    render: (item) => item.dishName,
  },
  {
    id: "unitPrice",
    header: "Đơn giá",
    width: 120,
    align: "right",
    render: (item) => money(item.unitPrice),
  },
  {
    id: "quantity",
    header: "Số lượng",
    width: 96,
    align: "center",
    render: (item) =>
      numberFormatter.format(item.quantity),
  },
  {
    id: "lineTotal",
    header: "Thành tiền",
    width: 144,
    align: "right",
    render: (item) => money(item.lineTotal),
  },
];

export function FoodOrderItems({ order }: OrderProps) {
  return (
    <OrderCard
      title="Món đã đặt"
      description={
        `${numberFormatter.format(order.dishCount)} món · ` +
        `${numberFormatter.format(order.totalQuantity)} phần`
      }
      className="food-order-items"
    >
      <DataTable
        caption={`Các món trong đơn #${order.id}`}
        columns={itemColumns}
        data={order.items}
        getRowKey={(item) => item.id}
        showFooter={false}
        showPagination={false}
        emptyTitle="Chưa có món trong đơn"
        emptyDescription="Danh sách món hiện đang trống."
      />

      <p className="food-order-card__note">
        Đơn giá được ghi nhận tại thời điểm đặt món.
      </p>
    </OrderCard>
  );
}

export function DeliveryInfoCard({ order }: OrderProps) {
  return (
    <OrderCard
      title="Thông tin nhận hàng"
      className="food-order-card--divided"
    >
      <dl className="food-order-fields">
        <div>
          <dt>
            <Phone size={20} aria-hidden="true" />
            <span>Số điện thoại</span>
          </dt>

          <dd>{order.deliveryPhone || "—"}</dd>
        </div>

        <div>
          <dt>
            <MapPin size={20} aria-hidden="true" />
            <span>Địa chỉ</span>
          </dt>

          <dd>{order.deliveryAddress || "—"}</dd>
        </div>
      </dl>
    </OrderCard>
  );
}

export function FoodOrderStatusCard({ order }: OrderProps) {
  const cancelled = order.status === "CANCELLED";

  const timestamp = cancelled
    ? order.cancelledAt
    : order.createdAt;

  return (
    <OrderCard
      title="Trạng thái đơn"
      trailing={
        <Badge variant={cancelled ? "cancelled" : "placed"}>
          {cancelled ? "Đã hủy" : "Đã đặt"}
        </Badge>
      }
    >
      <p className="food-order-card__body">
        {cancelled
          ? "Đơn đặt món đã được khách hàng hủy."
          : "Đơn đặt món đã được ghi nhận."}
      </p>

      <div className="food-order-status__event">
        <Clock size={20} aria-hidden="true" />

        <div>
          <span>{cancelled ? "Hủy lúc" : "Đặt lúc"}</span>
          <strong>{eventTime(timestamp)}</strong>
        </div>
      </div>
    </OrderCard>
  );
}

export function FoodOrderSummaryCard({ order }: OrderProps) {
  const cancelled = order.status === "CANCELLED";

  return (
    <OrderCard
      title="Tóm tắt đơn hàng"
      className="food-order-card--divided"
    >
      <dl className="food-order-summary">
        <div>
          <dt>Số món</dt>
          <dd>
            {numberFormatter.format(order.dishCount)} món
          </dd>
        </div>

        <div>
          <dt>Tổng số phần</dt>
          <dd>
            {numberFormatter.format(order.totalQuantity)} phần
          </dd>
        </div>

        <div>
          <dt>Tạm tính</dt>
          <dd>{money(order.totalAmount)}</dd>
        </div>

        <div>
          <dt>Thanh toán</dt>
          <dd>{order.paymentMethod}</dd>
        </div>

        <div className="food-order-summary__total">
          <dt>Tổng cộng</dt>
          <dd>{money(order.totalAmount)}</dd>
        </div>
      </dl>

      <p className="food-order-card__note">
        {cancelled
          ? "Đơn đã hủy; giá được giữ để đối chiếu."
          : "Thanh toán khi nhận hàng (COD)."}
      </p>
    </OrderCard>
  );
}
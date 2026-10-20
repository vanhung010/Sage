import { useCallback, useEffect } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { orderApi } from "../../../api/order.api";
import type { OrderDetailResponse } from "../../../types/order";
import { Badge } from "../../../components/ui/Badge";
import { CustomerOrdersLayout } from "./CustomerOrdersLayout";
import { EmptyOrderList, HelpfulInformation, ListPagination, ListState } from "./OrderListShared";
import { formatDate, readPage } from "./listFormatting";
import { useOrderList, type OrderListState } from "./useOrderList";

type OrderStatus = OrderDetailResponse["foodOrderStatus"];
const money = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 });

export function MyFoodOrdersPage() {
  const [params, setParams] = useSearchParams();
  const candidate = params.get("status");
  const status = candidate === "PLACED" || candidate === "CANCELLED" ? candidate : undefined;
  const page = readPage(params.get("page"));
  const changePage = useCallback((next: number) => {
    setParams((current) => {
      const updated = new URLSearchParams(current);
      if (next === 0) updated.delete("page");
      else updated.set("page", String(next));
      return updated;
    });
  }, [setParams]);

  return <FoodOrdersPageContent key={`${status ?? "all"}:${page}`} status={status} page={page} onPageChange={changePage}
    onStatusChange={(value) => setParams(value ? { status: value } : {})} />;
}

function FoodOrdersPageContent({ status, page, onPageChange, onStatusChange }: {
  status?: OrderStatus; page: number; onPageChange: (page: number) => void;
  onStatusChange: (status?: OrderStatus) => void;
}) {
  const load = useCallback((signal: AbortSignal) => orderApi.listMine({ status, page, size: 4 }, signal), [status, page]);
  const list = useOrderList(load);
  const empty = !list.loading && !list.error && list.data?.totalElements === 0 && !status;
  useEffect(() => {
    if (list.data && page > 0 && list.data.content.length === 0) onPageChange(Math.max(0, list.data.totalPages - 1));
  }, [list.data, page, onPageChange]);
  return <CustomerOrdersLayout kind="orders" empty={empty}>
    {!empty && <div className="customer-orders__toolbar">
      <div className="customer-orders__order-filters" role="group" aria-label="Trạng thái đơn đặt món">
        {([
          [undefined, "Tất cả"], ["PLACED", "Đã đặt"], ["CANCELLED", "Đã hủy"],
        ] as const).map(([value, label]) => <button key={label} type="button" aria-pressed={status === value}
          onClick={() => onStatusChange(value)}>{label}</button>)}
      </div>
      <p>Mới đặt nhất trước</p>
    </div>}
    <FoodOrderResults list={list} status={status} onPageChange={onPageChange} />
    {!empty && <HelpfulInformation title="Cần thay đổi đơn đặt món?">
      Mở chi tiết để hủy đơn khi đơn còn ở trạng thái Đã đặt.
    </HelpfulInformation>}
  </CustomerOrdersLayout>;
}

function FoodOrderResults({ status, list, onPageChange }: {
  status?: OrderStatus; list: OrderListState<OrderDetailResponse>; onPageChange: (page: number) => void;
}) {
  const { data, loading, error, retry } = list;

  if (loading) return <ListState loading />;
  if (error || !data) return <ListState error={error || "Không thể tải đơn đặt món."} onRetry={retry} />;
  if (data.totalElements === 0 && !status) return <EmptyOrderList kind="food" />;
  return <>
    <div className="customer-orders__summary" role="status">
      <p>{data.totalElements} đơn đặt món</p><p>Thanh toán khi nhận hàng · COD</p>
    </div>
    {data.content.length ? <div className="customer-orders__grid" aria-label="Danh sách đơn đặt món">
      {data.content.map((order) => <FoodOrderCard key={order.id} order={order} />)}
    </div> : <ListState title={status ? "Không có đơn phù hợp" : "Bạn chưa có đơn đặt món"}
      description={status ? "Hãy chọn trạng thái khác để xem các đơn của bạn." : "Khám phá thực đơn và chọn món ngon bạn yêu thích tại Sage."} />}
    <ListPagination data={data} onPageChange={onPageChange} />
  </>;
}

function FoodOrderCard({ order }: { order: OrderDetailResponse }) {
  const cancelled = order.foodOrderStatus === "CANCELLED";
  const items = order.orderDishItemResponseList;
  return <article className={`customer-orders__card food-order-list-card${cancelled ? " food-order-list-card--cancelled" : ""}`} aria-label={`Đơn đặt món #${order.id}`}>
    <div className="customer-orders__identity">
      <div><p className="customer-orders__caption">ĐƠN MÓN</p><h2>#{order.id}</h2>
        <p className="customer-orders__caption">{formatDate(order.createdAt, true)}</p></div>
      <Badge variant={cancelled ? "error" : "info"}>{cancelled ? "Đã hủy" : "Đã đặt"}</Badge>
    </div>
    <div className="customer-orders__divider" />
    <div className="food-order-list-card__items">
      {items[0] && <p>{items[0].nameDish} × {items[0].quantity}</p>}
      {items.length === 2 && <p className="food-order-list-card__other">{items[1].nameDish} × {items[1].quantity}</p>}
      {items.length > 2 && <p className="food-order-list-card__other">+ {items.length - 1} món khác</p>}
      <p className="customer-orders__caption">{items.length} món · {order.totalQuantity} phần</p>
    </div>
    {cancelled && order.cancelledAt && <p className="food-order-list-card__cancelled-at">Hủy lúc {formatDate(order.cancelledAt, true)}</p>}
    <div className="food-order-list-card__total"><span>Tổng tiền</span><strong>{money.format(order.totalAmountPrice)}đ</strong></div>
    <div className="customer-orders__card-footer">
      <div className="food-order-list-card__payment"><Badge>{order.paymentMethod}</Badge><span>Thanh toán khi nhận hàng</span></div>
      <Link className="sage-button sage-button--outline sage-button--md" to={`/orders/${order.id}`} aria-label={`Xem chi tiết đơn #${order.id}`}>Xem chi tiết</Link>
    </div>
  </article>;
}

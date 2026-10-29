import { useCallback, useEffect } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { reservationApi } from "../../../api/reservation.api";
import type { ReservationResponse, ReservationStatus } from "../../../types/reservation";
import { Badge, type BadgeVariant } from "../../../components/ui/Badge";
import { Select } from "../../../components/ui/Select";
import { timeRange } from "../../../utils/reservation";
import calendar from "../../../assets/my-orders/calendar.svg";
import chevron from "../../../assets/my-orders/select-chevron.svg";
import { CustomerOrdersLayout } from "./CustomerOrdersLayout";
import { EmptyOrderList, HelpfulInformation, ListPagination, ListState } from "./OrderListShared";
import { formatDate, readPage } from "./listFormatting";
import { useOrderList, type OrderListState } from "./useOrderList";
import { useRealtimeRefresh } from "../../../realtime/useRealtimeRefresh";

const statuses: Record<ReservationStatus, { label: string; variant: BadgeVariant }> = {
  PENDING: { label: "Chờ xác nhận", variant: "warning" },
  CONFIRMED: { label: "Đã xác nhận", variant: "success" },
  ARRIVED: { label: "Đã đến", variant: "info" },
  COMPLETED: { label: "Hoàn thành", variant: "neutral" },
  CANCELLED: { label: "Đã hủy", variant: "error" },
  REJECTED: { label: "Bị từ chối", variant: "error" },
  NO_SHOW: { label: "Không đến", variant: "warning" },
};
const statusOptions = [
  { value: "", label: "Tất cả trạng thái" },
  ...Object.entries(statuses).map(([value, { label }]) => ({ value, label })),
];

export function MyReservationsPage() {
  const [params, setParams] = useSearchParams();
  const scope = params.get("scope") === "history" ? "history" : "upcoming";
  const candidate = params.get("status");
  const status = scope === "history" && candidate && Object.hasOwn(statuses, candidate)
    ? candidate as ReservationStatus : undefined;
  const page = readPage(params.get("page"));

  const changePage = useCallback((next: number) => {
    setParams((current) => {
      const updated = new URLSearchParams(current);
      if (next === 0) updated.delete("page");
      else updated.set("page", String(next));
      return updated;
    });
  }, [setParams]);

  function changeScope(next: "upcoming" | "history") {
    setParams(next === "upcoming" ? {} : { scope: next });
  }

  return <ReservationPageContent key={`${scope}:${status ?? "all"}:${page}`} scope={scope} status={status} page={page}
    onPageChange={changePage} onScopeChange={changeScope}
    onStatusChange={(value) => setParams({ scope: "history", ...(value ? { status: value } : {}) })} />;
}

function ReservationPageContent({ scope, status, page, onPageChange, onScopeChange, onStatusChange }: {
  scope: "upcoming" | "history"; status?: ReservationStatus; page: number;
  onPageChange: (page: number) => void; onScopeChange: (scope: "upcoming" | "history") => void;
  onStatusChange: (status: string) => void;
}) {
  const load = useCallback((signal: AbortSignal) => reservationApi.listMine({ scope, status, page, size: 4 }, signal), [scope, status, page]);
  const list = useOrderList(load);
  useRealtimeRefresh(
  (signal) =>
    signal.type === "SYNC" ||
    signal.type.startsWith("RESERVATION_"),
  list.retry,
);
  const empty = !list.loading && !list.error && list.data?.totalElements === 0 && !status;
  useEffect(() => {
    if (list.data && page > 0 && list.data.content.length === 0) onPageChange(Math.max(0, list.data.totalPages - 1));
  }, [list.data, page, onPageChange]);
  return (
    <CustomerOrdersLayout kind="reservations" empty={empty}>
      <div className="customer-orders__toolbar">
        <div className="customer-orders__scope" role="group" aria-label="Thời gian đặt bàn">
          <button type="button" aria-pressed={scope === "upcoming"} onClick={() => onScopeChange("upcoming")}>Sắp tới</button>
          <button type="button" aria-pressed={scope === "history"} onClick={() => onScopeChange("history")}>Lịch sử</button>
        </div>
        {!empty && <p>{scope === "upcoming" ? "Lịch hẹn gần nhất trước" : "Ngày hẹn gần nhất trước"}</p>}
      </div>
      {scope === "history" && !empty && <div className="customer-orders__status-filter">
        <label htmlFor="reservation-history-status">Trạng thái</label>
        <Select id="reservation-history-status" options={statusOptions} value={status ?? ""}
          trailingIcon={<img src={chevron} alt="" />}
          onValueChange={onStatusChange} />
        <p>Lọc các lịch hẹn đã kết thúc hoặc đã hủy.</p>
      </div>}
      <ReservationResults list={list} scope={scope} status={status} onPageChange={onPageChange} />
      {!empty && <HelpfulInformation title={scope === "history" ? "Lịch sử đặt bàn" : "Quản lý lịch hẹn của bạn"}>
        {scope === "history"
          ? "Xem lại thông tin và trạng thái các lịch hẹn trước đây của bạn."
          : "Mở chi tiết đơn để đổi lịch hoặc hủy trước giờ hẹn ít nhất 2 giờ."}
      </HelpfulInformation>}
    </CustomerOrdersLayout>
  );
}

function ReservationResults({ scope, status, list, onPageChange }: {
  scope: "upcoming" | "history"; status?: ReservationStatus; list: OrderListState<ReservationResponse>; onPageChange: (page: number) => void;
}) {
  const { data, loading, error, retry } = list;

  if (loading) return <ListState loading />;
  if (error || !data) return <ListState error={error || "Không thể tải đơn đặt bàn."} onRetry={retry} />;
  if (data.totalElements === 0 && !status) return <EmptyOrderList kind={scope} />;
  return <>
    <div className="customer-orders__summary" role="status">
      <p>{data.totalElements} {scope === "upcoming" ? "lịch hẹn sắp tới" : "đơn trong lịch sử"}</p>
      {scope === "upcoming" && <p>Đến đúng giờ để Sage đón tiếp bạn chu đáo.</p>}
    </div>
    {data.content.length ? <div className="customer-orders__grid" aria-label="Danh sách đơn đặt bàn">
      {data.content.map((reservation) => <ReservationCard key={reservation.id} reservation={reservation} />)}
    </div> : <ListState
      title={status ? "Không có lịch hẹn phù hợp" : scope === "upcoming" ? "Bạn chưa có lịch hẹn sắp tới" : "Chưa có lịch sử đặt bàn"}
      description={status ? "Hãy chọn trạng thái khác để xem các lịch hẹn của bạn." : "Đặt bàn tại Sage để cùng sẻ chia những bữa ăn đáng nhớ."}
    />}
    <ListPagination data={data} onPageChange={onPageChange} hideSinglePage={scope === "upcoming"} />
  </>;
}

function ReservationCard({ reservation }: { reservation: ReservationResponse }) {
  const status = statuses[reservation.status];
  return <article className="customer-orders__card reservation-list-card" aria-label={`Đơn đặt bàn ${reservation.code}`}>
    <div className="customer-orders__identity">
      <div><p className="customer-orders__caption">MÃ ĐẶT BÀN</p><h2>{reservation.code}</h2></div>
      <Badge variant={status.variant}>{status.label}</Badge>
    </div>
    <div className="customer-orders__divider" />
    <div className="reservation-list-card__schedule">
      <span className="reservation-list-card__calendar"><img src={calendar} alt="" /></span>
      <div><p className="reservation-list-card__date">{formatDate(reservation.startTime)}</p><p>{timeRange(reservation)}</p></div>
    </div>
    <dl className="reservation-list-card__information">
      <div><dt>Số khách</dt><dd>{reservation.partySize} người</dd></div>
      <div><dt>Khu vực yêu cầu</dt><dd>{reservation.requestedArea?.name ?? "Không yêu cầu"}</dd></div>
    </dl>
    <div className="customer-orders__card-footer">
      <p className="customer-orders__caption">Đặt ngày {formatDate(reservation.createdAt)}</p>
      <Link className="sage-button sage-button--outline sage-button--md" to={`/reservations/${reservation.id}`} aria-label={`Xem chi tiết đơn ${reservation.code}`}>Xem chi tiết</Link>
    </div>
  </article>;
}

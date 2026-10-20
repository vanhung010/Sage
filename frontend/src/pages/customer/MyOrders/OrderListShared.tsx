import type { PageResponse } from "../../../types/api";
import { useNavigate } from "react-router-dom";

import { Button } from "../../../components/ui/Button";
import { Pagination } from "../../../components/ui/Pagination";
import previous from "../../../assets/my-orders/previous.svg";
import next from "../../../assets/my-orders/next.svg";
import emptyCalendar from "../../../assets/my-orders/empty-calendar.svg";
import emptyCart from "../../../assets/my-orders/empty-cart.svg";

const emptyStates = {
  upcoming: {
    title: "Bạn chưa có lịch hẹn sắp tới",
    description: "Chọn thời gian phù hợp để Sage chuẩn bị bàn cho bạn.",
  },
  history: {
    title: "Chưa có lịch sử đặt bàn",
    description: "Các lịch hẹn đã kết thúc hoặc đã hủy sẽ xuất hiện tại đây.",
  },
  food: {
    title: "Bạn chưa đặt món nào",
    description: "Khám phá thực đơn và chọn món bạn yêu thích để Sage chuẩn bị cho bạn.",
  },
};

export function EmptyOrderList({ kind }: { kind: keyof typeof emptyStates }) {
  const navigate = useNavigate();
  const copy = emptyStates[kind];
  const food = kind === "food";
  return (
    <section className="customer-orders__empty" role="status" aria-labelledby="empty-orders-title">
      <div className="customer-orders__empty-icon"><img src={food ? emptyCart : emptyCalendar} alt="" /></div>
      <div className="customer-orders__empty-copy">
        <h2 id="empty-orders-title">{copy.title}</h2>
        <p>{copy.description}</p>
      </div>
      <div className="customer-orders__empty-action">
        <Button type="button" size="lg" onClick={() => navigate(food ? "/order" : "/reservation")}>
          {food ? "Đặt món ngay" : "Đặt bàn ngay"}
        </Button>
      </div>
    </section>
  );
}

export function ListState({
  loading, error, title, description, onRetry,
}: {
  loading?: boolean; error?: string; title?: string; description?: string; onRetry?: () => void;
}) {
  return (
    <section className="customer-orders__state" role={error ? "alert" : "status"}>
      <h2>{loading ? "Đang tải danh sách đơn…" : error ? "Chưa thể tải danh sách" : title}</h2>
      {!loading && <p>{error || description}</p>}
      {error && <Button variant="outline" onClick={onRetry}>Thử lại</Button>}
    </section>
  );
}

export function ListPagination<T>({
  data, onPageChange, hideSinglePage = false,
}: { data: PageResponse<T>; onPageChange: (page: number) => void; hideSinglePage?: boolean }) {
  if (data.totalElements === 0 || (hideSinglePage && data.totalPages <= 1)) return null;
  const start = data.page * data.size + 1;
  const end = data.page * data.size + data.content.length;
  return (
    <div className="customer-orders__pagination">
      <p>Hiển thị {start}–{end} trong {data.totalElements} đơn</p>
      {data.totalPages > 1 && <Pagination
        currentPage={data.page + 1}
        totalPages={data.totalPages}
        onPageChange={(page) => onPageChange(page - 1)}
        previousIcon={<img src={previous} alt="" />}
        nextIcon={<img src={next} alt="" />}
      />}
    </div>
  );
}

export function HelpfulInformation({ title, children }: { title: string; children: string }) {
  return <aside className="customer-orders__help"><h2>{title}</h2><p>{children}</p></aside>;
}



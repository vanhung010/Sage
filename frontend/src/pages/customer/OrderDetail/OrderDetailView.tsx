import { useNavigate } from "react-router-dom";
import type { OrderDetailResponse } from "../../../types/order";
import { Header } from "../../../components/layout/Header";
import { Footer } from "../../../components/layout/Footer";
import { Badge } from "../../../components/ui/Badge";
import { Button } from "../../../components/ui/Button";
import { AppFeedbackModal } from "../../../components/ui/AppFeedbackModal";
import headerLogo from "../../../assets/order-detail/header-logo.png";
import footerLogo from "../../../assets/order-detail/footer-logo.png";
import addressIcon from "../../../assets/order-detail/address.svg";
import phoneIcon from "../../../assets/order-detail/phone.svg";
import mailIcon from "../../../assets/order-detail/mail.svg";

import "./OrderDetailView.css";

interface Props {
  order: OrderDetailResponse | null;
  loading: boolean;
  error: string;
  cancelling: boolean;
  uncertain: boolean;
  confirmOpen: boolean;
  feedback: { error: boolean; message: string } | null;
  onRetry: () => void;
  onOpenCancel: () => void;
  onConfirmCancel: () => void;
  onCloseCancel: () => void;
}

const restaurantInfo = {
  description: "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address: "123 Nguyễn Thị Minh Khai,\nQuận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};
const contactIcons = { address: addressIcon, phone: phoneIcon, mail: mailIcon };
const number = new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 0 });
const money = (value: number) => `${number.format(value)}đ`;

function formatDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "—";
  const day = new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh", day: "2-digit", month: "2-digit", year: "numeric",
  }).format(date);
  const time = new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh", hour: "2-digit", minute: "2-digit", hourCycle: "h23",
  }).format(date);
  return `${day} · ${time}`;
}

export function OrderDetailView({
  order, loading, error, cancelling, uncertain, confirmOpen, feedback,
  onRetry, onOpenCancel, onConfirmCancel, onCloseCancel,
}: Props) {
  const navigate = useNavigate();
  const cancelled = order?.foodOrderStatus === "CANCELLED";

  return (
    <div className="food-order-detail">
      <Header active="order" logoSrc={headerLogo} />
      <main>
        <section className="food-order-detail__hero">
          <div className="food-order-detail__container">
            <p className="food-order-detail__eyebrow">ĐƠN MÓN CỦA TÔI</p>
            <h1>Chi tiết đơn hàng{order && !loading && !error ? ` #${order.id}` : ""}</h1>
            <p className="food-order-detail__subtitle">
              {order && !loading && !error ? `Đặt lúc ${formatDate(order.createdAt)}` : "Theo dõi thông tin đơn đặt món của bạn."}
            </p>
          </div>
        </section>

        <div className="food-order-detail__content">
          <div className="food-order-detail__container">
            {loading ? (
              <section className="food-order-detail__card" role="status">Đang tải thông tin đơn…</section>
            ) : error || !order ? (
              <section className="food-order-detail__card">
                <h2>Không thể hiển thị đơn hàng</h2>
                <p role="alert">{error || "Không tìm thấy đơn đặt món."}</p>
                <div className="food-order-detail__state-actions">
                  <Button type="button" onClick={onRetry}>Thử lại</Button>
                  <Button type="button" variant="outline" onClick={() => navigate("/order")}>Về trang đặt món</Button>
                </div>
              </section>
            ) : (
              <>
                {feedback && <p className={`food-order-detail__notice ${feedback.error ? "is-error" : ""}`}
                  role={feedback.error ? "alert" : "status"}>{feedback.message}</p>}

                <div className="food-order-detail__grid">
                  <div className="food-order-detail__column">
                    <section className="food-order-detail__card">
                      <div className="food-order-detail__section-heading">
                        <h2>Món đã đặt</h2>
                        <p>{order.orderDishItemResponseList.length} món · {order.totalQuantity} phần</p>
                      </div>
                      <div className="food-order-detail__table-wrap" tabIndex={0} role="region" aria-label="Danh sách món đã đặt">
                        <table>
                          <caption className="food-order-detail__sr-only">Đơn giá và số lượng được ghi nhận khi đặt món</caption>
                          <colgroup><col /><col /><col /><col /></colgroup>
                          <thead><tr><th scope="col">Món ăn</th><th scope="col">Đơn giá</th><th scope="col">Số lượng</th><th scope="col">Thành tiền</th></tr></thead>
                          <tbody>{order.orderDishItemResponseList.map((item) => (
                            <tr key={item.id}><th scope="row">{item.nameDish}</th><td>{money(item.unitPrice)}</td><td>{item.quantity}</td><td>{money(item.lineTotal)}</td></tr>
                          ))}</tbody>
                        </table>
                      </div>
                      <p className="food-order-detail__note">Đơn giá được ghi nhận tại thời điểm bạn đặt món.</p>
                    </section>

                    <section className="food-order-detail__card">
                      <h2>Thông tin nhận hàng</h2>
                      <dl className="food-order-detail__delivery">
                        <div><dt>Địa chỉ</dt><dd>{order.deleveryAddress}</dd></div>
                        <div><dt>Số điện thoại</dt><dd>{order.deliveryPhone}</dd></div>
                      </dl>
                    </section>
                  </div>

                  <aside className="food-order-detail__column" aria-label="Trạng thái và tóm tắt đơn hàng">
                    <section className="food-order-detail__card">
                      <div className="food-order-detail__status-heading">
                        <h2>Trạng thái đơn</h2>
                        <Badge variant={cancelled ? "error" : "info"}>{cancelled ? "Đã hủy" : "Đã đặt"}</Badge>
                      </div>
                      <p className="food-order-detail__description">
                        {cancelled ? (order.cancelledAt ? `Đã hủy lúc ${formatDate(order.cancelledAt)}.` : "Đơn hàng đã được hủy.") : "Đơn hàng của bạn đã được ghi nhận."}
                      </p>
                    </section>

                    <section className="food-order-detail__card">
                      <h2>Tóm tắt đơn hàng</h2>
                      <dl className="food-order-detail__summary">
                        <div><dt>Số lượng</dt><dd>{order.totalQuantity} phần</dd></div>
                        <div><dt>Tạm tính</dt><dd>{money(order.totalAmountPrice)}</dd></div>
                        <div><dt>Thanh toán</dt><dd>{order.paymentMethod}</dd></div>
                        <div className="food-order-detail__divider" />
                        <div className="food-order-detail__total"><dt>Tổng cộng</dt><dd>{money(order.totalAmountPrice)}</dd></div>
                      </dl>
                      <p className="food-order-detail__note">Thanh toán khi nhận hàng (COD).</p>
                      <Button type="button" size="lg" fullWidth onClick={() => navigate("/menu")}>Tiếp tục xem thực đơn</Button>
                    </section>

                    {!cancelled && <section className="food-order-detail__card">
                      <h2>Quản lý đơn hàng</h2>
                      <p className="food-order-detail__description">
                        {uncertain ? "Vui lòng kiểm tra trạng thái trước khi gửi lại yêu cầu hủy." : "Bạn có thể hủy đơn khi trạng thái vẫn là “Đã đặt”."}
                      </p>
                      {uncertain ? (
                        <Button type="button" size="lg" fullWidth variant="outline" onClick={onRetry}>Kiểm tra trạng thái</Button>
                      ) : (
                        <Button type="button" size="lg" fullWidth variant="outline" loading={cancelling} onClick={onOpenCancel}>Hủy đơn hàng</Button>
                      )}
                    </section>}
                  </aside>
                </div>
              </>
            )}
          </div>
        </div>
      </main>

      <Footer restaurantInfo={restaurantInfo} logoSrc={footerLogo} contactIcons={contactIcons} />

     <AppFeedbackModal
  open={confirmOpen}
  severity="destructive"
  title={`Hủy đơn hàng #${order?.id ?? ""}?`}
  description="Bạn có chắc muốn hủy đơn hàng này? Sau khi hủy, đơn hàng không thể khôi phục."
  primaryButtonLabel="Xác nhận hủy đơn"
  secondaryButtonLabel="Giữ đơn hàng"
  primaryDisabled={!order}
  loading={cancelling}
  loadingLabel="Đang hủy..."
  onPrimaryAction={onConfirmCancel}
  onClose={onCloseCancel}
/>
    </div>
  );
}

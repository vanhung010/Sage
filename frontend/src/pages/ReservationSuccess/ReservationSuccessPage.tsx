import { CircleCheck, Clock3 } from "lucide-react";
import {
  Navigate,
  useLocation,
  useNavigate,
} from "react-router-dom";

import type { ReservationResponse } from "../../api/reservation.api";

import { Header } from "../../components/layout/Header";
import { Footer } from "../../components/layout/Footer";
import { Badge } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";

import "./ReservationSuccessPage.css";

interface SuccessLocationState {
  reservation?: ReservationResponse;
}

const restaurantInfo = {
  description: "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address: "123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(value));
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: "Asia/Ho_Chi_Minh",
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(new Date(value));
}

export function ReservationSuccessPage() {
  const location = useLocation();
  const navigate = useNavigate();

  const state = location.state as SuccessLocationState | null;
  const reservation = state?.reservation;

  // Khách mở URL trực tiếp mà không có kết quả đặt bàn.
  if (!reservation) {
    return <Navigate to="/reservation" replace />;
  }

  const pending = reservation.status === "PENDING";

  return (
    <div className="reservation-success">
      <Header variant="customer" active="reservation" />

      <main className="reservation-success__main">
        <div className="reservation-success__container">
          {pending ? (
            <Clock3
              className="reservation-success__icon reservation-success__icon--pending"
              size={54}
              strokeWidth={1.8}
              aria-hidden="true"
            />
          ) : (
            <CircleCheck
              className="reservation-success__icon"
              size={54}
              strokeWidth={1.8}
              aria-hidden="true"
            />
          )}

          <div className="reservation-success__heading">
            <p className="reservation-success__eyebrow">
              {pending ? "ĐÃ NHẬN YÊU CẦU ĐẶT BÀN" : "ĐẶT BÀN THÀNH CÔNG"}
            </p>

            <h1>
              {pending
                ? "Đơn đặt bàn đang chờ xác nhận"
                : "Bàn của bạn đã được xác nhận"}
            </h1>

            <p className="reservation-success__description">
              {pending
                ? "Sage đã nhận thông tin và sẽ thông báo khi đơn được xác nhận."
                : "Thông tin đặt bàn đã được ghi nhận tại Sage."}
            </p>
          </div>

          <section
            className="reservation-success__card"
            aria-label="Thông tin đơn đặt bàn vừa tạo"
          >
            <div className="reservation-success__card-heading">
              <div className="reservation-success__code">
                <span>Mã đặt bàn</span>
                <strong>{reservation.code}</strong>
              </div>

              <Badge variant={pending ? "warning" : "success"}>
                {pending ? "Chờ xác nhận" : "Đã xác nhận"}
              </Badge>
            </div>

            <dl className="reservation-success__details">
              <div>
                <dt>Ngày</dt>
                <dd>{formatDate(reservation.startTime)}</dd>
              </div>

              <div>
                <dt>Thời gian</dt>
                <dd>
                  {formatTime(reservation.startTime)} –{" "}
                  {formatTime(reservation.endTime)}
                </dd>
              </div>

              <div>
                <dt>Số người</dt>
                <dd>{reservation.partySize} khách</dd>
              </div>

              <div>
                <dt>Khu vực</dt>
                <dd>
                  {reservation.requestedArea?.name ?? "Không yêu cầu"}
                </dd>
              </div>
            </dl>
          </section>

          <p className="reservation-success__reminder">
            {pending
              ? "Đơn đang chờ nhà hàng duyệt. Vui lòng kiểm tra thông báo xác nhận trước khi đến."
              : "Vui lòng có mặt đúng giờ. Sage sẽ giữ bàn trong thời gian quy định kể từ giờ hẹn."}
          </p>

          <div className="reservation-success__actions">
            {/* Bật nút khi đã có route xem chi tiết đơn. */}
            <Button
              type="button"
              size="lg"
              disabled
              title="Chức năng xem chi tiết đơn sẽ được bổ sung"
            >
              Xem đơn đặt bàn
            </Button>

            <Button
              type="button"
              variant="outline"
              size="lg"
              onClick={() => navigate("/")}
            >
              Về trang chủ
            </Button>
          </div>
        </div>
      </main>

      <Footer restaurantInfo={restaurantInfo} />
    </div>
  );
}
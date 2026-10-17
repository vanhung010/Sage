import { CalendarDays, Clock3 } from "lucide-react";
import { useNavigate } from "react-router-dom";

import type {
  ReservationResponse,
  ReservationStatus,
} from "../../api/reservation.api";

import { Header } from "../../components/layout/Header";
import {
  Footer,
  type FooterRestaurantInfo,
} from "../../components/layout/Footer";
import { Badge, type BadgeVariant } from "../../components/ui/Badge";
import { Button } from "../../components/ui/Button";
import { NavigationBreadcrumb } from "../../components/ui/NavigationBreadcrumb";

import "./ReservationDetailPage.css";

interface ReservationDetailPageProps {
  reservation: ReservationResponse;

  // Trang cha xử lý chuyển trang đổi lịch hoặc mở modal xác nhận hủy.
  onReschedule?: () => void;
  onCancel?: () => void;
}

const restaurantInfo: FooterRestaurantInfo = {
  description: "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address: "123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};

const TIME_ZONE = "Asia/Ho_Chi_Minh";
const CHANGE_DEADLINE_HOURS = 2;
const MAX_RESCHEDULE_COUNT = 2;

const statusInfo: Record<
  ReservationStatus,
  { label: string; variant: BadgeVariant; description: string }
> = {
  PENDING: {
    label: "Chờ xác nhận",
    variant: "warning",
    description: "Đơn đặt bàn đang chờ nhà hàng xác nhận.",
  },
  CONFIRMED: {
    label: "Đã xác nhận",
    variant: "success",
    description:
      "Nhà hàng đã xác nhận đơn đặt bàn. Vui lòng có mặt đúng giờ hẹn.",
  },
  ARRIVED: {
    label: "Đã đến",
    variant: "info",
    description: "Nhà hàng đã ghi nhận bạn đến dùng bữa.",
  },
  COMPLETED: {
    label: "Hoàn thành",
    variant: "neutral",
    description: "Lượt đặt bàn đã hoàn thành.",
  },
  NO_SHOW: {
    label: "Không đến",
    variant: "neutral",
    description: "Đơn được ghi nhận là khách không đến.",
  },
  CANCELLED: {
    label: "Đã hủy",
    variant: "neutral",
    description: "Đơn đặt bàn này đã được hủy.",
  },
  REJECTED: {
    label: "Bị từ chối",
    variant: "error",
    description: "Nhà hàng không thể xác nhận đơn đặt bàn này.",
  },
};

function formatDate(value: string): string {
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: TIME_ZONE,
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(new Date(value));
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat("vi-VN", {
    timeZone: TIME_ZONE,
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
  }).format(new Date(value));
}

function durationMinutes(start: string, end: string): number {
  return Math.round(
    (new Date(end).getTime() - new Date(start).getTime()) / 60_000,
  );
}

export function ReservationDetailPage({
  reservation,
  onReschedule,
  onCancel,
}: ReservationDetailPageProps) {
  const navigate = useNavigate();

  const status = statusInfo[reservation.status];
  const remainingChanges = Math.max(
    0,
    MAX_RESCHEDULE_COUNT - reservation.rescheduleCount,
  );

  const deadline = new Date(
    new Date(reservation.startTime).getTime() -
      CHANGE_DEADLINE_HOURS * 60 * 60 * 1_000,
  );

  const active =
    reservation.status === "PENDING" ||
    reservation.status === "CONFIRMED";

  const withinDeadline = Date.now() < deadline.getTime();

  const canCancel = active && withinDeadline;
  const canReschedule =
    active && withinDeadline && remainingChanges > 0;

  return (
    <div className="reservation-detail-page">
      <Header variant="customer" active="reservation" />

      <main className="reservation-detail-page__main">
        <section className="reservation-detail-page__heading">
          <div className="container">
            <NavigationBreadcrumb
              items={[
                {
                  label: "Đơn đặt bàn của tôi",
                  to: "/reservations",
                },
                { label: "Chi tiết đơn" },
              ]}
            />

            <div className="reservation-detail-page__heading-row">
              <div>
                <h1>Chi tiết đơn đặt bàn</h1>
                <p>
                  Theo dõi thông tin và quản lý lịch hẹn của bạn tại Sage.
                </p>
              </div>

              <Button
                type="button"
                variant="outline"
                size="md"
                onClick={() => navigate("/reservations")}
              >
                Về đơn đặt bàn của tôi
              </Button>
            </div>
          </div>
        </section>

        <section className="reservation-detail-page__content">
          <div className="container reservation-detail-page__grid">
            <div className="reservation-detail-page__left">
              <article className="reservation-detail-page__card">
                <h2>Thông tin đặt bàn</h2>

                <div className="reservation-detail-page__appointment">
                  <div>
                    <span className="reservation-detail-page__appointment-label">
                      <CalendarDays size={20} aria-hidden="true" />
                      Ngày đặt bàn
                    </span>
                    <strong>{formatDate(reservation.startTime)}</strong>
                  </div>

                  <div>
                    <span className="reservation-detail-page__appointment-label">
                      <Clock3 size={20} aria-hidden="true" />
                      Thời gian
                    </span>
                    <strong>
                      {formatTime(reservation.startTime)} –{" "}
                      {formatTime(reservation.endTime)}
                    </strong>
                  </div>
                </div>

                <dl className="reservation-detail-page__rows">
                  <div>
                    <dt>Thời lượng</dt>
                    <dd>
                      {durationMinutes(
                        reservation.startTime,
                        reservation.endTime,
                      )}{" "}
                      phút
                    </dd>
                  </div>
                  <div>
                    <dt>Số khách</dt>
                    <dd>{reservation.partySize} khách</dd>
                  </div>
                  <div>
                    <dt>Khu vực yêu cầu</dt>
                    <dd>
                      {reservation.requestedArea?.name ?? "Không yêu cầu"}
                    </dd>
                  </div>
                </dl>

                <div className="reservation-detail-page__note">
                  <h3>Ghi chú đặc biệt</h3>
                  <p>{reservation.specialNote || "Không có ghi chú."}</p>
                </div>

                <div className="reservation-detail-page__created">
                  <span>Ngày tạo đơn</span>
                  <span>
                    {formatDate(reservation.createdAt)},{" "}
                    {formatTime(reservation.createdAt)}
                  </span>
                </div>
              </article>

              <aside className="reservation-detail-page__card reservation-detail-page__reminder">
                <h2>Trước khi đến Sage</h2>
                <p>
                  Vui lòng có mặt đúng giờ và cung cấp mã đặt bàn để nhà
                  hàng hỗ trợ đón tiếp.
                </p>
              </aside>
            </div>

            <div className="reservation-detail-page__right">
              <article className="reservation-detail-page__card reservation-detail-page__status">
                <span>Mã đặt bàn</span>
                <strong>{reservation.code}</strong>
                <Badge variant={status.variant}>{status.label}</Badge>
                <p>{status.description}</p>
              </article>

              <article className="reservation-detail-page__card reservation-detail-page__actions">
                <h2>Quản lý đơn</h2>

                <Button
                  type="button"
                  size="lg"
                  fullWidth
                  disabled={!canReschedule || !onReschedule}
                  onClick={onReschedule}
                >
                  Đổi lịch
                </Button>

                <p>Còn {remainingChanges} lần đổi lịch</p>

                <Button
                  type="button"
                  variant="outline"
                  size="lg"
                  fullWidth
                  disabled={!canCancel || !onCancel}
                  onClick={onCancel}
                >
                  Hủy đơn
                </Button>

                <p>
                  {active && withinDeadline
                    ? `Có thể thay đổi hoặc hủy đến ${formatTime(
                        deadline.toISOString(),
                      )} ngày ${formatDate(deadline.toISOString())} (trước giờ hẹn ít nhất 2 giờ).`
                    : "Đơn hiện không thể đổi lịch hoặc hủy từ trang này."}
                </p>
              </article>
            </div>
          </div>
        </section>
      </main>

      <Footer restaurantInfo={restaurantInfo} />
    </div>
  );
}
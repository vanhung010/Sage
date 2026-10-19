import { Header } from "../../../components/layout/Header";
import { Footer } from "../../../components/layout/Footer";
import { Button } from "../../../components/ui/Button";

import "./ReservationPageState.css";

interface ReservationPageStateProps {
  message: string;
  loading?: boolean;
  onRetry?: () => void;
}

export function ReservationPageState({
  message,
  loading = false,
  onRetry,
}: ReservationPageStateProps) {
  return (
    <div className="reservation-page-state">
      <Header active="reservation" />

      <main className="container reservation-page-state__content">
        <h1>
          {loading
            ? "Đang tải thông tin"
            : "Không thể mở đơn đặt bàn"}
        </h1>

        <p role={loading ? "status" : "alert"}>
          {message}
        </p>

        {onRetry && (
          <Button type="button" onClick={onRetry}>
            Thử lại
          </Button>
        )}
      </main>

      <Footer
        restaurantInfo={{
          description:
            "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
          address:
            "123 Nguyễn Thị Minh Khai, Quận 1, TP. Hồ Chí Minh",
          phone: "028 3822 1234",
          email: "hello@sage.vn",
        }}
      />
    </div>
  );
}
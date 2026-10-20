import type { ReactNode } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { Header } from "../../../components/layout/Header";
import { Footer } from "../../../components/layout/Footer";
import { Button } from "../../../components/ui/Button";
import { NavigationBreadcrumb } from "../../../components/ui/NavigationBreadcrumb";
import headerLogo from "../../../assets/order-detail/header-logo.png";
import footerLogo from "../../../assets/order-detail/footer-logo.png";
import address from "../../../assets/my-orders/address.svg";
import phone from "../../../assets/my-orders/phone.svg";
import mail from "../../../assets/my-orders/mail.svg";
import mobileMenu from "../../../assets/my-orders/mobile-menu.svg";
import "./MyOrders.css";

const restaurantInfo = {
  description: "Món ăn ngon hơn khi được sẻ chia cùng những người thân yêu.",
  address: "123 Nguyễn Thị Minh Khai,\nQuận 1, TP. Hồ Chí Minh",
  phone: "028 3822 1234",
  email: "hello@sage.vn",
};

export function CustomerOrdersLayout({
  kind, children, empty = false,
}: { kind: "reservations" | "orders"; children: ReactNode; empty?: boolean }) {
  const navigate = useNavigate();
  const reservations = kind === "reservations";
  const label = reservations ? "Đơn đặt bàn" : "Đơn đặt món";

  return (
    <div className={`customer-orders${empty ? " customer-orders--empty" : ""}`}>
      <Header active={null} logoSrc={headerLogo} mobileMenuIcon={empty ? mobileMenu : undefined} />
      <main id="customer-orders-main">
        <section className="customer-orders__heading" aria-labelledby="customer-orders-title">
          <div className="customer-orders__container customer-orders__heading-content">
            <NavigationBreadcrumb
              separator={<span aria-hidden="true">/</span>}
              items={[{ label: "Tài khoản" }, { label }]}
            />
            <nav className="customer-orders__navigation" aria-label="Đơn hàng của tôi">
              <NavLink to="/reservations" end>Đơn đặt bàn</NavLink>
              <NavLink to="/orders" end>Đơn đặt món</NavLink>
            </nav>
            <div className="customer-orders__title-row">
              <div>
                <h1 id="customer-orders-title">{label} của tôi</h1>
                <p>{reservations
                  ? "Theo dõi và quản lý những lịch hẹn tại Sage."
                  : "Xem lại các món ngon bạn đã đặt tại Sage."}</p>
              </div>
              {!empty && <Button onClick={() => navigate(reservations ? "/reservation" : "/order")}>
                {reservations ? "Đặt bàn mới" : "Đặt món mới"}
              </Button>}
            </div>
          </div>
        </section>
        <div className="customer-orders__content customer-orders__container">{children}</div>
      </main>
      <Footer logoSrc={footerLogo} contactIcons={{ address, phone, mail }} restaurantInfo={restaurantInfo}
        mobileSystemLabel={empty ? "Hệ thống đặt bàn nhà hàng" : undefined} />
    </div>
  );
}

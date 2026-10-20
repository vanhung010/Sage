import {
  Link,
  useLocation,
  useNavigate,
} from "react-router-dom";
import { useId, useState } from "react";

import { useAuth } from "../../../auth/AuthContext";

import { Button } from "../../ui/Button";
import { IconButton } from "../../ui/IconButton";
import { UserMenu } from "../../common/UserMenu";

import sageLogo from "../../../assets/logo/sage-logo-light.svg";
import { useCart } from "../../../cart/CartContext";
import "./Header.css";

export type HeaderVariant =
  | "guest"
  | "customer"
  | "auth";

export type HeaderActiveItem =
  | "home"
  | "menu"
  | "reservation"
  | "order"
  | "about";

interface HeaderProps {
  logoSrc?: string;
  variant?: HeaderVariant;
  /** Omit to match the current URL; use null to clear the selection. */
  active?: HeaderActiveItem | null;
  userName?: string;
  className?: string;
  mobileMenuIcon?: string;
}

const navItems = [
  {
    id: "home",
    label: "Trang chủ",
    to: "/",
  },
  {
    id: "menu",
    label: "Thực đơn",
    to: "/menu",
  },
  {
    id: "reservation",
    label: "Đặt bàn",
    to: "/reservation",
  },
  {
    id: "order",
    label: "Đặt món ăn",
    to: "/order",
  },
  {
    id: "about",
    label: "Về Sage",
    to: "/about",
  },
] as const;

export function Header({
  logoSrc,
  variant: requestedVariant = "guest",
  active,
  userName: fallbackUserName = "Khách hàng",
  className = "",
  mobileMenuIcon,
}: HeaderProps) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const navigationId = useId();

  const { totalQuantity } = useCart();

  const navigate = useNavigate();
  const { pathname, hash } = useLocation();
  const { user, logout } = useAuth();

  const activeItem = active !== undefined
    ? active
    : navItems.find((item) => {
        if (item.id === "about") {
          return pathname === "/about" || (pathname === "/" && hash === "#about");
        }

        if (item.id === "home") {
          return pathname === "/" && hash !== "#about";
        }

        return pathname === item.to || pathname.startsWith(`${item.to}/`);
      })?.id;

  // Trang login/register giữ giao diện auth.
  // Các trang khác hiển thị theo trạng thái đăng nhập thực tế.
  const variant: HeaderVariant =
    requestedVariant === "auth"
      ? "auth"
      : user
        ? "customer"
        : "guest";

  const userName = user?.fullName?.trim() || fallbackUserName;

  const classes = [
    "sage-header",
    mobileMenuOpen ? "sage-header--mobile-open" : "",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  function handleProfile() {
    navigate("/profile");
  }

  function handleReservations() {
    navigate("/reservations");
  }

  function handleOrders() {
    navigate("/orders");
  }

  function handleChangePassword() {
    navigate("/change-password");
  }

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <header className={classes}>
      {/* Logo */}
      <Link
        to="/"
        className="sage-header__logo-link"
        aria-label="Sage - Trang chủ"
      >
        <img
          className="sage-header__logo"
          src={logoSrc ?? sageLogo}
          alt="Sage"
        />
      </Link>

      {/* Main Navigation */}
      {variant !== "auth" && (
        <nav
          id={navigationId}
          onClick={() => setMobileMenuOpen(false)}
          className="sage-header__navigation"
          aria-label="Điều hướng chính"
        >
          {navItems.map((item) => (
            <Link
              key={item.to}
              to={item.to}
              aria-current={activeItem === item.id ? "page" : undefined}
              aria-label={
                item.id === "order" && totalQuantity > 0
                  ? `${item.label}, giỏ hàng có ${totalQuantity} món`
                  : undefined
              }
              className={
                [
                  "sage-header__nav-item",
                  activeItem === item.id
                    ? "sage-header__nav-item--active"
                    : "",
                ]
                  .filter(Boolean)
                  .join(" ")
              }
            >
              {item.label}
              {item.id === "order" && totalQuantity > 0 && (
                <span className="sage-header__cart-count" aria-hidden="true">
                  {totalQuantity > 99 ? "99+" : totalQuantity}
                </span>
              )}
            </Link>
          ))}
        </nav>
      )}

      {/* Guest */}
      {variant === "guest" && (
        <div className="sage-header__actions">
          <Button
            type="button"
            variant="outline"
            size="md"
            className="sage-header__login-button"
            onClick={() => {
              navigate("/login");
            }}
          >
            Đăng nhập
          </Button>

          <Button
            type="button"
            variant="primary"
            size="md"
            onClick={() => {
              navigate("/register");
            }}
          >
            Đăng ký
          </Button>
        </div>
      )}

      {/* Customer */}
      {variant === "customer" && (
        <div className="sage-header__customer-actions">
          <UserMenu
            userName={userName}
            onProfile={handleProfile}
            onReservations={handleReservations}
            onOrders={handleOrders}
            onChangePassword={handleChangePassword}
            onLogout={handleLogout}
          />
        </div>
      )}

      {/* Auth */}
      {mobileMenuIcon && variant !== "auth" && (
        <IconButton type="button" variant="ghost" size="md"
          className="sage-header__mobile-toggle"
          icon={<img src={mobileMenuIcon} alt="" />}
          aria-label={mobileMenuOpen ? "Đóng menu điều hướng" : "Mở menu điều hướng"}
          aria-expanded={mobileMenuOpen} aria-controls={navigationId}
          onClick={() => setMobileMenuOpen((value) => !value)} />
      )}
      {variant === "auth" && (
        <Link
          to="/"
          className="sage-header__back-home"
        >
          Về trang chủ
        </Link>
      )}
    </header>
  );
}

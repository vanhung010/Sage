import { Button } from "../../ui/Button";
import { UserMenu } from "../../common/UserMenu";

import sageLogo from "../../../assets/logo/sage-logo-light.svg";

import "./Header.css";

export type HeaderVariant =
  | "guest"
  | "customer"
  | "auth";

interface HeaderProps {
  variant?: HeaderVariant;

  userName?: string;

  onLogin?: () => void;
  onRegister?: () => void;

  onProfile?: () => void;
  onReservations?: () => void;
  onOrders?: () => void;
  onChangePassword?: () => void;
  onLogout?: () => void;

  className?: string;
}

const navItems = [
  {
    label: "Trang chủ",
    href: "/",
  },
  {
    label: "Thực đơn",
    href: "/menu",
  },
  {
    label: "Đặt bàn",
    href: "/reservation",
  },
  {
    label: "Đặt món ăn",
    href: "/order",
  },
  {
    label: "Về Sage",
    href: "/#about",
  },
];

export function Header({
  variant = "guest",

  userName = "Khách hàng",

  onLogin,
  onRegister,

  onProfile,
  onReservations,
  onOrders,
  onChangePassword,
  onLogout,

  className = "",
}: HeaderProps) {
  const classes = [
    "sage-header",
    className,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <header className={classes}>
      <a
        href="/"
        className="sage-header__logo-link"
        aria-label="Sage - Trang chủ"
      >
        <img
          className="sage-header__logo"
          src={sageLogo}
          alt="Sage"
        />
      </a>

      {variant !== "auth" && (
        <nav
          className="sage-header__navigation"
          aria-label="Điều hướng chính"
        >
          {navItems.map((item) => (
            <a
              key={item.href}
              href={item.href}
              className="sage-header__nav-item"
            >
              {item.label}
            </a>
          ))}
        </nav>
      )}

      {variant === "guest" && (
        <div className="sage-header__actions">
          <Button
            variant="outline"
            size="md"
            onClick={onLogin}
            className="sage-header__login-button"
          >
            Đăng nhập
          </Button>

          <Button
            variant="primary"
            size="md"
            onClick={onRegister}
          >
            Đăng kí
          </Button>
        </div>
      )}

      {variant === "customer" && (
        <div className="sage-header__customer-actions">
          <UserMenu
            userName={userName}
            onProfile={onProfile}
            onReservations={onReservations}
            onOrders={onOrders}
            onChangePassword={onChangePassword}
            onLogout={onLogout}
          />
        </div>
      )}

      {variant === "auth" && (
        <a
          href="/"
          className="sage-header__back-home"
        >
          Về trang chủ
        </a>
      )}
    </header>
  );
}
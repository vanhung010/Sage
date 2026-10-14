import {
  Link,
  NavLink,
  useNavigate,
} from "react-router-dom";

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
  className?: string;
}

const navItems = [
  {
    label: "Trang chủ",
    to: "/",
  },
  {
    label: "Thực đơn",
    to: "/menu",
  },
  {
    label: "Đặt bàn",
    to: "/reservation",
  },
  {
    label: "Đặt món ăn",
    to: "/order",
  },
  {
    label: "Về Sage",
    to: "/#about",
  },
];

export function Header({
  variant = "guest",
  userName = "Khách hàng",
  className = "",
}: HeaderProps) {
  const navigate = useNavigate();

  const classes = [
    "sage-header",
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
    /*
      Tạm thời chỉ chuyển về login.

      Sau này khi có AuthContext:
      logout();
      navigate("/login");
    */

    navigate("/login");
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
          src={sageLogo}
          alt="Sage"
        />
      </Link>

      {/* Main Navigation */}
      {variant !== "auth" && (
        <nav
          className="sage-header__navigation"
          aria-label="Điều hướng chính"
        >
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/"}
              className={({ isActive }) =>
                [
                  "sage-header__nav-item",
                  isActive
                    ? "sage-header__nav-item--active"
                    : "",
                ]
                  .filter(Boolean)
                  .join(" ")
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      )}

      {/* Guest */}
      {variant === "guest" && (
        <div className="sage-header__actions">
          <Button
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
            variant="primary"
            size="md"
            onClick={() => {
              navigate("/register");
            }}
          >
            Đăng kí
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
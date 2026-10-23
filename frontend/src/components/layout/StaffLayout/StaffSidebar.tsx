import { Link, NavLink } from "react-router-dom";
import {
  CalendarDays,
  ShoppingCart,
  House,
  LogOut,
} from "lucide-react";

import logo from "../../../assets/logo/sage-logo-light.svg";

import "./StaffSidebar.css";

interface StaffSidebarProps {
  onLogout: () => void;
  ordersEnabled?: boolean;
}

export function StaffSidebar({
  onLogout,
  ordersEnabled = false,
}: StaffSidebarProps) {
  const navigationItems = [
    {
      to: "/staff/reservations",
      label: "Đặt bàn",
      icon: CalendarDays,
      enabled: true,
    },
    {
      to: "/staff/orders",
      label: "Đơn đặt món",
      icon: ShoppingCart,
      enabled: ordersEnabled,
    },
  ];

  return (
    <aside className="staff-sidebar" aria-label="Thanh bên nhân viên">
      <Link
        to="/staff"
        className="staff-sidebar__brand"
        aria-label="Sage - Khu vực nhân viên"
      >
        <img
          className="staff-sidebar__logo"
          src={logo}
          width={96}
          height={48}
          alt="Sage"
        />

        <span className="staff-sidebar__caption">
          KHÔNG GIAN QUẢN LÝ
        </span>
      </Link>

      <nav
        className="staff-sidebar__navigation"
        aria-label="Điều hướng nhân viên"
      >
        <p className="staff-sidebar__section-label">VẬN HÀNH</p>

        <ul className="staff-sidebar__list">
          {navigationItems.map((item) => {
            const Icon = item.icon;

            const content = (
              <>
                <Icon
                  size={24}
                  strokeWidth={1.75}
                  aria-hidden="true"
                />

                <span className="staff-sidebar__item-label">
                  {item.label}
                </span>
              </>
            );

            return (
              <li key={item.to}>
                {item.enabled ? (
                  <NavLink
                    to={item.to}
                    aria-label={item.label}
                    title={item.label}
                    className={({ isActive }) =>
                      [
                        "staff-sidebar__item",
                        isActive ? "staff-sidebar__item--active" : "",
                      ]
                        .filter(Boolean)
                        .join(" ")
                    }
                  >
                    {content}
                  </NavLink>
                ) : (
                  <span
                    className="staff-sidebar__item staff-sidebar__item--disabled"
                    aria-disabled="true"
                    aria-label={`${item.label} - Chưa khả dụng`}
                    title={`${item.label} - Chưa khả dụng`}
                  >
                    {content}
                  </span>
                )}
              </li>
            );
          })}
        </ul>
      </nav>

      <div className="staff-sidebar__footer">
        <Link
          to="/"
          className="staff-sidebar__item"
          aria-label="Trang khách hàng"
          title="Trang khách hàng"
        >
          <House size={24} strokeWidth={1.75} aria-hidden="true" />

          <span className="staff-sidebar__item-label">
            Trang khách hàng
          </span>
        </Link>

        <button
          type="button"
          className="staff-sidebar__item"
          onClick={onLogout}
          aria-label="Đăng xuất"
          title="Đăng xuất"
        >
          <LogOut size={24} strokeWidth={1.75} aria-hidden="true" />

          <span className="staff-sidebar__item-label">
            Đăng xuất
          </span>
        </button>
      </div>
    </aside>
  );
}
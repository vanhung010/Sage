import { Link, NavLink } from "react-router-dom";
import {
  CalendarDays,
  House,
  LayoutDashboard,
  LogOut,
  Settings,
  ShoppingCart,
  Store,
  Table2,
  Users,
  UtensilsCrossed,
  type LucideIcon,
} from "lucide-react";

import { getRoleHome } from "../../../auth/redirects";
import type { AccountRole } from "../../../types/auth";

import logo from "../../../assets/logo/sage-logo-light.svg";

import "./StaffSidebar.css";

interface StaffSidebarProps {
  role: AccountRole;
  onLogout: () => void;
  ordersEnabled?: boolean;
}

interface SidebarItem {
  to: string;
  label: string;
  icon: LucideIcon;
  enabled: boolean;
}

interface SidebarGroup {
  id: string;
  label?: string;
  items: SidebarItem[];
}

export function StaffSidebar({
  role,
  onLogout,
  ordersEnabled = false,
}: StaffSidebarProps) {
  // Sidebar quản lý chỉ dành cho hai vai trò này.
  if (role !== "STAFF" && role !== "ADMIN") {
    return null;
  }

  const isAdmin = role === "ADMIN";

  const roleLabel = isAdmin
    ? "quản trị viên"
    : "nhân viên";

  // Dùng trang chủ theo vai trò đã được dự án cấu hình.
  const homeTo = getRoleHome(role);

  // STAFF và ADMIN cùng sử dụng các màn vận hành hiện có.
  const operationsGroup: SidebarGroup = {
    id: "operations",
    label: "VẬN HÀNH",
    items: [
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
    ],
  };

  const navigationGroups: SidebarGroup[] = isAdmin
    ? [
        // Thống kê nằm đầu sidebar Admin, trước nhóm Vận hành.
        {
          id: "overview",
          items: [
            {
              to: "/admin/statistics",
              label: "Thống kê",
              icon: LayoutDashboard,
              enabled: false,
            },
          ],
        },

        operationsGroup,

        {
          id: "administration",
          label: "QUẢN TRỊ",
          items: [
            {
              to: "/admin/menu",
              label: "Thực đơn",
              icon: UtensilsCrossed,
              enabled: false,
            },
            {
              to: "/admin/tables",
              label: "Bàn và khu vực",
              icon: Table2,
              enabled: false,
            },
            {
              to: "/admin/accounts",
              label: "Tài khoản",
              icon: Users,
              enabled: false,
            },
            {
              to: "/admin/restaurant",
              label: "Nhà hàng",
              icon: Store,
              enabled: false,
            },
            {
              to: "/admin/settings",
              label: "Cấu hình",
              icon: Settings,
              enabled: false,
            },
          ],
        },
      ]
    : [operationsGroup];

  return (
    <aside
      className="staff-sidebar"
      aria-label={`Thanh bên ${roleLabel}`}
    >
      <Link
        to={homeTo}
        className="staff-sidebar__brand"
        aria-label={`Sage - Khu vực ${roleLabel}`}
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
        aria-label={`Điều hướng ${roleLabel}`}
      >
        {navigationGroups.map((group) => (
          <div
            key={group.id}
            className={[
              "staff-sidebar__group",
              !group.label
                ? "staff-sidebar__group--overview"
                : "",
            ]
              .filter(Boolean)
              .join(" ")}
          >
            {group.label && (
              <p className="staff-sidebar__section-label">
                {group.label}
              </p>
            )}

            <ul className="staff-sidebar__list">
              {group.items.map((item) => {
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
                            isActive
                              ? "staff-sidebar__item--active"
                              : "",
                          ]
                            .filter(Boolean)
                            .join(" ")
                        }
                      >
                        {content}
                      </NavLink>
                    ) : (
                      <span
                        className={
                          "staff-sidebar__item " +
                          "staff-sidebar__item--disabled"
                        }
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
          </div>
        ))}
      </nav>

      <div className="staff-sidebar__footer">
        <Link
          to="/"
          className="staff-sidebar__item"
          aria-label="Trang khách hàng"
          title="Trang khách hàng"
        >
          <House
            size={24}
            strokeWidth={1.75}
            aria-hidden="true"
          />

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
          <LogOut
            size={24}
            strokeWidth={1.75}
            aria-hidden="true"
          />

          <span className="staff-sidebar__item-label">
            Đăng xuất
          </span>
        </button>
      </div>
    </aside>
  );
}
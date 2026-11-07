import { Link, NavLink } from "react-router-dom";
import { House, LogOut } from "lucide-react";

import { getRoleHome } from "../../../auth/redirects";
import logo from "../../../assets/logo/sage-logo-light.svg";

import {
  getManagementNavigation,
  MANAGEMENT_ROLE_LABELS,
  type ManagementRole,
} from "./managementNavigation";

import "./StaffSidebar.css";

interface StaffSidebarProps {
  role: ManagementRole;
  onLogout: () => void;
  ordersEnabled?: boolean;
}

export function StaffSidebar({
  role,
  onLogout,
  ordersEnabled = false,
}: StaffSidebarProps) {
  const roleLabel = MANAGEMENT_ROLE_LABELS[role];
  const groups = getManagementNavigation(role, ordersEnabled);

  return (
    <aside
      className="staff-sidebar"
      aria-label={`Thanh bên ${roleLabel}`}
    >
      <Link
        to={getRoleHome(role)}
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
        {groups.map((group) => (
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
                // Icon là component lấy từ lucide-react.
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
                  <li key={item.id}>
                    {item.enabled ? (
                      <NavLink
                        to={item.to}
                        end={item.end ?? false}
                        title={item.label}
                        aria-label={item.label}
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
                      // Không dùng Link cho mục chưa có route,
                      // tránh đưa người dùng đến trang không tồn tại.
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
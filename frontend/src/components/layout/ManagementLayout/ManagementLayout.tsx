import { useId } from "react";
import {
  Outlet,
  useMatches,
  useNavigate,
} from "react-router-dom";

import { useAuth } from "../../../auth/AuthContext";
import type { BreadcrumbEntry } from "../../ui/NavigationBreadcrumb";

import { StaffHeader } from "./ManagementHeader";
import { StaffSidebar } from "./ManagementSidebar";
import { MANAGEMENT_ROLE_LABELS } from "./managementNavigation";

import "./StaffLayout.css";

export interface StaffRouteHandle {
  breadcrumb?: string;
}

interface StaffLayoutProps {
  // Giữ prop hiện có để tương thích với staff.routes.tsx.
  ordersEnabled?: boolean;
}

// Route handle có kiểu unknown, vì vậy kiểm tra trước khi đọc.
// Hàm này chỉ lấy metadata hiển thị, không xử lý phân quyền.
function readBreadcrumb(handle: unknown): string | undefined {
  if (
    typeof handle !== "object" ||
    handle === null ||
    !("breadcrumb" in handle)
  ) {
    return undefined;
  }

  return typeof handle.breadcrumb === "string"
    ? handle.breadcrumb
    : undefined;
}

export function StaffLayout({
  ordersEnabled = false,
}: StaffLayoutProps) {
  const { user, logout } = useAuth();
  const matches = useMatches();
  const navigate = useNavigate();
  const contentId = useId();

  // RoleRoute vẫn chịu trách nhiệm bảo vệ route.
  // Kiểm tra này giúp layout chỉ render với vai trò phù hợp.
  if (
    !user ||
    (user.role !== "STAFF" && user.role !== "ADMIN")
  ) {
    return null;
  }

  const role = user.role;
  const userName =
    user.fullName.trim() || MANAGEMENT_ROLE_LABELS[role];

  // Admin có thể mở màn vận hành trong /staff hoặc màn quản trị
  // trong /admin. Breadcrumb phải theo nhóm route đang mở.
  const rootPath = matches.some(
    (match) => match.pathname === "/admin",
  )
    ? "/admin"
    : "/staff";

  const pageBreadcrumbs: BreadcrumbEntry[] = matches.flatMap(
    (match) => {
      const label = readBreadcrumb(match.handle);

      return label
        ? [{ label, to: match.pathname }]
        : [];
    },
  );

  const breadcrumbs: BreadcrumbEntry[] = [
    { label: "Quản lý", to: rootPath },
    ...pageBreadcrumbs,
  ];

  function handleLogout() {
    logout();
    void navigate("/login", { replace: true });
  }

  return (
    <div className="staff-layout">
      {/* Cho phép người dùng bàn phím bỏ qua sidebar và header. */}
      <a
        href={`#${contentId}`}
        className="staff-layout__skip-link"
      >
        Chuyển đến nội dung chính
      </a>

      <StaffSidebar
        role={role}
        ordersEnabled={ordersEnabled}
        onLogout={handleLogout}
      />

      <div className="staff-layout__workspace">
        <StaffHeader
          breadcrumbs={breadcrumbs}
          userName={userName}
          role={role}
          onLogout={handleLogout}
        />

        <main
          id={contentId}
          className="staff-layout__content"
          tabIndex={-1}
        >
          {/*
            Mỗi page tự render tiêu đề và nội dung của mình.
            Outlet là vị trí gắn màn hình tương ứng với route.
          */}
          <Outlet />
        </main>
      </div>
    </div>
  );
}
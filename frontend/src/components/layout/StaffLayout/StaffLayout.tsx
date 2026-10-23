import { useId } from "react";
import {
  Outlet,
  useMatches,
  useNavigate,
} from "react-router-dom";

import { useAuth } from "../../../auth/AuthContext";
import type { BreadcrumbEntry } from "../../ui/NavigationBreadcrumb";

import { StaffSidebar } from "./StaffSidebar";
import { StaffHeader } from "./StaffHeader";

import "./StaffLayout.css";

export interface StaffRouteHandle {
  breadcrumb?: string;
}

interface StaffLayoutProps {
  ordersEnabled?: boolean;
}

export function StaffLayout({
  ordersEnabled = false,
}: StaffLayoutProps) {
  const { user, logout } = useAuth();

  const navigate = useNavigate();
  const matches = useMatches();
  const contentId = useId();

  const pageBreadcrumbs: BreadcrumbEntry[] = matches.flatMap(
    (match) => {
      const handle = match.handle as StaffRouteHandle | undefined;

      if (!handle?.breadcrumb) return [];

      return [
        {
          label: handle.breadcrumb,
          to: match.pathname,
        },
      ];
    },
  );

  const breadcrumbs: BreadcrumbEntry[] = [
    {
      label: "Quản lý",
      to: "/staff",
    },
    ...pageBreadcrumbs,
  ];

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  // RoleRoute chịu trách nhiệm bảo vệ nhóm route này.
  if (!user) return null;

  return (
    <div className="staff-layout">
      <a
        href={`#${contentId}`}
        className="staff-layout__skip-link"
      >
        Chuyển đến nội dung chính
      </a>

      <StaffSidebar
        onLogout={handleLogout}
        ordersEnabled={ordersEnabled}
      />

      <div className="staff-layout__workspace">
        <StaffHeader
          breadcrumbs={breadcrumbs}
          userName={user.fullName.trim() || "Nhân viên"}
          role={user.role}
          onLogout={handleLogout}
        />

        <main
          id={contentId}
          className="staff-layout__content"
          tabIndex={-1}
        >
          <Outlet />
        </main>
      </div>
    </div>
  );
}
import { Badge } from "../../ui/Badge";
import {
  NavigationBreadcrumb,
  type BreadcrumbEntry,
} from "../../ui/NavigationBreadcrumb";

import { StaffUserMenu } from "./ManagementUserMenu";

import {
  MANAGEMENT_ROLE_LABELS,
  type ManagementRole,
} from "./managementNavigation";

import "./StaffHeader.css";

interface StaffHeaderProps {
  breadcrumbs: BreadcrumbEntry[];
  userName: string;
  role: ManagementRole;
  onLogout: () => void;
}

export function StaffHeader({
  breadcrumbs,
  userName,
  role,
  onLogout,
}: StaffHeaderProps) {
  return (
    <header className="staff-header">
      <div className="staff-header__breadcrumb">
        <NavigationBreadcrumb items={breadcrumbs} />
      </div>

      <div className="staff-header__account">
        {/* Nhãn hiển thị theo tài khoản đang đăng nhập. */}
        <Badge className="staff-header__role">
          {MANAGEMENT_ROLE_LABELS[role]}
        </Badge>

        {/* Tái sử dụng xử lý mở/đóng, Escape và click bên ngoài. */}
        <StaffUserMenu
          userName={userName}
          onLogout={onLogout}
        />
      </div>
    </header>
  );
}
import type { AccountRole } from "../../../types/auth";

import { Badge } from "../../ui/Badge";
import {
  NavigationBreadcrumb,
  type BreadcrumbEntry,
} from "../../ui/NavigationBreadcrumb";

import { StaffUserMenu } from "./StaffUserMenu";

import "./StaffHeader.css";

interface StaffHeaderProps {
  breadcrumbs: BreadcrumbEntry[];
  userName: string;
  role: AccountRole;
  onLogout: () => void;
}

const roleLabels: Record<AccountRole, string> = {
  CUSTOMER: "Khách hàng",
  STAFF: "Nhân viên",
  ADMIN: "Quản trị viên",
};

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
        <Badge className="staff-header__role">
          {roleLabels[role]}
        </Badge>

        <StaffUserMenu
          userName={userName}
          onLogout={onLogout}
        />
      </div>
    </header>
  );
}
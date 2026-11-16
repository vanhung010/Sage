import {
  CalendarDays,
  ChartNoAxesCombined,
  Settings,
  ShoppingCart,
  Store,
  Table2,
  Users,
  UtensilsCrossed,
  type LucideIcon,
} from "lucide-react";

import type { AccountRole } from "../../../types/auth";

// Layout quản lý chỉ hỗ trợ STAFF và ADMIN.
// Dùng Extract để tận dụng kiểu vai trò đã có trong dự án.
export type ManagementRole = Extract<
  AccountRole,
  "STAFF" | "ADMIN"
>;

export interface ManagementNavigationItem {
  id: string;
  label: string;
  to: string;
  icon: LucideIcon;

  // false: hiển thị mục menu nhưng chưa cho điều hướng.
  // Chỉ bật khi route và màn hình tương ứng đã được triển khai.
  enabled: boolean;

  // true: chỉ active khi URL khớp hoàn toàn.
  // Mặc định false để các trang chi tiết vẫn active mục cha.
  end?: boolean;
}

export interface ManagementNavigationGroup {
  id: string;
  label?: string;
  items: ManagementNavigationItem[];
}

export const MANAGEMENT_ROLE_LABELS: Record<
  ManagementRole,
  string
> = {
  STAFF: "Nhân viên",
  ADMIN: "Quản trị viên",
};

const adminOverview: ManagementNavigationGroup = {
  id: "overview",
  items: [
    {
      id: "statistics",
      label: "Thống kê",
      to: "/admin/statistics",
      icon: ChartNoAxesCombined,
      enabled: false,
    },
  ],
};

const administration: ManagementNavigationGroup = {
  id: "administration",
  label: "QUẢN TRỊ",
  items: [
    {
      id: "menu",
      label: "Thực đơn",
      to: "/admin/menu",
      icon: UtensilsCrossed,
      enabled: true,
    },
    {
      id: "tables",
      label: "Bàn và khu vực",
      to: "/admin/tables",
      icon: Table2,
      enabled: false,
    },
    {
      id: "accounts",
      label: "Tài khoản",
      to: "/admin/accounts",
      icon: Users,
      enabled: false,
    },
    {
      id: "restaurant",
      label: "Nhà hàng",
      to: "/admin/restaurant",
      icon: Store,
      enabled: false,
    },
    {
      id: "settings",
      label: "Cấu hình",
      to: "/admin/settings",
      icon: Settings,
      enabled: false,
    },
  ],
};

export function getManagementNavigation(
  role: ManagementRole,
  ordersEnabled: boolean,
): ManagementNavigationGroup[] {
  // Các màn vận hành hiện nằm trong /staff.
  // Admin sử dụng chung các route này thay vì tạo page trùng.
  const operations: ManagementNavigationGroup = {
    id: "operations",
    label: "VẬN HÀNH",
    items: [
      {
        id: "reservations",
        label: "Đặt bàn",
        to: "/staff/reservations",
        icon: CalendarDays,
        enabled: true,
      },
      {
        id: "orders",
        label: "Đơn đặt món",
        to: "/staff/orders",
        icon: ShoppingCart,
        enabled: ordersEnabled,
      },
    ],
  };

  // Thứ tự các nhóm tương ứng với hai variant trong Figma.
  return role === "ADMIN"
    ? [adminOverview, operations, administration]
    : [operations];
}
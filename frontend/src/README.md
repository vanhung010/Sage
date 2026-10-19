# Cấu trúc frontend Sage

```text
src/
├── pages/
│   ├── public/       # Home, Menu, DishDetail, Reservation, ReservationSuccess
│   ├── auth/         # Login, Register; sau này thêm ForgotPassword, OTP...
│   ├── customer/     # Order, OrderDetail, ReservationDetail, ReservationReschedule
│   ├── admin/        # Màn hình quản trị, URL /admin/...
│   └── staff/        # Màn hình nhân viên, URL /staff/...
├── router/
│   ├── AppRouter.tsx # Khởi tạo BrowserRouter một lần
│   ├── guards/      # RoleRoute dùng chung; CustomerRoute giữ UI từ chối hiện tại
│   └── routes/      # Mỗi nhóm có file *.routes.tsx; index.tsx ghép các nhóm
├── components/
│   ├── ui/          # Button, Input, Badge, Toast...
│   ├── common/      # DishCard, AppFeedbackModal...
│   └── layout/      # Header, Footer...; thêm layout cho admin/staff khi có thiết kế
├── auth/            # Phiên đăng nhập dùng chung cho mọi role
├── cart/            # Trạng thái giỏ hàng
├── api/             # API client và các module theo nghiệp vụ
├── hooks/
├── types/
├── storage/
├── utils/
├── assets/
└── styles/          # Token và CSS toàn cục
```

## Quy ước

- Thư mục màn hình chứa page, CSS và các component chỉ dùng trong màn hình đó.
- Component dùng lại giữa các màn hình đặt trong `components`. Không import một
  page của role khác để dùng làm component chung.
- `public` là các trang truy cập công khai; khách đã đăng nhập vẫn xem được.
  `auth` là các màn hình xác thực dùng chung cho mọi role.
- Các URL hiện có giữ nguyên, ví dụ `/menu`, `/order`, `/orders/:id`.
  Chuyển thư mục không đồng nghĩa thêm `/customer` vào URL.
- API, types, token CSS và assets dùng chung không sao chép theo role.
- `adminRoutes` và `staffRoutes` đang rỗng vì chưa có màn hình tương ứng.
  Không có trang trắng hoặc route giả được đăng ký cho hai nhóm này.

## Thêm màn hình Admin

1. Tạo `pages/admin/Dashboard/DashboardPage.tsx`.
2. Thay nội dung `router/routes/admin.routes.tsx` bằng:

```tsx
import type { RouteObject } from "react-router-dom";
import { RoleRoute } from "../guards/RoleRoute";
import { DashboardPage } from "../../pages/admin/Dashboard/DashboardPage";

export const adminRoutes: RouteObject[] = [
  {
    path: "/admin",
    element: <RoleRoute allowedRoles={["ADMIN"]} />,
    children: [
      { index: true, element: <DashboardPage /> },
    ],
  },
];
```

`routes/index.tsx` đã ghép `adminRoutes`, không cần sửa `AppRouter`.
Thêm Staff tương tự với `staff.routes.tsx`, `/staff` và role `STAFF`.
Không mặc định cho Admin truy cập mọi màn hình Staff; khi nghiệp vụ cho phép,
khai báo rõ `allowedRoles={["STAFF", "ADMIN"]}`.

Khi có layout riêng, đặt component trong `components/layout/AdminLayout/`
hoặc `StaffLayout/` và cho layout render `<Outlet />`. Đặt layout làm route con
bên trong guard, rồi đặt các page làm con của layout.

## Hành vi guard

- Chưa đăng nhập: chuyển `/login`, giữ `state.returnTo` như trước.
- Đúng role: render route con qua `<Outlet />`.
- Sai role: render `fallback` nếu có, mặc định chuyển về `/`.
- `CustomerRoute` giữ nguyên thông báo từ chối trước khi refactor.
- Guard điều khiển giao diện; backend vẫn kiểm tra quyền của từng API.

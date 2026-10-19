# Màn hình Admin

Đặt mỗi màn hình trong một thư mục con, ví dụ `Dashboard/DashboardPage.tsx`
và `Dashboard/DashboardPage.css`. Component chỉ dùng trong màn hình đặt cạnh page.

Đăng ký URL dưới `/admin` tại `src/router/routes/admin.routes.tsx`, bọc nhóm
bằng `RoleRoute` với `allowedRoles={["ADMIN"]}`. Ví dụ đầy đủ: `src/README.md`.

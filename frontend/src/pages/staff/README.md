# Màn hình Staff

Đặt mỗi màn hình trong một thư mục con, ví dụ `Orders/OrdersPage.tsx`
và `Orders/OrdersPage.css`. Component chỉ dùng trong màn hình đặt cạnh page.

Đăng ký URL dưới `/staff` tại `src/router/routes/staff.routes.tsx`, bọc nhóm
bằng `RoleRoute` với `allowedRoles={["STAFF"]}`. Ví dụ đầy đủ: `src/README.md`.

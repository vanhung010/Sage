# Saga Res: Tóm tắt dự án

*Hệ thống đặt bàn, xem menu và đặt món giao hàng*

| | |
|---|---|
| **Tên dự án** | Saga Res (Restaurant Reservation System) |
| **Nhà hàng demo** | Sage |
| **Tác giả** | Hùng |
| **Hình thức** | Dự án cá nhân, phục vụ portfolio |
| **Trạng thái** | Đã chốt logic đặt bàn, luồng đặt món giao hàng mở rộng và thiết kế xác thực JWT cho MVP |
| **Phiên bản tài liệu** | 1.4 |

---

## 1. Mục tiêu

**Saga Res** là ứng dụng web cho phép người dùng **xem menu**, **đặt bàn trực tuyến** và thực hiện **đặt món giao hàng ở mức cơ bản**, đồng thời cung cấp trang quản trị để nhà hàng quản lý menu, bàn và các đơn đặt bàn. Bản demo dùng nhà hàng mẫu **Sage** để minh họa hệ thống hoạt động như một sản phẩm thật.

**Mục tiêu học tập và portfolio**
- Xây dựng một hệ thống hoàn chỉnh từ thiết kế đến triển khai, có bản demo chạy thật.
- Giải quyết bài toán kỹ thuật trọng tâm: **đặt bàn theo khung giờ và chống đặt trùng khi có nhiều yêu cầu đồng thời**.
- Có tài liệu thiết kế bài bản (yêu cầu, ERD, API) để trình bày với nhà tuyển dụng.

## 2. Bài toán

Nhiều nhà hàng vẫn nhận đặt bàn qua điện thoại hoặc tin nhắn, dẫn đến khó theo dõi bàn trống, dễ nhầm lịch, khách khó xem menu và giá trước khi đến. Hệ thống giải quyết bằng cách:
- Cho khách tự kiểm tra bàn trống và đặt bàn bất kể giờ nào.
- Tự động kiểm tra sức chứa, khung giờ và ngăn đặt trùng.
- Cho người dùng chọn món, nhập thông tin nhận hàng và gửi một đơn món giao hàng cơ bản.
- Cho nhà hàng một nơi tập trung để quản lý lịch đặt bàn.

## 3. Đối tượng sử dụng

| Vai trò | Mô tả |
|---|---|
| **Khách vãng lai** | Xem menu, thông tin nhà hàng, kiểm tra bàn trống; phải đăng nhập bằng tài khoản CUSTOMER trước khi gửi đơn món giao hàng |
| **Khách hàng** | Đăng nhập, đặt bàn, xem lịch sử, hủy hoặc đổi lịch, đánh giá; đặt món giao hàng COD, xem lịch sử đơn giao hàng và hủy đơn của mình khi còn `PLACED` |
| **Nhân viên (STAFF)** | Quản lý đơn đặt bàn; xem danh sách và chi tiết đơn món giao hàng ở chế độ chỉ đọc |
| **Quản trị (ADMIN)** | Quản lý menu, bàn, cấu hình, tài khoản, thống kê; xem danh sách và chi tiết đơn món giao hàng ở chế độ chỉ đọc |

## 4. Phạm vi

### Sẽ làm (MVP)
- Menu theo danh mục, ảnh, giá, tìm kiếm và lọc món.
- Đăng ký, đăng nhập và phân quyền theo vai trò bằng Spring Security + JWT access token stateless (TTL 1 giờ, không refresh token ở MVP).
- Đặt bàn theo ngày, giờ, số người, ghi chú.
- Xem lịch sử, hủy hoặc đổi lịch đặt bàn.
- Trang admin: quản lý menu, bàn, đơn đặt bàn (xác nhận, đánh dấu khách đến hoặc không đến).
- Đặt món giao hàng: CUSTOMER đăng nhập chọn món và số lượng, nhập địa chỉ + số điện thoại nhận hàng, xác nhận thanh toán COD, gửi đơn với trạng thái `PLACED`; `food_order_item` snapshot `unit_price` tại thời điểm đặt. CUSTOMER xem lịch sử/chi tiết đơn của mình và được hủy đơn khi còn `PLACED`, chuyển sang `CANCELLED`.
- STAFF/ADMIN xem danh sách và chi tiết đơn món giao hàng; chưa có thao tác cập nhật trạng thái.

### Nên có (tạo điểm nhấn)
- Tự động gán bàn phù hợp với số người.
- Chống đặt trùng bàn ở cả tầng ứng dụng và tầng database, kèm test đồng thời.
- Email xác nhận và nhắc lịch.
- Lịch đặt bàn dạng timeline cho nhân viên.
- Lưới giờ bắt đầu còn trống theo thời lượng khách chọn, gợi ý 3 khoảng thời gian gần nhất khi hết chỗ.
- Nhân viên gia hạn hoặc rút ngắn thời lượng khi khách ngồi lâu hơn dự kiến.

### Làm thêm nếu còn thời gian
- Sơ đồ bàn trực quan, đánh giá, thống kê bằng biểu đồ, đăng nhập Google.

### Chưa đưa vào luồng đặt món cơ bản
- Thanh toán trực tuyến, phí giao hàng, vùng giao hàng, theo dõi shipper, khuyến mãi, tồn kho, sửa nội dung đơn sau khi gửi và workflow cập nhật trạng thái đơn từ STAFF/ADMIN. Các nội dung này để sau.
- Giá món được snapshot vào `food_order_item.unit_price` khi tạo đơn, nên lịch sử đơn giữ được giá bán tại thời điểm đặt; chưa có payment transaction/đối soát COD riêng.
- **Đặt món trước gắn với một đơn đặt bàn / phục vụ tại bàn** vẫn chưa nằm trong phạm vi hiện tại; tính năng mới ở đây là **đặt món để giao hàng**.

### Không làm (ngoài phạm vi hiện tại)
- Thanh toán hoặc đặt cọc trực tuyến cho đặt bàn, ghép bàn, nhiều chi nhánh, ứng dụng mobile, gợi ý món bằng AI, quản lý kho và nhân sự.

## 5. Quy tắc nghiệp vụ chính

> Các giá trị dưới đây là **đề xuất ban đầu**, sẽ chốt lại khi viết đặc tả yêu cầu.

| Quy tắc | Giá trị đề xuất |
|---|---|
| Giờ mở cửa | Mặc định 10:00 – 22:00, cấu hình theo thứ trong tuần, có ngày đóng cửa đặc biệt |
| Chọn thời gian | Khách chọn giờ bắt đầu và giờ kết thúc bất kỳ (bước 15 phút), giờ kết thúc không muộn hơn giờ đóng cửa |
| Thời lượng một lượt ăn | Do khách hoặc nhân viên chọn (gợi ý 90 phút, khách chọn 60–180 phút), cộng 15 phút đệm dọn bàn |
| Thời gian đặt trước | Tối thiểu 2 giờ (đơn từ 7 người: 4 giờ), tối đa 30 ngày |
| Số người tối đa mỗi đơn | 12 (nhóm đông liên hệ trực tiếp) |
| Hủy hoặc đổi lịch | Được phép trước giờ hẹn 2 giờ, đổi tối đa 2 lần mỗi đơn |
| Khách không đến | Giữ bàn 15 phút sau giờ hẹn, sau đó nhân viên đánh dấu "không đến" thủ công, không phạt |
| Gán bàn | Hệ thống tự gán bàn nhỏ nhất phù hợp (bàn có sức chứa tối thiểu và tối đa), không ghép bàn |
| Xác nhận đơn | Đơn đến 6 người tự động xác nhận, từ 7 người chờ nhân viên duyệt; đơn chờ duyệt tự hủy nếu chưa duyệt trước giờ hẹn 3 giờ |
| Trạng thái đơn | Chờ duyệt → Đã xác nhận → Đã đến → Hoàn thành; hoặc Không đến / Đã hủy / Bị từ chối |
| Đặt món giao hàng | Bắt buộc CUSTOMER đăng nhập; chọn món + số lượng → snapshot `unit_price` → nhập địa chỉ và số điện thoại → COD → tạo đơn `PLACED`; CUSTOMER xem lịch sử/chi tiết đơn và được hủy đơn của mình khi còn `PLACED` (`PLACED → CANCELLED`); STAFF/ADMIN chỉ xem danh sách/chi tiết |

## 6. Công nghệ dự kiến

| Thành phần | Công nghệ |
|---|---|
| Backend | Java, Spring Boot, Spring Data JPA, Spring Security; JWT access token stateless, Bearer token, TTL 1 giờ, không refresh token ở MVP |
| Cơ sở dữ liệu | PostgreSQL (transaction, khóa dòng, exclusion constraint chống trùng giờ) |
| Frontend | React, giao diện responsive dùng được trên điện thoại |
| Email | Spring Mail |
| Tài liệu API | Swagger / OpenAPI |
| Kiểm thử | JUnit, Testcontainers (test đặt trùng đồng thời) |
| Đóng gói và triển khai | Docker, nền tảng hosting miễn phí hoặc giá rẻ (chọn sau) |



---

## 7. Tài liệu bảo mật liên quan

Thiết kế JWT chi tiết của MVP nằm tại `09-authentication-jwt-security-design.md`: access token HS256, TTL 1 giờ, Bearer header, xác thực stateless, không refresh token, kiểm tra trạng thái tài khoản hiện tại trên mỗi request đã xác thực và chuẩn hóa lỗi `401/403`.

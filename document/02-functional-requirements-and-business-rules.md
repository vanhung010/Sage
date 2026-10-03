# Saga Res: Danh sách yêu cầu chức năng và quy tắc nghiệp vụ

| | |
|---|---|
| **Dự án** | Saga Res (nhà hàng demo: Sage) |
| **Tác giả** | Hùng |
| **Phiên bản** | 1.6 (bổ sung thiết kế xác thực JWT access token cho MVP) |
| **Tài liệu liên quan** | `01-project-summary.md`, `09-authentication-jwt-security-design.md` |

## 1. Mục đích và cách dùng tài liệu

Tài liệu liệt kê **yêu cầu chức năng**, **quy tắc nghiệp vụ** và **yêu cầu phi chức năng** của Saga Res. Các quy tắc đặt bàn đã được chốt và là đầu vào cho use case/database. Từ phiên bản 1.5, luồng **đặt món giao hàng** được mở rộng: bắt buộc CUSTOMER đăng nhập, snapshot `unit_price` trên order item, CUSTOMER xem lịch sử/chi tiết đơn của mình và được hủy đơn khi còn `PLACED`; trạng thái tối thiểu gồm `PLACED` và `CANCELLED`; STAFF/ADMIN vẫn chỉ xem đơn; thanh toán COD. Chi tiết cập nhật food order nằm tại `08-food-ordering-customer-history-cancellation-price-snapshot.md`. Thiết kế xác thực JWT chi tiết nằm tại `09-authentication-jwt-security-design.md`.

**Quy ước**
- Mã yêu cầu: `FR-<nhóm>-<số>`; quy tắc nghiệp vụ: `BR-<số>`; phi chức năng: `NFR-<số>`.
- Ưu tiên: **MUST** (bắt buộc cho MVP), **SHOULD** (nên có, tạo điểm nhấn), **COULD** (làm nếu còn thời gian).
- Các giá trị số (2 giờ, 15 phút, 3 đơn, 6 người...) là cấu hình, không viết cứng trong mã.

## 2. Thuật ngữ và vai trò

| Thuật ngữ | Ý nghĩa |
|---|---|
| **Đơn đặt bàn** | Yêu cầu giữ một bàn cho một số người trong một khoảng thời gian cụ thể |
| **Lượt ăn** | Khoảng từ giờ bắt đầu đến giờ kết thúc của một đơn |
| **Khoảng bị chiếm** | Lượt ăn cộng thời gian đệm dọn bàn; trong khoảng này bàn không thể gán cho đơn khác |
| **Giữ chỗ** | Trạng thái đơn khiến bàn bị chiếm (`PENDING`, `CONFIRMED`, `ARRIVED`) |
| **Đặt hộ** | Nhân viên tạo đơn thay khách qua điện thoại hoặc tại quầy |
| **Đơn món giao hàng** | Yêu cầu giao một hoặc nhiều món đã chọn đến thông tin nhận hàng do người đặt cung cấp |
| **Người đặt hàng** | CUSTOMER đã đăng nhập và thực hiện luồng đặt món giao hàng |

| Vai trò | Quyền chính |
|---|---|
| **Khách vãng lai** | Xem thông tin, menu, kiểm tra bàn trống; phải đăng nhập thành CUSTOMER trước khi gửi đơn món giao hàng |
| **Khách hàng (CUSTOMER)** | Đặt bàn, xem, hủy, đổi lịch đơn của mình, đánh giá; đặt món giao hàng COD |
| **Nhân viên (STAFF)** | Quản lý đơn đặt bàn; xem danh sách và chi tiết đơn món giao hàng ở chế độ chỉ đọc |
| **Quản trị (ADMIN)** | Quản lý menu, bàn, cấu hình, tài khoản, thống kê; xem danh sách và chi tiết đơn món giao hàng ở chế độ chỉ đọc |

## 3. Yêu cầu chức năng

### 3.1 Tài khoản và xác thực (AUTH)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-AUTH-01 | Đăng ký tài khoản bằng email, mật khẩu, họ tên, số điện thoại | Khách vãng lai | MUST |
| FR-AUTH-02 | Đăng nhập và đăng xuất | Tất cả | MUST |
| FR-AUTH-03 | Phân quyền truy cập theo ba vai trò CUSTOMER, STAFF, ADMIN | Tất cả | MUST |
| FR-AUTH-04 | Xem và cập nhật hồ sơ cá nhân | Khách hàng | SHOULD |
| FR-AUTH-05 | Đổi mật khẩu | Khách hàng | SHOULD |
| FR-AUTH-06 | Quên mật khẩu, đặt lại qua email | Khách hàng | SHOULD |
| FR-AUTH-07 | Xác thực email khi đăng ký; ở MVP không bắt buộc | Khách hàng | COULD |
| FR-AUTH-08 | Admin xem danh sách, khóa/mở khóa tài khoản, đổi vai trò | Admin | SHOULD |
| FR-AUTH-09 | Đăng nhập bằng Google | Khách hàng | COULD |
| FR-AUTH-10 | Khi đăng nhập thành công, hệ thống cấp JWT access token có thời hạn 1 giờ | Tất cả | MUST |
| FR-AUTH-11 | API cần xác thực chỉ chấp nhận JWT hợp lệ qua `Authorization: Bearer <token>` và trả `401` khi thiếu/token không hợp lệ | Tất cả | MUST |
| FR-AUTH-12 | MVP dùng phiên stateless, không dùng server session và không có refresh token; đăng xuất bằng cách xóa access token phía client | Tất cả | MUST |

### 3.2 Thông tin nhà hàng (INFO)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-INFO-01 | Xem thông tin nhà hàng Sage: giới thiệu, địa chỉ, liên hệ, giờ mở cửa, ảnh không gian | Tất cả | MUST |
| FR-INFO-02 | Admin cập nhật thông tin nhà hàng và giờ mở cửa theo thứ trong tuần | Admin | MUST |
| FR-INFO-03 | Admin khai báo ngày đóng cửa đặc biệt (lễ, sửa chữa) | Admin | MUST |

### 3.3 Menu (MENU)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-MENU-01 | Xem menu theo danh mục | Tất cả | MUST |
| FR-MENU-02 | Xem chi tiết món: ảnh (URL), mô tả, giá, nhãn | Tất cả | MUST |
| FR-MENU-03 | Tìm kiếm món theo tên | Tất cả | MUST |
| FR-MENU-04 | Lọc món theo danh mục, khoảng giá, nhãn | Tất cả | SHOULD |
| FR-MENU-05 | Thêm, sửa, ngừng dùng danh mục | Admin | MUST |
| FR-MENU-06 | Thêm, sửa, ngừng dùng món | Admin | MUST |
| FR-MENU-07 | Đổi trạng thái món: còn / hết / ẩn | Admin | MUST |
| FR-MENU-08 | Quản lý nhãn món và gán nhãn cho món | Admin | MUST |
| FR-MENU-09 | Sắp xếp thứ tự hiển thị danh mục và món | Admin | COULD |
| FR-MENU-10 | Tải ảnh món lên từ máy (thay cho nhập URL) | Admin | COULD |
| FR-MENU-11 | Đánh dấu món yêu thích | Khách hàng | COULD |

### 3.4 Bàn và khu vực (TBL)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-TBL-01 | Thêm, sửa, ngừng dùng khu vực (trong nhà, sân vườn, phòng riêng...) | Admin | MUST |
| FR-TBL-02 | Thêm, sửa, ngừng dùng bàn: mã bàn, khu vực, sức chứa tối thiểu và tối đa | Admin | MUST |
| FR-TBL-03 | Tạm ngưng bàn trong một khoảng thời gian (bảo trì) | Admin | SHOULD |
| FR-TBL-04 | Xem sơ đồ bàn trực quan | Admin, khách | COULD |

### 3.5 Đặt bàn phía khách (RSV)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-RSV-01 | Kiểm tra bàn trống theo ngày, giờ bắt đầu, giờ kết thúc, số người, khu vực (tùy chọn) | Tất cả | MUST |
| FR-RSV-02 | Hiển thị các giờ bắt đầu còn khả dụng trong ngày cho thời lượng khách đã chọn | Tất cả | MUST |
| FR-RSV-03 | Khi hết chỗ, gợi ý tối đa 3 khoảng thời gian gần nhất còn trống, giữ nguyên thời lượng | Tất cả | SHOULD |
| FR-RSV-04 | Tạo đơn đặt bàn: ngày, giờ bắt đầu, giờ kết thúc, số người, khu vực, ghi chú đặc biệt; yêu cầu đăng nhập | Khách hàng | MUST |
| FR-RSV-05 | Hệ thống tự gán bàn phù hợp, khách không chọn bàn | Hệ thống | MUST |
| FR-RSV-06 | Ngăn đặt trùng bàn khi có nhiều yêu cầu đồng thời | Hệ thống | MUST |
| FR-RSV-07 | Hiển thị màn hình xác nhận kèm mã đặt bàn | Khách hàng | MUST |
| FR-RSV-08 | Xem danh sách đơn của mình (sắp tới, lịch sử) và chi tiết từng đơn | Khách hàng | MUST |
| FR-RSV-09 | Hủy đơn trong thời hạn cho phép | Khách hàng | MUST |
| FR-RSV-10 | Đổi lịch đơn (ngày, giờ, thời lượng, số người) trong thời hạn cho phép | Khách hàng | SHOULD |
| FR-RSV-11 | Chặn vượt quá số đơn đang hoạt động và chặn đặt hai đơn chồng thời gian của cùng một khách | Hệ thống | SHOULD |

### 3.6 Quản lý đặt bàn phía nhà hàng (ADM)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-ADM-01 | Xem danh sách đơn, lọc theo ngày, trạng thái, khu vực, tên hoặc số điện thoại | Staff, Admin | MUST |
| FR-ADM-02 | Xem chi tiết đơn, gồm ghi chú đặc biệt của khách | Staff, Admin | MUST |
| FR-ADM-03 | Duyệt hoặc từ chối đơn chờ duyệt, từ chối phải kèm lý do | Staff, Admin | MUST |
| FR-ADM-04 | Đánh dấu khách đã đến, hoàn thành, không đến (thủ công) | Staff, Admin | MUST |
| FR-ADM-05 | Xem lịch đặt bàn theo ngày dạng lưới bàn × thời gian (timeline) | Staff, Admin | SHOULD |
| FR-ADM-06 | Đặt hộ khách (điện thoại, walk-in) không cần tài khoản, tự chọn thời lượng | Staff, Admin | SHOULD |
| FR-ADM-07 | Chuyển đơn sang bàn khác khi cần | Staff, Admin | SHOULD |
| FR-ADM-08 | Hủy đơn thay khách, kèm lý do | Staff, Admin | SHOULD |
| FR-ADM-09 | Xem nhật ký thay đổi trạng thái của từng đơn | Staff, Admin | SHOULD |
| FR-ADM-10 | Gia hạn hoặc rút ngắn thời lượng của đơn khi không chồng đơn khác | Staff, Admin | SHOULD |

### 3.7 Thông báo (NTF)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-NTF-01 | Gửi email xác nhận khi đặt bàn thành công | Hệ thống | SHOULD |
| FR-NTF-02 | Gửi email khi đơn bị hủy, từ chối hoặc đổi lịch | Hệ thống | SHOULD |
| FR-NTF-03 | Gửi email nhắc lịch trước giờ hẹn 3 giờ | Hệ thống | SHOULD |
| FR-NTF-04 | Thông báo trong ứng dụng | Hệ thống | COULD |

### 3.8 Đánh giá (REV)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-REV-01 | Đánh giá sau bữa ăn: 1–5 sao và nhận xét | Khách hàng | COULD |
| FR-REV-02 | Xem đánh giá của nhà hàng | Tất cả | COULD |
| FR-REV-03 | Ẩn đánh giá vi phạm | Admin | COULD |

### 3.9 Thống kê (RPT)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-RPT-01 | Số lượt đặt theo ngày và tuần | Admin | SHOULD |
| FR-RPT-02 | Giờ cao điểm và tỷ lệ lấp đầy bàn | Admin | COULD |
| FR-RPT-03 | Tỷ lệ hủy và không đến | Admin | COULD |

### 3.10 Tác vụ hệ thống (SYS)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-SYS-01 | Tự động hủy đơn chờ duyệt quá hạn theo BR-21, giải phóng bàn và gửi email cho khách | Hệ thống | SHOULD |

### 3.11 Đặt món giao hàng cơ bản (ORD)

| Mã | Yêu cầu | Vai trò | Ưu tiên |
|---|---|---|---|
| FR-ORD-01 | CUSTOMER đã đăng nhập chọn một hoặc nhiều món từ menu và số lượng cần đặt | Khách hàng | MUST |
| FR-ORD-02 | Nhập thông tin nhận hàng gồm **địa chỉ** và **số điện thoại** | Khách hàng | MUST |
| FR-ORD-03 | Xem lại các món, số lượng, giá bán, thông tin nhận hàng và phương thức thanh toán COD trước khi gửi đơn | Khách hàng | MUST |
| FR-ORD-04 | Gửi đơn món giao hàng; đơn mới được tạo với trạng thái `PLACED`, phương thức `COD` và mỗi `food_order_item` lưu `unit_price` bằng giá món tại thời điểm tạo đơn | Khách hàng | MUST |
| FR-ORD-05 | Khi tạo đơn thành công, hiển thị thông báo **“Đặt hàng thành công. Vui lòng chờ giao hàng.”** | Khách hàng | MUST |
| FR-ORD-06 | STAFF/ADMIN xem danh sách đơn món giao hàng | Staff/Admin | MUST |
| FR-ORD-07 | STAFF/ADMIN xem chi tiết đơn món giao hàng gồm thông tin nhận hàng, món, số lượng, `unit_price` và tổng tính theo giá snapshot; không có thao tác đổi trạng thái | Staff/Admin | MUST |
| FR-ORD-08 | CUSTOMER xem lịch sử các đơn món giao hàng của chính mình, sắp xếp mới nhất trước | Khách hàng | MUST |
| FR-ORD-09 | CUSTOMER xem chi tiết một đơn của chính mình gồm thông tin nhận hàng, trạng thái, COD, món, số lượng, `unit_price` và tổng đơn | Khách hàng | MUST |
| FR-ORD-10 | CUSTOMER hủy đơn món của chính mình khi trạng thái còn `PLACED`; hệ thống chuyển đơn sang `CANCELLED` | Khách hàng | MUST |

> **Giới hạn hiện tại:** chưa có phí/vùng giao hàng, tracking shipper, tồn kho, khuyến mãi, sửa nội dung đơn sau khi gửi, thanh toán online hoặc workflow vận hành giao hàng từ STAFF/ADMIN. CUSTOMER history, CUSTOMER cancel và snapshot giá đã thuộc MVP; xem BR-52 đến BR-58 và tài liệu 08.

### 3.12 Tóm tắt theo mức ưu tiên

| Nhóm | MUST | SHOULD | COULD | Tổng |
|---|---|---|---|---|
| Tài khoản và xác thực | 6 | 4 | 2 | 12 |
| Thông tin nhà hàng | 3 | 0 | 0 | 3 |
| Menu | 7 | 1 | 3 | 11 |
| Bàn và khu vực | 2 | 1 | 1 | 4 |
| Đặt bàn phía khách | 8 | 3 | 0 | 11 |
| Quản lý đặt bàn | 4 | 6 | 0 | 10 |
| Thông báo | 0 | 3 | 1 | 4 |
| Đánh giá | 0 | 0 | 3 | 3 |
| Thống kê | 0 | 1 | 2 | 3 |
| Tác vụ hệ thống | 0 | 1 | 0 | 1 |
| Đặt món giao hàng | 10 | 0 | 0 | 10 |
| **Tổng** | **40** | **20** | **12** | **72** |

Phạm vi MVP hiện gồm **40 yêu cầu MUST**, trong đó 10 yêu cầu thuộc luồng đặt món giao hàng. Các yêu cầu SHOULD làm sau khi MVP chạy ổn, ưu tiên nhóm tạo điểm nhấn kỹ thuật: chống đặt trùng, gợi ý giờ gần nhất, timeline cho nhân viên, email nhắc lịch. Các yêu cầu COULD chỉ làm khi còn thời gian.

### 3.13 Ngoài phạm vi MVP

- Thanh toán và đặt cọc trực tuyến cho đặt bàn. **Đặt món trước gắn với reservation/phục vụ tại bàn** vẫn ngoài phạm vi; **đặt món giao hàng cơ bản** đã được đưa vào MVP từ phiên bản 1.3.
- Ghép nhiều bàn cho nhóm đông; nhóm trên 12 người liên hệ trực tiếp nhà hàng.
- Giữ chỗ tạm khi khách đang điền form: hệ thống kiểm tra lại chỗ trống ở bước gửi đơn và báo lỗi kèm gợi ý nếu bàn vừa bị giữ.
- Tự động đánh dấu không đến và chính sách phạt khách; ở MVP nhân viên đánh dấu thủ công.
- Bắt buộc xác thực email khi đăng ký; tải ảnh món lên từ máy.
- Khách tự đặt bàn không cần tài khoản (chỉ nhân viên đặt hộ mới không cần tài khoản). **Đơn món giao hàng bắt buộc CUSTOMER đăng nhập**.

## 4. Quy tắc nghiệp vụ

### 4.1 Thời gian và khoảng giờ

| Mã | Quy tắc | Nội dung |
|---|---|---|
| BR-01 | Múi giờ | Hiển thị theo `Asia/Ho_Chi_Minh`, lưu trong DB dạng `timestamptz` |
| BR-02 | Giờ mở cửa | Mặc định 10:00 – 22:00, cấu hình được theo thứ trong tuần |
| BR-03 | Cách chọn thời gian | Khách chọn giờ bắt đầu và giờ kết thúc bất kỳ trong ngày, theo bước 15 phút |
| BR-04 | Thời lượng lượt ăn | Bằng giờ kết thúc trừ giờ bắt đầu. Hệ thống gợi ý mặc định 90 phút; khách chọn từ 60 đến 180 phút; nhân viên chọn tự do trong giờ mở cửa |
| BR-05 | Ràng buộc giờ mở, đóng cửa | Giờ bắt đầu không sớm hơn giờ mở cửa; giờ kết thúc không muộn hơn giờ đóng cửa của ngày đó |
| BR-06 | Thời gian đệm dọn bàn | 15 phút sau mỗi lượt, không tính vào giờ mở cửa |
| BR-07 | Khoảng bàn bị chiếm | Từ giờ bắt đầu đến giờ kết thúc cộng 15 phút đệm |
| BR-08 | Đặt trước tối thiểu | Khách: 2 giờ trước giờ bắt đầu. Riêng đơn cần duyệt (từ 7 người) phải đặt trước tối thiểu 4 giờ, để nhân viên còn ít nhất 1 giờ duyệt trước hạn tự hủy của BR-21. Áp dụng cả khi đổi lịch; các mốc này là cấu hình. Nhân viên đặt hộ không bị giới hạn này để phục vụ khách đang đến quầy |
| BR-09 | Đặt trước tối đa | 30 ngày |
| BR-10 | Ngày đóng cửa đặc biệt | Không nhận đơn nào trong ngày đó |

### 4.2 Sức chứa và gán bàn

| Mã | Quy tắc | Nội dung |
|---|---|---|
| BR-11 | Số người mỗi đơn | Từ 1 đến 12. Nhóm đông hơn liên hệ trực tiếp nhà hàng |
| BR-12 | Bàn phù hợp với đơn | `min_capacity ≤ số người ≤ max_capacity` và bàn đang hoạt động |
| BR-13 | Thuật toán gán bàn | Trong các bàn phù hợp, đang hoạt động, **không bị tạm ngưng chồng khoảng bị chiếm của đơn** và không bị đơn khác chiếm, chọn bàn có `max_capacity` nhỏ nhất; nếu bằng nhau chọn mã bàn nhỏ nhất. Khi gán bàn trong transaction phải khóa row `restaurant_table` của bàn được chọn rồi kiểm tra lại reservation và `table_suspension` trước khi ghi |
| BR-14 | Khu vực | Nếu khách chọn khu vực thì chỉ xét bàn trong khu vực đó; lựa chọn này được lưu trên đơn dưới dạng `requested_area_id` (NULL = không yêu cầu khu vực). Hết chỗ thì báo và gợi ý theo BR-13 trong cùng khu vực. Nhân viên chuyển bàn thủ công được chọn bàn thuộc khu vực khác (có cảnh báo so với `requested_area_id`); việc chuyển bàn không thay đổi `requested_area_id` và không gửi email cho khách |
| BR-15 | Ghép bàn | Không hỗ trợ ở MVP |
| BR-16 | Bàn tạm ngưng hoặc ngừng dùng | Không được gán cho đơn mới. Một bàn được coi là tạm ngưng đối với đơn nếu có `table_suspension` chồng với **khoảng bị chiếm** của đơn. Tạo/sửa tạm ngưng phải khóa row `restaurant_table` tương ứng; create/reschedule/move/đặt hộ cũng dùng cùng row lock để tránh race giữa việc giữ bàn và tạm ngưng bàn |

### 4.3 Vòng đời đơn đặt bàn

| Mã | Quy tắc | Nội dung |
|---|---|---|
| BR-17 | Mã đặt bàn | Duy nhất, dễ đọc (ví dụ `SAGE-250925-7K3F`) |
| BR-18 | Trạng thái đơn | `PENDING` (chờ duyệt), `CONFIRMED` (đã xác nhận), `ARRIVED` (khách đã đến), `COMPLETED` (hoàn thành), `NO_SHOW` (không đến), `CANCELLED` (đã hủy), `REJECTED` (bị từ chối) |
| BR-19 | Giữ chỗ và thời gian dọn bàn | `PENDING`, `CONFIRMED`, `ARRIVED` là các trạng thái giữ chỗ đang hoạt động. `COMPLETED` không còn là đơn hoạt động nhưng **vẫn tham gia kiểm tra chiếm bàn trên `occupied_range` đã cam kết đến hết `end_time + buffer`**, để không làm mất thời gian dọn bàn. `CANCELLED`, `REJECTED`, `NO_SHOW` giải phóng bàn ngay |
| BR-20 | Cách xác nhận đơn | Đơn từ 1 đến 6 người tự động `CONFIRMED` nếu còn bàn; đơn từ 7 người trở lên vào `PENDING` chờ duyệt. Ngưỡng 6 người là cấu hình |
| BR-21 | Đơn chờ duyệt quá hạn | Chưa được duyệt trước giờ bắt đầu 3 giờ thì tự động hủy và gửi email cho khách |

**Bảng chuyển trạng thái hợp lệ**

| Từ | Sang | Người thực hiện | Điều kiện |
|---|---|---|---|
| PENDING | CONFIRMED | Staff/Admin hoặc hệ thống | Staff/Admin duyệt, hoặc hệ thống xác định lại theo BR-20 khi đổi lịch sang nhóm tự xác nhận |
| CONFIRMED | PENDING | Hệ thống | Đổi lịch thành công làm số người mới thuộc nhóm cần duyệt theo BR-20 |
| PENDING | REJECTED | Staff/Admin | Bắt buộc ghi lý do |
| PENDING | CANCELLED | Khách (trong hạn), Staff/Admin, hệ thống (hết hạn theo BR-21) | |
| CONFIRMED | CANCELLED | Khách (trong hạn, theo BR-22), Staff/Admin (bất kỳ lúc nào) | |
| CONFIRMED | ARRIVED | Staff/Admin | Trong ngày hẹn |
| CONFIRMED | NO_SHOW | Staff/Admin (thủ công) | Sau giờ bắt đầu 15 phút |
| ARRIVED | COMPLETED | Staff/Admin | |

`REJECTED`, `CANCELLED`, `NO_SHOW`, `COMPLETED` là trạng thái cuối, không chuyển tiếp.

### 4.4 Hủy, đổi lịch, không đến

| Mã | Quy tắc | Nội dung |
|---|---|---|
| BR-22 | Hạn hủy hoặc đổi lịch của khách | Còn ít nhất 2 giờ trước giờ bắt đầu |
| BR-23 | Số lần đổi lịch tối đa mỗi đơn | 2 lần |
| BR-24 | Cách đổi lịch | Cập nhật chính đơn cũ trong một transaction: kiểm tra chỗ cho thời gian mới rồi mới đổi; nếu không còn chỗ thì giữ nguyên đơn cũ. Khoảng thời gian đang giữ của chính đơn không bị tính là xung đột và hệ thống ưu tiên giữ nguyên bàn hiện tại nếu vẫn phù hợp và còn trống. Sau khi đổi, trạng thái được xác định lại theo BR-20 dựa trên số người mới (đến 6 người là `CONFIRMED`, từ 7 người là `PENDING` để duyệt lại) |
| BR-25 | Giữ bàn khi khách chưa đến | 15 phút sau giờ bắt đầu; sau mốc này nhân viên có thể đánh dấu `NO_SHOW` và bàn được giải phóng cho phần thời gian còn lại |
| BR-26 | Xử lý khách không đến | Chỉ ghi nhận `NO_SHOW` cho thống kê, không chặn đặt bàn, không phạt |

### 4.5 Giới hạn đối với khách hàng

| Mã | Quy tắc | Nội dung |
|---|---|---|
| BR-27 | Số đơn đang hoạt động (`PENDING`, `CONFIRMED`) tối đa mỗi khách | 3. Khi tạo đơn của khách đã đăng nhập, transaction phải **khóa row `account` của khách trước**, sau đó đếm các đơn đang hoạt động và chỉ tạo nếu còn dưới giới hạn. Mọi luồng tạo đơn cho cùng customer dùng cùng thứ tự khóa để chống race giữa hai request đồng thời |
| BR-28 | Đơn của cùng một khách | Không được chồng **lượt ăn thực tế** `[start_time, end_time)`. Thời gian đệm dọn bàn chỉ thuộc tài nguyên bàn và **không** được dùng để chặn lịch của chính khách |

### 4.6 Đồng thời và toàn vẹn dữ liệu

| Mã | Quy tắc |
|---|---|
| BR-29 | Không bao giờ có hai reservation cùng bàn chồng `occupied_range` khi chúng thuộc các trạng thái tham gia chiếm bàn theo BR-19 (`PENDING`, `CONFIRMED`, `ARRIVED`, `COMPLETED`), kể cả khi nhiều yêu cầu đến cùng lúc |
| BR-30 | Bảo đảm BR-29 ở hai lớp: kiểm tra trong transaction ở tầng ứng dụng và ràng buộc ở tầng database trên `occupied_range` (giờ bắt đầu đến giờ kết thúc cộng đệm). BR-28 dùng một `reservation_range = [start_time, end_time)` riêng, không cộng đệm |
| BR-31 | Bàn, món, danh mục đã được tham chiếu thì chuyển sang ngừng dùng thay vì xóa cứng; chưa từng dùng thì được xóa cứng |
| BR-32 | Mọi thay đổi trên đơn (tạo, đổi lịch, chuyển bàn, đổi thời lượng, đổi trạng thái) được ghi nhật ký: người thực hiện, thời điểm, loại sự kiện, giá trị trước và sau, lý do nếu có |
| BR-33 | Gia hạn hoặc rút ngắn thời lượng của một đơn chỉ hợp lệ khi khoảng bị chiếm mới không chồng đơn khác trên cùng bàn; nếu chồng thì báo xung đột và gợi ý chuyển bàn. Chỉ áp dụng cho đơn `CONFIRMED` hoặc `ARRIVED` |
| BR-48 | Khi tạm ngưng bàn, đổi giờ mở cửa hoặc thêm ngày đóng cửa làm ảnh hưởng các đơn đang giữ chỗ, hệ thống cảnh báo và liệt kê các đơn bị ảnh hưởng, không tự hủy; nhân viên xử lý bằng chuyển bàn, liên hệ khách hoặc hủy thay khách. Riêng tạo/sửa `table_suspension` phải khóa row bàn và kiểm tra xung đột trong cùng transaction; sau khi suspension tồn tại, mọi luồng gán/chuyển bàn mới phải loại bàn đó nếu khoảng bị chiếm bị chồng |

### 4.7 Tài khoản và bảo mật

| Mã | Quy tắc | Nội dung |
|---|---|---|
| BR-34 | Email | Duy nhất trong hệ thống |
| BR-35 | Số điện thoại | Đúng định dạng Việt Nam (10 số, bắt đầu bằng 0) |
| BR-36 | Mật khẩu | Tối thiểu 8 ký tự, có chữ và số; lưu bằng BCrypt |
| BR-37 | Phiên đăng nhập | Sau khi xác thực email/mật khẩu thành công, hệ thống cấp JWT access token có TTL 1 giờ. MVP không cấp refresh token |
| BR-38 | Truy cập dữ liệu | Khách hàng chỉ xem và thao tác trên đơn của chính mình; STAFF chỉ quản lý đơn; ADMIN toàn quyền. Quản trị không thể tự khóa hoặc tự hạ quyền của chính mình |
| BR-39 | Đơn đặt hộ | Khách tự đặt phải đăng nhập; đơn do nhân viên đặt hộ có thể không gắn tài khoản và lưu họ tên, số điện thoại trực tiếp trên đơn. Đơn đặt hộ có trạng thái ban đầu `CONFIRMED`, không áp dụng giới hạn thời lượng 60–180 phút và giới hạn đặt trước của khách, vẫn tối đa 12 người, và không lưu email nên không gửi email cho khách |
| BR-59 | Kiểu JWT | JWT access token của MVP dùng HMAC SHA-256 (`HS256`). Secret tối thiểu 256 bit, lấy từ biến môi trường/secret manager; không commit vào source code hoặc repository |
| BR-60 | JWT claims | Token tối thiểu có `iss`, `sub`, `role`, `iat`, `exp`; `sub` là `account.id`. Không đưa mật khẩu, số điện thoại, địa chỉ hoặc dữ liệu nhạy cảm vào token |
| BR-61 | Truyền token | API protected nhận token qua header `Authorization: Bearer <token>`. Không nhận JWT qua query string |
| BR-62 | Xác thực request | Với request protected, backend phải kiểm tra signature, `iss`, `exp` và cấu trúc claims; sau đó tải `Account` theo `sub`. Account không tồn tại hoặc `is_locked=true` thì trả `401` |
| BR-63 | Phân quyền | Authority dùng cho quyết định authorization phải lấy từ role hiện tại trong DB. Nếu `role` trong token khác role hiện tại, token được coi là stale và trả `401`, yêu cầu đăng nhập lại |
| BR-64 | Stateless và logout | `SessionCreationPolicy.STATELESS`; server không tạo HTTP session cho auth. Đăng xuất ở MVP là xóa access token phía client. Không có blacklist/revocation list và không có endpoint refresh |
| BR-65 | Mã lỗi auth | Thiếu/token sai/hết hạn/account bị khóa/token stale → `401 Unauthorized`; token hợp lệ nhưng role không đủ quyền → `403 Forbidden`. Response lỗi không tiết lộ chi tiết signature/secret hoặc lý do xác thực mật khẩu |

### 4.8 Menu

| Mã | Quy tắc | Nội dung |
|---|---|---|
| BR-40 | Giá món | Số nguyên VND lớn hơn 0, hiển thị dạng `125.000đ` |
| BR-41 | Trạng thái món | `AVAILABLE` hiển thị bình thường; `SOLD_OUT` hiển thị mờ và ghi "Hết món"; `HIDDEN` không hiển thị với khách |
| BR-42 | Danh mục và ảnh | Không xóa cứng danh mục còn món; ảnh món lưu dưới dạng URL |
| BR-43 | Nhãn món | Mỗi món có thể gán nhiều nhãn (chay, cay, bán chạy...), lưu bằng bảng `tag` và `dish_tag` |

### 4.9 Thông báo và đánh giá

| Mã | Quy tắc | Nội dung |
|---|---|---|
| BR-44 | Sự kiện gửi email | Xác nhận (khi đơn chuyển sang `CONFIRMED`), hủy, từ chối, đổi lịch, nhắc lịch. Đơn `PENDING` không gửi email cho đến khi có kết quả duyệt, từ chối hoặc hủy |
| BR-45 | Thời điểm nhắc lịch | Trước giờ bắt đầu 3 giờ, sớm hơn hạn hủy để khách kịp hủy |
| BR-46 | Lỗi gửi email | Không được làm thất bại việc đặt bàn; gửi bất đồng bộ, lỗi chỉ ghi log |
| BR-47 | Điều kiện đánh giá (COULD) | Đơn `COMPLETED`, mỗi đơn một đánh giá, trong 14 ngày sau bữa ăn |

### 4.10 Đặt món giao hàng cơ bản

| Mã | Quy tắc | Nội dung |
|---|---|---|
| BR-49 | Luồng đặt món | CUSTOMER đăng nhập chọn món và số lượng → nhập địa chỉ + số điện thoại nhận hàng → xem lại đơn với phương thức COD → gửi đơn → hệ thống snapshot giá từng item, tạo đơn `PLACED` → hiển thị **“Đặt hàng thành công. Vui lòng chờ giao hàng.”** |
| BR-50 | Validation tối thiểu | Đơn phải có ít nhất một item; mỗi item có món tồn tại và `quantity > 0`; `delivery_address` và `delivery_phone` không được để trống. Chưa áp dụng min order, giới hạn số lượng, vùng giao, phí giao hoặc tồn kho |
| BR-51 | Xác thực đặt món | Chỉ tài khoản đã đăng nhập với vai trò `CUSTOMER` được tạo đơn món giao hàng; `food_order.customer_id` luôn NOT NULL |
| BR-52 | Snapshot giá món | Khi tạo đơn, mỗi `food_order_item.unit_price` được copy từ `dish.price` hiện tại và phải là số nguyên VND > 0. Sau khi item được tạo, giá lịch sử của item đọc từ `unit_price`, không đọc lại `dish.price`; tổng đơn được tính bằng `SUM(unit_price * quantity)` |
| BR-53 | Trạng thái đơn | Trạng thái food order hiện gồm `PLACED` và `CANCELLED`. Chuyển trạng thái duy nhất trong phạm vi hiện tại là `PLACED → CANCELLED` do CUSTOMER sở hữu đơn thực hiện. `CANCELLED` là trạng thái cuối |
| BR-54 | Thanh toán | Chỉ hỗ trợ `COD` (thanh toán khi nhận hàng). Không tạo payment transaction/gateway và không có thanh toán online |
| BR-55 | Quyền STAFF/ADMIN | STAFF và ADMIN chỉ được xem danh sách/chi tiết đơn món giao hàng ở cả `PLACED` và `CANCELLED`. Không có quyền thay đổi trạng thái hoặc chỉnh sửa đơn ở giai đoạn này |
| BR-56 | Phạm vi để sau | Delivery fee/zone, ETA, shipper tracking, inventory, promotion, sửa nội dung đơn sau khi gửi, các trạng thái giao hàng khác và workflow vận hành giao hàng chưa thuộc phạm vi hiện tại |
| BR-57 | Lịch sử đơn CUSTOMER | CUSTOMER chỉ được xem danh sách và chi tiết `food_order` có `customer_id` bằng tài khoản hiện tại; danh sách sắp xếp `created_at` giảm dần. Không được lộ đơn của CUSTOMER khác |
| BR-58 | Hủy đơn CUSTOMER | CUSTOMER chỉ được hủy đơn thuộc chính mình và chỉ khi status hiện tại là `PLACED`. Hủy phải chạy trong transaction, cập nhật `status=CANCELLED` và `cancelled_at`; nếu đơn đã `CANCELLED` hoặc không thuộc người dùng thì không thay đổi dữ liệu |

## 5. Yêu cầu phi chức năng

| Mã | Yêu cầu |
|---|---|
| NFR-01 | **Đồng thời:** bài test 50 yêu cầu cùng đặt một bàn với các khoảng thời gian chồng nhau chỉ cho đúng 1 đơn thành công |
| NFR-02 | **Hiệu năng:** tra cứu giờ khả dụng cho một ngày phản hồi dưới 500 ms với dữ liệu mẫu khoảng 30 bàn và 10.000 đơn; nên lấy các đơn trong ngày bằng một truy vấn rồi tính các giờ khả dụng trong bộ nhớ |
| NFR-03 | **Bảo mật:** BCrypt cho mật khẩu; JWT HS256 secret tối thiểu 256 bit nằm ngoài source code; API protected bắt buộc Bearer token; kiểm tra account hiện tại trước khi cấp quyền; phân biệt `401` và `403`; không lộ thông tin đơn của người khác |
| NFR-04 | **Giao diện:** tiếng Việt, responsive, dùng tốt trên màn hình điện thoại từ 360 px |
| NFR-05 | **Khả năng bảo trì:** cấu hình nghiệp vụ (đệm, giới hạn thời lượng, hạn hủy, ngưỡng tự xác nhận, số đơn tối đa, giờ mở cửa) nằm trong bảng cấu hình hoặc file cấu hình, không viết cứng trong mã |
| NFR-06 | **Tài liệu API:** Swagger/OpenAPI bằng tiếng Anh, mô tả rõ mã lỗi nghiệp vụ (hết chỗ, quá hạn hủy, chồng lịch, vượt giới hạn thời lượng...) |
| NFR-07 | **Kiểm thử:** ngoài các ca biên hiện có, bắt buộc có test cho 7 sửa logic: (1) `COMPLETED` vẫn chặn bàn đến hết buffer; (2) hai request đồng thời khi khách đang có 2 đơn chỉ tạo thêm tối đa 1 đơn; (3) `@Version` khớp schema và bắt concurrent update; (4) reschedule `CONFIRMED → PENDING`; (5) race booking với tạo `table_suspension` không gán bàn sai; (6) `requested_area_id` giữ nguyên sau chuyển bàn; (7) hai booking của cùng khách được phép liền nhau đúng `end_time == next.start_time` dù bàn có buffer |
| NFR-08 | **Khả năng triển khai:** chạy được bằng Docker Compose (backend, database, frontend) |
| NFR-09 | **Kiểm thử xác thực:** có test cho login đúng/sai, token thiếu/sai/hết hạn, account bị khóa, role thay đổi làm token stale, `401`/`403`, và CUSTOMER không truy cập được API STAFF/ADMIN |


**Các quy tắc ảnh hưởng trực tiếp đến ERD**
- Đơn lưu `start_time`, `end_time`, `reservation_range = [start_time,end_time)` để chống chồng lịch theo khách và `occupied_range` (kèm đệm) để chống trùng bàn.
- `customer_id` cho phép rỗng, kèm `guest_name`, `guest_phone` cho đơn đặt hộ.
- `requested_area_id` cho phép rỗng để lưu khu vực khách yêu cầu độc lập với bàn hiện tại; chuyển bàn không thay đổi giá trị này.
- Bảng giờ mở cửa theo thứ và bảng ngày đóng cửa.
- Bàn có `min_capacity`, `max_capacity` và cờ ngừng dùng.
- Bảng `tag`, `dish_tag`; món có trạng thái và URL ảnh.
- Bảng nhật ký trạng thái đơn và bảng cấu hình nghiệp vụ.
- Đếm số lần đổi lịch trên đơn.
- Các mốc giờ bước 15 phút, giới hạn thời lượng và đệm đọc từ bảng cấu hình.


## 6. Ghi chú thiết kế cho tính năng đặt món giao hàng

Các quyết định food order đã được mở rộng. Tài liệu `04-erd-database-design.md` và `05-jpa-entities.md` phải có `food_order` / `food_order_item`, quan hệ bắt buộc tới CUSTOMER, trạng thái `PLACED`/`CANCELLED`, `cancelled_at`, phương thức `COD` và `food_order_item.unit_price` snapshot từ `dish.price` lúc tạo đơn. CUSTOMER có API lịch sử/chi tiết theo ownership và command hủy đơn `PLACED`. Chi tiết nghiệp vụ xem `08-food-ordering-customer-history-cancellation-price-snapshot.md`.

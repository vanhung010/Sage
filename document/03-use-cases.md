# Saga Res: Đặc tả use case

| | |
|---|---|
| **Dự án** | Saga Res (nhà hàng demo: Sage) |
| **Tác giả** | Hùng |
| **Phiên bản** | 1.5 (bổ sung đặc tả JWT access token và security flow cho MVP) |
| **Tài liệu liên quan** | `01-project-summary.md`, `02-functional-requirements-and-business-rules.md`, `07-food-ordering-basic-specification.md`, `08-food-ordering-customer-history-cancellation-price-snapshot.md`, `09-authentication-jwt-security-design.md` |

## 1. Giới thiệu và quy ước

Tài liệu mô tả cách người dùng và hệ thống tương tác để thực hiện các yêu cầu chức năng (FR) trong tài liệu 02, dựa trên các quy tắc nghiệp vụ (BR) đã chốt.

- **Đặc tả chi tiết** (phần 4) dành cho các luồng nghiệp vụ cốt lõi: tiền điều kiện, hậu điều kiện, luồng chính, luồng thay thế và ngoại lệ.
- **Đặc tả rút gọn** (phần 5) dành cho các chức năng quản lý và tra cứu đơn giản.
- Luồng thay thế đánh số theo bước của luồng chính (ví dụ `4a` là nhánh rẽ ở bước 4).
- Mã tham chiếu: `UC-xx` (use case), `FR-xxx-xx` (yêu cầu chức năng), `BR-xx` (quy tắc nghiệp vụ), `G-xx` (quyết định bổ sung ở phần 6).
- Ưu tiên lấy theo tài liệu 02: **MUST**, **SHOULD**, **COULD**.
- Mọi thời điểm hiển thị theo `Asia/Ho_Chi_Minh` (BR-01). Mọi thao tác thay đổi đơn đặt bàn chạy trong transaction.

## 2. Tác nhân

| Tác nhân           | Mô tả                                                                                                                            |
| ------------------ | -------------------------------------------------------------------------------------------------------------------------------- |
| **Khách vãng lai** | Người chưa đăng nhập; xem thông tin, menu, giờ khả dụng                                                                          |
| **Khách hàng**     | Người đã đăng nhập với vai trò CUSTOMER; kế thừa mọi quyền của khách vãng lai và là tác nhân duy nhất được tạo đơn món giao hàng |
| **Nhân viên**      | Vai trò STAFF; quản lý đơn đặt bàn và xem đơn món giao hàng ở chế độ chỉ đọc                                                     |
| **Quản trị**       | Vai trò ADMIN; kế thừa quyền của nhân viên, thêm quản lý menu, bàn, cấu hình, tài khoản, thống kê                                |
| **Hệ thống**       | Các tác vụ tự động: gửi email, chạy job định kỳ                                                                                  |


## 3. Danh sách use case

| Mã    | Tên use case                                 | Tác nhân            | Ưu tiên | Yêu cầu liên quan          |
| ----- | -------------------------------------------- | ------------------- | ------- | -------------------------- |
| UC-01 | Đăng ký tài khoản                            | Khách vãng lai      | MUST    | FR-AUTH-01                 |
| UC-02 | Đăng nhập, sử dụng JWT và đăng xuất          | Tất cả              | MUST    | FR-AUTH-02, 03, 10, 11, 12 |
| UC-03 | Quản lý hồ sơ và mật khẩu                    | Khách hàng          | SHOULD  | FR-AUTH-04, 05, 06         |
| UC-04 | Xem thông tin nhà hàng                       | Tất cả              | MUST    | FR-INFO-01                 |
| UC-05 | Xem, tìm kiếm và lọc menu                    | Tất cả              | MUST    | FR-MENU-01, 02, 03, 04     |
| UC-06 | Tra cứu bàn trống và giờ khả dụng            | Tất cả              | MUST    | FR-RSV-01, 02, 03          |
| UC-07 | Đặt bàn                                      | Khách hàng          | MUST    | FR-RSV-04, 05, 06, 07, 11  |
| UC-08 | Xem đơn đặt bàn của tôi                      | Khách hàng          | MUST    | FR-RSV-08                  |
| UC-09 | Hủy đơn đặt bàn                              | Khách hàng          | MUST    | FR-RSV-09                  |
| UC-10 | Đổi lịch đơn đặt bàn                         | Khách hàng          | SHOULD  | FR-RSV-10                  |
| UC-11 | Xem và lọc danh sách đơn                     | Nhân viên           | MUST    | FR-ADM-01, 02              |
| UC-12 | Duyệt hoặc từ chối đơn chờ duyệt             | Nhân viên           | MUST    | FR-ADM-03                  |
| UC-13 | Đánh dấu khách đến, hoàn thành, không đến    | Nhân viên           | MUST    | FR-ADM-04                  |
| UC-14 | Đặt bàn hộ khách                             | Nhân viên           | SHOULD  | FR-ADM-06                  |
| UC-15 | Hủy đơn thay khách                           | Nhân viên           | SHOULD  | FR-ADM-08                  |
| UC-16 | Chuyển đơn sang bàn khác                     | Nhân viên           | SHOULD  | FR-ADM-07                  |
| UC-17 | Gia hạn hoặc rút ngắn thời lượng             | Nhân viên           | SHOULD  | FR-ADM-10                  |
| UC-18 | Xem lịch đặt bàn dạng timeline               | Nhân viên           | SHOULD  | FR-ADM-05                  |
| UC-19 | Xem nhật ký thay đổi của đơn                 | Nhân viên           | SHOULD  | FR-ADM-09                  |
| UC-20 | Quản lý danh mục, món và nhãn                | Quản trị            | MUST    | FR-MENU-05, 06, 07, 08     |
| UC-21 | Quản lý khu vực và bàn                       | Quản trị            | MUST    | FR-TBL-01, 02, 03          |
| UC-22 | Quản lý thông tin, giờ mở cửa, ngày đóng cửa | Quản trị            | MUST    | FR-INFO-02, 03             |
| UC-23 | Quản lý tài khoản người dùng                 | Quản trị            | SHOULD  | FR-AUTH-08                 |
| UC-24 | Xem thống kê                                 | Quản trị            | SHOULD  | FR-RPT-01, 02, 03          |
| UC-25 | Gửi thông báo email                          | Hệ thống            | SHOULD  | FR-NTF-01, 02, 03          |
| UC-26 | Tự động hủy đơn chờ duyệt quá hạn            | Hệ thống            | SHOULD  | FR-SYS-01, BR-21           |
| UC-35 | Đặt món giao hàng cơ bản                     | Khách hàng          | MUST    | FR-ORD-01, 02, 03, 04, 05  |
| UC-36 | Xem danh sách và chi tiết đơn món giao hàng  | Nhân viên, Quản trị | MUST    | FR-ORD-06, 07              |
| UC-37 | Xem lịch sử và chi tiết đơn món của tôi      | Khách hàng          | MUST    | FR-ORD-08, 09              |
| UC-38 | Hủy đơn món của tôi                          | Khách hàng          | MUST    | FR-ORD-10                  |

Các use case ưu tiên COULD được liệt kê ở mục 5.3.

## 4. Đặc tả chi tiết

### 4.1 Tài khoản

#### UC-01. Đăng ký tài khoản

| | |
|---|---|
| **Tác nhân** | Khách vãng lai |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-AUTH-01; BR-34, BR-35, BR-36 |
| **Tiền điều kiện** | Chưa đăng nhập |
| **Hậu điều kiện** | Tài khoản CUSTOMER được tạo, mật khẩu lưu dạng băm BCrypt |

**Luồng chính**
1. Khách mở trang đăng ký.
2. Khách nhập họ tên, email, số điện thoại, mật khẩu và xác nhận mật khẩu.
3. Hệ thống kiểm tra dữ liệu theo BR-34, BR-35, BR-36.
4. Hệ thống tạo tài khoản với vai trò CUSTOMER.
5. Hệ thống thông báo thành công và chuyển đến trang đăng nhập.

**Luồng thay thế và ngoại lệ**
- 3a. Email đã tồn tại: báo lỗi, giữ nguyên các trường còn lại.
- 3b. Số điện thoại hoặc mật khẩu sai định dạng: báo lỗi ngay dưới từng trường.
- 3c. Mật khẩu xác nhận không khớp: báo lỗi.

#### UC-02. Đăng nhập, sử dụng JWT và đăng xuất

| | |
|---|---|
| **Tác nhân** | Khách hàng, Nhân viên, Quản trị |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-AUTH-02, FR-AUTH-03, FR-AUTH-10, FR-AUTH-11, FR-AUTH-12; BR-37, BR-38, BR-59 đến BR-65 |
| **Tiền điều kiện** | Đã có tài khoản và tài khoản không bị khóa |
| **Hậu điều kiện** | Client có JWT access token hợp lệ để gọi API theo role hiện tại; backend không tạo HTTP session |

**Luồng chính — đăng nhập**
1. Người dùng nhập email và mật khẩu.
2. Backend tải tài khoản theo email, kiểm tra tài khoản tồn tại, chưa bị khóa và so khớp mật khẩu BCrypt.
3. Nếu hợp lệ, backend phát JWT access token `HS256` có TTL 1 giờ; tối thiểu gồm `iss`, `sub=account.id`, `role`, `iat`, `exp`.
4. Backend trả token cùng metadata cần thiết (`tokenType=Bearer`, `expiresIn=3600`) và thông tin người dùng tối thiểu để frontend điều hướng.
5. Frontend lưu access token trong phạm vi phiên người dùng và gửi `Authorization: Bearer <token>` cho mọi API protected. CUSTOMER được điều hướng về khu vực khách; STAFF/ADMIN vào khu vực quản lý.

**Luồng chính — xác thực request protected**
6. Security filter lấy Bearer token từ header; nếu không có token ở endpoint protected thì trả `401`.
7. Backend xác minh signature, issuer và thời hạn `exp`; token sai/hết hạn trả `401`.
8. Backend lấy `sub`, tải `Account` hiện tại từ DB và kiểm tra `is_locked`. Account không tồn tại hoặc bị khóa trả `401`.
9. Backend so `role` trong token với role hiện tại trong DB. Nếu khác nhau, token được coi là stale, trả `401` và yêu cầu đăng nhập lại.
10. Backend tạo `Authentication` với authority lấy từ role hiện tại trong DB rồi áp dụng authorization cho endpoint. Token hợp lệ nhưng role không đủ quyền trả `403`.

**Luồng chính — đăng xuất**
11. Frontend xóa access token và dữ liệu auth phía client rồi chuyển về trạng thái chưa đăng nhập. MVP không gọi refresh/revoke API vì không có refresh token hoặc blacklist.

**Luồng thay thế và ngoại lệ**
- 2a. Sai email hoặc mật khẩu: trả thông báo chung “Email hoặc mật khẩu không đúng”, không tiết lộ trường nào sai.
- 2b. Tài khoản bị khóa: không cấp token; trả lỗi xác thực phù hợp.
- 3a. Secret JWT không được cấu hình hoặc không đủ điều kiện triển khai: backend phải fail-fast khi khởi động, không tự dùng secret mặc định.
- 6a. Header không theo dạng `Bearer <token>`: trả `401`.
- 7a. Token hết hạn: trả `401`; do MVP không có refresh token, người dùng đăng nhập lại.
- 8a. Account đã bị xóa/ngừng hợp lệ hoặc bị khóa sau khi token được cấp: trả `401` ngay ở request kế tiếp.
- 9a. Admin đổi role của account sau khi token được cấp: token cũ stale, trả `401`; đăng nhập lại để nhận token với role mới.
- 10a. Token hợp lệ nhưng gọi endpoint ngoài quyền: trả `403`; không chuyển thành `401`.

### 4.2 Tra cứu

#### UC-06. Tra cứu bàn trống và giờ khả dụng

| | |
|---|---|
| **Tác nhân** | Khách vãng lai, Khách hàng |
| **Ưu tiên** | MUST (gợi ý giờ gần nhất: SHOULD) |
| **Yêu cầu / quy tắc** | FR-RSV-01, 02, 03; BR-02 đến BR-14, BR-16 |
| **Tiền điều kiện** | Không |
| **Hậu điều kiện** | Người dùng thấy các giờ bắt đầu còn trống; không tạo hay thay đổi dữ liệu |

**Luồng chính**
1. Người dùng chọn ngày, số người và (tùy chọn) khu vực. Thời lượng mặc định 90 phút, có thể chỉnh trong khoảng 60–180 phút.
2. Hệ thống kiểm tra ngày và số người hợp lệ (BR-08, BR-09, BR-10, BR-11).
3. Hệ thống lấy giờ mở cửa của ngày đó (BR-02), các bàn phù hợp đang hoạt động, các khoảng `table_suspension` liên quan (BR-12, BR-14, BR-16) và các reservation có `occupied_range` cần tính cho ngày đó, gồm cả `COMPLETED` khi khoảng đệm chưa kết thúc (BR-19).
4. Hệ thống sinh các giờ bắt đầu theo bước 15 phút sao cho giờ kết thúc không muộn hơn giờ đóng cửa (BR-03, BR-05). Một giờ bắt đầu được giữ lại nếu có ít nhất một bàn phù hợp mà `occupied_range` ứng viên không chồng reservation đang chiếm bàn và không chồng `table_suspension` của bàn đó (BR-06, BR-07, BR-16, BR-19).
5. Hệ thống hiển thị lưới các giờ bắt đầu còn trống.
6. Người dùng bấm chọn một giờ để chuyển sang UC-07. Nếu chưa đăng nhập, người dùng được yêu cầu đăng nhập trước và giữ nguyên lựa chọn.

**Luồng thay thế và ngoại lệ**
- 2a. Ngày là ngày đóng cửa đặc biệt: báo nhà hàng đóng cửa ngày đó.
- 2b. Thời điểm quá gần (chưa đủ 2 giờ, hoặc chưa đủ 4 giờ với nhóm từ 7 người) hoặc quá xa (quá 30 ngày): báo giới hạn đặt trước (BR-08, BR-09).
- 2c. Số người lớn hơn 12: báo cần liên hệ trực tiếp nhà hàng.
- 4a. Không còn giờ nào: hệ thống gợi ý tối đa 3 khoảng thời gian gần nhất còn trống trong cùng ngày, giữ nguyên thời lượng. Nếu khách đã chọn khu vực thì chỉ gợi ý trong khu vực đó (BR-14).
- 4b. Cả ngày không còn chỗ: báo hết chỗ và gợi ý chọn ngày khác.

### 4.3 Đặt bàn phía khách

#### UC-07. Đặt bàn

| | |
|---|---|
| **Tác nhân** | Khách hàng |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-RSV-04, 05, 06, 07, 11; BR-03 đến BR-16, BR-17 đến BR-20, BR-27 đến BR-30, BR-32, BR-46; G-12, G-13, G-14, G-16, G-17 |
| **Tiền điều kiện** | Khách đã đăng nhập; đã chọn một giờ bắt đầu từ UC-06 |
| **Hậu điều kiện (thành công)** | Đơn được tạo với mã đặt bàn; bàn được gán và giữ chỗ; trạng thái là `CONFIRMED` (đến 6 người) hoặc `PENDING` (từ 7 người); nhật ký được ghi |

**Luồng chính**
1. Khách chọn một giờ bắt đầu từ lưới giờ khả dụng. Hệ thống điền sẵn giờ kết thúc theo thời lượng đã chọn.
2. Khách kiểm tra hoặc chỉnh giờ kết thúc (thời lượng 60–180 phút), số người, khu vực yêu cầu (tùy chọn), và nhập ghi chú đặc biệt nếu có. Khu vực này sẽ được lưu độc lập với bàn được gán (`requested_area_id`).
3. Khách nhấn "Đặt bàn".
4. Hệ thống kiểm tra đầu vào theo BR-03, BR-04, BR-05, BR-08, BR-09, BR-10, BR-11.
5. Trong transaction tạo đơn, hệ thống khóa row `account` của khách, đếm lại số đơn `PENDING`/`CONFIRMED` và kiểm tra không có `reservation_range` nào của chính khách chồng `[start_time,end_time)` mới (BR-27, BR-28).
6. Hệ thống chọn bàn theo BR-12, BR-13, BR-14, BR-16; khóa row `restaurant_table` của bàn đó, kiểm tra lại không có reservation chiếm bàn và không có `table_suspension` chồng `occupied_range` ứng viên, rồi mới tạo đơn. `requested_area_id` được lưu theo lựa chọn ở bước 2 (BR-14, BR-29, BR-30).
7. Hệ thống xác định trạng thái theo BR-20, sinh mã đặt bàn (BR-17) và ghi nhật ký (BR-32).
8. Hệ thống hiển thị màn hình xác nhận gồm mã đặt bàn, ngày giờ, số người, khu vực và trạng thái. Với đơn `PENDING`, hiển thị rõ đơn đang chờ nhà hàng xác nhận.
9. Hệ thống gửi email theo UC-25 (bất đồng bộ).

**Luồng thay thế và ngoại lệ**
- 3a. Khách chưa đăng nhập: chuyển đến UC-02, sau khi đăng nhập quay lại form với dữ liệu đã chọn.
- 4a. Dữ liệu không hợp lệ (ngoài giờ mở cửa hoặc đóng cửa, thời lượng ngoài 60–180 phút, quá 12 người, ngày đóng cửa, quá gần hoặc quá xa, nhóm từ 7 người đặt chưa đủ 4 giờ trước giờ bắt đầu): báo lỗi cụ thể, giữ nguyên form.
- 5a. Sau khi khóa `account`, nếu đã đủ 3 đơn hoạt động hoặc `reservation_range` mới chồng đơn khác của chính khách: từ chối; với trường hợp chồng lịch có thể nêu mã đơn đang xung đột.
- 6a. Không còn bàn phù hợp, bàn bị reservation khác giữ hoặc vừa có `table_suspension` chồng khoảng trong lúc khách điền form: rollback, báo hết chỗ và gợi ý tối đa 3 khoảng thời gian gần nhất như UC-06 bước 4a.
- 6b. Hai yêu cầu đồng thời cho cùng một bàn: transaction và ràng buộc ở tầng database (BR-30) chỉ cho một yêu cầu thành công. Yêu cầu còn lại thử tiếp các bàn phù hợp khác trước khi báo hết chỗ theo 6a.
- 9a. Gửi email lỗi: chỉ ghi log, đơn vẫn hợp lệ (BR-46).

#### UC-09. Hủy đơn đặt bàn

| | |
|---|---|
| **Tác nhân** | Khách hàng |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-RSV-09; BR-19, BR-22, BR-32, BR-38, BR-44 |
| **Tiền điều kiện** | Đơn thuộc về khách, trạng thái `PENDING` hoặc `CONFIRMED`, còn ít nhất 2 giờ trước giờ bắt đầu |
| **Hậu điều kiện** | Đơn chuyển sang `CANCELLED`, bàn được giải phóng, nhật ký được ghi, email hủy được gửi |

**Luồng chính**
1. Khách mở chi tiết đơn từ UC-08 và nhấn "Hủy đơn".
2. Hệ thống kiểm tra quyền sở hữu và điều kiện hủy (BR-22, BR-38).
3. Hệ thống hiển thị hộp thoại xác nhận.
4. Khách xác nhận.
5. Hệ thống chuyển đơn sang `CANCELLED`, giải phóng bàn và ghi nhật ký.
6. Hệ thống gửi email hủy theo UC-25.

**Luồng thay thế và ngoại lệ**
- 2a. Còn dưới 2 giờ trước giờ bắt đầu: không cho hủy, hiển thị cách liên hệ nhà hàng.
- 2b. Đơn ở trạng thái không cho phép hủy (`ARRIVED`, `COMPLETED`, `NO_SHOW`, `CANCELLED`, `REJECTED`): thông báo trạng thái hiện tại.
- 4a. Khách chọn "Giữ đơn": đóng hộp thoại, không thay đổi gì.

#### UC-10. Đổi lịch đơn đặt bàn

| | |
|---|---|
| **Tác nhân** | Khách hàng |
| **Ưu tiên** | SHOULD |
| **Yêu cầu / quy tắc** | FR-RSV-10; BR-08, BR-09, BR-14, BR-16, BR-20, BR-22, BR-23, BR-24, BR-28, BR-29, BR-30, BR-32; G-01, G-02, G-06, G-12, G-14, G-16 |
| **Tiền điều kiện** | Đơn thuộc về khách, trạng thái `PENDING` hoặc `CONFIRMED`, còn ít nhất 2 giờ trước giờ bắt đầu hiện tại, đã đổi dưới 2 lần |
| **Hậu điều kiện (thành công)** | Đơn được cập nhật ngày, giờ, thời lượng, số người; mã đặt bàn giữ nguyên; số lần đổi tăng 1; trạng thái xác định lại theo BR-20; nhật ký ghi lại. **Thất bại:** đơn cũ giữ nguyên hoàn toàn |

**Luồng chính**
1. Khách mở chi tiết đơn và nhấn "Đổi lịch".
2. Hệ thống kiểm tra quyền sở hữu, trạng thái, hạn 2 giờ và số lần đổi (BR-22, BR-23).
3. Khách chọn ngày, giờ, thời lượng, số người và khu vực yêu cầu mới (tùy chọn), dùng lưới giờ khả dụng như UC-06. Khoảng thời gian đang giữ của chính đơn này không bị tính là xung đột; `requested_area_id` mới sẽ được lưu nếu đổi lịch thành công.
4. Khách xác nhận.
5. Trong một transaction, hệ thống kiểm tra đầu vào mới (BR-03, BR-04, BR-05, BR-08, BR-09, BR-10, BR-11), kiểm tra `reservation_range` mới không chồng đơn khác của chính khách (loại trừ chính reservation đang đổi), ưu tiên giữ nguyên bàn hiện tại nếu vẫn phù hợp, không bị suspension và còn trống; nếu không thì chọn bàn theo BR-13/14/16. Trước khi cập nhật, khóa row bàn mục tiêu và kiểm tra lại reservation + `table_suspension` không chồng `occupied_range` mới (BR-28, BR-29, BR-30).
6. Hệ thống cập nhật đơn, `requested_area_id`, tăng số lần đổi, xác định lại trạng thái theo BR-20 và ghi nhật ký. Nếu đang `CONFIRMED` mà số người mới thuộc nhóm cần duyệt, chuyển `CONFIRMED → PENDING` theo bảng trạng thái đã bổ sung.
7. Hệ thống hiển thị thông tin mới và gửi email đổi lịch theo UC-25.

**Luồng thay thế và ngoại lệ**
- 2a. Quá hạn 2 giờ, đã đủ 2 lần đổi hoặc trạng thái không cho phép: báo đúng lý do và không cho vào bước 3.
- 5a. Không còn bàn phù hợp cho thời gian mới: rollback, đơn cũ giữ nguyên, báo hết chỗ và gợi ý tối đa 3 khoảng thời gian gần nhất.
- 5b. Thông tin mới vi phạm quy tắc (ví dụ quá 12 người, giờ kết thúc quá giờ đóng cửa): báo lỗi cụ thể.
- 5c. Số người mới đưa đơn vào nhóm cần duyệt (từ 7 người): đơn chuyển về `PENDING` theo G-02 và khách được thông báo đơn cần nhà hàng xác nhận lại.

### 4.4 Vận hành phía nhà hàng

#### UC-12. Duyệt hoặc từ chối đơn chờ duyệt

| | |
|---|---|
| **Tác nhân** | Nhân viên, Quản trị |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-ADM-03; BR-19, BR-20, BR-21, BR-32, BR-44 |
| **Tiền điều kiện** | Đã đăng nhập với vai trò STAFF hoặc ADMIN; đơn ở trạng thái `PENDING` |
| **Hậu điều kiện** | Đơn chuyển sang `CONFIRMED` hoặc `REJECTED`; nhật ký được ghi; email được gửi |

**Luồng chính**
1. Nhân viên mở danh sách đơn và lọc trạng thái `PENDING` (UC-11).
2. Nhân viên mở chi tiết đơn để xem giờ, số người, ghi chú đặc biệt và bàn đã được giữ.
3. Nhân viên chọn "Duyệt" hoặc "Từ chối".
4. Nếu từ chối, nhân viên nhập lý do (bắt buộc).
5. Hệ thống chuyển trạng thái (`CONFIRMED` hoặc `REJECTED`), giải phóng bàn nếu từ chối, ghi nhật ký.
6. Hệ thống gửi email cho khách theo UC-25 (xác nhận hoặc từ chối kèm lý do).

**Luồng thay thế và ngoại lệ**
- 3a. Nhân viên thấy bàn được giữ chưa phù hợp với nhóm: dùng UC-16 để chuyển bàn rồi quay lại duyệt.
- 4a. Từ chối nhưng để trống lý do: hệ thống chặn và yêu cầu nhập.
- 5a. Đơn đã bị hủy hoặc tự động hủy (UC-26) trong lúc nhân viên đang xem: báo trạng thái đã thay đổi và tải lại danh sách.

#### UC-13. Đánh dấu khách đến, hoàn thành, không đến

| | |
|---|---|
| **Tác nhân** | Nhân viên, Quản trị |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-ADM-04; BR-18, BR-19, BR-25, BR-26, BR-32 |
| **Tiền điều kiện** | Đã đăng nhập với vai trò STAFF hoặc ADMIN; đơn ở trạng thái phù hợp |
| **Hậu điều kiện** | Đơn chuyển sang trạng thái mới đúng bảng chuyển trạng thái; nhật ký được ghi |

**Luồng chính**
1. Nhân viên tìm đơn theo mã đặt bàn, tên hoặc số điện thoại (UC-11), hoặc chọn từ timeline (UC-18).
2. Khi khách đến, nhân viên chọn "Khách đã đến": `CONFIRMED` sang `ARRIVED`.
3. Khi khách ăn xong, nhân viên chọn "Hoàn thành": `ARRIVED` sang `COMPLETED`. Reservation không còn là đơn hoạt động nhưng `occupied_range` vẫn được giữ đến hết `end_time + buffer` để bảo toàn thời gian dọn bàn (G-12).
4. Hệ thống ghi nhật ký cho mỗi lần chuyển trạng thái.

**Luồng thay thế và ngoại lệ**
- 1a. Khách không đến sau giờ bắt đầu 15 phút: nhân viên chọn "Không đến", hệ thống hiển thị hộp thoại xác nhận rồi chuyển `CONFIRMED` sang `NO_SHOW`, giải phóng bàn cho phần thời gian còn lại, chỉ ghi nhận cho thống kê, không phạt (BR-25, BR-26).
- 2a. Đánh dấu "đã đến" ngoài ngày hẹn: hệ thống chặn.
- 1b. Chưa đủ 15 phút sau giờ bắt đầu mà chọn "Không đến": hệ thống chặn và nêu thời điểm sớm nhất được phép.
- 4a. Đánh dấu nhầm: `COMPLETED`, `NO_SHOW` là trạng thái cuối và không thể chuyển tiếp, vì vậy hệ thống luôn yêu cầu xác nhận trước khi chuyển sang các trạng thái này.

#### UC-14. Đặt bàn hộ khách

| | |
|---|---|
| **Tác nhân** | Nhân viên, Quản trị |
| **Ưu tiên** | SHOULD |
| **Yêu cầu / quy tắc** | FR-ADM-06; BR-03, BR-05, BR-09 đến BR-16, BR-29, BR-30, BR-39; G-03, G-14, G-16 |
| **Tiền điều kiện** | Đã đăng nhập với vai trò STAFF hoặc ADMIN |
| **Hậu điều kiện** | Đơn được tạo, không gắn tài khoản, lưu họ tên và số điện thoại của khách; trạng thái `CONFIRMED` |

**Luồng chính**
1. Nhân viên chọn "Đặt bàn hộ".
2. Nhân viên nhập họ tên và số điện thoại khách, ngày, giờ bắt đầu, giờ kết thúc (thời lượng tự do trong giờ mở cửa), số người, khu vực (tùy chọn) và ghi chú.
3. Hệ thống kiểm tra đầu vào. Không áp dụng giới hạn thời lượng 60–180 phút và giới hạn đặt trước 2 giờ của khách (BR-04, BR-08).
4. Trong một transaction, hệ thống chọn bàn theo BR-12, BR-13, BR-14, BR-16; khóa row bàn, kiểm tra lại reservation và `table_suspension` không chồng `occupied_range`, lưu `requested_area_id` nếu nhân viên đã chọn khu vực và tạo đơn với trạng thái `CONFIRMED`.
5. Hệ thống ghi nhật ký và hiển thị mã đặt bàn để nhân viên đọc lại cho khách.

**Luồng thay thế và ngoại lệ**
- 3a. Số người lớn hơn 12, giờ ngoài giờ mở cửa hoặc ngày đóng cửa: báo lỗi cụ thể.
- 4a. Không còn bàn phù hợp: gợi ý tối đa 3 khoảng thời gian gần nhất; nhân viên có thể thử khu vực khác hoặc thời lượng khác.
- 4b. Đơn đặt hộ không lưu email nên không gửi email xác nhận; nhân viên xác nhận trực tiếp với khách (G-03).

#### UC-16. Chuyển đơn sang bàn khác

| | |
|---|---|
| **Tác nhân** | Nhân viên, Quản trị |
| **Ưu tiên** | SHOULD |
| **Yêu cầu / quy tắc** | FR-ADM-07; BR-12, BR-14, BR-16, BR-19, BR-29, BR-30, BR-32; G-05, G-07, G-14, G-16 |
| **Tiền điều kiện** | Đơn ở trạng thái `PENDING`, `CONFIRMED` hoặc `ARRIVED` |
| **Hậu điều kiện** | Đơn được gán bàn mới; giờ và trạng thái giữ nguyên; nhật ký ghi lại việc chuyển bàn |

**Luồng chính**
1. Nhân viên mở chi tiết đơn và chọn "Chuyển bàn".
2. Hệ thống liệt kê các bàn phù hợp với số người, đang hoạt động, không bị `table_suspension` chồng và còn trống trong toàn bộ `occupied_range` của đơn. Bàn thuộc khu vực khác `requested_area_id` được đánh dấu cảnh báo.
3. Nhân viên chọn bàn mới và xác nhận.
4. Trong một transaction, hệ thống khóa row bàn mới, kiểm tra lại reservation và `table_suspension` không chồng (BR-16, BR-29), rồi cập nhật `table_id`. `requested_area_id` giữ nguyên.
5. Hệ thống ghi nhật ký.

**Luồng thay thế và ngoại lệ**
- 2a. Không có bàn phù hợp nào còn trống: báo cho nhân viên, gợi ý liên hệ khách để đổi lịch.
- 4a. Bàn vừa bị đơn khác giữ trong lúc chọn: rollback, tải lại danh sách bàn.

#### UC-17. Gia hạn hoặc rút ngắn thời lượng

| | |
|---|---|
| **Tác nhân** | Nhân viên, Quản trị |
| **Ưu tiên** | SHOULD |
| **Yêu cầu / quy tắc** | FR-ADM-10; BR-05, BR-06, BR-07, BR-29, BR-30, BR-32, BR-33; G-05, G-11 |
| **Tiền điều kiện** | Đơn ở trạng thái `CONFIRMED` hoặc `ARRIVED` |
| **Hậu điều kiện** | Giờ kết thúc và khoảng bị chiếm của đơn được cập nhật; nhật ký ghi lại |

**Luồng chính**
1. Nhân viên mở chi tiết đơn và chọn "Đổi thời lượng".
2. Nhân viên nhập giờ kết thúc mới (bước 15 phút, sau giờ bắt đầu, không muộn hơn giờ đóng cửa).
3. Hệ thống tính khoảng bị chiếm mới (đến giờ kết thúc mới cộng 15 phút đệm) và kiểm tra không chồng đơn giữ chỗ khác trên cùng bàn (BR-33).
4. Hệ thống cập nhật đơn và ghi nhật ký.

**Luồng thay thế và ngoại lệ**
- 2a. Giờ kết thúc mới vượt giờ đóng cửa hoặc không sau giờ bắt đầu: báo lỗi.
- 3a. Chồng đơn kế tiếp: báo xung đột kèm mã đơn và giờ bắt đầu của đơn đó, đồng thời gợi ý hai hướng: rút giờ kết thúc mới lại cho vừa khoảng trống, hoặc chuyển bàn (UC-16) cho đơn này hoặc đơn kế tiếp.

### 4.5 Hệ thống

#### UC-25. Gửi thông báo email

| | |
|---|---|
| **Tác nhân** | Hệ thống |
| **Ưu tiên** | SHOULD |
| **Yêu cầu / quy tắc** | FR-NTF-01, 02, 03; BR-44, BR-45, BR-46; G-03, G-04 |
| **Tiền điều kiện** | Sự kiện nghiệp vụ đã được ghi nhận thành công; khách có email (đơn của tài khoản) |
| **Hậu điều kiện** | Email được gửi hoặc lỗi được ghi log; nghiệp vụ không bị ảnh hưởng |

**Sự kiện kích hoạt**

| Sự kiện | Nội dung email |
|---|---|
| Đơn chuyển sang `CONFIRMED` (tự động hoặc do duyệt) | Xác nhận đặt bàn: mã, ngày giờ, số người, khu vực, liên kết xem đơn |
| Đơn bị hủy (do khách, nhân viên hoặc quá hạn duyệt) | Thông báo hủy và người hủy |
| Đơn bị từ chối | Thông báo từ chối kèm lý do |
| Đổi lịch thành công | Thông tin lịch mới |
| Còn khoảng 3 giờ trước giờ bắt đầu | Nhắc lịch, nhắc thêm rằng hạn hủy là trước giờ bắt đầu 2 giờ |

**Luồng chính**
1. Sự kiện xảy ra trong một transaction nghiệp vụ.
2. Sau khi transaction commit, hệ thống đưa tác vụ gửi email vào hàng đợi bất đồng bộ (BR-46).
3. Hệ thống dựng email theo mẫu và gửi tới email của khách.
4. Hệ thống ghi log kết quả gửi.

**Luồng nhắc lịch (job định kỳ)**
1. Job quét các đơn `CONFIRMED` có giờ bắt đầu trong khoảng 3 giờ tới và chưa gửi nhắc.
2. Với mỗi đơn, gửi email nhắc lịch rồi đánh dấu đã gửi để không gửi lặp.

**Luồng thay thế và ngoại lệ**
- 3a. Gửi lỗi: ghi log, có thể thử lại một số lần, không ảnh hưởng trạng thái đơn.
- 3b. Đơn đặt hộ không có email: bỏ qua, không coi là lỗi.
- Đơn được tạo khi còn dưới 3 giờ trước giờ bắt đầu: không gửi nhắc lịch vì khách vừa nhận email xác nhận.

#### UC-26. Tự động hủy đơn chờ duyệt quá hạn

| | |
|---|---|
| **Tác nhân** | Hệ thống (job định kỳ) |
| **Ưu tiên** | SHOULD |
| **Yêu cầu / quy tắc** | FR-SYS-01; BR-19, BR-21, BR-32, BR-44; G-01, G-10 |
| **Tiền điều kiện** | Có đơn `PENDING` chưa được duyệt |
| **Hậu điều kiện** | Các đơn quá hạn chuyển sang `CANCELLED`, bàn được giải phóng, khách nhận email |

**Luồng chính**
1. Job chạy định kỳ (khoảng vài phút một lần, cấu hình được).
2. Job tìm các đơn `PENDING` có giờ bắt đầu cách hiện tại không quá 3 giờ.
3. Với mỗi đơn, hệ thống chuyển sang `CANCELLED`, ghi nhật ký (người thực hiện là hệ thống, lý do hết hạn duyệt) và giải phóng bàn.
4. Hệ thống gửi email hủy theo UC-25.

Ghi chú: nhờ quy tắc đặt trước tối thiểu 4 giờ cho đơn cần duyệt (BR-08), nhân viên luôn có ít nhất 1 giờ để duyệt trước khi đơn bị tự hủy.

**Luồng thay thế và ngoại lệ**
- 3a. Nhân viên vừa duyệt hoặc từ chối cùng lúc: job chỉ hủy khi đơn vẫn còn `PENDING` tại thời điểm cập nhật, tránh ghi đè trạng thái.

### 4.6 Đặt món giao hàng

#### UC-35. Đặt món giao hàng cơ bản

| | |
|---|---|
| **Tác nhân** | Khách hàng (CUSTOMER) |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-ORD-01 đến FR-ORD-05; BR-49 đến BR-54 |
| **Tiền điều kiện** | Người dùng đã đăng nhập với vai trò CUSTOMER; menu có món để lựa chọn |
| **Hậu điều kiện (thành công)** | Một đơn món giao hàng được ghi nhận với `customer_id`, địa chỉ, số điện thoại, `status=PLACED`, `payment_method=COD` và danh sách món/số lượng/`unit_price` snapshot; giao diện báo đặt hàng thành công |

**Luồng chính**
1. Khách hàng xem menu và chọn một hoặc nhiều món, kèm số lượng.
2. Khách hàng mở bước thông tin giao hàng.
3. Khách hàng nhập **địa chỉ** và **số điện thoại** nhận hàng.
4. Hệ thống hiển thị bước xem lại đơn; phương thức thanh toán là **COD**.
5. Khách hàng nhấn gửi đơn.
6. Hệ thống kiểm tra BR-50, xác thực tài khoản là CUSTOMER, tải giá hiện tại của từng `Dish`, rồi tạo `food_order` ở trạng thái `PLACED` cùng các `food_order_item`; mỗi item lưu `unit_price = dish.price` tại thời điểm tạo theo BR-52.
7. Khi transaction tạo đơn thành công, hệ thống hiển thị: **“Đặt hàng thành công. Vui lòng chờ giao hàng.”**

**Luồng thay thế và ngoại lệ**
- 6a. Tài khoản không phải CUSTOMER hoặc phiên đăng nhập không hợp lệ: từ chối tạo đơn.
- 6b. Thiếu địa chỉ/số điện thoại, không có item, món không tồn tại hoặc `quantity <= 0`: không tạo đơn và trả lỗi validation.
- 6c. Ghi dữ liệu thất bại: rollback toàn bộ order + items; không hiển thị thông báo thành công.
- Không áp dụng thêm rule về phí/vùng giao, tồn kho, tracking hoặc sửa nội dung đơn; hủy đơn được xử lý riêng ở UC-38.

#### UC-36. Xem danh sách và chi tiết đơn món giao hàng

| | |
|---|---|
| **Tác nhân** | Nhân viên (STAFF), Quản trị (ADMIN) |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-ORD-06, FR-ORD-07; BR-52 đến BR-55 |
| **Tiền điều kiện** | Người dùng đã đăng nhập với vai trò STAFF hoặc ADMIN |
| **Hậu điều kiện** | Không thay đổi dữ liệu; chỉ đọc danh sách hoặc chi tiết đơn |

**Luồng chính**
1. STAFF/ADMIN mở danh sách đơn món giao hàng.
2. Hệ thống hiển thị các đơn với thông tin cơ bản như mã nội bộ/id, thời điểm tạo, khách hàng, số điện thoại nhận hàng, trạng thái (`PLACED` hoặc `CANCELLED`) và phương thức `COD`.
3. Người dùng chọn một đơn.
4. Hệ thống hiển thị địa chỉ nhận hàng và danh sách món/số lượng/`unit_price`; giá và tổng được tính từ giá snapshot theo BR-52.
5. Giao diện **không** hiển thị hành động cập nhật trạng thái hoặc chỉnh sửa đơn.

**Luồng thay thế và ngoại lệ**
- Không tìm thấy đơn: trả 404/hiển thị trạng thái không tồn tại.
- CUSTOMER hoặc khách chưa đăng nhập truy cập API quản trị đơn: từ chối theo RBAC.

#### UC-37. Xem lịch sử và chi tiết đơn món của tôi

| | |
|---|---|
| **Tác nhân** | Khách hàng (CUSTOMER) |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-ORD-08, FR-ORD-09; BR-52, BR-53, BR-54, BR-57 |
| **Tiền điều kiện** | Người dùng đã đăng nhập với vai trò CUSTOMER |
| **Hậu điều kiện** | Không thay đổi dữ liệu; CUSTOMER chỉ xem được đơn thuộc chính mình |

**Luồng chính**
1. CUSTOMER mở trang **Đơn món của tôi**.
2. Hệ thống lấy các `food_order` có `customer_id` bằng tài khoản hiện tại, sắp xếp mới nhất trước.
3. Mỗi dòng hiển thị tối thiểu id/mã nội bộ, thời điểm tạo, trạng thái, COD và tổng đơn tính từ `SUM(unit_price * quantity)`.
4. CUSTOMER chọn một đơn.
5. Hệ thống kiểm tra ownership rồi hiển thị địa chỉ, số điện thoại, trạng thái, thời điểm tạo/hủy (nếu có), danh sách món, số lượng, `unit_price`, thành tiền từng dòng và tổng đơn.
6. Nếu đơn còn `PLACED`, giao diện hiển thị hành động **Hủy đơn** để chuyển sang UC-38.

**Luồng thay thế và ngoại lệ**
- Không có đơn: hiển thị empty state, không coi là lỗi.
- Truy cập id của đơn thuộc CUSTOMER khác: trả 404 hoặc từ chối theo chính sách không lộ dữ liệu; không trả nội dung đơn.

#### UC-38. Hủy đơn món của tôi

| | |
|---|---|
| **Tác nhân** | Khách hàng (CUSTOMER) |
| **Ưu tiên** | MUST |
| **Yêu cầu / quy tắc** | FR-ORD-10; BR-53, BR-57, BR-58 |
| **Tiền điều kiện** | CUSTOMER đã đăng nhập; đơn thuộc chính CUSTOMER và đang ở trạng thái `PLACED` |
| **Hậu điều kiện (thành công)** | Đơn chuyển `PLACED → CANCELLED`, `cancelled_at` được ghi; items và giá snapshot giữ nguyên |

**Luồng chính**
1. CUSTOMER mở chi tiết đơn từ UC-37 và nhấn **Hủy đơn**.
2. Hệ thống hiển thị hộp thoại xác nhận.
3. CUSTOMER xác nhận hủy.
4. Trong transaction, hệ thống tải lại đơn, kiểm tra ownership và status hiện tại.
5. Nếu vẫn là `PLACED`, hệ thống cập nhật `status=CANCELLED`, `cancelled_at=now()` rồi commit.
6. Giao diện hiển thị trạng thái `CANCELLED`; đơn vẫn còn trong lịch sử và giữ nguyên `unit_price` của các item.

**Luồng thay thế và ngoại lệ**
- Đơn không thuộc CUSTOMER hiện tại: từ chối/404, không thay đổi dữ liệu.
- Đơn đã `CANCELLED`: không thực hiện cập nhật lần hai; tải lại trạng thái hiện tại.
- CUSTOMER đóng hộp thoại xác nhận: không thay đổi dữ liệu.

## 5. Đặc tả rút gọn

### 5.1 Khách hàng và nhân viên

| Mã | Use case | Luồng chính | Ngoại lệ chính | Quy tắc |
|---|---|---|---|---|
| UC-03 | Quản lý hồ sơ và mật khẩu | Cập nhật họ tên, số điện thoại; đổi mật khẩu bằng cách nhập mật khẩu cũ và mới; quên mật khẩu: nhập email, hệ thống gửi liên kết đặt lại có thời hạn, khách đặt mật khẩu mới | Mật khẩu cũ sai; mật khẩu mới không đạt yêu cầu; liên kết hết hạn; email không tồn tại (không tiết lộ điều này cho người nhập) | BR-34, 35, 36 |
| UC-04 | Xem thông tin nhà hàng | Hiển thị giới thiệu, địa chỉ, liên hệ, giờ mở cửa theo thứ, các ngày đóng cửa sắp tới, ảnh không gian | Chưa có dữ liệu: hiển thị mục trống | BR-02, 10 |
| UC-05 | Xem, tìm kiếm và lọc menu | Chọn danh mục để xem món; tìm theo tên; lọc theo danh mục, khoảng giá, nhãn; mở chi tiết món (ảnh, mô tả, giá, nhãn) | Không có kết quả: hiển thị gợi ý bỏ bớt bộ lọc. Món `SOLD_OUT` hiển thị mờ ghi "Hết món", món `HIDDEN` không hiển thị | BR-40, 41, 42, 43 |
| UC-08 | Xem đơn đặt bàn của tôi | Hiển thị hai nhóm "Sắp tới" và "Lịch sử"; chi tiết đơn gồm mã, ngày giờ, số người, khu vực, trạng thái, ghi chú, số lần đổi còn lại; hiện nút Hủy hoặc Đổi lịch khi đủ điều kiện | Truy cập đơn của người khác: từ chối (không lộ thông tin) | BR-22, 23, 38 |
| UC-11 | Xem và lọc danh sách đơn | Lọc theo ngày, trạng thái, khu vực, tên, số điện thoại, mã đặt bàn; mở chi tiết đơn gồm ghi chú, bàn, khoảng thời gian, lịch sử trạng thái | Không có kết quả; STAFF không xem được dữ liệu ngoài phạm vi vai trò | BR-38 |
| UC-15 | Hủy đơn thay khách | Chọn đơn `PENDING` hoặc `CONFIRMED`, nhập lý do (bắt buộc), xác nhận; hệ thống chuyển `CANCELLED`, giải phóng bàn, ghi nhật ký, gửi email nếu có | Đơn `ARRIVED` hoặc trạng thái cuối không hủy được; thiếu lý do; không bị giới hạn 2 giờ như khách | BR-19, 32, 44 |
| UC-18 | Xem lịch đặt bàn dạng timeline | Chọn ngày; lưới có hàng là bàn (nhóm theo khu vực), cột là giờ theo bước 15 phút trong giờ mở cửa; mỗi đơn là một khối hiển thị mã, tên, số người, trạng thái; phần đệm hiển thị nhạt hơn; bàn tạm ngưng hiển thị xám; bấm vào khối để mở chi tiết | Ngày đóng cửa: hiển thị thông báo | BR-02, 06, 07, 16 |
| UC-19 | Xem nhật ký thay đổi của đơn | Hiển thị danh sách sự kiện: thời điểm, người thực hiện, loại sự kiện, giá trị trước và sau, lý do | Đơn chưa có sự kiện nào ngoài lúc tạo | BR-32; G-05 |
| UC-36 | Xem danh sách và chi tiết đơn món giao hàng | STAFF/ADMIN xem danh sách, mở chi tiết địa chỉ, số điện thoại và món/số lượng/giá snapshot; dữ liệu chỉ đọc | Không tìm thấy đơn; CUSTOMER/khách vãng lai bị từ chối | BR-52 đến BR-55 |
| UC-37 | Xem lịch sử và chi tiết đơn món của tôi | CUSTOMER xem danh sách đơn của chính mình mới nhất trước, mở chi tiết món, `unit_price`, tổng và trạng thái | Không có đơn; truy cập đơn người khác bị từ chối/404 | BR-52, 53, 54, 57 |
| UC-38 | Hủy đơn món của tôi | Từ chi tiết đơn `PLACED`, CUSTOMER xác nhận hủy; hệ thống chuyển `CANCELLED` và ghi `cancelled_at` | Không sở hữu đơn; đơn đã `CANCELLED`; người dùng bỏ xác nhận | BR-53, 57, 58 |

### 5.2 Quản trị

| Mã | Use case | Luồng chính | Ngoại lệ chính | Quy tắc |
|---|---|---|---|---|
| UC-20 | Quản lý danh mục, món và nhãn | Thêm, sửa, ngừng dùng danh mục; thêm, sửa, ngừng dùng món (tên, mô tả, giá, URL ảnh, danh mục, nhãn); đổi trạng thái món `AVAILABLE`, `SOLD_OUT`, `HIDDEN`; quản lý danh sách nhãn và gán nhãn cho món | Giá không hợp lệ (không phải số nguyên lớn hơn 0); ngừng dùng danh mục còn món; xóa cứng dữ liệu đã được tham chiếu (chuyển sang ngừng dùng) | BR-31, 40, 41, 42, 43 |
| UC-21 | Quản lý khu vực và bàn | Thêm, sửa, ngừng dùng khu vực; thêm, sửa, ngừng dùng bàn; tạm ngưng bàn trong một khoảng thời gian. Khi tạo/sửa tạm ngưng, hệ thống khóa row `restaurant_table`, kiểm tra các reservation bị ảnh hưởng rồi mới ghi suspension | Sức chứa tối thiểu lớn hơn tối đa; mã bàn trùng; tạm ngưng hoặc ngừng dùng bàn đang có đơn giữ chỗ trong khoảng đó: cảnh báo và liệt kê các đơn bị ảnh hưởng, không tự hủy (G-08). Lock row bàn dùng chung với luồng booking để không xảy ra race | BR-12, 16, 31, 48 |
| UC-22 | Quản lý thông tin, giờ mở cửa, ngày đóng cửa | Cập nhật thông tin nhà hàng; đặt giờ mở cửa và đóng cửa cho từng thứ trong tuần; thêm, sửa, xóa ngày đóng cửa đặc biệt | Giờ đóng cửa không sau giờ mở cửa; thay đổi làm một số đơn đang giữ chỗ nằm ngoài giờ hoặc rơi vào ngày đóng cửa: cảnh báo và liệt kê đơn, không tự hủy (G-08) | BR-02, 05, 10 |
| UC-23 | Quản lý tài khoản người dùng | Xem danh sách, tìm theo email, tên, số điện thoại; khóa hoặc mở khóa tài khoản; đổi vai trò giữa CUSTOMER, STAFF, ADMIN | Quản trị tự khóa hoặc tự hạ quyền chính mình: hệ thống chặn (G-09) | BR-38 |
| UC-24 | Xem thống kê | Chọn khoảng ngày; hiển thị số lượt đặt theo ngày và tuần; (COULD) giờ cao điểm, tỷ lệ lấp đầy, tỷ lệ hủy và không đến | Khoảng ngày không có dữ liệu; định nghĩa chính xác của tỷ lệ lấp đầy sẽ chốt khi thiết kế truy vấn | BR-18, 26 |

### 5.3 Use case ưu tiên COULD (chưa đặc tả)

| Mã | Use case | Tác nhân | Yêu cầu liên quan |
|---|---|---|---|
| UC-27 | Xác thực email khi đăng ký | Khách hàng | FR-AUTH-07 |
| UC-28 | Đăng nhập bằng Google | Khách hàng | FR-AUTH-09 |
| UC-29 | Đánh dấu món yêu thích | Khách hàng | FR-MENU-11 |
| UC-30 | Sắp xếp thứ tự hiển thị menu | Quản trị | FR-MENU-09 |
| UC-31 | Tải ảnh món lên từ máy | Quản trị | FR-MENU-10 |
| UC-32 | Xem sơ đồ bàn trực quan | Quản trị, khách | FR-TBL-04 |
| UC-33 | Thông báo trong ứng dụng | Hệ thống | FR-NTF-04 |
| UC-34 | Đánh giá sau bữa ăn, xem và ẩn đánh giá | Khách hàng, Quản trị | FR-REV-01, 02, 03 |

## 6. Các quyết định bổ sung (đã xác nhận)

Các chỗ tài liệu 02 chưa nói rõ hoặc mâu thuẫn, phát hiện khi viết use case, đã được xác nhận và cập nhật vào tài liệu 02. G-19 ghi nhận phạm vi food-order đã chốt ở phiên bản 1.3; các tính năng ngoài phạm vi hiện tại được liệt kê trong tài liệu 07 và không được tự suy diễn thành yêu cầu.

| Mã | Vấn đề | Quyết định | Đã cập nhật ở tài liệu 02 |
|---|---|---|---|
| G-01 | Xung đột giữa BR-08 (đặt trước tối thiểu 2 giờ) và BR-21 (tự hủy đơn chờ duyệt khi còn 3 giờ) | Đơn cần duyệt (từ 7 người) phải đặt trước tối thiểu 4 giờ để nhân viên còn ít nhất 1 giờ duyệt; thời hạn là cấu hình; áp dụng cả khi đổi lịch | BR-08 |
| G-02 | Trạng thái sau khi đổi lịch | Xác định lại theo BR-20 dựa trên số người mới: đến 6 người là `CONFIRMED`, từ 7 người là `PENDING` để duyệt lại | BR-24 |
| G-03 | Đơn đặt hộ: trạng thái ban đầu, giới hạn, email | Trạng thái ban đầu `CONFIRMED`; không áp dụng giới hạn thời lượng 60–180 phút và đặt trước 2 giờ; tối đa 12 người; không lưu email nên không gửi email | BR-39 |
| G-04 | Đơn `PENDING` có gửi email không | Không gửi cho đến khi có kết quả; email xác nhận gửi khi đơn thành `CONFIRMED`; khách xem trạng thái ở màn hình xác nhận và trang "Đơn của tôi" | BR-44 |
| G-05 | Phạm vi nhật ký đơn | Ghi mọi thay đổi trên đơn: tạo, đổi lịch, chuyển bàn, đổi thời lượng, đổi trạng thái; mỗi dòng có người thực hiện, thời điểm, loại sự kiện, giá trị trước và sau, lý do | BR-32 |
| G-06 | Đổi lịch và xung đột với chính đơn | Khoảng thời gian đang giữ của chính đơn không bị tính là xung đột; ưu tiên giữ nguyên bàn hiện tại nếu còn phù hợp và còn trống | BR-24 |
| G-07 | Chuyển bàn sang khu vực khác khu vực khách chọn | Được phép, có cảnh báo; việc chuyển bàn không gửi email cho khách | BR-14 |
| G-08 | Tạm ngưng bàn, đổi giờ mở cửa hoặc thêm ngày đóng cửa khi đã có đơn bị ảnh hưởng | Hệ thống cảnh báo và liệt kê các đơn bị ảnh hưởng, không tự hủy; nhân viên xử lý | BR-48 (mới) |
| G-09 | Quản trị tự khóa hoặc tự hạ quyền | Hệ thống chặn | BR-38 |
| G-10 | BR-21 chưa có yêu cầu chức năng | Thêm FR-SYS-01 (SHOULD) | Mục 3.10 (mới) |
| G-11 | Trạng thái áp dụng cho gia hạn hoặc rút ngắn thời lượng | `CONFIRMED` và `ARRIVED` | BR-33 |
| G-12 | `COMPLETED` làm mất buffer dọn bàn nếu bị loại ngay khỏi kiểm tra overlap | `COMPLETED` vẫn tham gia kiểm tra chiếm bàn trên `occupied_range` đến hết khoảng đã cam kết; chỉ `CANCELLED`, `REJECTED`, `NO_SHOW` giải phóng ngay | BR-19, BR-30 |
| G-13 | Giới hạn 3 đơn có race khi hai request cùng COUNT rồi INSERT | Khi khách tự đặt, khóa row `account` trước khi đếm đơn hoạt động và tạo reservation | BR-27 |
| G-14 | Tạm ngưng bàn chưa có cơ chế concurrency chung với booking | Mọi luồng gán/chuyển bàn và tạo/sửa suspension đều khóa cùng row `restaurant_table`, rồi kiểm tra reservation + suspension trong transaction | BR-13, BR-16, BR-48 |
| G-15 | Bảng chuyển trạng thái thiếu `CONFIRMED → PENDING` khi đổi lịch sang nhóm cần duyệt | Bổ sung transition do hệ thống thực hiện sau reschedule thành công | BR-24, bảng trạng thái |
| G-16 | Khu vực khách yêu cầu không thể suy ra tin cậy sau khi staff chuyển bàn | Lưu `requested_area_id` nullable trên reservation; chuyển bàn không thay đổi giá trị này | BR-14 |
| G-17 | Constraint lịch khách dùng `occupied_range` khiến buffer bàn chặn lịch khách | Dùng `reservation_range = [start_time,end_time)` riêng cho BR-28; `occupied_range` chỉ dùng cho tài nguyên bàn | BR-28, BR-30 |
| G-18 | JPA có `@Version` nhưng ERD chưa có cột tương ứng | Schema `reservation` phải có `version BIGINT NOT NULL DEFAULT 0` | Thiết kế DB/JPA |
| G-19 | Phạm vi đặt món giao hàng hiện tại | Bắt buộc CUSTOMER đăng nhập; địa chỉ + số điện thoại; COD; item snapshot `unit_price`; CUSTOMER xem lịch sử/chi tiết và hủy đơn của mình khi `PLACED`; trạng thái `PLACED`/`CANCELLED`; STAFF/ADMIN chỉ xem danh sách/chi tiết, không cập nhật trạng thái | BR-49 đến BR-58 |
| G-20 | JWT MVP có refresh token không? | Không. Chỉ access token TTL 1 giờ; logout phía client; hết hạn thì đăng nhập lại | BR-37, BR-64; tài liệu 09 |
| G-21 | Token role hay DB role là nguồn phân quyền? | DB role hiện tại là nguồn authority; role claim phải khớp, nếu khác thì token stale và trả `401` | BR-63; tài liệu 09 |
| G-22 | Account bị khóa sau khi cấp token xử lý thế nào? | Mỗi request protected tải Account theo `sub`; account bị khóa/không tồn tại trả `401` ngay, không chờ token hết hạn | BR-62; tài liệu 09 |

## 7. Ma trận truy vết yêu cầu sang use case

| Yêu cầu | Use case |
|---|---|
| FR-AUTH-01 | UC-01 |
| FR-AUTH-02, 03, 10, 11, 12 | UC-02 (đăng nhập/JWT/phân quyền áp dụng cho mọi use case protected) |
| FR-AUTH-04, 05, 06 | UC-03 |
| FR-AUTH-07 | UC-27 |
| FR-AUTH-08 | UC-23 |
| FR-AUTH-09 | UC-28 |
| FR-INFO-01 | UC-04 |
| FR-INFO-02, 03 | UC-22 |
| FR-MENU-01, 02, 03, 04 | UC-05 |
| FR-MENU-05, 06, 07, 08 | UC-20 |
| FR-MENU-09 | UC-30 |
| FR-MENU-10 | UC-31 |
| FR-MENU-11 | UC-29 |
| FR-TBL-01, 02, 03 | UC-21 |
| FR-TBL-04 | UC-32 |
| FR-RSV-01, 02, 03 | UC-06 |
| FR-RSV-04, 05, 06, 07, 11 | UC-07 |
| FR-RSV-08 | UC-08 |
| FR-RSV-09 | UC-09 |
| FR-RSV-10 | UC-10 |
| FR-ADM-01, 02 | UC-11 |
| FR-ADM-03 | UC-12 |
| FR-ADM-04 | UC-13 |
| FR-ADM-05 | UC-18 |
| FR-ADM-06 | UC-14 |
| FR-ADM-07 | UC-16 |
| FR-ADM-08 | UC-15 |
| FR-ADM-09 | UC-19 |
| FR-ADM-10 | UC-17 |
| FR-NTF-01, 02, 03 | UC-25 |
| FR-NTF-04 | UC-33 |
| FR-REV-01, 02, 03 | UC-34 |
| FR-RPT-01, 02, 03 | UC-24 |
| FR-SYS-01 | UC-26 |
| FR-ORD-01, 02, 03, 04, 05 | UC-35 |
| FR-ORD-06, 07 | UC-36 |
| FR-ORD-08, 09 | UC-37 |
| FR-ORD-10 | UC-38 |

Toàn bộ **72 yêu cầu chức năng** trong tài liệu 02 (gồm FR-SYS-01 và 10 yêu cầu FR-ORD) đều có ít nhất một use case tương ứng.


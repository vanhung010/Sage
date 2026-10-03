# Saga Res: Đặt món giao hàng — đặc tả cơ bản đã chốt

| | |
|---|---|
| **Dự án** | Saga Res (nhà hàng demo: Sage) |
| **Phiên bản** | 1.0 (baseline lịch sử) |
| **Ngày cập nhật** | 2026-10-02 |
| **Trạng thái** | Baseline đã được mở rộng; các quyết định mới về history/cancel/snapshot giá nằm ở tài liệu 08 |
| **Tài liệu liên quan** | `01-project-summary.md`, `02-functional-requirements-and-business-rules.md`, `03-use-cases.md`, `04-erd-database-design.md`, `05-jpa-entities.md`, `08-food-ordering-customer-history-cancellation-price-snapshot.md` |

---

## 1. Mục tiêu

Đây là **baseline 1.0** của luồng đặt món giao hàng. Các quyết định trong tài liệu này được giữ để theo dõi lịch sử; phiên bản hiện hành đã được mở rộng bởi `08-food-ordering-customer-history-cancellation-price-snapshot.md`. Khi có mâu thuẫn giữa 07 và 08, dùng tài liệu 08 và các tài liệu 01–05 phiên bản mới hơn.

## 2. Các quyết định đã xác nhận

| Mã | Quyết định | Kết quả |
|---|---|---|
| ORD-D01 | Có bắt buộc đăng nhập không? | **Có.** Chỉ tài khoản `CUSTOMER` đã đăng nhập được tạo đơn món giao hàng |
| ORD-D02 | Có snapshot giá món trên order item không? | **Không.** `food_order_item` chỉ tham chiếu `dish`; giá đọc từ `dish.price` hiện tại |
| ORD-D03 | Trạng thái đơn ở phiên bản đầu | Chỉ có `PLACED` |
| ORD-D04 | STAFF/ADMIN xử lý đơn thế nào? | Chỉ **xem danh sách và chi tiết**, không cập nhật trạng thái |
| ORD-D05 | Thanh toán | Chỉ **COD — thanh toán khi nhận hàng** |

## 3. Luồng người dùng cơ bản

```text
Menu
  ↓
Chọn món + số lượng
  ↓
Đăng nhập CUSTOMER nếu chưa đăng nhập
  ↓
Thông tin nhận hàng
  - Địa chỉ
  - Số điện thoại
  ↓
Xem lại đơn
  - Món + số lượng
  - Thông tin nhận hàng
  - Thanh toán: COD
  ↓
Gửi đơn
  ↓
Backend tạo FoodOrder(status = PLACED, paymentMethod = COD)
  + FoodOrderItem[]
  ↓
“Đặt hàng thành công. Vui lòng chờ giao hàng.”
```

Không hiển thị trạng thái giao hàng tiếp theo vì phiên bản này chưa có state machine sau `PLACED`.

## 4. Dữ liệu đầu vào tối thiểu

Payload nghiệp vụ ở mức khái niệm:

```text
items[]
  dishId
  quantity

deliveryInfo
  address
  phone
```

Thông tin người đặt được lấy từ phiên đăng nhập hiện tại, không nhận `customerId` tùy ý từ client để tránh đặt đơn dưới danh nghĩa tài khoản khác.

Validation tối thiểu:

- Actor phải đăng nhập và có role `CUSTOMER`.
- Có ít nhất một item.
- `dishId` phải tham chiếu món tồn tại.
- `quantity > 0`.
- `address` không rỗng.
- `phone` không rỗng.
- Chỉ báo thành công sau khi transaction tạo order và toàn bộ items commit thành công.

Chưa áp dụng validation về vùng giao, khoảng cách, giá trị đơn tối thiểu, giới hạn số lượng, ETA hoặc tồn kho.

## 5. Mô hình dữ liệu tối thiểu

### 5.1 `food_order`

```text
id
customer_id        NOT NULL -> account.id
delivery_phone     NOT NULL
delivery_address   NOT NULL
status             NOT NULL = PLACED
payment_method     NOT NULL = COD
created_at         NOT NULL
```

Không có `total_amount`, `payment_status`, `delivery_fee`, `delivery_status` hoặc shipment data ở phiên bản này.

### 5.2 `food_order_item`

```text
id
food_order_id      NOT NULL -> food_order.id
dish_id            NOT NULL -> dish.id
quantity           NOT NULL > 0
```

**Không có `unit_price`.** Đây là quyết định chủ động cho phiên bản cơ bản, không phải thiếu sót schema.

## 6. Quy tắc giá và COD

Giá hiển thị cho một order item được lấy từ `dish.price` tại thời điểm đọc dữ liệu. Tổng tạm tính, nếu UI cần hiển thị, được tính từ:

```text
SUM(dish.price * food_order_item.quantity)
```

Hệ quả của quyết định không snapshot giá:

- Nếu ADMIN đổi `dish.price`, đơn cũ có thể hiển thị giá/tổng khác so với lúc khách bấm đặt.
- Dữ liệu food order hiện tại **không phù hợp để đối soát doanh thu hoặc chứng minh giá lịch sử**.
- COD ở phiên bản này chỉ mô tả **phương thức thanh toán**, chưa có payment transaction hay số tiền thanh toán cố định được lưu riêng.

Khi dự án cần lịch sử giá hoặc đối soát tiền thật, phải bổ sung snapshot `unit_price` hoặc một mô hình pricing/payment khác bằng migration mới.

## 7. Quyền truy cập

| Tác nhân | Quyền |
|---|---|
| Khách vãng lai | Xem menu; không tạo food order |
| CUSTOMER | Tạo food order |
| STAFF | Xem danh sách và chi tiết food order; read-only |
| ADMIN | Xem danh sách và chi tiết food order; read-only |

CUSTOMER chưa có trang lịch sử đơn giao hàng ở phiên bản này. STAFF/ADMIN không có nút hoặc API command để đổi `PLACED` sang trạng thái khác.

## 8. Luồng STAFF/ADMIN

```text
Danh sách đơn món
  ↓
Chọn một đơn
  ↓
Chi tiết
  - Thời điểm tạo
  - Khách hàng
  - Số điện thoại nhận hàng
  - Địa chỉ nhận hàng
  - Status: PLACED
  - Payment: COD
  - Danh sách món + số lượng
  - Giá hiện tại của món (nếu UI hiển thị)
```

Luồng này hoàn toàn read-only.

## 9. Transaction tạo đơn

Việc tạo đơn phải nằm trong một transaction:

1. Xác thực actor là CUSTOMER.
2. Validate address, phone và danh sách item.
3. Tải các `Dish` theo `dishId` và xác nhận chúng tồn tại.
4. Tạo `food_order` với `status = PLACED`, `payment_method = COD`.
5. Tạo toàn bộ `food_order_item`.
6. Commit.
7. Chỉ sau commit thành công mới trả kết quả đặt hàng thành công.

Nếu bất kỳ bước ghi dữ liệu nào lỗi, rollback cả order và items.

## 10. Nội dung cố ý để sau

Không tự bổ sung các tính năng sau nếu chưa có quyết định mới:

- `DELIVERING`, `COMPLETED`, `CANCELLED` hoặc state machine giao hàng.
- STAFF/ADMIN cập nhật trạng thái.
- Customer order history.
- Hủy/sửa đơn sau khi gửi.
- Phí giao hàng và vùng giao hàng.
- ETA và tracking shipper.
- Thanh toán online/payment gateway.
- Payment status hoặc đối soát COD.
- Snapshot giá và lịch sử giá.
- Inventory/nguyên liệu.
- Promotion/voucher.
- Email hoặc notification cho food order.
- Đặt món trước gắn với reservation/phục vụ tại bàn.

## 11. Tiêu chí hoàn thành phiên bản cơ bản

Phiên bản food ordering cơ bản được coi là hoàn thành khi:

1. Chỉ CUSTOMER đã đăng nhập có thể gửi đơn.
2. CUSTOMER chọn được ít nhất một món và số lượng hợp lệ.
3. CUSTOMER nhập được địa chỉ và số điện thoại nhận hàng.
4. Checkout thể hiện COD.
5. Backend lưu `food_order` + toàn bộ `food_order_item` trong một transaction.
6. Order mới có `status = PLACED` và `payment_method = COD`.
7. UI chỉ báo **“Đặt hàng thành công. Vui lòng chờ giao hàng.”** sau khi backend tạo đơn thành công.
8. STAFF/ADMIN xem được danh sách và chi tiết đơn.
9. STAFF/ADMIN không thể cập nhật trạng thái.
10. Schema/entity không có `unit_price` trên `food_order_item`.

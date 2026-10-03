# Saga Res: Đặt món giao hàng — lịch sử đơn, hủy đơn và snapshot giá

| | |
|---|---|
| **Dự án** | Saga Res (nhà hàng demo: Sage) |
| **Phiên bản** | 1.0 |
| **Ngày cập nhật** | 2026-10-02 |
| **Trạng thái** | Đã chốt thay đổi, dùng làm đặc tả hiện hành cho phần mở rộng food ordering |
| **Thay thế / mở rộng** | Mở rộng baseline `07-food-ordering-basic-specification.md` |
| **Tài liệu liên quan** | `01-project-summary.md`, `02-functional-requirements-and-business-rules.md`, `03-use-cases.md`, `04-erd-database-design.md`, `05-jpa-entities.md`, `07-food-ordering-basic-specification.md` |

---

## 1. Mục tiêu cập nhật

Phiên bản này mở rộng luồng đặt món giao hàng hiện có ở đúng ba điểm:

1. CUSTOMER có **lịch sử đơn món giao hàng** và được xem chi tiết các đơn của chính mình.
2. CUSTOMER có thể **hủy đơn của chính mình khi đơn còn `PLACED`**.
3. `food_order_item` lưu **`unit_price` snapshot** bằng giá món tại thời điểm tạo đơn, để lịch sử đơn không thay đổi khi ADMIN sửa `dish.price` sau này.

Các phần còn lại của baseline vẫn giữ nguyên: chỉ CUSTOMER đã đăng nhập được tạo đơn, thanh toán chỉ COD, STAFF/ADMIN chỉ xem danh sách/chi tiết và chưa có workflow vận hành giao hàng.

## 2. Quyết định mới đã xác nhận

| Mã | Vấn đề | Quyết định |
|---|---|---|
| ORD-D06 | CUSTOMER có xem lại đơn đã đặt không? | **Có.** Có danh sách lịch sử và trang chi tiết; chỉ xem đơn thuộc chính tài khoản hiện tại |
| ORD-D07 | CUSTOMER có được hủy đơn không? | **Có.** Chỉ đơn của chính mình và chỉ khi status hiện tại là `PLACED` |
| ORD-D08 | Cần trạng thái nào để hỗ trợ hủy? | Food order có `PLACED` và `CANCELLED`; transition hiện tại duy nhất là `PLACED → CANCELLED` |
| ORD-D09 | Có snapshot giá item không? | **Có.** `food_order_item.unit_price` bắt buộc, lấy từ `dish.price` lúc tạo item |
| ORD-D10 | Có lưu `total_amount` không? | **Chưa.** Tổng được tính từ `SUM(unit_price * quantity)` |
| ORD-D11 | Có cần log trạng thái food order riêng không? | **Chưa.** Hủy tối thiểu chỉ lưu `cancelled_at`; có thể bổ sung log khi workflow giao hàng mở rộng |

> Quyết định ORD-D09 **thay thế** quyết định ORD-D02 của baseline 07 về việc không snapshot giá. ORD-D08 cũng mở rộng ORD-D03: status không còn chỉ có `PLACED`.

## 3. Phạm vi chức năng hiện hành

### 3.1 CUSTOMER

CUSTOMER đã đăng nhập có thể:

- Xem menu và chi tiết món.
- Chọn một hoặc nhiều món và số lượng.
- Nhập địa chỉ, số điện thoại nhận hàng.
- Xem lại đơn và phương thức COD.
- Tạo food order mới ở trạng thái `PLACED`.
- Xem lịch sử các food order của chính mình, mới nhất trước.
- Xem chi tiết một food order của chính mình.
- Hủy food order của chính mình nếu status còn `PLACED`.

### 3.2 STAFF / ADMIN

STAFF và ADMIN vẫn chỉ:

- Xem danh sách food order.
- Xem chi tiết food order.
- Xem cả đơn `PLACED` và `CANCELLED`.

Không có command cập nhật trạng thái từ STAFF/ADMIN.

## 4. Luồng CUSTOMER cập nhật

### 4.1 Đặt món

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
  - Món
  - Số lượng
  - Giá hiện tại
  - Thành tiền
  - COD
  ↓
Gửi đơn
  ↓
Trong transaction:
  FoodOrder(status = PLACED, paymentMethod = COD)
  FoodOrderItem(unitPrice = Dish.price tại thời điểm tạo)
  ↓
“Đặt hàng thành công. Vui lòng chờ giao hàng.”
```

Sau khi tạo, thay đổi `dish.price` **không được làm thay đổi giá hiển thị của item trong đơn cũ**.

### 4.2 Lịch sử đơn

```text
Tài khoản / Đơn món của tôi
  ↓
Danh sách đơn của CUSTOMER hiện tại
  - Mới nhất trước
  - Thời điểm tạo
  - Trạng thái
  - COD
  - Tổng đơn theo giá snapshot
  ↓
Chọn một đơn
  ↓
Chi tiết
  - Địa chỉ, số điện thoại
  - PLACED / CANCELLED
  - Danh sách món
  - Số lượng
  - unit_price
  - Thành tiền từng dòng
  - Tổng đơn
  - cancelled_at nếu đã hủy
```

### 4.3 Hủy đơn

```text
Chi tiết đơn PLACED
  ↓
Hủy đơn
  ↓
Xác nhận
  ↓
Backend kiểm tra lại ownership + status trong transaction
  ↓
PLACED → CANCELLED
cancelled_at = now()
  ↓
Đơn vẫn nằm trong lịch sử
```

Vì phiên bản hiện tại chưa có `CONFIRMED`, `PREPARING`, `DELIVERING` hoặc trạng thái vận hành tương đương, rule hủy tối thiểu là: **hủy được miễn là status vẫn là `PLACED`**. Khi bổ sung workflow giao hàng, điều kiện hủy phải được thiết kế lại trước khi mở thêm state transition.

## 5. Yêu cầu chức năng mới

Các yêu cầu mới đã được đưa vào tài liệu 02:

| Mã | Nội dung |
|---|---|
| FR-ORD-08 | CUSTOMER xem lịch sử các đơn món của chính mình, mới nhất trước |
| FR-ORD-09 | CUSTOMER xem chi tiết một đơn của chính mình, gồm giá snapshot và tổng |
| FR-ORD-10 | CUSTOMER hủy đơn của chính mình khi còn `PLACED` |

FR-ORD-03, FR-ORD-04 và FR-ORD-07 cũng được cập nhật để sử dụng `unit_price` snapshot.

## 6. Quy tắc nghiệp vụ hiện hành

### 6.1 Ownership

- `food_order.customer_id` luôn NOT NULL.
- Client không được gửi một `customerId` tùy ý để tạo hoặc truy cập order.
- Tài khoản hiện tại được lấy từ authentication context.
- CUSTOMER list/detail/cancel phải luôn lọc theo `customer_id` của actor.
- Khi id tồn tại nhưng thuộc CUSTOMER khác, API không được trả dữ liệu đơn đó.

### 6.2 Snapshot giá

Khi tạo item:

```text
food_order_item.unit_price = dish.price
```

Ràng buộc:

- `unit_price` là số nguyên VND > 0.
- `unit_price` không thay đổi nếu `dish.price` thay đổi sau đó.
- Giá hiển thị trong lịch sử lấy từ `unit_price`.
- Thành tiền item:

```text
line_total = unit_price * quantity
```

- Tổng đơn:

```text
order_total = SUM(unit_price * quantity)
```

Chưa cần `food_order.total_amount` vì tổng có thể tính chính xác từ snapshot item.

### 6.3 Trạng thái và hủy

State machine hiện tại:

```text
PLACED ── CUSTOMER cancel ──> CANCELLED
```

| Từ | Sang | Actor | Điều kiện |
|---|---|---|---|
| `PLACED` | `CANCELLED` | CUSTOMER | Order thuộc CUSTOMER hiện tại |

`CANCELLED` là trạng thái cuối trong phạm vi hiện tại.

Không hỗ trợ:

- `CANCELLED → PLACED`.
- STAFF/ADMIN hủy thay CUSTOMER.
- Sửa món, số lượng, địa chỉ hoặc số điện thoại sau khi gửi.

## 7. Thay đổi mô hình dữ liệu

### 7.1 `food_order`

```text
id
customer_id        NOT NULL -> account.id
delivery_phone     NOT NULL
delivery_address   NOT NULL
status             NOT NULL IN (PLACED, CANCELLED)
payment_method     NOT NULL = COD
cancelled_at       NULL
created_at         NOT NULL
```

Khuyến nghị constraint:

```sql
CHECK (
    (status = 'PLACED' AND cancelled_at IS NULL)
    OR
    (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
)
```

### 7.2 `food_order_item`

```text
id
food_order_id      NOT NULL -> food_order.id
dish_id            NOT NULL -> dish.id
quantity           NOT NULL > 0
unit_price         NOT NULL > 0
```

`unit_price` là snapshot lịch sử, không phải relation động tới giá hiện tại của `dish`.

### 7.3 Index

Để phục vụ history:

```sql
CREATE INDEX idx_food_order_customer_created_at
    ON food_order (customer_id, created_at DESC);
```

Index `food_order(created_at)` vẫn hữu ích cho danh sách STAFF/ADMIN toàn hệ thống.

## 8. Migration delta đề xuất

Nếu schema baseline 07 đã được migrate, migration mới tối thiểu có thể gồm:

```sql
ALTER TABLE food_order_item
    ADD COLUMN unit_price INT;

-- Nếu database đã có dữ liệu cũ, phải backfill trước khi SET NOT NULL.
-- Với dữ liệu demo hiện tại có thể dùng giá dish tại thời điểm migration,
-- nhưng đây chỉ là approximation vì baseline trước đó không lưu lịch sử giá.
UPDATE food_order_item foi
SET unit_price = d.price
FROM dish d
WHERE d.id = foi.dish_id
  AND foi.unit_price IS NULL;

ALTER TABLE food_order_item
    ALTER COLUMN unit_price SET NOT NULL,
    ADD CONSTRAINT chk_food_order_item_unit_price CHECK (unit_price > 0);

ALTER TABLE food_order
    DROP CONSTRAINT IF EXISTS food_order_status_check;

ALTER TABLE food_order
    ADD CONSTRAINT food_order_status_check
    CHECK (status IN ('PLACED', 'CANCELLED'));

ALTER TABLE food_order
    ADD COLUMN cancelled_at TIMESTAMPTZ;

ALTER TABLE food_order
    ADD CONSTRAINT chk_food_order_cancelled_at
    CHECK (
        (status = 'PLACED' AND cancelled_at IS NULL)
        OR
        (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
    );

CREATE INDEX idx_food_order_customer_created_at
    ON food_order (customer_id, created_at DESC);
```

Tên constraint thực tế phải khớp migration baseline đang dùng; không giả định tên `food_order_status_check` nếu code SQL thật đã đặt tên khác.

### Lưu ý dữ liệu cũ

Nếu đã tồn tại order tạo theo baseline không có `unit_price`, không thể khôi phục chính xác giá lịch sử chỉ từ schema cũ. Backfill từ `dish.price` hiện tại chỉ giúp hoàn tất migration kỹ thuật; nó **không chứng minh được giá thực tế tại thời điểm các order cũ được tạo**.

## 9. JPA / service contract

### 9.1 Enum

```java
public enum FoodOrderStatus {
    PLACED,
    CANCELLED
}
```

### 9.2 `FoodOrderItem`

```java
@Column(name = "unit_price", nullable = false)
private Integer unitPrice;
```

Khi create:

```java
item.setUnitPrice(dish.getPrice());
```

### 9.3 `FoodOrder`

```java
@Column(name = "cancelled_at")
private Instant cancelledAt;
```

Service không expose generic `setStatus`. Nghiệp vụ nên đi qua command có tên rõ nghĩa, ví dụ `cancelOwnedOrder(...)`, để kiểm tra ownership và transition.

### 9.4 Query CUSTOMER

Repository/service cần hỗ trợ:

```text
findByCustomerIdOrderByCreatedAtDesc(customerId, pageable)
findOwnedDetail(orderId, customerId)
```

Không dùng `findById(orderId)` rồi trả thẳng dữ liệu cho CUSTOMER nếu chưa kiểm tra ownership.

## 10. Transaction

### 10.1 Tạo order

Trong một transaction:

1. Xác thực actor là CUSTOMER.
2. Validate delivery info và items.
3. Tải toàn bộ `Dish` cần dùng.
4. Với từng item, đọc `dish.price` và chuẩn bị `unit_price` snapshot.
5. Tạo `FoodOrder(PLACED, COD)`.
6. Tạo toàn bộ `FoodOrderItem` gồm `dish`, `quantity`, `unit_price`.
7. Commit.
8. Chỉ sau commit thành công mới trả success.

Nếu một item không tạo được, rollback toàn bộ order.

### 10.2 Hủy order

Trong một transaction:

1. Xác thực actor là CUSTOMER.
2. Tải order theo `orderId + customerId`.
3. Kiểm tra status hiện tại là `PLACED`.
4. Gán `status = CANCELLED`.
5. Gán `cancelled_at = now()`.
6. Commit.

Hai request hủy đồng thời không được tạo hai lần thay đổi nghiệp vụ. Có thể dùng optimistic locking (`@Version`) hoặc update có điều kiện `WHERE status='PLACED'`; nếu triển khai `@Version` cho `FoodOrder`, schema phải thêm cột `version` tương ứng.

> `@Version` cho `FoodOrder` là lựa chọn triển khai nên cân nhắc, chưa được bắt buộc trong schema hiện tại.

## 11. API ở mức contract

Tên endpoint có thể thay đổi theo convention của backend, nhưng cần đủ các khả năng sau:

```text
POST   /api/orders                  CUSTOMER tạo đơn
GET    /api/me/orders               CUSTOMER xem lịch sử của mình
GET    /api/me/orders/{id}          CUSTOMER xem owned detail
POST   /api/me/orders/{id}/cancel   CUSTOMER hủy owned PLACED order

GET    /api/admin/orders            STAFF/ADMIN xem danh sách
GET    /api/admin/orders/{id}       STAFF/ADMIN xem chi tiết
```

Không có endpoint STAFF/ADMIN update status trong phạm vi hiện tại.

## 12. UI cần bổ sung

Phía CUSTOMER cần thêm:

- **Đơn món của tôi / Order History**.
- **Chi tiết đơn món**.
- Hành động **Hủy đơn** trên chi tiết khi status = `PLACED`.
- Confirm dialog trước khi hủy.
- Badge/status cho `PLACED` và `CANCELLED`.
- Giá item và tổng đơn phải dùng giá snapshot từ order item.

Sau khi hủy, đơn không bị xóa khỏi lịch sử.

## 13. Nội dung vẫn để sau

Chưa đưa vào phạm vi hiện tại:

- `CONFIRMED`, `PREPARING`, `DELIVERING`, `COMPLETED` hoặc workflow giao hàng đầy đủ.
- STAFF/ADMIN cập nhật hoặc hủy order.
- Sửa order sau khi gửi.
- Phí giao hàng, vùng giao hàng, khoảng cách.
- ETA, shipper tracking.
- Thanh toán online, payment transaction, payment status.
- Inventory/nguyên liệu.
- Promotion/voucher.
- Email/push notification cho food order.
- Refund.
- Đặt món trước gắn với reservation/phục vụ tại bàn.

## 14. Tiêu chí hoàn thành

Bản cập nhật này hoàn thành khi:

1. `food_order_item.unit_price` tồn tại, `NOT NULL`, > 0.
2. Tạo order snapshot đúng `Dish.price` vào từng item.
3. ADMIN đổi giá món sau đó không làm thay đổi giá hiển thị của order cũ.
4. CUSTOMER xem được danh sách order của chính mình, mới nhất trước.
5. CUSTOMER xem được chi tiết order của chính mình với giá snapshot và tổng đúng.
6. CUSTOMER không xem được order của tài khoản khác.
7. CUSTOMER hủy được owned order đang `PLACED`.
8. Hủy cập nhật `status=CANCELLED` và `cancelled_at`.
9. Order `CANCELLED` vẫn còn trong history và giữ nguyên items/giá.
10. Không thể hủy lại order đã `CANCELLED`.
11. STAFF/ADMIN tiếp tục chỉ đọc food order.
12. Không có API/command ngoài phạm vi để chuyển sang trạng thái giao hàng khác.

---

## 15. Tài liệu đã đồng bộ theo thay đổi này

- `01-project-summary.md` → phiên bản 1.3.
- `02-functional-requirements-and-business-rules.md` → phiên bản 1.5; thêm FR-ORD-08 đến 10, BR-57 đến 58 và sửa BR-52/53.
- `03-use-cases.md` → phiên bản 1.4; thêm UC-37, UC-38.
- `04-erd-database-design.md` → phiên bản 1.5; thêm `unit_price`, `cancelled_at`, index history và D-17 đến D-19.
- `05-jpa-entities.md` → phiên bản 1.3; cập nhật enum/entity/repository/service contract.
- `07-food-ordering-basic-specification.md` → giữ làm baseline lịch sử và trỏ sang tài liệu 08.

# Saga Res: Thiết kế JPA Entity

| | |
|---|---|
| **Dự án** | Saga Res (nhà hàng demo: Sage) |
| **Tác giả** | Hùng |
| **Phiên bản** | 1.4 (đồng bộ auth JWT stateless; không thêm entity token) |
| **Tài liệu liên quan** | `04-erd-database-design.md`, `07-food-ordering-basic-specification.md`, `08-food-ordering-customer-history-cancellation-price-snapshot.md`, `09-authentication-jwt-security-design.md` |
| **Stack** | Spring Boot 3.x, Spring Data JPA (Hibernate 6.x), Lombok, PostgreSQL |
| **Package gốc dùng trong ví dụ** | `com.sagares.domain` — đổi lại cho khớp cấu trúc dự án thật |

---

## 1. Quy ước và lưu ý chung — đọc trước khi copy code

| Vấn đề | Quyết định | Vì sao |
|---|---|---|
| Sinh schema | `spring.jpa.hibernate.ddl-auto=validate` (hoặc `none`), schema thật sinh bằng **Flyway migration** viết tay | Hibernate không tự sinh được exclusion constraint, cột `GENERATED ALWAYS AS ... STORED`, hay `CHECK` phức tạp trong tài liệu 04 — để Hibernate tự `update`/`create` chắc chắn thiếu hoặc sai các phần này |
| Cột `reservation_range`, `occupied_range` | **Không map** vào entity `Reservation` | Đây là hai cột `tstzrange` do Postgres tự tính; `reservation_range` dùng cho overlap của khách, `occupied_range` dùng cho overlap của bàn. JPA không cần ghi hai cột này |
| Cột JSONB (`old_value`, `new_value` ở log) | Dùng thư viện `io.hypersistence:hypersistence-utils-hibernate-63` + `@Type(JsonType.class)` | Hibernate 6 không map JSONB ra `Map`/`JsonNode` sẵn; đây là thư viện phổ biến nhất cho việc này với Postgres |
| Trạng thái (status, role...) | Enum Java thật + `@Enumerated(EnumType.STRING)` | Khớp với cột `VARCHAR` + `CHECK` đã thiết kế ở tài liệu 04, tránh lưu số thứ tự enum (dễ vỡ khi thêm giá trị giữa danh sách) |
| Thời gian tạo/sửa | Spring Data JPA Auditing: `@CreatedDate`, `@LastModifiedDate`, `@EntityListeners(AuditingEntityListener.class)` | Idiomatic cho Spring Boot, tránh viết `@PrePersist`/`@PreUpdate` thủ công lặp lại ở mọi entity — nhớ thêm `@EnableJpaAuditing` ở một class `@Configuration` |
| equals/hashCode | Dựa trên `id`, tự viết tay (không dùng `@Data` hay Lombok `@EqualsAndHashCode` mặc định) | `@Data` sinh equals/hashCode theo toàn bộ field, dễ lỗi với entity JPA (lazy proxy, entity chưa persist chưa có id) — dùng `@Getter/@Setter` riêng, entity chưa persist (id null) không nên coi là "bằng nhau" theo id |
| Khóa ngoại | Luôn `FetchType.LAZY` kể cả `@ManyToOne` | Mặc định của `@ManyToOne` là `EAGER`, dễ gây N+1 hoặc load thừa — khai báo tường minh |
| `Reservation.version` | Thêm `@Version` (optimistic locking) | Không thay thế 2 exclusion constraint (đó là lưới an toàn chống **trùng bàn/trùng khách**); `@Version` chống mất dữ liệu khi 2 nhân viên sửa cùng một đơn cùng lúc (ví dụ cùng bấm duyệt) |

**Dependency cần thêm** (ngoài `spring-boot-starter-data-jpa`, driver Postgres):
```xml
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.7.3</version> <!-- kiểm tra bản mới nhất tương ứng Hibernate 6.x đang dùng -->
</dependency>
```

---

## 2. Enum dùng chung

```java
package com.sagares.domain.enums;

public enum AccountRole { CUSTOMER, STAFF, ADMIN }
```

```java
package com.sagares.domain.enums;

public enum DishStatus { AVAILABLE, SOLD_OUT, HIDDEN }
```

```java
package com.sagares.domain.enums;

public enum ReservationStatus {
    PENDING, CONFIRMED, ARRIVED, COMPLETED, NO_SHOW, CANCELLED, REJECTED
}
```

```java
package com.sagares.domain.enums;

public enum ReservationLogEventType {
    CREATED, STATUS_CHANGED, TABLE_CHANGED, TIME_CHANGED, RESCHEDULED
}
```

```java
package com.sagares.domain.enums;

public enum FoodOrderStatus { PLACED, CANCELLED }
```

```java
package com.sagares.domain.enums;

public enum PaymentMethod { COD }
```

---

## 3. Entity

### 3.1 `Account`

```java
package com.sagares.domain;

import com.sagares.domain.enums.AccountRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "account")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, length = 10)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountRole role;

    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}
```

> Tất cả entity còn lại dùng chung khuôn mẫu `equals/hashCode` này — từ đây chỉ ghi lại phần khác biệt để đỡ lặp, nhưng khi copy code thật thì thêm đầy đủ.

### 3.2 `RestaurantInfo` — bảng 1 dòng

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "restaurant_info")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantInfo {

    @Id
    private Short id; // luôn = 1, không dùng @GeneratedValue

    private String name;

    @Column(columnDefinition = "text")
    private String description;

    private String address;
    private String phone;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
```

Service tầng trên nên có một method duy nhất `getRestaurantInfo()` gọi `repository.findById((short) 1)` — không expose CRUD chung chung cho bảng này.

### 3.3 `OpeningHour`

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "opening_hour")
@Getter
@Setter
@NoArgsConstructor
public class OpeningHour {

    @Id
    @Column(name = "day_of_week")
    private Short dayOfWeek; // 0 = Chủ nhật ... 6 = Thứ Bảy

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = false;
}
```

### 3.4 `SpecialClosedDay`

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate;

@Entity
@Table(name = "special_closed_day")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class SpecialClosedDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "closed_date", nullable = false, unique = true)
    private LocalDate closedDate;

    private String reason;
}
```

### 3.5 `Area`

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "area")
@Getter
@Setter
@NoArgsConstructor
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
```

### 3.6 `RestaurantTable`

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restaurant_table")
@Getter
@Setter
@NoArgsConstructor
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @Column(name = "min_capacity", nullable = false)
    private Short minCapacity;

    @Column(name = "max_capacity", nullable = false)
    private Short maxCapacity;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
```

### 3.7 `TableSuspension`

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity
@Table(name = "table_suspension")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class TableSuspension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Account createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

### 3.8 `Category`

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
```

### 3.9 `Tag`

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tag")
@Getter
@Setter
@NoArgsConstructor
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
```

### 3.10 `Dish`

```java
package com.sagares.domain;

import com.sagares.domain.enums.DishStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "dish")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Integer price; // VND, BR-40

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DishStatus status = DishStatus.AVAILABLE;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    // dish_tag không có cột phụ -> dùng thẳng @ManyToMany, không cần entity riêng
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "dish_tag",
        joinColumns = @JoinColumn(name = "dish_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

### 3.11 `Reservation` — entity trung tâm

```java
package com.sagares.domain;

import com.sagares.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity
@Table(name = "reservation")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code; // BR-17, sinh ở service (ví dụ SAGE-250925-7K3F)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Account customer; // null nếu là đơn đặt hộ khách vãng lai (BR-39)

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_phone", length = 10)
    private String guestPhone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_area_id")
    private Area requestedArea; // null = khách không yêu cầu khu vực; không đổi khi staff chuyển bàn

    @Column(name = "party_size", nullable = false)
    private Short partySize;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "buffer_minutes", nullable = false)
    private Short bufferMinutes; // snapshot từ business_config lúc tạo/đổi lịch

    // reservation_range và occupied_range KHÔNG map ở đây.
    // Cả hai là cột GENERATED ALWAYS AS ... STORED do Postgres tự tính;
    // reservation_range = [startTime,endTime), occupied_range = [startTime,endTime+buffer).

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "special_note", columnDefinition = "text")
    private String specialNote;

    @Column(name = "reschedule_count", nullable = false)
    private short rescheduleCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Account createdBy; // nhân viên tạo hộ; null = khách tự đặt

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(name = "reminder_sent_at")
    private Instant reminderSentAt;

    @Version
    @Column(nullable = false)
    private Long version = 0L; // schema có version BIGINT NOT NULL DEFAULT 0; không thay exclusion constraint

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

**Lưu ý bắt buộc khi viết service tạo/đổi lịch đơn:** insert/update vẫn có thể bị DB từ chối bằng `23P01`. `reservation_table_no_overlap` dùng `occupied_range` và tính cả `COMPLETED`; `reservation_customer_no_overlap` dùng `reservation_range` không có buffer. Map lỗi theo tên constraint thành "hết chỗ" hoặc "bạn đã có đơn trùng giờ". Trước khi gán/chuyển bàn phải lock row `RestaurantTable` và kiểm tra `TableSuspension` chồng khoảng; khi khách tự tạo đơn phải lock row `Account` trước khi kiểm tra BR-27.

### 3.12 `ReservationStatusLog`

```java
package com.sagares.domain;

import com.sagares.domain.enums.ReservationLogEventType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "reservation_status_log")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ReservationStatusLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private ReservationLogEventType eventType;

    @Type(JsonType.class)
    @Column(name = "old_value", columnDefinition = "jsonb")
    private Map<String, Object> oldValue;

    @Type(JsonType.class)
    @Column(name = "new_value", columnDefinition = "jsonb")
    private Map<String, Object> newValue;

    @Column(columnDefinition = "text")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private Account changedBy; // null = hệ thống (job tự động, UC-26)

    @CreatedDate
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;
}
```

### 3.13 `BusinessConfig`

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity
@Table(name = "business_config")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class BusinessConfig {

    @Id
    @Column(name = "config_key", length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 255)
    private String configValue;

    @Column(columnDefinition = "text")
    private String description;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
```

Gợi ý: viết thêm một `@Service` nhỏ (`BusinessConfigService`) có cache trong bộ nhớ (ví dụ Spring `@Cacheable`, hoặc `ConcurrentHashMap` refresh định kỳ) — bảng này được đọc ở mọi request tính giờ trống nên không nên query DB mỗi lần.

### 3.14 `FoodOrder`

```java
package com.sagares.domain;

import com.sagares.domain.enums.FoodOrderStatus;
import com.sagares.domain.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "food_order")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class FoodOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Account customer;

    @Column(name = "delivery_phone", nullable = false, length = 30)
    private String deliveryPhone;

    @Column(name = "delivery_address", nullable = false, columnDefinition = "text")
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FoodOrderStatus status = FoodOrderStatus.PLACED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod = PaymentMethod.COD;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @OneToMany(mappedBy = "foodOrder", fetch = FetchType.LAZY)
    private List<FoodOrderItem> items = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

`FoodOrder` tạo mới luôn dùng `PLACED` + `COD`. Command hủy chỉ được phép đổi `PLACED → CANCELLED` cho CUSTOMER sở hữu đơn và đồng thời gán `cancelledAt = Instant.now()`. Không cung cấp command chuyển sang trạng thái khác. Kiểm tra `customer.role == CUSTOMER` và ownership nằm ở service/API vì FK database không thể tự đảm bảo các rule này.

### 3.15 `FoodOrderItem`

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "food_order_item")
@Getter
@Setter
@NoArgsConstructor
public class FoodOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_order_id", nullable = false)
    private FoodOrder foodOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice; // snapshot Dish.price tại thời điểm tạo item, BR-52
}
```

Theo BR-52, `unitPrice` là snapshot bắt buộc và không được cập nhật khi `Dish.price` thay đổi. Giá lịch sử và tổng đơn đọc từ `FoodOrderItem.unitPrice`; tổng có thể tính bằng `SUM(unitPrice * quantity)`.

---

## 4. Repository — các truy vấn cần lưu ý riêng

```java
package com.sagares.repository;

import com.sagares.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    // NFR-02: lấy TẤT CẢ reservation chiếm bàn trong khoảng bằng một truy vấn duy nhất
    // (không lọc theo table_id ở đây) - việc nhóm theo bàn để tính giờ trống
    // làm ở tầng service, trong bộ nhớ.
    // Availability phải dùng chính occupied_range để không bỏ sót phần buffer,
    // kể cả reservation COMPLETED hoặc reservation của ngày trước có buffer tràn sang đầu ngày.
    @Query(value = """
        SELECT r.*
        FROM reservation r
        WHERE r.status IN ('PENDING','CONFIRMED','ARRIVED','COMPLETED')
          AND r.occupied_range && tstzrange(:dayStart, :dayEnd, '[)')
        """, nativeQuery = true)
    List<Reservation> findTableOccupancyInRange(
        @Param("dayStart") Instant dayStart,
        @Param("dayEnd") Instant dayEnd
    );

    // BR-27: trước khi gọi count này, service phải lock Account của customer bằng PESSIMISTIC_WRITE.
    // Không dùng COUNT ... FOR UPDATE; serialization nằm trên row account.
    @Query("""
        SELECT COUNT(r) FROM Reservation r
        WHERE r.customer.id = :customerId
          AND r.status IN ('PENDING','CONFIRMED')
        """)
    long countActiveByCustomer(@Param("customerId") Long customerId);

    java.util.Optional<Reservation> findByCode(String code);
}
```

`findTableOccupancyInRange` dùng trực tiếp cột generated `occupied_range`, nên bắt đúng cả phần buffer và tận dụng GiST index. Service nhóm kết quả theo `table.id`, đồng thời tải `TableSuspension` chồng khoảng bằng một truy vấn riêng và loại các bàn bị suspension khi sinh slot.

**Repository/locking bổ sung cần có** *(các import/package lặp lại được lược bớt trong ví dụ)*:

```java
public interface AccountRepository extends JpaRepository<Account, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);
}

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RestaurantTable t where t.id = :id")
    Optional<RestaurantTable> findByIdForUpdate(@Param("id") Long id);
}

public interface TableSuspensionRepository extends JpaRepository<TableSuspension, Long> {
    @Query("""
        select s from TableSuspension s
        where s.table.id = :tableId
          and s.startTime < :occupiedEnd
          and s.endTime > :occupiedStart
        """)
    List<TableSuspension> findOverlapping(
        @Param("tableId") Long tableId,
        @Param("occupiedStart") Instant occupiedStart,
        @Param("occupiedEnd") Instant occupiedEnd
    );
}
```

Luồng khách tự đặt lock `Account` trước khi kiểm tra BR-27. Khi đã chọn một bàn ứng viên, create/reschedule/move reservation và create/update suspension đều lock `RestaurantTable` trước khi kiểm tra conflict rồi mới ghi. Giữ thứ tự lock ổn định trong toàn bộ service để hạn chế deadlock.

**Repository cho food order:**

```java
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    Page<FoodOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<FoodOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);

    @Query("""
        select distinct o
        from FoodOrder o
        left join fetch o.customer
        left join fetch o.items i
        left join fetch i.dish
        where o.id = :id
        """)
    Optional<FoodOrder> findDetailById(@Param("id") Long id);

    @Query("""
        select distinct o
        from FoodOrder o
        left join fetch o.items i
        left join fetch i.dish
        where o.id = :id and o.customer.id = :customerId
        """)
    Optional<FoodOrder> findOwnedDetail(@Param("id") Long id, @Param("customerId") Long customerId);
}

public interface FoodOrderItemRepository extends JpaRepository<FoodOrderItem, Long> {
    List<FoodOrderItem> findByFoodOrderId(Long foodOrderId);
}
```

`FoodOrderService.create(...)` phải chạy trong một transaction: xác thực actor có role `CUSTOMER`, validate địa chỉ/số điện thoại và item, tải các `Dish` được tham chiếu, tạo `FoodOrder(PLACED, COD)` rồi tạo toàn bộ `FoodOrderItem` với `unitPrice = dish.getPrice()`. CUSTOMER history dùng query theo `customerId`; detail phải kiểm tra ownership. `cancelOwnedOrder(...)` chạy trong transaction, chỉ đổi `PLACED → CANCELLED` và gán `cancelledAt`. STAFF/ADMIN service vẫn chỉ expose read methods.

---

## 5. Entity chưa triển khai (giữ tham khảo — quyết định D-03)

`Review` **chưa có migration Flyway** ở giai đoạn MVP. Code dưới đây chỉ để tham khảo khi triển khai thật, không thêm vào module chính bây giờ:

```java
package com.sagares.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@Entity
@Table(name = "review")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Account customer;

    @Column(nullable = false)
    private Short rating;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
```

---

## 6. Bước tiếp theo

1. Viết migration Flyway cho schema đã chốt trong tài liệu `04-erd-database-design.md`, bao gồm `food_order.status IN (PLACED,CANCELLED)`, `cancelled_at`, `food_order_item.unit_price > 0`, `COD` và `quantity > 0`.
2. Thêm class `@Configuration @EnableJpaAuditing` để `@CreatedDate`/`@LastModifiedDate` hoạt động.
3. Cấu hình `spring.jpa.hibernate.ddl-auto=validate` trong `application.yml` để Hibernate chỉ kiểm tra khớp schema, không tự sinh.
4. Viết test Testcontainers cho NFR-01 của đặt bàn và test food order: create snapshot đúng `unit_price`; order + items cùng commit/rollback; hủy chỉ cho owner và chỉ từ `PLACED`.
5. Viết authorization test: chỉ CUSTOMER tạo food order; CUSTOMER chỉ xem/hủy đơn của chính mình; STAFF/ADMIN chỉ đọc và không có command đổi trạng thái.


---

## 7. JPA cho tính năng đặt món giao hàng — đã chốt

`FoodOrder` và `FoodOrderItem` đã được thêm theo schema tài liệu 04. Contract hiện tại:

- `FoodOrder.customer` bắt buộc và service chỉ nhận actor role `CUSTOMER`.
- `FoodOrder.status` gồm `PLACED`, `CANCELLED`; command hiện tại chỉ cho `PLACED → CANCELLED` bởi owner.
- `FoodOrder.cancelledAt` được gán khi hủy.
- `FoodOrder.paymentMethod` chỉ có `PaymentMethod.COD`.
- `FoodOrderItem` chứa `dish` + `quantity` + `unitPrice` snapshot từ `Dish.price`.
- CUSTOMER có repository/service cho history và owned detail; không được xem đơn người khác.
- Không có entity payment, delivery, shipment hoặc order status log.
- STAFF/ADMIN có repository/service đọc danh sách và chi tiết; không có command/update-status method.

Khi bổ sung vòng đời giao hàng sau này, phải cập nhật tài liệu 02/03/08 trước, sau đó đổi ERD/Flyway và cuối cùng mới mở rộng enum/entity/service.


---

## 8. Ghi chú JPA cho JWT

MVP JWT **không tạo entity** `AccessToken`, `RefreshToken`, `Session` hoặc `TokenBlacklist`. JWT access token là stateless và không lưu DB. Security layer lấy `sub` từ token rồi tải `Account` hiện tại để kiểm tra `isLocked` và role trước khi tạo `Authentication`; vì vậy entity `Account` hiện tại đã đủ dữ liệu cho thiết kế JWT ở tài liệu `09-authentication-jwt-security-design.md`.

# Saga Res Backend Business Rules

> Backend-focused implementation summary for `saga-res-api`.
>
> Use this file as a quick reference when implementing services, validation, authorization, transactions, and tests.
> Detailed JPA mapping belongs in `05-jpa-entities.md`.
> Detailed JWT/security design belongs in `09-authentication-jwt-security-design.md`.
> When this summary conflicts with a more detailed project document, follow the detailed/canonical document and update this file.

## 1. Scope and implementation principles

- MVP backend modules: authentication/account, restaurant information, menu, table/area, availability, reservation, food order, staff/admin operations, and selected scheduler/email behavior.
- Business values such as opening hours, cleanup buffer, reservation duration limits, cancellation deadline, auto-confirm threshold, and active-reservation limit are configuration. Do not scatter numeric literals through service code.
- Use `Asia/Ho_Chi_Minh` for business-time interpretation. Persist reservation timestamps as timezone-aware values (`timestamptz` / `Instant` as defined by the entity/database design).
- Customer-owned resources must always be queried or validated by the authenticated account. Role authorization alone does not replace ownership checks.
- Commands that modify business state must run transactionally.
- Do not expose JPA entities directly as public API contracts. Use request/response DTOs.
- Database constraints are part of correctness, especially for reservation concurrency. Application checks do not replace database exclusion/unique constraints.

## 2. Roles and access boundaries

### CUSTOMER

- Self-registers as `CUSTOMER`; registration must never accept an arbitrary role from the client.
- Can manage only their own reservation/order data.
- Can create delivery food orders.
- Can cancel their own food order only while it is `PLACED`.

### STAFF

- Manages reservation operations allowed by the reservation workflow.
- Can view food-order lists/details.
- Cannot change food-order status in the current MVP.

### ADMIN

- Has administrative access to menu, tables, business configuration, accounts, and reservation management.
- Can view food-order lists/details.
- Cannot change food-order status in the current MVP.
- Must not be allowed to self-lock or self-demote where account administration exposes those operations.

### Authentication boundary

- Protected APIs use Bearer JWT.
- Authentication/authorization details are defined in `09-authentication-jwt-security-design.md`.
- Missing/invalid/expired/stale token or locked/missing account -> `401 Unauthorized`.
- Valid authentication with insufficient role -> `403 Forbidden`.

## 3. Restaurant time and availability rules

- Default opening hours: `10:00-22:00`, configurable by weekday.
- Special closed day: no reservations are accepted for that day.
- Reservation start/end times use 15-minute increments.
- Customer reservation duration:
  - suggested default: 90 minutes;
  - minimum: 60 minutes;
  - maximum: 180 minutes.
- Staff-created guest reservations may use a duration outside the customer 60-180 minute limit, but must still remain within opening hours.
- Cleanup buffer: 15 minutes after the dining interval.
- Dining interval: `reservation_range = [start_time, end_time)`.
- Table occupancy interval: `occupied_range = [start_time, end_time + cleanup_buffer)`.
- Availability must account for opening hours, special closed days, active tables, table capacity, requested area when supplied, table suspensions, and reservation occupancy.
- For performance, availability for a day should avoid N+1 per-slot database queries. Prefer loading relevant occupancy for the requested period and calculating candidate slots efficiently.

## 4. Reservation creation rules

### 4.1 Party size and advance booking

- Party size: 1-12 people.
- Groups above 12 are outside online-booking MVP scope.
- Customer minimum advance:
  - normal booking: at least 2 hours;
  - booking requiring approval (7+ people): at least 4 hours.
- Maximum advance booking window: 30 days.
- Staff-created guest reservations are not subject to the customer advance-booking restriction.

### 4.2 Table suitability

A table is eligible only when:

```text
min_capacity <= party_size <= max_capacity
```

and the table is active, not suspended for an overlapping `occupied_range`, and has no overlapping reservation that currently occupies the table.

### 4.3 Automatic table assignment

Among eligible tables:

1. If `requested_area_id` is supplied, consider only tables in that area.
2. Choose the smallest suitable `max_capacity`.
3. If tied, choose the lowest/smallest table code.
4. Lock the selected `restaurant_table` row.
5. Re-check reservation conflict and table suspension inside the same transaction.
6. Only then persist the reservation assignment.

No table merging is supported in the MVP.

`requested_area_id` represents the customer's preference and is independent of the current assigned table. A later staff table move must not overwrite it.

### 4.4 Initial status

- Party size 1-6 -> `CONFIRMED` when a table is successfully assigned.
- Party size 7-12 -> `PENDING`.
- The auto-confirm threshold is configuration, not a hard-coded service constant.
- Staff-created guest reservations start as `CONFIRMED`.

## 5. Reservation status model

Supported statuses:

```text
PENDING
CONFIRMED
ARRIVED
COMPLETED
NO_SHOW
CANCELLED
REJECTED
```

### Occupancy semantics

- `PENDING`, `CONFIRMED`, `ARRIVED` actively hold the reservation.
- `COMPLETED` is no longer an active booking for customer-count purposes, but its table still remains occupied until the committed cleanup buffer ends.
- `CANCELLED`, `REJECTED`, `NO_SHOW` release the table immediately.

### Allowed transitions

```text
PENDING   -> CONFIRMED
PENDING   -> REJECTED
PENDING   -> CANCELLED

CONFIRMED -> PENDING
CONFIRMED -> CANCELLED
CONFIRMED -> ARRIVED
CONFIRMED -> NO_SHOW

ARRIVED   -> COMPLETED
```

Rules:

- Rejecting a pending reservation requires a reason.
- `CONFIRMED -> ARRIVED` is a staff/admin operation for the reservation day.
- Staff/admin may mark `NO_SHOW` after 15 minutes past start time.
- `REJECTED`, `CANCELLED`, `NO_SHOW`, and `COMPLETED` are terminal in the current workflow.
- A pending reservation not approved by 3 hours before its start is automatically cancelled.

## 6. Customer reservation limits and ownership

- A CUSTOMER may have at most 3 active reservations in `PENDING` or `CONFIRMED`.
- On customer reservation creation:
  1. lock the customer's `account` row;
  2. count active reservations;
  3. reject creation when the configured limit is reached;
  4. continue using a consistent lock order for all creation paths for that customer.
- Two reservations belonging to the same customer must not overlap on the actual dining interval.
- Compare `reservation_range`, not the cleanup-buffer-expanded table range.
- Back-to-back customer reservations are allowed when:

```text
first.end_time == second.start_time
```

even though table occupancy may include cleanup buffer.

## 7. Reservation cancellation and reschedule

### Customer cancellation

- Customer may cancel only their own reservation.
- Cancellation/reschedule deadline: at least 2 hours before reservation start.
- Staff/admin may cancel according to the operational workflow without the customer deadline restriction.

### Customer reschedule

- Maximum customer reschedules per reservation: 2.
- Reschedule updates the existing reservation; do not create a replacement reservation.
- Reschedule must be atomic:
  1. validate the new time/business rules;
  2. find/validate table availability while excluding the reservation's own existing occupancy;
  3. prefer keeping the current table when it still fits and is available;
  4. update only when the new reservation can be committed;
  5. otherwise leave the original reservation unchanged.
- After reschedule, re-evaluate status using the auto-confirm threshold:
  - new party size in auto-confirm range -> `CONFIRMED`;
  - new party size requiring approval -> `PENDING`.
- A successful reschedule can therefore cause `CONFIRMED -> PENDING`.

## 8. Reservation concurrency and integrity

### Table conflict

Never allow two reservations on the same table to overlap on `occupied_range` when their statuses participate in table occupancy.

Enforce this at two layers:

1. application transaction check;
2. PostgreSQL database exclusion constraint.

If PostgreSQL rejects an overlap (for example SQLSTATE `23P01`), map it to a business conflict response instead of leaking a raw database exception.

### Pessimistic locks

Use the same table-row lock discipline for create reservation, reschedule, move table, staff-created guest reservation, and create/update `table_suspension`.

The purpose is to prevent a race where one transaction assigns a table while another transaction suspends it.

### Optimistic locking

Reservation `@Version` protects against lost updates of the same reservation aggregate. It does not replace table-overlap database constraints.

### Audit log

Record reservation changes such as create, reschedule, table move, duration change, and status change.

Log actor, timestamp, event type, old/new values, and reason where relevant.

## 9. Table suspension and operational changes

- Suspended/inactive tables are not eligible for new assignments.
- A suspension conflicts with a reservation when it overlaps the reservation's `occupied_range`.
- Create/update of `table_suspension` must lock the corresponding table row and perform conflict checks in the same transaction.
- If a table suspension, opening-hour change, or special closed day affects existing held reservations:
  - warn/list impacted reservations;
  - do not automatically cancel them;
  - staff handles reassignment/contact/cancellation.

## 10. Menu rules

### Category / dish lifecycle

- Referenced categories, dishes, or tables should be deactivated/hidden rather than hard-deleted.
- Hard delete is acceptable only when the record has never been referenced and database integrity permits it.

### Dish

- Price is integer VND and must be greater than 0.
- Public display formatting such as `125.000đ` belongs to the presentation contract/UI; backend stores the integer value.
- Status behavior:
  - `AVAILABLE`: visible normally;
  - `SOLD_OUT`: still visible publicly but marked sold out;
  - `HIDDEN`: not returned by public menu APIs.
- Dish image is stored as a URL.
- A dish may have multiple tags through `tag` / `dish_tag`.
- Public menu queries must not accidentally expose `HIDDEN` dishes.

## 11. Food-order rules

### 11.1 Scope

Current delivery-order MVP is intentionally small:

```text
CUSTOMER create order
CUSTOMER view own history/detail
CUSTOMER cancel own PLACED order
STAFF/ADMIN read list/detail
COD only
```

Do not implement a full delivery workflow unless the project scope is explicitly changed.

### 11.2 Create order

Only an authenticated `CUSTOMER` may create an order.

Minimum validation:

- at least one item;
- every referenced dish exists;
- `quantity > 0`;
- `delivery_address` is not blank;
- `delivery_phone` is not blank;
- payment method is `COD`.

Create the order in one transaction:

1. validate actor and request;
2. load all required dishes;
3. snapshot each current `dish.price` into `food_order_item.unit_price`;
4. create `FoodOrder` with `status = PLACED` and `payment_method = COD`;
5. persist all items;
6. commit atomically.

If any item fails, rollback the whole order.

### 11.3 Price snapshot

`unit_price` is historical data.

At creation:

```text
food_order_item.unit_price = dish.price
```

After creation, order history/detail must use `unit_price`, never current `dish.price`.

Totals:

```text
line_total  = unit_price * quantity
order_total = SUM(unit_price * quantity)
```

A separate `total_amount` column is not required by the current design.

### 11.4 Status and cancellation

Current statuses:

```text
PLACED
CANCELLED
```

Only transition:

```text
PLACED -> CANCELLED
```

and only the CUSTOMER who owns the order can perform it.

Cancellation transaction:

1. query by both `orderId` and current `customerId`;
2. require `status == PLACED`;
3. set `status = CANCELLED`;
4. set `cancelled_at = now`;
5. commit.

A cancelled order remains in history with its items and price snapshots unchanged.

Do not expose a generic arbitrary `setStatus` business command for food orders.

### 11.5 Ownership and read access

CUSTOMER:

- history contains only `customer_id == current account id`;
- newest orders first;
- detail query must enforce ownership.

Do not use a plain `findById(orderId)` and return the entity to CUSTOMER without ownership validation.

STAFF/ADMIN:

- may read list/detail;
- may see both `PLACED` and `CANCELLED`;
- may not change status or edit the order in the current MVP.

## 12. Email and scheduler behavior

Reservation email events may include confirmation when reservation becomes `CONFIRMED`, cancellation, rejection, reschedule, and reminder.

Rules:

- A `PENDING` reservation does not receive a confirmation email until it is approved/confirmed.
- Reminder time: 3 hours before start.
- Email failure must not roll back or fail the reservation business transaction; send asynchronously and log failures.
- Scheduler may auto-cancel overdue `PENDING` reservations according to the approval deadline rule.
- Food-order email/push notification is outside the current scope.

## 13. Out of scope unless explicitly added later

Do not add these while implementing the current MVP:

- refresh token / JWT blacklist / server-side auth session;
- online reservation payment or deposit;
- table merging;
- customer booking without account;
- automatic no-show penalties;
- full delivery workflow (`CONFIRMED`, `PREPARING`, `DELIVERING`, `COMPLETED`);
- STAFF/ADMIN food-order status updates;
- editing a food order after submission;
- delivery fee/zone;
- ETA or shipper tracking;
- payment gateway / online payment;
- inventory/ingredient management;
- promotion/voucher;
- food-order refund;
- pre-order food linked to a reservation;
- food-order email/push notifications.

## 14. Backend test invariants

At minimum, preserve these invariants in automated tests:

- overlapping concurrent requests cannot double-book the same table;
- `COMPLETED` still blocks its table until cleanup buffer ends;
- two concurrent creates when a customer already has 2 active reservations can create at most 1 additional active reservation;
- reservation optimistic versioning detects conflicting updates;
- successful reschedule can perform `CONFIRMED -> PENDING`;
- reservation assignment cannot race successfully against a conflicting table suspension;
- `requested_area_id` survives staff table movement unchanged;
- customer reservations may be exactly back-to-back on dining range;
- CUSTOMER cannot access another customer's reservation or food order;
- food-order historical price remains unchanged after an admin changes `Dish.price`;
- `CANCELLED` food order remains in history and cannot be cancelled again;
- STAFF/ADMIN food-order access remains read-only.

## 15. Guidance for Codex / implementation agents

Before implementing a feature:

1. Read this file for business behavior.
2. Read `05-jpa-entities.md` before changing entities/repositories/database-facing code.
3. Read `09-authentication-jwt-security-design.md` before changing authentication/authorization.
4. Inspect existing migrations and source code before proposing schema changes.
5. Do not invent fields, statuses, transitions, endpoints, or workflows that are outside the documented MVP.
6. Preserve transaction boundaries, ownership checks, and concurrency rules even when simplifying code.
7. Prefer clear feature-local services and DTOs over large cross-domain "god" services.

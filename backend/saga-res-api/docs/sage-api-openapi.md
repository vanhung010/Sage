# Sage / Saga Res API Document

> OpenAPI 3.1 contract generated from the current `vanhung010/Sage` backend, the Sage Figma customer UI, and the current project specification documents.
>
> Generated: **2026-10-12**  
> Repository ref observed: `main` @ `118fd19d403f5631e2441d66592351782da7f5f4`

## 1. Scope and source precedence

This document intentionally separates three levels of API maturity:

- **`implemented`** — the HTTP controller/method currently exists in the backend codebase. The request/response shape follows the current Java DTOs.
- **`planned`** — required by the current MUST/SHOULD requirements, use cases, food-order contract, security design, or a screen already present in Figma, but the matching controller is not present yet. When the source defines the capability but not the exact URL, the URL in this document is a **proposed REST convention**.
- **`future`** — COULD/later-scope API. It is included for architectural visibility but is not part of the immediate MVP implementation target.

When sources differ, this document follows this precedence:

1. **Current code** for exact implemented paths, methods and DTO fields.
2. **Current requirements/business rules and use cases** for planned behavior.
3. **Figma** for customer-facing fields, filters, states and handoff behavior.
4. A proposed API convention only where the sources require a capability but do not define a route.

## 2. Important compatibility / implementation notes

### 2.1 Admin authorization gap in the current code

The current `SecurityConfig` permits `/api/admin/**` with `permitAll()` while the intended `hasRole("ADMIN")` rule is commented out. The project security specification requires protected role-based access. Therefore this OpenAPI file documents the **target security contract** (`Bearer JWT` + `x-sage-roles`) and marks the current implementation gap on affected implemented admin operations.

This should be fixed before treating the implemented admin endpoints as production-safe.

### 2.2 Register UI vs backend DTO

Figma contains a `confirmPassword` field and a confirmation-mismatch state. The current backend `RegisterRequest` accepts only:

`email`, `password`, `fullName`, `phone`.

Therefore `confirmPassword` is **not** part of the implemented request contract below. The frontend can validate confirmation locally, or the backend DTO can be deliberately extended later.

### 2.3 Logout and refresh token

The MVP is stateless JWT:

- Access token only.
- HS256.
- TTL: 1 hour / 3600 seconds.
- No refresh token.
- No server-side logout/revoke/blacklist endpoint.
- Logout means deleting the token in the client.

Accordingly, this document does **not** define `/api/auth/logout` or `/api/auth/refresh`.

### 2.4 Time and pagination conventions

- Business display timezone: `Asia/Ho_Chi_Minh`.
- Planned date-time request/response fields use RFC 3339 `date-time` values with an explicit offset where practical.
- Reservation customer slots use 15-minute increments.
- The reusable `PageResponse` already present in the codebase uses a **zero-based** `page` index, and this contract keeps that convention.
- Public Menu Figma shows pagination; the planned menu endpoint therefore returns the same `PageResponse` shape.

### 2.5 Public dish visibility

Customer-facing menu and dish-detail endpoints must never expose `HIDDEN` dishes. `SOLD_OUT` dishes may be returned so the UI can render them as unavailable.

### 2.6 Food-order price history

Food-order responses use `food_order_item.unit_price` as the immutable price snapshot. `totalAmount` is a computed API field:

`SUM(unitPrice * quantity)`

It is not assumed to be a persisted `food_order.total_amount` column.

## 3. Endpoint inventory

### 3.1 Implemented operations (22)

| Method | Path | Priority | Roles | Summary |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | MUST | ANONYMOUS, CUSTOMER, STAFF, ADMIN | Register a CUSTOMER account |
| `POST` | `/api/auth/login` | MUST | ANONYMOUS, CUSTOMER, STAFF, ADMIN | Log in and issue a JWT access token |
| `GET` | `/api/auth/me` | MUST | CUSTOMER, STAFF, ADMIN | Get the current authenticated account |
| `GET` | `/api/opening-hours` | MUST | ANONYMOUS, CUSTOMER, STAFF, ADMIN | Get public weekly opening hours |
| `GET` | `/api/admin/opening-hours` | MUST | ADMIN | List opening hours for administration |
| `POST` | `/api/admin/opening-hours` | MUST | ADMIN | Create an opening-hour row |
| `PATCH` | `/api/admin/opening-hours/{id}` | MUST | ADMIN | Update an opening-hour row |
| `GET` | `/api/admin/menu/categories` | MUST | ADMIN | List menu categories for administration |
| `POST` | `/api/admin/menu/categories` | MUST | ADMIN | Create a menu category |
| `PATCH` | `/api/admin/menu/categories/{id}` | MUST | ADMIN | Update or deactivate a menu category |
| `GET` | `/api/admin/menu/tags` | MUST | ADMIN | List menu tags for administration |
| `POST` | `/api/admin/menu/tags` | MUST | ADMIN | Create a menu tag |
| `PATCH` | `/api/admin/menu/tags/{id}` | MUST | ADMIN | Update a menu tag |
| `DELETE` | `/api/admin/menu/tags/{id}` | MUST | ADMIN | Delete an unused menu tag |
| `GET` | `/api/admin/areas` | MUST | ADMIN | List restaurant areas for administration |
| `POST` | `/api/admin/areas` | MUST | ADMIN | Create a restaurant area |
| `PATCH` | `/api/admin/areas/{id}` | MUST | ADMIN | Update a restaurant area |
| `PATCH` | `/api/admin/areas/{id}/deactivate` | MUST | ADMIN | Deactivate a restaurant area |
| `GET` | `/api/admin/tables` | MUST | ADMIN | List restaurant tables for administration |
| `POST` | `/api/admin/tables` | MUST | ADMIN | Create a restaurant table |
| `PATCH` | `/api/admin/tables/{id}` | MUST | ADMIN | Update a restaurant table |
| `PATCH` | `/api/admin/tables/{id}/deactivate` | MUST | ADMIN | Deactivate a restaurant table |

### 3.2 Planned operations (51)

| Method   | Path                                                     | Priority | Roles                             | Summary                                              |
| -------- | -------------------------------------------------------- | -------- | --------------------------------- | ---------------------------------------------------- |
| `GET`    | `/api/restaurant`                                        | MUST     | ANONYMOUS, CUSTOMER, STAFF, ADMIN | Get public restaurant information                    |
| `GET`    | `/api/areas`                                             | MUST     | ANONYMOUS, CUSTOMER, STAFF, ADMIN | List active areas for reservation selection          |
| `GET`    | `/api/menu/categories`                                   | MUST     | ANONYMOUS, CUSTOMER, STAFF, ADMIN | List active public menu categories                   |
| `GET`    | `/api/menu/tags`                                         | SHOULD   | ANONYMOUS, CUSTOMER, STAFF, ADMIN | List tags available for public menu filtering        |
| `GET`    | `/api/menu/dishes`                                       | MUST     | ANONYMOUS, CUSTOMER, STAFF, ADMIN | Browse, search and filter public dishes              |
| `GET`    | `/api/menu/dishes/{id}`                                  | MUST     | ANONYMOUS, CUSTOMER, STAFF, ADMIN | Get public dish detail                               |
| `PATCH`  | `/api/admin/restaurant`                                  | MUST     | ADMIN                             | Update restaurant information                        |
| `GET`    | `/api/admin/special-closed-days`                         | MUST     | ADMIN                             | List special closed days                             |
| `POST`   | `/api/admin/special-closed-days`                         | MUST     | ADMIN                             | Declare a special closed day                         |
| `DELETE` | `/api/admin/special-closed-days/{id}`                    | MUST     | ADMIN                             | Remove a special closed-day declaration              |
| `GET`    | `/api/admin/menu/dishes`                                 | MUST     | ADMIN                             | List dishes for administration                       |
| `POST`   | `/api/admin/menu/dishes`                                 | MUST     | ADMIN                             | Create a dish                                        |
| `GET`    | `/api/admin/menu/dishes/{id}`                            | MUST     | ADMIN                             | Get a dish for administration                        |
| `PATCH`  | `/api/admin/menu/dishes/{id}`                            | MUST     | ADMIN                             | Update a dish                                        |
| `PATCH`  | `/api/admin/menu/dishes/{id}/status`                     | MUST     | ADMIN                             | Change dish availability/visibility status           |
| `GET`    | `/api/admin/tables/{tableId}/suspensions`                | SHOULD   | ADMIN                             | List table suspensions                               |
| `POST`   | `/api/admin/tables/{tableId}/suspensions`                | SHOULD   | ADMIN                             | Suspend a table for a time range                     |
| `DELETE` | `/api/admin/tables/{tableId}/suspensions/{suspensionId}` | SHOULD   | ADMIN                             | Remove a table suspension                            |
| `GET`    | `/api/availability`                                      | MUST     | ANONYMOUS, CUSTOMER, STAFF, ADMIN | Search available reservation start times             |
| `POST`   | `/api/reservations`                                      | MUST     | CUSTOMER                          | Create a CUSTOMER reservation                        |
| `GET`    | `/api/me/reservations`                                   | MUST     | CUSTOMER                          | List my reservations                                 |
| `GET`    | `/api/me/reservations/{id}`                              | MUST     | CUSTOMER                          | Get one of my reservations                           |
| `POST`   | `/api/me/reservations/{id}/cancel`                       | MUST     | CUSTOMER                          | Cancel my reservation                                |
| `POST`   | `/api/me/reservations/{id}/reschedule`                   | SHOULD   | CUSTOMER                          | Reschedule my reservation                            |
| `GET`    | `/api/admin/reservations`                                | MUST     | STAFF, ADMIN                      | List and filter reservations for operations          |
| `GET`    | `/api/admin/reservations/{id}`                           | MUST     | STAFF, ADMIN                      | Get reservation operation detail                     |
| `POST`   | `/api/admin/reservations/{id}/approve`                   | MUST     | STAFF, ADMIN                      | Approve a pending reservation                        |
| `POST`   | `/api/admin/reservations/{id}/reject`                    | MUST     | STAFF, ADMIN                      | Reject a pending reservation                         |
| `POST`   | `/api/admin/reservations/{id}/arrive`                    | MUST     | STAFF, ADMIN                      | Mark customer as arrived                             |
| `POST`   | `/api/admin/reservations/{id}/complete`                  | MUST     | STAFF, ADMIN                      | Mark reservation as completed                        |
| `POST`   | `/api/admin/reservations/{id}/no-show`                   | MUST     | STAFF, ADMIN                      | Mark reservation as no-show                          |
| `POST`   | `/api/admin/reservations/{id}/cancel`                    | SHOULD   | STAFF, ADMIN                      | Cancel a reservation on behalf of the customer       |
| `POST`   | `/api/admin/reservations/{id}/move-table`                | SHOULD   | STAFF, ADMIN                      | Move a reservation to another table                  |
| `POST`   | `/api/admin/reservations/{id}/change-duration`           | SHOULD   | STAFF, ADMIN                      | Change reservation end time                          |
| `POST`   | `/api/admin/reservations/guest`                          | SHOULD   | STAFF, ADMIN                      | Create a reservation on behalf of a guest            |
| `GET`    | `/api/admin/reservations/{id}/logs`                      | SHOULD   | STAFF, ADMIN                      | Get reservation change log                           |
| `GET`    | `/api/admin/reservations/timeline`                       | SHOULD   | STAFF, ADMIN                      | Get reservation timeline for a day                   |
| `POST`   | `/api/orders`                                            | MUST     | CUSTOMER                          | Place a delivery food order                          |
| `GET`    | `/api/me/orders`                                         | MUST     | CUSTOMER                          | List my delivery orders                              |
| `GET`    | `/api/me/orders/{id}`                                    | MUST     | CUSTOMER                          | Get one of my delivery orders                        |
| `POST`   | `/api/me/orders/{id}/cancel`                             | MUST     | CUSTOMER                          | Cancel my PLACED delivery order                      |
| `GET`    | `/api/admin/orders`                                      | MUST     | STAFF, ADMIN                      | List delivery orders for operations (read-only)      |
| `GET`    | `/api/admin/orders/{id}`                                 | MUST     | STAFF, ADMIN                      | Get delivery order detail for operations (read-only) |
| `PATCH`  | `/api/me/profile`                                        | SHOULD   | CUSTOMER                          | Update my profile                                    |
| `POST`   | `/api/me/change-password`                                | SHOULD   | CUSTOMER                          | Change my password                                   |
| `POST`   | `/api/auth/forgot-password`                              | SHOULD   | ANONYMOUS, CUSTOMER               | Request password-reset email                         |
| `POST`   | `/api/auth/reset-password`                               | SHOULD   | ANONYMOUS                         | Reset password using an email token                  |
| `GET`    | `/api/admin/accounts`                                    | SHOULD   | ADMIN                             | List accounts for administration                     |
| `PATCH`  | `/api/admin/accounts/{id}/lock`                          | SHOULD   | ADMIN                             | Lock or unlock an account                            |
| `PATCH`  | `/api/admin/accounts/{id}/role`                          | SHOULD   | ADMIN                             | Change an account role                               |
| `GET`    | `/api/admin/reports/reservations`                        | SHOULD   | ADMIN                             | Get reservation volume report                        |

### 3.3 Future / COULD operations (5)

| Method | Path | Priority | Roles | Summary |
|---|---|---|---|---|
| `GET` | `/api/admin/reports/occupancy` | COULD | ADMIN | Get peak-hour and occupancy report |
| `GET` | `/api/admin/reports/cancellations` | COULD | ADMIN | Get cancellation and no-show report |
| `POST` | `/api/me/reservations/{id}/review` | COULD | CUSTOMER | Review a completed reservation |
| `GET` | `/api/reviews` | COULD | ANONYMOUS, CUSTOMER, STAFF, ADMIN | List visible restaurant reviews |
| `PATCH` | `/api/admin/reviews/{id}/hide` | COULD | ADMIN | Hide a review |

## 4. Functional rules encoded by the contract

### Authentication

- Public register always creates `CUSTOMER`.
- Protected APIs use `Authorization: Bearer <token>`.
- Missing/invalid/expired token, missing/locked account, or stale role claim -> `401`.
- Valid token without sufficient role -> `403`.
- Ownership checks are required in addition to role checks.

### Reservations

- Party size: 1-12.
- Customer duration: 60-180 minutes.
- Start times: 15-minute grid.
- Minimum lead time: 2 hours; party size >= 7 requires 4 hours.
- Maximum booking horizon: 30 days.
- Optional requested area; customer never selects a concrete table.
- The backend auto-assigns the smallest suitable available table.
- Party size 1-6 -> `CONFIRMED` when successful.
- Party size 7-12 -> `PENDING`.
- CUSTOMER cancel/reschedule requires at least 2 hours before start.
- Maximum 2 successful customer reschedules.
- Maximum 3 active (`PENDING`/`CONFIRMED`) reservations per customer.
- `PENDING`, `CONFIRMED`, `ARRIVED` occupy a table; `COMPLETED` still participates in table-overlap protection until its committed cleanup buffer ends.
- Staff/Admin state transitions follow the documented reservation state machine.

### Food orders

- Only authenticated `CUSTOMER` may place an order.
- At least one item; each quantity > 0.
- Address and delivery phone are required.
- Payment is `COD` only.
- New status is `PLACED`.
- Each item snapshots the current dish price into `unitPrice`.
- CUSTOMER may cancel only its own `PLACED` order -> `CANCELLED`.
- STAFF/ADMIN are read-only for food orders in the current scope.
- No delivery fee/zone, shipper tracking, online payment, inventory, promotion, or delivery workflow is added here.

## 5. Standard error envelope

Implemented backend errors use:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "..."
}
```

The current global handler already standardizes `UNAUTHORIZED`, `VALIDATION_ERROR`, `INVALID_REQUEST`, and `DATA_INTEGRITY_CONFLICT`, and passes through `BusinessException` status/code/message.

For **planned** reservation/order endpoints, the exact business `code` strings have not all been fixed in the existing source documents. The OpenAPI contract therefore standardizes the envelope without pretending that unimplemented code names are already canonical. When those services are implemented, assign stable codes for conditions such as no availability, customer schedule overlap, active-reservation limit, cancellation window closed, invalid state transition, and ownership/not-found behavior.

## 6. OpenAPI 3.1 specification

The following YAML is the machine-readable contract. Custom extensions are intentional:

- `x-sage-status`: `implemented | planned | future`
- `x-sage-priority`: `MUST | SHOULD | COULD`
- `x-sage-roles`: intended role authorization
- `x-sage-source`: traceability to code/document/Figma
- `x-sage-implementation-gap`: known difference between current code and target contract

```yaml
openapi: 3.1.0
info:
  title: Sage / Saga Res API
  version: 1.0.0-draft
  description: 'Unified API contract for the Sage demo restaurant. Operations are classified with `x-sage-status`: `implemented`,
    `planned`, or `future`. Implemented paths and DTO shapes are derived from the current Spring Boot codebase. Planned/future
    operations are derived from the project requirements/use cases and Figma UI; where the source documents define a capability
    but not an exact URL, the URL in this document is a proposed convention.'
servers:
- url: /
  description: Relative to the deployed Sage backend origin
tags:
- name: Auth
  description: Authentication and current-user session state
- name: Restaurant
  description: Public restaurant information and opening data
- name: Menu
  description: Public customer-facing menu
- name: Availability
  description: Reservation availability search
- name: Reservations
  description: CUSTOMER-owned reservation operations
- name: Food Orders
  description: CUSTOMER delivery-order operations
- name: Admin Restaurant
  description: ADMIN restaurant schedule/information management
- name: Admin Menu
  description: ADMIN category, tag and dish management
- name: Admin Tables
  description: ADMIN area, table and suspension management
- name: Staff Reservations
  description: STAFF/ADMIN reservation operations
- name: Operations Orders
  description: STAFF/ADMIN read-only delivery-order operations
- name: Accounts
  description: Profile and account administration
- name: Reports
  description: ADMIN reporting endpoints
- name: Reviews
  description: Optional review feature
components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: 'HS256 access token. TTL 1 hour. Send as `Authorization: Bearer <token>`. The MVP has no refresh token
        or server-side logout/revocation endpoint.'
  responses:
    BadRequest:
      description: Invalid request or validation failure.
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    Unauthorized:
      description: Authentication is missing, invalid, expired, locked, or stale.
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    Forbidden:
      description: Authenticated user does not have the required role.
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    NotFound:
      description: Requested resource was not found.
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    Conflict:
      description: Business/data conflict.
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
  schemas:
    ErrorResponse:
      type: object
      required:
      - status
      - code
      - message
      properties:
        status:
          type: integer
          example: 400
        code:
          type: string
          example: VALIDATION_ERROR
        message:
          type: string
          example: 'email: must be a well-formed email address'
    AccountRole:
      type: string
      enum:
      - CUSTOMER
      - STAFF
      - ADMIN
    DayOfWeek:
      type: string
      enum:
      - MONDAY
      - TUESDAY
      - WEDNESDAY
      - THURSDAY
      - FRIDAY
      - SATURDAY
      - SUNDAY
      description: Text enum. Numeric enum ordinals are rejected by the current backend deserializer.
    RegisterRequest:
      type: object
      required:
      - email
      - password
      - fullName
      - phone
      additionalProperties: false
      properties:
        email:
          type: string
          format: email
        password:
          type: string
          minLength: 8
          pattern: ^(?=.*[A-Za-z])(?=.*\d).+$
          description: At least 8 characters, including at least one letter and one digit.
        fullName:
          type: string
          minLength: 1
        phone:
          type: string
          pattern: ^0[0-9]{9}$
          example: 0901234567
      description: Exact current backend DTO. The Figma form also has confirmPassword, but the current backend DTO does not;
        confirmation is therefore a frontend validation concern unless the backend contract is changed later.
    LoginRequest:
      type: object
      required:
      - email
      - password
      additionalProperties: false
      properties:
        email:
          type: string
          format: email
        password:
          type: string
          minLength: 1
    AuthUserResponse:
      type: object
      required:
      - id
      - fullName
      - role
      properties:
        id:
          type: integer
          format: int64
        fullName:
          type: string
        role:
          $ref: '#/components/schemas/AccountRole'
    CurrentUserResponse:
      type: object
      required:
      - id
      - email
      - fullName
      - phone
      - role
      properties:
        id:
          type: integer
          format: int64
        email:
          type: string
          format: email
        fullName:
          type: string
        phone:
          type: string
        role:
          $ref: '#/components/schemas/AccountRole'
    LoginResponse:
      type: object
      required:
      - accessToken
      - tokenType
      - expiresIn
      - user
      properties:
        accessToken:
          type: string
          description: JWT access token
        tokenType:
          type: string
          const: Bearer
        expiresIn:
          type: integer
          const: 3600
          description: Seconds
        user:
          $ref: '#/components/schemas/AuthUserResponse'
    OpeningHourResponse:
      type: object
      required:
      - dayOfWeek
      - closed
      properties:
        dayOfWeek:
          $ref: '#/components/schemas/DayOfWeek'
        openTime:
          type:
          - string
          - 'null'
          format: time
          example: '10:00:00'
        closeTime:
          type:
          - string
          - 'null'
          format: time
          example: '22:00:00'
        closed:
          type: boolean
    AdminOpeningHourResponse:
      allOf:
      - $ref: '#/components/schemas/OpeningHourResponse'
      - type: object
        required:
        - id
        - impactedReservationIds
        properties:
          id:
            type: integer
            format: int32
          impactedReservationIds:
            type: array
            items:
              type: integer
              format: int64
    CreateOpeningHourRequest:
      type: object
      required:
      - dayOfWeek
      - openTime
      - closeTime
      additionalProperties: false
      properties:
        dayOfWeek:
          $ref: '#/components/schemas/DayOfWeek'
        openTime:
          type: string
          format: time
          example: '10:00:00'
        closeTime:
          type: string
          format: time
          example: '22:00:00'
    UpdateOpeningHourRequest:
      type: object
      additionalProperties: false
      properties:
        dayOfWeek:
          allOf:
          - $ref: '#/components/schemas/DayOfWeek'
          description: Optional echo field. The weekday primary key cannot be changed by the current service contract.
        openTime:
          type: string
          format: time
        closeTime:
          type: string
          format: time
    AdminCategoryResponse:
      type: object
      required:
      - id
      - name
      - displayOrder
      - active
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        displayOrder:
          type: integer
        active:
          type: boolean
    CreateCategoryRequest:
      type: object
      required:
      - name
      - displayOrder
      additionalProperties: false
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 100
        displayOrder:
          type: integer
          minimum: 0
    UpdateCategoryRequest:
      type: object
      additionalProperties: false
      properties:
        name:
          type: string
          maxLength: 100
        displayOrder:
          type: integer
          minimum: 0
        active:
          type: boolean
    AdminTagResponse:
      type: object
      required:
      - id
      - name
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
    CreateTagRequest:
      type: object
      required:
      - name
      additionalProperties: false
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 50
    UpdateTagRequest:
      type: object
      additionalProperties: false
      properties:
        name:
          type: string
          maxLength: 50
    AdminAreaResponse:
      type: object
      required:
      - id
      - name
      - active
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        description:
          type:
          - string
          - 'null'
        active:
          type: boolean
    CreateAreaRequest:
      type: object
      required:
      - name
      additionalProperties: false
      properties:
        name:
          type: string
          minLength: 1
          maxLength: 100
        description:
          type:
          - string
          - 'null'
    UpdateAreaRequest:
      type: object
      additionalProperties: false
      properties:
        name:
          type: string
          maxLength: 100
        description:
          type:
          - string
          - 'null'
    AdminTableAreaResponse:
      type: object
      required:
      - id
      - name
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
    AdminRestaurantTableResponse:
      type: object
      required:
      - id
      - code
      - area
      - minCapacity
      - maxCapacity
      - active
      properties:
        id:
          type: integer
          format: int64
        code:
          type: string
        area:
          $ref: '#/components/schemas/AdminTableAreaResponse'
        minCapacity:
          type: integer
          format: int32
        maxCapacity:
          type: integer
          format: int32
        active:
          type: boolean
    CreateRestaurantTableRequest:
      type: object
      required:
      - code
      - areaId
      - minCapacity
      - maxCapacity
      additionalProperties: false
      properties:
        code:
          type: string
          minLength: 1
          maxLength: 20
        areaId:
          type: integer
          format: int64
        minCapacity:
          type: integer
          format: int32
          minimum: 1
        maxCapacity:
          type: integer
          format: int32
          minimum: 1
    UpdateRestaurantTableRequest:
      type: object
      additionalProperties: false
      properties:
        code:
          type: string
          maxLength: 20
        areaId:
          type: integer
          format: int64
        minCapacity:
          type: integer
          format: int32
          minimum: 1
        maxCapacity:
          type: integer
          format: int32
          minimum: 1
    PageMetaFields:
      type: object
      required:
      - page
      - size
      - totalElements
      - totalPages
      - first
      - last
      properties:
        page:
          type: integer
          minimum: 0
          description: Zero-based page index, matching Spring Data PageResponse.
        size:
          type: integer
          minimum: 1
        totalElements:
          type: integer
          format: int64
          minimum: 0
        totalPages:
          type: integer
          minimum: 0
        first:
          type: boolean
        last:
          type: boolean
    PublicArea:
      type: object
      required:
      - id
      - name
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        description:
          type:
          - string
          - 'null'
    PublicCategory:
      type: object
      required:
      - id
      - name
      - displayOrder
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        displayOrder:
          type: integer
    PublicTag:
      type: object
      required:
      - id
      - name
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
    DishStatus:
      type: string
      enum:
      - AVAILABLE
      - SOLD_OUT
      - HIDDEN
    DishSummary:
      type: object
      required:
      - id
      - name
      - price
      - status
      - category
      - tags
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        description:
          type:
          - string
          - 'null'
        price:
          type: integer
          minimum: 1
          description: VND integer
        imageUrl:
          type:
          - string
          - 'null'
          format: uri
        status:
          allOf:
          - $ref: '#/components/schemas/DishStatus'
          description: Public endpoints never return HIDDEN dishes. SOLD_OUT may be returned for disabled purchase UI.
        category:
          $ref: '#/components/schemas/PublicCategory'
        tags:
          type: array
          items:
            $ref: '#/components/schemas/PublicTag'
    DishDetail:
      allOf:
      - $ref: '#/components/schemas/DishSummary'
      - type: object
        properties:
          displayOrder:
            type: integer
    PageDishSummary:
      allOf:
      - $ref: '#/components/schemas/PageMetaFields'
      - type: object
        required:
        - content
        properties:
          content:
            type: array
            items:
              $ref: '#/components/schemas/DishSummary'
    CreateDishRequest:
      type: object
      required:
      - categoryId
      - name
      - price
      properties:
        categoryId:
          type: integer
          format: int64
        name:
          type: string
          maxLength: 255
        description:
          type:
          - string
          - 'null'
        price:
          type: integer
          minimum: 1
        imageUrl:
          type:
          - string
          - 'null'
          format: uri
        status:
          $ref: '#/components/schemas/DishStatus'
        displayOrder:
          type: integer
          minimum: 0
          default: 0
        tagIds:
          type: array
          items:
            type: integer
            format: int64
    UpdateDishRequest:
      type: object
      properties:
        categoryId:
          type: integer
          format: int64
        name:
          type: string
          maxLength: 255
        description:
          type:
          - string
          - 'null'
        price:
          type: integer
          minimum: 1
        imageUrl:
          type:
          - string
          - 'null'
          format: uri
        displayOrder:
          type: integer
          minimum: 0
        tagIds:
          type: array
          items:
            type: integer
            format: int64
    UpdateDishStatusRequest:
      type: object
      required:
      - status
      properties:
        status:
          $ref: '#/components/schemas/DishStatus'
    RestaurantInfoResponse:
      type: object
      required:
      - name
      properties:
        name:
          type: string
        description:
          type:
          - string
          - 'null'
        address:
          type:
          - string
          - 'null'
        phone:
          type:
          - string
          - 'null'
        contactEmail:
          type:
          - string
          - 'null'
          format: email
        coverImageUrl:
          type:
          - string
          - 'null'
          format: uri
        openingHours:
          type: array
          items:
            $ref: '#/components/schemas/OpeningHourResponse'
    UpdateRestaurantInfoRequest:
      type: object
      properties:
        name:
          type: string
        description:
          type:
          - string
          - 'null'
        address:
          type:
          - string
          - 'null'
        phone:
          type:
          - string
          - 'null'
        contactEmail:
          type:
          - string
          - 'null'
          format: email
        coverImageUrl:
          type:
          - string
          - 'null'
          format: uri
    SpecialClosedDay:
      type: object
      required:
      - id
      - closedDate
      properties:
        id:
          type: integer
          format: int64
        closedDate:
          type: string
          format: date
        reason:
          type:
          - string
          - 'null'
    CreateSpecialClosedDayRequest:
      type: object
      required:
      - closedDate
      properties:
        closedDate:
          type: string
          format: date
        reason:
          type:
          - string
          - 'null'
    TableSuspension:
      type: object
      required:
      - id
      - tableId
      - startTime
      - endTime
      properties:
        id:
          type: integer
          format: int64
        tableId:
          type: integer
          format: int64
        startTime:
          type: string
          format: date-time
        endTime:
          type: string
          format: date-time
        reason:
          type:
          - string
          - 'null'
        impactedReservationIds:
          type: array
          items:
            type: integer
            format: int64
    CreateTableSuspensionRequest:
      type: object
      required:
      - startTime
      - endTime
      properties:
        startTime:
          type: string
          format: date-time
        endTime:
          type: string
          format: date-time
        reason:
          type:
          - string
          - 'null'
    AvailabilitySlot:
      type: object
      required:
      - startTime
      - endTime
      properties:
        startTime:
          type: string
          format: date-time
        endTime:
          type: string
          format: date-time
    AvailabilityResponse:
      type: object
      required:
      - date
      - partySize
      - durationMinutes
      - slots
      - suggestions
      properties:
        date:
          type: string
          format: date
        partySize:
          type: integer
          minimum: 1
          maximum: 12
        durationMinutes:
          type: integer
          minimum: 60
          maximum: 180
          multipleOf: 15
        areaId:
          type:
          - integer
          - 'null'
          format: int64
        slots:
          type: array
          items:
            $ref: '#/components/schemas/AvailabilitySlot'
        suggestions:
          type: array
          maxItems: 3
          items:
            $ref: '#/components/schemas/AvailabilitySlot'
          description: Up to 3 nearest alternatives when the requested search has no suitable slot.
    ReservationStatus:
      type: string
      enum:
      - PENDING
      - CONFIRMED
      - ARRIVED
      - COMPLETED
      - NO_SHOW
      - CANCELLED
      - REJECTED
    ReservationResponse:
      type: object
      required:
      - id
      - code
      - partySize
      - startTime
      - endTime
      - status
      - rescheduleCount
      properties:
        id:
          type: integer
          format: int64
        code:
          type: string
          example: SAGE-261018-7K3F
        partySize:
          type: integer
          minimum: 1
          maximum: 12
        startTime:
          type: string
          format: date-time
        endTime:
          type: string
          format: date-time
        status:
          $ref: '#/components/schemas/ReservationStatus'
        requestedArea:
          anyOf:
          - $ref: '#/components/schemas/PublicArea'
          - type: 'null'
        specialNote:
          type:
          - string
          - 'null'
        rescheduleCount:
          type: integer
          minimum: 0
          maximum: 2
        rejectionReason:
          type:
          - string
          - 'null'
        cancellationReason:
          type:
          - string
          - 'null'
        createdAt:
          type: string
          format: date-time
    CreateReservationRequest:
      type: object
      required:
      - startTime
      - endTime
      - partySize
      properties:
        startTime:
          type: string
          format: date-time
        endTime:
          type: string
          format: date-time
        partySize:
          type: integer
          minimum: 1
          maximum: 12
        requestedAreaId:
          type:
          - integer
          - 'null'
          format: int64
        specialNote:
          type:
          - string
          - 'null'
    RescheduleReservationRequest:
      type: object
      required:
      - startTime
      - endTime
      - partySize
      properties:
        startTime:
          type: string
          format: date-time
        endTime:
          type: string
          format: date-time
        partySize:
          type: integer
          minimum: 1
          maximum: 12
        requestedAreaId:
          type:
          - integer
          - 'null'
          format: int64
    PageReservation:
      allOf:
      - $ref: '#/components/schemas/PageMetaFields'
      - type: object
        required:
        - content
        properties:
          content:
            type: array
            items:
              $ref: '#/components/schemas/ReservationResponse'
    AdminReservationDetail:
      allOf:
      - $ref: '#/components/schemas/ReservationResponse'
      - type: object
        properties:
          customerId:
            type:
            - integer
            - 'null'
            format: int64
          customerName:
            type:
            - string
            - 'null'
          customerPhone:
            type:
            - string
            - 'null'
          guestName:
            type:
            - string
            - 'null'
          guestPhone:
            type:
            - string
            - 'null'
          table:
            anyOf:
            - $ref: '#/components/schemas/AdminRestaurantTableResponse'
            - type: 'null'
    RejectReservationRequest:
      type: object
      required:
      - reason
      properties:
        reason:
          type: string
          minLength: 1
    ReasonRequest:
      type: object
      properties:
        reason:
          type:
          - string
          - 'null'
    MoveTableRequest:
      type: object
      required:
      - tableId
      properties:
        tableId:
          type: integer
          format: int64
    ChangeDurationRequest:
      type: object
      required:
      - endTime
      properties:
        endTime:
          type: string
          format: date-time
    GuestReservationRequest:
      type: object
      required:
      - guestName
      - guestPhone
      - startTime
      - endTime
      - partySize
      properties:
        guestName:
          type: string
        guestPhone:
          type: string
          pattern: ^0[0-9]{9}$
        startTime:
          type: string
          format: date-time
        endTime:
          type: string
          format: date-time
        partySize:
          type: integer
          minimum: 1
          maximum: 12
        requestedAreaId:
          type:
          - integer
          - 'null'
          format: int64
        specialNote:
          type:
          - string
          - 'null'
    ReservationLogEntry:
      type: object
      required:
      - id
      - eventType
      - changedAt
      properties:
        id:
          type: integer
          format: int64
        eventType:
          type: string
          enum:
          - CREATED
          - STATUS_CHANGED
          - TABLE_CHANGED
          - TIME_CHANGED
          - RESCHEDULED
        oldValue:
          type:
          - object
          - 'null'
          additionalProperties: true
        newValue:
          type:
          - object
          - 'null'
          additionalProperties: true
        reason:
          type:
          - string
          - 'null'
        changedBy:
          type:
          - integer
          - 'null'
          format: int64
        changedAt:
          type: string
          format: date-time
    FoodOrderStatus:
      type: string
      enum:
      - PLACED
      - CANCELLED
    PaymentMethod:
      type: string
      const: COD
    FoodOrderCreateItemRequest:
      type: object
      required:
      - dishId
      - quantity
      properties:
        dishId:
          type: integer
          format: int64
        quantity:
          type: integer
          minimum: 1
    CreateFoodOrderRequest:
      type: object
      required:
      - items
      - deliveryAddress
      - deliveryPhone
      properties:
        items:
          type: array
          minItems: 1
          items:
            $ref: '#/components/schemas/FoodOrderCreateItemRequest'
        deliveryAddress:
          type: string
          minLength: 1
        deliveryPhone:
          type: string
          minLength: 1
      description: customerId and payment method are not accepted from the client; customer comes from JWT and payment is
        fixed to COD.
    FoodOrderItemResponse:
      type: object
      required:
      - dishId
      - dishName
      - quantity
      - unitPrice
      - lineTotal
      properties:
        dishId:
          type: integer
          format: int64
        dishName:
          type: string
        quantity:
          type: integer
          minimum: 1
        unitPrice:
          type: integer
          minimum: 1
          description: Immutable snapshot of dish.price at order creation.
        lineTotal:
          type: integer
          minimum: 1
          description: unitPrice * quantity
    FoodOrderResponse:
      type: object
      required:
      - id
      - deliveryPhone
      - deliveryAddress
      - status
      - paymentMethod
      - items
      - totalAmount
      - createdAt
      properties:
        id:
          type: integer
          format: int64
        deliveryPhone:
          type: string
        deliveryAddress:
          type: string
        status:
          $ref: '#/components/schemas/FoodOrderStatus'
        paymentMethod:
          $ref: '#/components/schemas/PaymentMethod'
        cancelledAt:
          type:
          - string
          - 'null'
          format: date-time
        items:
          type: array
          items:
            $ref: '#/components/schemas/FoodOrderItemResponse'
        totalAmount:
          type: integer
          minimum: 0
          description: Computed as SUM(unitPrice * quantity); not a persisted food_order column.
        createdAt:
          type: string
          format: date-time
    PageFoodOrder:
      allOf:
      - $ref: '#/components/schemas/PageMetaFields'
      - type: object
        required:
        - content
        properties:
          content:
            type: array
            items:
              $ref: '#/components/schemas/FoodOrderResponse'
    UpdateProfileRequest:
      type: object
      properties:
        fullName:
          type: string
        phone:
          type: string
          pattern: ^0[0-9]{9}$
    ChangePasswordRequest:
      type: object
      required:
      - currentPassword
      - newPassword
      properties:
        currentPassword:
          type: string
        newPassword:
          type: string
          minLength: 8
          pattern: ^(?=.*[A-Za-z])(?=.*\d).+$
    AccountAdminResponse:
      allOf:
      - $ref: '#/components/schemas/CurrentUserResponse'
      - type: object
        properties:
          locked:
            type: boolean
          createdAt:
            type: string
            format: date-time
    PageAccount:
      allOf:
      - $ref: '#/components/schemas/PageMetaFields'
      - type: object
        required:
        - content
        properties:
          content:
            type: array
            items:
              $ref: '#/components/schemas/AccountAdminResponse'
    SetAccountLockRequest:
      type: object
      required:
      - locked
      properties:
        locked:
          type: boolean
    SetAccountRoleRequest:
      type: object
      required:
      - role
      properties:
        role:
          $ref: '#/components/schemas/AccountRole'
    ReservationReport:
      type: object
      properties:
        from:
          type: string
          format: date
        to:
          type: string
          format: date
        totalReservations:
          type: integer
          minimum: 0
        series:
          type: array
          items:
            type: object
            properties:
              period:
                type: string
              count:
                type: integer
                minimum: 0
    OccupancyReport:
      type: object
      properties:
        peakHours:
          type: array
          items:
            type: string
        occupancyRate:
          type: number
          minimum: 0
          maximum: 1
    CancellationReport:
      type: object
      properties:
        cancellationRate:
          type: number
          minimum: 0
          maximum: 1
        noShowRate:
          type: number
          minimum: 0
          maximum: 1
    CreateReviewRequest:
      type: object
      required:
      - rating
      properties:
        rating:
          type: integer
          minimum: 1
          maximum: 5
        comment:
          type:
          - string
          - 'null'
    ReviewResponse:
      type: object
      required:
      - id
      - reservationId
      - rating
      - createdAt
      properties:
        id:
          type: integer
          format: int64
        reservationId:
          type: integer
          format: int64
        rating:
          type: integer
          minimum: 1
          maximum: 5
        comment:
          type:
          - string
          - 'null'
        createdAt:
          type: string
          format: date-time
paths:
  /api/auth/register:
    post:
      tags:
      - Auth
      summary: Register a CUSTOMER account
      operationId: register
      x-sage-status: implemented
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - code:AuthController
      - code:RegisterRequest
      - figma:Register
      - doc:FR-AUTH-01
      x-sage-priority: MUST
      description: Creates a CUSTOMER account. No role can be selected by the public client.
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RegisterRequest'
      responses:
        '201':
          description: Account created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CurrentUserResponse'
        '400': &id001
          $ref: '#/components/responses/BadRequest'
        '409': &id003
          $ref: '#/components/responses/Conflict'
  /api/auth/login:
    post:
      tags:
      - Auth
      summary: Log in and issue a JWT access token
      operationId: login
      x-sage-status: implemented
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - code:AuthController
      - code:LoginResponse
      - figma:Login
      - doc:UC-02
      x-sage-priority: MUST
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoginRequest'
      responses:
        '200':
          description: Authenticated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LoginResponse'
        '400': *id001
        '401': &id002
          $ref: '#/components/responses/Unauthorized'
  /api/auth/me:
    get:
      tags:
      - Auth
      summary: Get the current authenticated account
      operationId: getCurrentUser
      x-sage-status: implemented
      x-sage-roles:
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - code:AuthController
      - code:CurrentUserResponse
      - doc:09-auth
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      responses:
        '200':
          description: Current user.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CurrentUserResponse'
        '401': *id002
  /api/opening-hours:
    get:
      tags:
      - Restaurant
      summary: Get public weekly opening hours
      operationId: getOpeningHours
      x-sage-status: implemented
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - code:OpeningHourController
      - figma:Home
      - doc:FR-INFO-01
      x-sage-priority: MUST
      security: []
      responses:
        '200':
          description: Weekly opening hours.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/OpeningHourResponse'
  /api/admin/opening-hours:
    get:
      tags:
      - Admin Restaurant
      summary: List opening hours for administration
      operationId: adminListOpeningHours
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminOpeningHourController
      - doc:FR-INFO-02
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      responses:
        '200':
          description: Opening-hour rows.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/AdminOpeningHourResponse'
        '401': *id002
        '403': &id004
          $ref: '#/components/responses/Forbidden'
    post:
      tags:
      - Admin Restaurant
      summary: Create an opening-hour row
      operationId: adminCreateOpeningHour
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminOpeningHourController
      - doc:FR-INFO-02
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOpeningHourRequest'
      responses:
        '201':
          description: Opening hour created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminOpeningHourResponse'
        '400': *id001
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/opening-hours/{id}:
    patch:
      tags:
      - Admin Restaurant
      summary: Update an opening-hour row
      operationId: adminUpdateOpeningHour
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminOpeningHourController
      - doc:FR-INFO-02
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int32
          minimum: 0
        description: Day-of-week primary key used by the current table
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateOpeningHourRequest'
      responses:
        '200':
          description: Opening hour updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminOpeningHourResponse'
        '400': *id001
        '404': &id005
          $ref: '#/components/responses/NotFound'
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/menu/categories:
    get:
      tags:
      - Admin Menu
      summary: List menu categories for administration
      operationId: adminListCategories
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminMenuCategoryController
      - doc:FR-MENU-05
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      responses:
        '200':
          description: Categories.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/AdminCategoryResponse'
        '401': *id002
        '403': *id004
    post:
      tags:
      - Admin Menu
      summary: Create a menu category
      operationId: adminCreateCategory
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminMenuCategoryController
      - doc:FR-MENU-05
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateCategoryRequest'
      responses:
        '201':
          description: Category created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminCategoryResponse'
        '400': *id001
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/menu/categories/{id}:
    patch:
      tags:
      - Admin Menu
      summary: Update or deactivate a menu category
      operationId: adminUpdateCategory
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminMenuCategoryController
      - code:UpdateCategoryRequest
      - doc:FR-MENU-05
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateCategoryRequest'
      responses:
        '200':
          description: Category updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminCategoryResponse'
        '400': *id001
        '404': *id005
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/menu/tags:
    get:
      tags:
      - Admin Menu
      summary: List menu tags for administration
      operationId: adminListTags
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminMenuTagController
      - doc:FR-MENU-08
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      responses:
        '200':
          description: Tags.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/AdminTagResponse'
        '401': *id002
        '403': *id004
    post:
      tags:
      - Admin Menu
      summary: Create a menu tag
      operationId: adminCreateTag
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminMenuTagController
      - doc:FR-MENU-08
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateTagRequest'
      responses:
        '201':
          description: Tag created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminTagResponse'
        '400': *id001
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/menu/tags/{id}:
    patch:
      tags:
      - Admin Menu
      summary: Update a menu tag
      operationId: adminUpdateTag
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminMenuTagController
      - doc:FR-MENU-08
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateTagRequest'
      responses:
        '200':
          description: Tag updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminTagResponse'
        '400': *id001
        '404': *id005
        '409': *id003
        '401': *id002
        '403': *id004
    delete:
      tags:
      - Admin Menu
      summary: Delete an unused menu tag
      operationId: adminDeleteTag
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminMenuTagController
      - doc:FR-MENU-08
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '204':
          description: Tag deleted.
        '404': *id005
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/areas:
    get:
      tags:
      - Admin Tables
      summary: List restaurant areas for administration
      operationId: adminListAreas
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminAreaController
      - doc:FR-TBL-01
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      responses:
        '200':
          description: Areas.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/AdminAreaResponse'
        '401': *id002
        '403': *id004
    post:
      tags:
      - Admin Tables
      summary: Create a restaurant area
      operationId: adminCreateArea
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminAreaController
      - doc:FR-TBL-01
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateAreaRequest'
      responses:
        '201':
          description: Area created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminAreaResponse'
        '400': *id001
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/areas/{id}:
    patch:
      tags:
      - Admin Tables
      summary: Update a restaurant area
      operationId: adminUpdateArea
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminAreaController
      - doc:FR-TBL-01
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateAreaRequest'
      responses:
        '200':
          description: Area updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminAreaResponse'
        '400': *id001
        '404': *id005
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/areas/{id}/deactivate:
    patch:
      tags:
      - Admin Tables
      summary: Deactivate a restaurant area
      operationId: adminDeactivateArea
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminAreaController
      - doc:FR-TBL-01
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Area deactivated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminAreaResponse'
        '404': *id005
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/tables:
    get:
      tags:
      - Admin Tables
      summary: List restaurant tables for administration
      operationId: adminListTables
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminRestaurantTableController
      - doc:FR-TBL-02
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      responses:
        '200':
          description: Tables.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/AdminRestaurantTableResponse'
        '401': *id002
        '403': *id004
    post:
      tags:
      - Admin Tables
      summary: Create a restaurant table
      operationId: adminCreateTable
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminRestaurantTableController
      - doc:FR-TBL-02
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateRestaurantTableRequest'
      responses:
        '201':
          description: Table created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminRestaurantTableResponse'
        '400': *id001
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/tables/{id}:
    patch:
      tags:
      - Admin Tables
      summary: Update a restaurant table
      operationId: adminUpdateTable
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminRestaurantTableController
      - doc:FR-TBL-02
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateRestaurantTableRequest'
      responses:
        '200':
          description: Table updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminRestaurantTableResponse'
        '400': *id001
        '404': *id005
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/tables/{id}/deactivate:
    patch:
      tags:
      - Admin Tables
      summary: Deactivate a restaurant table
      operationId: adminDeactivateTable
      x-sage-status: implemented
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - code:AdminRestaurantTableController
      - doc:FR-TBL-02
      x-sage-priority: MUST
      x-sage-implementation-gap: Current SecurityConfig currently permits `/api/admin/**` anonymously because the `hasRole("ADMIN")`
        rule is commented out. This OpenAPI contract documents the intended authorization target, not that temporary implementation
        gap.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Table deactivated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminRestaurantTableResponse'
        '404': *id005
        '409': *id003
        '401': *id002
        '403': *id004
  /api/restaurant:
    get:
      tags:
      - Restaurant
      summary: Get public restaurant information
      operationId: getRestaurantInfo
      x-sage-status: planned
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-INFO-01
      - doc:UC-04
      - figma:Home
      x-sage-priority: MUST
      description: Proposed URL; the capability is required by the documents and Home UI, but no controller exists yet.
      security: []
      responses:
        '200':
          description: Restaurant information.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RestaurantInfoResponse'
  /api/areas:
    get:
      tags:
      - Restaurant
      summary: List active areas for reservation selection
      operationId: listPublicAreas
      x-sage-status: planned
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - figma:Reservation area selector
      - doc:FR-RSV-01
      - doc:BR-14
      x-sage-priority: MUST
      description: UI-driven proposed endpoint for the optional area selector.
      security: []
      responses:
        '200':
          description: Active areas.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/PublicArea'
  /api/menu/categories:
    get:
      tags:
      - Menu
      summary: List active public menu categories
      operationId: listPublicCategories
      x-sage-status: planned
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-MENU-01
      - figma:Menu category tabs
      x-sage-priority: MUST
      security: []
      responses:
        '200':
          description: Active categories.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/PublicCategory'
  /api/menu/tags:
    get:
      tags:
      - Menu
      summary: List tags available for public menu filtering
      operationId: listPublicTags
      x-sage-status: planned
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-MENU-04
      - figma:Menu tag filter
      x-sage-priority: SHOULD
      security: []
      responses:
        '200':
          description: Tags.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/PublicTag'
  /api/menu/dishes:
    get:
      tags:
      - Menu
      summary: Browse, search and filter public dishes
      operationId: listPublicDishes
      x-sage-status: planned
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-MENU-01/03/04
      - doc:BR-40/41/43
      - figma:Menu search/filter/pagination
      x-sage-priority: MUST
      description: HIDDEN dishes must be excluded. SOLD_OUT may be returned so the UI can show the dish as unavailable.
      security: []
      parameters:
      - name: page
        in: query
        required: false
        schema:
          type: integer
          minimum: 0
          default: 0
        description: Zero-based page index.
      - name: size
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
          maximum: 100
          default: 9
      - name: search
        in: query
        required: false
        schema:
          type: string
        description: Case-insensitive dish-name search.
      - name: categoryId
        in: query
        required: false
        schema:
          type: integer
          format: int64
      - name: minPrice
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
      - name: maxPrice
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
      - name: tagId
        in: query
        required: false
        schema:
          type: integer
          format: int64
      responses:
        '200':
          description: Paginated dishes.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageDishSummary'
        '400': *id001
  /api/menu/dishes/{id}:
    get:
      tags:
      - Menu
      summary: Get public dish detail
      operationId: getPublicDish
      x-sage-status: planned
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-MENU-02
      - doc:BR-41
      - figma:Dish Detail
      x-sage-priority: MUST
      description: If the dish is HIDDEN, the public API should behave as not found.
      security: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Dish detail.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DishDetail'
        '404': *id005
  /api/admin/restaurant:
    patch:
      tags:
      - Admin Restaurant
      summary: Update restaurant information
      operationId: adminUpdateRestaurantInfo
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-INFO-02
      - doc:UC-22
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateRestaurantInfoRequest'
      responses:
        '200':
          description: Restaurant information updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RestaurantInfoResponse'
        '400': *id001
        '401': *id002
        '403': *id004
  /api/admin/special-closed-days:
    get:
      tags:
      - Admin Restaurant
      summary: List special closed days
      operationId: adminListSpecialClosedDays
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-INFO-03
      - doc:UC-22
      - doc:BR-10
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      responses:
        '200':
          description: Special closed days.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/SpecialClosedDay'
        '401': *id002
        '403': *id004
    post:
      tags:
      - Admin Restaurant
      summary: Declare a special closed day
      operationId: adminCreateSpecialClosedDay
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-INFO-03
      - doc:BR-48
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateSpecialClosedDayRequest'
      responses:
        '201':
          description: Closed day created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SpecialClosedDay'
        '400': *id001
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/special-closed-days/{id}:
    delete:
      tags:
      - Admin Restaurant
      summary: Remove a special closed-day declaration
      operationId: adminDeleteSpecialClosedDay
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-INFO-03
      - doc:UC-22
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '204':
          description: Special closed day removed.
        '404': *id005
        '401': *id002
        '403': *id004
  /api/admin/menu/dishes:
    get:
      tags:
      - Admin Menu
      summary: List dishes for administration
      operationId: adminListDishes
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-MENU-06/07/08
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: page
        in: query
        required: false
        schema:
          type: integer
          minimum: 0
          default: 0
      - name: size
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
          maximum: 100
          default: 20
      - name: categoryId
        in: query
        required: false
        schema:
          type: integer
          format: int64
      - name: status
        in: query
        required: false
        schema:
          $ref: '#/components/schemas/DishStatus'
      responses:
        '200':
          description: Paginated dishes.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageDishSummary'
        '401': *id002
        '403': *id004
    post:
      tags:
      - Admin Menu
      summary: Create a dish
      operationId: adminCreateDish
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-MENU-06/08
      - erd:Dish
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateDishRequest'
      responses:
        '201':
          description: Dish created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DishDetail'
        '400': *id001
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/menu/dishes/{id}:
    get:
      tags:
      - Admin Menu
      summary: Get a dish for administration
      operationId: adminGetDish
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-MENU-06/08
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Dish detail.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DishDetail'
        '404': *id005
        '401': *id002
        '403': *id004
    patch:
      tags:
      - Admin Menu
      summary: Update a dish
      operationId: adminUpdateDish
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-MENU-06/08
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateDishRequest'
      responses:
        '200':
          description: Dish updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DishDetail'
        '400': *id001
        '404': *id005
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/menu/dishes/{id}/status:
    patch:
      tags:
      - Admin Menu
      summary: Change dish availability/visibility status
      operationId: adminUpdateDishStatus
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-MENU-07
      - doc:BR-41
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateDishStatusRequest'
      responses:
        '200':
          description: Dish status updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/DishDetail'
        '400': *id001
        '404': *id005
        '401': *id002
        '403': *id004
  /api/admin/tables/{tableId}/suspensions:
    get:
      tags:
      - Admin Tables
      summary: List table suspensions
      operationId: adminListTableSuspensions
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-TBL-03
      - doc:BR-16/48
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      parameters:
      - name: tableId
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Suspensions.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/TableSuspension'
        '401': *id002
        '403': *id004
    post:
      tags:
      - Admin Tables
      summary: Suspend a table for a time range
      operationId: adminCreateTableSuspension
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-TBL-03
      - doc:BR-16/48
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      parameters:
      - name: tableId
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateTableSuspensionRequest'
      responses:
        '201':
          description: Suspension created. impactedReservationIds reports existing holds affected by the suspension.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TableSuspension'
        '400': *id001
        '404': *id005
        '409': *id003
        '401': *id002
        '403': *id004
  /api/admin/tables/{tableId}/suspensions/{suspensionId}:
    delete:
      tags:
      - Admin Tables
      summary: Remove a table suspension
      operationId: adminDeleteTableSuspension
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-TBL-03
      - doc:BR-16/48
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      parameters:
      - name: tableId
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      - name: suspensionId
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '204':
          description: Suspension removed.
        '404': *id005
        '401': *id002
        '403': *id004
  /api/availability:
    get:
      tags:
      - Availability
      summary: Search available reservation start times
      operationId: searchAvailability
      x-sage-status: planned
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-RSV-01/02/03
      - doc:UC-06
      - figma:Reservation
      x-sage-priority: MUST
      description: Generate 15-minute start slots in Asia/Ho_Chi_Minh. Customer lead time is at least 2 hours, or 4 hours
        for party size >= 7; maximum booking horizon is 30 days. The selected duration must finish no later than closing time.
      security: []
      parameters:
      - name: date
        in: query
        required: true
        schema:
          type: string
          format: date
      - name: partySize
        in: query
        required: true
        schema:
          type: integer
          minimum: 1
          maximum: 12
      - name: durationMinutes
        in: query
        required: true
        schema:
          type: integer
          minimum: 60
          maximum: 180
          multipleOf: 15
          default: 90
      - name: areaId
        in: query
        required: false
        schema:
          type: integer
          format: int64
        description: Optional requested area.
      responses:
        '200':
          description: Availability.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AvailabilityResponse'
        '400': *id001
  /api/reservations:
    post:
      tags:
      - Reservations
      summary: Create a CUSTOMER reservation
      operationId: createReservation
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:FR-RSV-04/05/06/07/11
      - doc:UC-07
      - figma:Reservation/Reservation Success
      x-sage-priority: MUST
      description: The backend auto-assigns a suitable table; the client does not choose a table. Party sizes 1-6 become CONFIRMED
        when capacity is available; 7-12 become PENDING.
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateReservationRequest'
      responses:
        '201':
          description: Reservation created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
        '400': *id001
        '401': *id002
        '403': *id004
        '409': *id003
  /api/me/reservations:
    get:
      tags:
      - Reservations
      summary: List my reservations
      operationId: listMyReservations
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:FR-RSV-08
      - doc:UC-08
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: scope
        in: query
        required: false
        schema:
          type: string
          enum:
          - upcoming
          - history
          - all
          default: all
      - name: page
        in: query
        required: false
        schema:
          type: integer
          minimum: 0
          default: 0
      - name: size
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
          maximum: 100
          default: 20
      responses:
        '200':
          description: Owned reservations.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageReservation'
        '401': *id002
        '403': *id004
  /api/me/reservations/{id}:
    get:
      tags:
      - Reservations
      summary: Get one of my reservations
      operationId: getMyReservation
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:FR-RSV-08
      - doc:BR-38
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Owned reservation.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
        '401': *id002
        '403': *id004
        '404': *id005
  /api/me/reservations/{id}/cancel:
    post:
      tags:
      - Reservations
      summary: Cancel my reservation
      operationId: cancelMyReservation
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:FR-RSV-09
      - doc:UC-09
      - doc:BR-22
      x-sage-priority: MUST
      description: Allowed only for owned PENDING/CONFIRMED reservations while at least 2 hours remain before start time.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Reservation cancelled.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/me/reservations/{id}/reschedule:
    post:
      tags:
      - Reservations
      summary: Reschedule my reservation
      operationId: rescheduleMyReservation
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:FR-RSV-10
      - doc:UC-10
      - doc:BR-22/23/24
      x-sage-priority: SHOULD
      description: Maximum 2 successful reschedules. The original reservation remains unchanged if the new slot cannot be
        secured.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RescheduleReservationRequest'
      responses:
        '200':
          description: Reservation rescheduled.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationResponse'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reservations:
    get:
      tags:
      - Staff Reservations
      summary: List and filter reservations for operations
      operationId: opsListReservations
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source: &id006
      - doc:FR-ADM
      - doc:UC-11..19
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: date
        in: query
        required: false
        schema:
          type: string
          format: date
      - name: status
        in: query
        required: false
        schema:
          $ref: '#/components/schemas/ReservationStatus'
      - name: areaId
        in: query
        required: false
        schema:
          type: integer
          format: int64
      - name: search
        in: query
        required: false
        schema:
          type: string
        description: Name, phone, or reservation code.
      - name: page
        in: query
        required: false
        schema:
          type: integer
          minimum: 0
          default: 0
      - name: size
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
          maximum: 100
          default: 20
      responses:
        '200':
          description: Filtered reservations.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageReservation'
        '401': *id002
        '403': *id004
  /api/admin/reservations/{id}:
    get:
      tags:
      - Staff Reservations
      summary: Get reservation operation detail
      operationId: opsGetReservation
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source: *id006
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Reservation detail.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '401': *id002
        '403': *id004
        '404': *id005
  /api/admin/reservations/{id}/approve:
    post:
      tags:
      - Staff Reservations
      summary: Approve a pending reservation
      operationId: opsApproveReservation
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:UC-12
      - doc:BR reservation state rules
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Updated reservation.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reservations/{id}/reject:
    post:
      tags:
      - Staff Reservations
      summary: Reject a pending reservation
      operationId: opsRejectReservation
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:UC-12
      - doc:BR reservation state rules
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RejectReservationRequest'
      responses:
        '200':
          description: Updated reservation.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reservations/{id}/arrive:
    post:
      tags:
      - Staff Reservations
      summary: Mark customer as arrived
      operationId: opsMarkArrived
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:UC-13
      - doc:BR reservation state rules
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Updated reservation.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reservations/{id}/complete:
    post:
      tags:
      - Staff Reservations
      summary: Mark reservation as completed
      operationId: opsMarkCompleted
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:UC-13
      - doc:BR reservation state rules
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Updated reservation.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reservations/{id}/no-show:
    post:
      tags:
      - Staff Reservations
      summary: Mark reservation as no-show
      operationId: opsMarkNoShow
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:UC-13
      - doc:BR reservation state rules
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Updated reservation.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reservations/{id}/cancel:
    post:
      tags:
      - Staff Reservations
      summary: Cancel a reservation on behalf of the customer
      operationId: opsCancelReservation
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:UC-15
      - doc:BR reservation state rules
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ReasonRequest'
      responses:
        '200':
          description: Updated reservation.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reservations/{id}/move-table:
    post:
      tags:
      - Staff Reservations
      summary: Move a reservation to another table
      operationId: opsMoveReservationTable
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:UC-16
      - doc:BR reservation state rules
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/MoveTableRequest'
      responses:
        '200':
          description: Updated reservation.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reservations/{id}/change-duration:
    post:
      tags:
      - Staff Reservations
      summary: Change reservation end time
      operationId: opsChangeReservationDuration
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:UC-17
      - doc:BR reservation state rules
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ChangeDurationRequest'
      responses:
        '200':
          description: Updated reservation.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reservations/guest:
    post:
      tags:
      - Staff Reservations
      summary: Create a reservation on behalf of a guest
      operationId: opsCreateGuestReservation
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-ADM-06
      - doc:UC-14
      - doc:BR-39
      x-sage-priority: SHOULD
      description: Guest booking may have no customer account. It starts CONFIRMED and is not subject to CUSTOMER lead-time
        or 60-180 minute duration limits, but still must be within opening hours and party size <= 12.
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GuestReservationRequest'
      responses:
        '201':
          description: Guest reservation created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdminReservationDetail'
        '400': *id001
        '401': *id002
        '403': *id004
        '409': *id003
  /api/admin/reservations/{id}/logs:
    get:
      tags:
      - Staff Reservations
      summary: Get reservation change log
      operationId: opsGetReservationLogs
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-ADM-09
      - doc:BR-32
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Change log.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/ReservationLogEntry'
        '401': *id002
        '403': *id004
        '404': *id005
  /api/admin/reservations/timeline:
    get:
      tags:
      - Staff Reservations
      summary: Get reservation timeline for a day
      operationId: opsGetReservationTimeline
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-ADM-05
      - doc:UC-18
      x-sage-priority: SHOULD
      description: Response shape is intentionally left as a reservation list grouped/rendered by the client until the staff
        timeline UI is designed.
      security:
      - bearerAuth: []
      parameters:
      - name: date
        in: query
        required: true
        schema:
          type: string
          format: date
      responses:
        '200':
          description: Reservations for timeline rendering.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/AdminReservationDetail'
        '401': *id002
        '403': *id004
  /api/orders:
    post:
      tags:
      - Food Orders
      summary: Place a delivery food order
      operationId: createFoodOrder
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:08 §11
      - doc:FR-ORD-01..05
      - figma:Food Order/Order Success
      x-sage-priority: MUST
      description: Creates PLACED + COD order. unitPrice is copied from each current dish.price inside the transaction.
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateFoodOrderRequest'
      responses:
        '201':
          description: Order placed.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FoodOrderResponse'
        '400': *id001
        '401': *id002
        '403': *id004
        '409': *id003
  /api/me/orders:
    get:
      tags:
      - Food Orders
      summary: List my delivery orders
      operationId: listMyFoodOrders
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:08 §11
      - doc:FR-ORD-08
      - doc:BR-57
      x-sage-priority: MUST
      description: Always filters by the authenticated customer id and sorts newest first.
      security:
      - bearerAuth: []
      parameters:
      - name: page
        in: query
        required: false
        schema:
          type: integer
          minimum: 0
          default: 0
      - name: size
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
          maximum: 100
          default: 20
      responses:
        '200':
          description: Owned order history.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageFoodOrder'
        '401': *id002
        '403': *id004
  /api/me/orders/{id}:
    get:
      tags:
      - Food Orders
      summary: Get one of my delivery orders
      operationId: getMyFoodOrder
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:08 §11
      - doc:FR-ORD-09
      - doc:BR-57
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Owned order detail.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FoodOrderResponse'
        '401': *id002
        '403': *id004
        '404': *id005
  /api/me/orders/{id}/cancel:
    post:
      tags:
      - Food Orders
      summary: Cancel my PLACED delivery order
      operationId: cancelMyFoodOrder
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:08 §11
      - doc:FR-ORD-10
      - doc:BR-58
      x-sage-priority: MUST
      description: Only PLACED -> CANCELLED is allowed. The order remains in history and keeps all item price snapshots.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Order cancelled.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FoodOrderResponse'
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/orders:
    get:
      tags:
      - Operations Orders
      summary: List delivery orders for operations (read-only)
      operationId: opsListFoodOrders
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:08 §11
      - doc:FR-ORD-06/07
      - doc:BR-55
      x-sage-priority: MUST
      security:
      - bearerAuth: []
      parameters:
      - name: status
        in: query
        required: false
        schema:
          $ref: '#/components/schemas/FoodOrderStatus'
      - name: page
        in: query
        required: false
        schema:
          type: integer
          minimum: 0
          default: 0
      - name: size
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
          maximum: 100
          default: 20
      responses:
        '200':
          description: Orders.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageFoodOrder'
        '401': *id002
        '403': *id004
  /api/admin/orders/{id}:
    get:
      tags:
      - Operations Orders
      summary: Get delivery order detail for operations (read-only)
      operationId: opsGetFoodOrder
      x-sage-status: planned
      x-sage-roles:
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:08 §11
      - doc:FR-ORD-06/07
      - doc:BR-55
      x-sage-priority: MUST
      description: No STAFF/ADMIN status-update endpoint exists in the current scope.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '200':
          description: Order detail.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/FoodOrderResponse'
        '401': *id002
        '403': *id004
        '404': *id005
  /api/me/profile:
    patch:
      tags:
      - Accounts
      summary: Update my profile
      operationId: updateMyProfile
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:FR-AUTH-04
      - doc:UC-03
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UpdateProfileRequest'
      responses:
        '200':
          description: Profile updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CurrentUserResponse'
        '400': *id001
        '401': *id002
        '409': *id003
  /api/me/change-password:
    post:
      tags:
      - Accounts
      summary: Change my password
      operationId: changeMyPassword
      x-sage-status: planned
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:FR-AUTH-05
      - doc:UC-03
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ChangePasswordRequest'
      responses:
        '204':
          description: Password changed.
        '400': *id001
        '401': *id002
  /api/auth/forgot-password:
    post:
      tags:
      - Auth
      summary: Request password-reset email
      operationId: forgotPassword
      x-sage-status: planned
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      x-sage-source:
      - doc:FR-AUTH-06
      - figma:Login forgot-password link
      x-sage-priority: SHOULD
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
              - email
              properties:
                email:
                  type: string
                  format: email
      responses:
        '202':
          description: Request accepted without revealing whether the email exists.
        '400': *id001
  /api/auth/reset-password:
    post:
      tags:
      - Auth
      summary: Reset password using an email token
      operationId: resetPassword
      x-sage-status: planned
      x-sage-roles:
      - ANONYMOUS
      x-sage-source:
      - doc:FR-AUTH-06
      x-sage-priority: SHOULD
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required:
              - token
              - newPassword
              properties:
                token:
                  type: string
                newPassword:
                  type: string
                  minLength: 8
                  pattern: ^(?=.*[A-Za-z])(?=.*\d).+$
      responses:
        '204':
          description: Password reset.
        '400': *id001
  /api/admin/accounts:
    get:
      tags:
      - Accounts
      summary: List accounts for administration
      operationId: adminListAccounts
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-AUTH-08
      - doc:UC-23
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      parameters:
      - name: search
        in: query
        required: false
        schema:
          type: string
      - name: role
        in: query
        required: false
        schema:
          $ref: '#/components/schemas/AccountRole'
      - name: locked
        in: query
        required: false
        schema:
          type: boolean
      - name: page
        in: query
        required: false
        schema:
          type: integer
          minimum: 0
          default: 0
      - name: size
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
          maximum: 100
          default: 20
      responses:
        '200':
          description: Accounts.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/PageAccount'
        '401': *id002
        '403': *id004
  /api/admin/accounts/{id}/lock:
    patch:
      tags:
      - Accounts
      summary: Lock or unlock an account
      operationId: adminSetAccountLock
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-AUTH-08
      - doc:BR-38
      x-sage-priority: SHOULD
      description: An ADMIN must not lock itself.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SetAccountLockRequest'
      responses:
        '200':
          description: Account updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AccountAdminResponse'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/accounts/{id}/role:
    patch:
      tags:
      - Accounts
      summary: Change an account role
      operationId: adminSetAccountRole
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-AUTH-08
      - doc:BR-38/63
      x-sage-priority: SHOULD
      description: An ADMIN must not demote itself. Existing JWT becomes stale if its role claim no longer matches the DB
        role.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SetAccountRoleRequest'
      responses:
        '200':
          description: Account updated.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AccountAdminResponse'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/admin/reports/reservations:
    get:
      tags:
      - Reports
      summary: Get reservation volume report
      operationId: getReservationReport
      x-sage-status: planned
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-RPT-01
      - doc:UC-24
      x-sage-priority: SHOULD
      security:
      - bearerAuth: []
      parameters:
      - &id007
        name: from
        in: query
        required: true
        schema:
          type: string
          format: date
      - &id008
        name: to
        in: query
        required: true
        schema:
          type: string
          format: date
      - name: groupBy
        in: query
        required: false
        schema:
          type: string
          enum:
          - day
          - week
          default: day
      responses:
        '200':
          description: Reservation report.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReservationReport'
        '400': *id001
        '401': *id002
        '403': *id004
  /api/admin/reports/occupancy:
    get:
      tags:
      - Reports
      summary: Get peak-hour and occupancy report
      operationId: getOccupancyReport
      x-sage-status: future
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-RPT-02
      x-sage-priority: COULD
      security:
      - bearerAuth: []
      parameters: &id009
      - *id007
      - *id008
      responses:
        '200':
          description: Occupancy report.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OccupancyReport'
        '400': *id001
        '401': *id002
        '403': *id004
  /api/admin/reports/cancellations:
    get:
      tags:
      - Reports
      summary: Get cancellation and no-show report
      operationId: getCancellationReport
      x-sage-status: future
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-RPT-03
      x-sage-priority: COULD
      security:
      - bearerAuth: []
      parameters: *id009
      responses:
        '200':
          description: Cancellation report.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/CancellationReport'
        '400': *id001
        '401': *id002
        '403': *id004
  /api/me/reservations/{id}/review:
    post:
      tags:
      - Reviews
      summary: Review a completed reservation
      operationId: createReview
      x-sage-status: future
      x-sage-roles:
      - CUSTOMER
      x-sage-source:
      - doc:FR-REV-01
      - doc:BR-47
      x-sage-priority: COULD
      description: One review per completed reservation, within 14 days after the meal.
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateReviewRequest'
      responses:
        '201':
          description: Review created.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReviewResponse'
        '400': *id001
        '401': *id002
        '403': *id004
        '404': *id005
        '409': *id003
  /api/reviews:
    get:
      tags:
      - Reviews
      summary: List visible restaurant reviews
      operationId: listReviews
      x-sage-status: future
      x-sage-roles:
      - ANONYMOUS
      - CUSTOMER
      - STAFF
      - ADMIN
      x-sage-source:
      - doc:FR-REV-02
      x-sage-priority: COULD
      security: []
      parameters:
      - name: page
        in: query
        required: false
        schema:
          type: integer
          minimum: 0
          default: 0
      - name: size
        in: query
        required: false
        schema:
          type: integer
          minimum: 1
          maximum: 100
          default: 20
      responses:
        '200':
          description: Visible reviews.
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/ReviewResponse'
  /api/admin/reviews/{id}/hide:
    patch:
      tags:
      - Reviews
      summary: Hide a review
      operationId: adminHideReview
      x-sage-status: future
      x-sage-roles:
      - ADMIN
      x-sage-source:
      - doc:FR-REV-03
      x-sage-priority: COULD
      security:
      - bearerAuth: []
      parameters:
      - name: id
        in: path
        required: true
        schema:
          type: integer
          format: int64
          minimum: 1
      responses:
        '204':
          description: Review hidden.
        '401': *id002
        '403': *id004
        '404': *id005
x-sage-generated-from:
  repository: vanhung010/Sage
  repositoryRef: main
  repositoryCommitObserved: 118fd19d403f5631e2441d66592351782da7f5f4
  figmaFileKey: HV3JPSKjtm6rwGtZTFGAkc
  figmaRootNode: 0:1
  documents:
  - 01-project-summary.md
  - 02-functional-requirements-and-business-rules.md
  - 03-use-cases.md
  - 04-erd-database-design.md
  - 05-jpa-entities.md
  - 08-food-ordering-customer-history-cancellation-price-snapshot.md
  - 09-authentication-jwt-security-design.md
```

## 7. Explicitly excluded from the current API contract

These items are documented as outside the current MVP or not yet approved for implementation, so they are **not silently added as active API paths**:

- Refresh-token rotation, token blacklist/revocation, multi-device sessions, logout-all-devices.
- Online payment/payment gateway/refund.
- Delivery fee/zone, ETA, shipper tracking, full delivery state machine.
- Inventory/ingredient management.
- Promotion/voucher.
- Reservation deposit/payment.
- Multi-table merge.
- Pre-ordering food attached to a table reservation.
- Mandatory email verification.
- Google OAuth endpoint is a documented COULD item but has no approved concrete API contract yet.
- Favorite dishes and image-upload endpoints are COULD items; route/payload details should be specified when those features are scheduled.

## 8. Recommended implementation order

1. Fix `/api/admin/**` authorization before exposing the current admin controllers.
2. Public restaurant/menu APIs needed by Home, Menu and Dish Detail.
3. Availability + CUSTOMER reservation create/list/detail/cancel.
4. Food-order create/history/detail/cancel using price snapshots.
5. STAFF/ADMIN reservation operations and read-only food-order views.
6. Restaurant closed-day/table-suspension management.
7. SHOULD account/profile/reporting features.
8. COULD review/reporting extensions.


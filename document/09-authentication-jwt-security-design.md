# Saga Res: Thiết kế xác thực JWT và Spring Security

| | |
|---|---|
| **Dự án** | Saga Res (nhà hàng demo: Sage) |
| **Phiên bản** | 1.0 |
| **Ngày cập nhật** | 2026-10-03 |
| **Trạng thái** | Đã chốt thiết kế JWT cho MVP, đủ để triển khai Spring Security/API/test |
| **Tài liệu liên quan** | `01-project-summary.md`, `02-functional-requirements-and-business-rules.md`, `03-use-cases.md`, `04-erd-database-design.md`, `05-jpa-entities.md` |

---

## 1. Mục tiêu

Tài liệu này chốt cách Saga Res xác thực và phân quyền bằng **Spring Security + JWT** cho MVP. Thiết kế ưu tiên ít thành phần, dễ triển khai và dễ giải thích trong portfolio nhưng vẫn bảo đảm các yêu cầu quan trọng: mật khẩu BCrypt, Bearer token, phân quyền CUSTOMER/STAFF/ADMIN, account lock có hiệu lực ngay, lỗi `401/403` rõ ràng và không lưu token trong database.

## 2. Quyết định kiến trúc đã chốt

| Mã | Quyết định | Kết quả |
|---|---|---|
| JWT-D01 | Loại token | Chỉ **access token** trong MVP |
| JWT-D02 | Thời hạn | `1 giờ` / `3600` giây |
| JWT-D03 | Refresh token | **Không có** ở MVP; token hết hạn thì đăng nhập lại |
| JWT-D04 | Thuật toán | `HS256` |
| JWT-D05 | Secret | Tối thiểu 256 bit, lấy từ environment/secret manager; không commit source |
| JWT-D06 | Truyền token | `Authorization: Bearer <token>` |
| JWT-D07 | Session | `SessionCreationPolicy.STATELESS`; không dùng HTTP session cho auth |
| JWT-D08 | Logout | Client xóa access token; không revoke/blacklist phía server |
| JWT-D09 | Nguồn authority | Role hiện tại trong `account` DB; role claim chỉ phải khớp với DB |
| JWT-D10 | Account lock | Mỗi request protected tải Account theo `sub`; locked/nonexistent → `401` |
| JWT-D11 | Token storage DB | Không có bảng access token, refresh token hoặc blacklist |

Thiết kế này cố ý không bổ sung refresh token, token rotation hoặc revoke list để giữ MVP nhỏ. Khi cần session dài ngày hoặc đăng xuất/revoke trên nhiều thiết bị, mở rộng bằng refresh-token rotation trong một migration/đặc tả riêng.

## 3. JWT payload

Payload tối thiểu:

```json
{
  "iss": "saga-res",
  "sub": "123",
  "role": "CUSTOMER",
  "iat": 1790985600,
  "exp": 1790989200
}
```

Ý nghĩa:

| Claim | Bắt buộc | Ý nghĩa |
|---|---|---|
| `iss` | Có | Issuer cố định của Saga Res, ví dụ `saga-res` |
| `sub` | Có | `account.id` dạng chuỗi; không dùng email làm subject |
| `role` | Có | Role tại lúc cấp token: `CUSTOMER`, `STAFF`, `ADMIN` |
| `iat` | Có | Thời điểm phát hành |
| `exp` | Có | Hết hạn sau `iat + 1 giờ` |

Không đưa mật khẩu, password hash, số điện thoại, địa chỉ giao hàng, ghi chú đặt bàn hoặc dữ liệu nhạy cảm khác vào JWT. `email` cũng không cần nằm trong token vì backend tải Account theo `sub`.

## 4. Luồng đăng nhập

```text
Client
  │ POST /api/auth/login {email, password}
  ▼
AuthController
  ▼
AuthenticationManager / AuthService
  │ load Account by email
  │ reject if locked
  │ BCrypt password check
  ▼
JwtService
  │ issue HS256 access token
  │ sub=account.id, role=current role, exp=+1h
  ▼
200 LoginResponse
  accessToken
  tokenType = "Bearer"
  expiresIn = 3600
  user { id, fullName, role }
```

Sai email và sai mật khẩu phải trả cùng một thông báo chung để tránh account enumeration. Account bị khóa không được cấp token.

## 5. Luồng xác thực request protected

```text
HTTP request
  │ Authorization: Bearer <JWT>
  ▼
JwtAuthenticationFilter
  │ 1. parse Bearer header
  │ 2. verify HS256 signature
  │ 3. validate issuer + exp + required claims
  │ 4. read sub + role
  ▼
AccountRepository.findById(sub)
  │ reject if missing / locked
  │ compare token role with current DB role
  ▼
SecurityContext
  │ authority = current DB role
  ▼
Controller / Method Security
```

### Quy tắc quan trọng

- Không có Bearer token ở endpoint protected → `401`.
- Token sai signature, sai issuer, malformed hoặc hết hạn → `401`.
- `sub` không map tới Account hiện tại → `401`.
- Account đã bị khóa sau khi token được cấp → `401` ngay request kế tiếp.
- Role trong token khác role hiện tại trong DB → token stale → `401`, đăng nhập lại.
- Token hợp lệ nhưng authority không đủ quyền → `403`.

Việc tải Account mỗi request là quyết định có chủ đích: Saga Res có chức năng ADMIN khóa tài khoản/đổi role, nên thay đổi bảo mật phải có hiệu lực ngay thay vì đợi access token hết hạn.

## 6. API contract AUTH

### 6.1 `POST /api/auth/register`

**Public.**

Request:

```json
{
  "email": "customer@example.com",
  "password": "Secret123",
  "fullName": "Nguyen Van A",
  "phone": "0901234567"
}
```

Response thành công: `201 Created` với dữ liệu tài khoản tối thiểu; không trả `passwordHash`.

### 6.2 `POST /api/auth/login`

**Public.**

Request:

```json
{
  "email": "customer@example.com",
  "password": "Secret123"
}
```

Response `200 OK`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": 123,
    "fullName": "Nguyen Van A",
    "role": "CUSTOMER"
  }
}
```

### 6.3 `GET /api/auth/me`

**Authenticated.** Trả profile tối thiểu từ Account hiện tại. Endpoint này giúp frontend đồng bộ lại trạng thái người dùng sau reload và không phải tin hoàn toàn vào dữ liệu decode từ JWT.

### 6.4 Logout

MVP **không cần `POST /api/auth/logout`** vì server không lưu session/token. Frontend thực hiện logout bằng cách xóa token và auth state. Nếu sau này có refresh token, lúc đó mới bổ sung endpoint logout/revoke phía server.

### 6.5 Không có refresh endpoint

MVP không có `POST /api/auth/refresh`. Khi nhận `401` vì `exp`, frontend đưa người dùng về login và có thể lưu URL hiện tại để điều hướng lại sau khi đăng nhập.

## 7. Authorization matrix

Các pattern dưới đây là convention cho security layer; API chi tiết có thể tiếp tục được đặc tả trong OpenAPI.

| Nhóm endpoint | Anonymous | CUSTOMER | STAFF | ADMIN |
|---|---:|---:|---:|---:|
| `/api/auth/register`, `/api/auth/login` | ✓ | ✓ | ✓ | ✓ |
| Public restaurant info/menu/availability | ✓ | ✓ | ✓ | ✓ |
| `/api/auth/me` | ✗ | ✓ | ✓ | ✓ |
| Customer-owned reservation/food-order commands | ✗ | ✓ | ✗ | ✗ |
| Staff reservation operations | ✗ | ✗ | ✓ | ✓ |
| Food-order operation view | ✗ | ✗ | ✓ | ✓ |
| Admin configuration/menu/table/account management | ✗ | ✗ | ✗ | ✓ |

Ngoài role check, các API “của tôi” vẫn phải kiểm tra ownership bằng `customer_id == authenticatedAccountId`; role `CUSTOMER` không tự động cho phép đọc đơn của CUSTOMER khác.

## 8. Spring Security structure đề xuất

```text
security/
  SecurityConfig.java
  JwtAuthenticationFilter.java
  JwtService.java
  RestAuthenticationEntryPoint.java
  RestAccessDeniedHandler.java

auth/
  AuthController.java
  AuthService.java
  dto/
    RegisterRequest.java
    LoginRequest.java
    LoginResponse.java
    CurrentUserResponse.java
```

### Trách nhiệm

- `SecurityConfig`: filter chain, CORS, CSRF, stateless session, endpoint authorization, password encoder.
- `JwtService`: phát token, verify signature/issuer/expiry, đọc claims.
- `JwtAuthenticationFilter`: lấy Bearer token, validate, tải Account, kiểm tra locked/role stale, đặt `Authentication` vào `SecurityContext`.
- `RestAuthenticationEntryPoint`: chuẩn hóa `401`.
- `RestAccessDeniedHandler`: chuẩn hóa `403`.
- `AuthService`: register/login và mapping Account → auth response.

Không đặt logic parse JWT trong controller.

## 9. SecurityFilterChain contract

Cấu hình phải có các nguyên tắc sau:

```text
CSRF: disabled cho API Bearer stateless
Session: STATELESS
CORS: chỉ origin frontend được cấu hình
Public: register/login + các GET public của restaurant/menu/availability
Protected: authenticated theo role/ownership
JWT filter: chạy trước UsernamePasswordAuthenticationFilter
```

Nếu frontend/backend chạy khác origin trong development, CORS phải whitelist origin cụ thể từ config; không dùng `*` cùng credentials.

## 10. Secret và cấu hình

Ví dụ cấu hình logic:

```yaml
security:
  jwt:
    issuer: saga-res
    access-token-ttl: 1h
    secret: ${JWT_SECRET}
```

Quy tắc:

- `JWT_SECRET` không được có giá trị mặc định dùng cho production.
- Secret tối thiểu 256 bit entropy cho HS256.
- Không log secret hoặc toàn bộ JWT trong application log.
- Production nên inject secret qua secret manager / platform environment.
- Backend phải fail-fast khi secret thiếu hoặc không hợp lệ.

## 11. Frontend auth state

MVP dùng Bearer JWT nên frontend cần một auth layer tập trung:

- Sau login, giữ access token trong auth state; nếu cần tồn tại qua reload trong cùng phiên trình duyệt, có thể dùng `sessionStorage`.
- Không lưu access token vào URL.
- HTTP client interceptor thêm `Authorization: Bearer <token>`.
- `401`: xóa auth state/token và chuyển login; có thể giữ `returnUrl`.
- `403`: giữ phiên đăng nhập và hiển thị “Bạn không có quyền thực hiện thao tác này”.
- Logout: xóa auth state/token.

`localStorage` không phải lựa chọn mặc định trong thiết kế này vì token tồn tại lâu hơn vòng đời tab và vẫn truy cập được bởi JavaScript nếu có XSS. `sessionStorage` cũng không chống XSS; đây chỉ là lựa chọn giảm persistence cho MVP.

## 12. Error response chuẩn

Ví dụ `401`:

```json
{
  "status": 401,
  "code": "UNAUTHORIZED",
  "message": "Authentication is required or the access token is invalid."
}
```

Ví dụ `403`:

```json
{
  "status": 403,
  "code": "FORBIDDEN",
  "message": "You do not have permission to perform this action."
}
```

Không trả các message như “JWT signature mismatch”, secret key, stack trace hoặc chi tiết cryptographic ra client.

## 13. Test bắt buộc

### Unit tests

1. Password đúng/sai với BCrypt.
2. Tạo token có `iss/sub/role/iat/exp` đúng.
3. Token hết hạn bị reject.
4. Token sai issuer/signature/malformed bị reject.
5. Parse `sub` và role hợp lệ.

### Integration/security tests

1. `POST /api/auth/login` đúng thông tin → `200` + Bearer token.
2. Sai email hoặc password → lỗi chung, không cấp token.
3. Account locked → không login được.
4. Protected API không token → `401`.
5. Protected API token hết hạn/sai → `401`.
6. Token hợp lệ nhưng account bị khóa sau lúc cấp → `401`.
7. Token role khác DB role sau khi ADMIN đổi role → `401` stale token.
8. CUSTOMER gọi STAFF/ADMIN API → `403`.
9. STAFF gọi ADMIN-only API → `403`.
10. ADMIN gọi ADMIN API → thành công.
11. CUSTOMER A không đọc/sửa reservation hoặc food order của CUSTOMER B dù token CUSTOMER hợp lệ.
12. Logout phía client xóa token; request tiếp theo không có token → `401`.

## 14. Nội dung cố ý để sau

Không tự bổ sung vào MVP nếu chưa có quyết định mới:

- Refresh token và refresh-token rotation.
- Token blacklist / revoke list / Redis session store.
- Multi-device session management.
- “Logout all devices”.
- OAuth2/OIDC Google login (FR-AUTH-09 vẫn là COULD).
- MFA / OTP.
- Email verification bắt buộc.
- Asymmetric signing `RS256`/`ES256` và JWKS.

Nếu cần một trong các tính năng trên, phải mở rộng tài liệu JWT trước thay vì ngầm thêm vào code.

## 15. Tiêu chí hoàn thành JWT MVP

JWT được coi là hoàn thành khi:

1. Register lưu mật khẩu BCrypt và không trả password hash.
2. Login hợp lệ phát HS256 access token TTL 1 giờ.
3. JWT chứa đúng `iss`, `sub`, `role`, `iat`, `exp`.
4. API protected chỉ chấp nhận `Authorization: Bearer <token>` hợp lệ.
5. Backend tải Account hiện tại theo `sub` và chặn ngay account bị khóa.
6. Role token phải khớp role DB; khác nhau trả `401` và yêu cầu login lại.
7. Authorization tạo từ role DB và ownership được kiểm tra ở service/query.
8. `401` và `403` có contract riêng.
9. Security session là stateless; không có refresh token/token table/blacklist.
10. Secret không nằm trong source code và backend fail-fast nếu thiếu secret.
11. Có test cho expired/invalid/locked/stale-role/forbidden/ownership.
12. Swagger/OpenAPI đánh dấu security scheme Bearer JWT cho endpoint protected.

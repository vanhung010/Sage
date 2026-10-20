CREATE TABLE password_reset_request
(
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id             BIGINT      NOT NULL,

    -- HMAC-SHA256 của mã 6 số, lưu dưới dạng chuỗi hex.
    code_hash              VARCHAR(64) NOT NULL,
    code_expires_at        TIMESTAMPTZ NOT NULL,
    failed_attempts        INTEGER     NOT NULL DEFAULT 0,

    -- Được điền sau khi xác thực mã thành công.
    verified_at            TIMESTAMPTZ,

    -- SHA-256 của token cho phép đặt lại mật khẩu.
    reset_token_hash       VARCHAR(64),
    reset_token_expires_at TIMESTAMPTZ,

    -- Đã dùng để đổi mật khẩu thành công.
    used_at                TIMESTAMPTZ,

    -- Bị vô hiệu hóa, ví dụ khi người dùng yêu cầu mã mới.
    revoked_at             TIMESTAMPTZ,

    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_password_reset_request_account
        FOREIGN KEY (account_id)
            REFERENCES account (id),

    CONSTRAINT ck_password_reset_request_code_hash
        CHECK (code_hash ~ '^[0-9a-f]{64}$'),

    CONSTRAINT ck_password_reset_request_code_expiry
        CHECK (code_expires_at > created_at),

    CONSTRAINT ck_password_reset_request_failed_attempts
        CHECK (failed_attempts >= 0),

    CONSTRAINT ck_password_reset_request_token_hash
        CHECK (
            reset_token_hash IS NULL
            OR reset_token_hash ~ '^[0-9a-f]{64}$'
        ),

    -- Chưa xác thực: chưa có token.
    -- Đã xác thực: phải có đủ hash và hạn sử dụng token.
    CONSTRAINT ck_password_reset_request_verification
        CHECK (
            (
                verified_at IS NULL
                AND reset_token_hash IS NULL
                AND reset_token_expires_at IS NULL
            )
            OR
            (
                verified_at IS NOT NULL
                AND reset_token_hash IS NOT NULL
                AND reset_token_expires_at IS NOT NULL
                AND reset_token_expires_at > verified_at
            )
        ),

    CONSTRAINT ck_password_reset_request_used
        CHECK (
            used_at IS NULL
            OR (
                verified_at IS NOT NULL
                AND used_at >= verified_at
                AND revoked_at IS NULL
            )
        ),

    CONSTRAINT uq_password_reset_request_token
        UNIQUE (reset_token_hash)
);

-- Tra cứu lịch sử yêu cầu và hỗ trợ giới hạn gửi mã theo tài khoản.
CREATE INDEX idx_password_reset_request_account_created
    ON password_reset_request (account_id, created_at DESC);

-- Mỗi tài khoản chỉ có một yêu cầu chưa dùng, chưa bị vô hiệu hóa.
CREATE UNIQUE INDEX uq_password_reset_request_open_account
    ON password_reset_request (account_id)
    WHERE used_at IS NULL AND revoked_at IS NULL;

-- Hỗ trợ dọn dữ liệu cũ theo chính sách lưu trữ.
CREATE INDEX idx_password_reset_request_created
    ON password_reset_request (created_at);
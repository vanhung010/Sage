-- =========================================================
-- Saga Res - Initial Schema
-- PostgreSQL 15+
-- =========================================================

CREATE EXTENSION IF NOT EXISTS btree_gist;


-- =========================================================
-- 1. ACCOUNT
-- =========================================================

CREATE TABLE account
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(10)  NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    is_locked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_account_phone
        CHECK (phone ~ '^0[0-9]{9}$'),

    CONSTRAINT chk_account_role
        CHECK (role IN ('CUSTOMER', 'STAFF', 'ADMIN'))
);


-- =========================================================
-- 2. RESTAURANT INFO
-- =========================================================

CREATE TABLE restaurant_info
(
    id              SMALLINT PRIMARY KEY,
    name            VARCHAR(255),
    description     TEXT,
    address         VARCHAR(255),
    phone           VARCHAR(50),
    contact_email   VARCHAR(255),
    cover_image_url VARCHAR(500),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_restaurant_info_single_row
        CHECK (id = 1)
);


-- =========================================================
-- 3. OPENING HOURS
-- 0 = Sunday ... 6 = Saturday
-- =========================================================

CREATE TABLE opening_hour
(
    day_of_week SMALLINT PRIMARY KEY,
    open_time   TIME,
    close_time  TIME,
    is_closed   BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT chk_opening_hour_day
        CHECK (day_of_week BETWEEN 0 AND 6),

    CONSTRAINT chk_opening_hour_time
        CHECK (close_time > open_time)
);


-- =========================================================
-- 4. SPECIAL CLOSED DAY
-- =========================================================

CREATE TABLE special_closed_day
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    closed_date DATE NOT NULL UNIQUE,
    reason      VARCHAR(255)
);


-- =========================================================
-- 5. AREA
-- =========================================================

CREATE TABLE area
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);


-- =========================================================
-- 6. RESTAURANT TABLE
-- =========================================================

CREATE TABLE restaurant_table
(
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code         VARCHAR(20) NOT NULL UNIQUE,
    area_id      BIGINT      NOT NULL,
    min_capacity SMALLINT    NOT NULL,
    max_capacity SMALLINT    NOT NULL,
    is_active    BOOLEAN     NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_restaurant_table_area
        FOREIGN KEY (area_id)
            REFERENCES area (id),

    CONSTRAINT chk_restaurant_table_min_capacity
        CHECK (min_capacity > 0),

    CONSTRAINT chk_restaurant_table_capacity
        CHECK (max_capacity >= min_capacity)
);

CREATE INDEX idx_restaurant_table_area_active
    ON restaurant_table (area_id, is_active);


-- =========================================================
-- 7. TABLE SUSPENSION
-- =========================================================

CREATE TABLE table_suspension
(
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_id   BIGINT      NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time   TIMESTAMPTZ NOT NULL,
    reason     VARCHAR(255),
    created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_table_suspension_table
        FOREIGN KEY (table_id)
            REFERENCES restaurant_table (id),

    CONSTRAINT fk_table_suspension_created_by
        FOREIGN KEY (created_by)
            REFERENCES account (id),

    CONSTRAINT chk_table_suspension_time
        CHECK (end_time > start_time)
);


-- =========================================================
-- 8. CATEGORY
-- =========================================================

CREATE TABLE category
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name          VARCHAR(100) NOT NULL UNIQUE,
    display_order INT          NOT NULL DEFAULT 0,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE
);


-- =========================================================
-- 9. TAG
-- =========================================================

CREATE TABLE tag
(
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);


-- =========================================================
-- 10. DISH
-- =========================================================

CREATE TABLE dish
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id   BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    description   TEXT,
    price         INT          NOT NULL,
    image_url     VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    display_order INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT fk_dish_category
        FOREIGN KEY (category_id)
            REFERENCES category (id),

    CONSTRAINT chk_dish_price
        CHECK (price > 0),

    CONSTRAINT chk_dish_status
        CHECK (status IN ('AVAILABLE', 'SOLD_OUT', 'HIDDEN'))
);

CREATE INDEX idx_dish_category_status
    ON dish (category_id, status);


-- =========================================================
-- 11. DISH TAG
-- =========================================================

CREATE TABLE dish_tag
(
    dish_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,

    PRIMARY KEY (dish_id, tag_id),

    CONSTRAINT fk_dish_tag_dish
        FOREIGN KEY (dish_id)
            REFERENCES dish (id),

    CONSTRAINT fk_dish_tag_tag
        FOREIGN KEY (tag_id)
            REFERENCES tag (id)
);


-- =========================================================
-- 12. BUSINESS CONFIG
-- =========================================================

CREATE TABLE business_config
(
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    description  TEXT,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- =========================================================
-- 13. RESERVATION
-- =========================================================

CREATE TABLE reservation
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                VARCHAR(20) NOT NULL UNIQUE,

    customer_id         BIGINT,
    guest_name          VARCHAR(255),
    guest_phone         VARCHAR(10),

    table_id             BIGINT   NOT NULL,
    requested_area_id    BIGINT,

    party_size           SMALLINT NOT NULL,

    start_time           TIMESTAMPTZ NOT NULL,
    end_time             TIMESTAMPTZ NOT NULL,

    buffer_minutes       SMALLINT NOT NULL,

    -- PostgreSQL generated columns cannot use the required timestamptz
    -- arithmetic here because the generation expression must be immutable.
    -- These two columns are maintained by trg_sync_reservation_ranges.
    reservation_range   TSTZRANGE NOT NULL,
    occupied_range      TSTZRANGE NOT NULL,

    status               VARCHAR(20) NOT NULL,

    special_note         TEXT,

    reschedule_count     SMALLINT NOT NULL DEFAULT 0,

    created_by           BIGINT,

    rejection_reason     TEXT,
    cancellation_reason  TEXT,

    reminder_sent_at     TIMESTAMPTZ,

    version              BIGINT NOT NULL DEFAULT 0,

    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_reservation_customer
        FOREIGN KEY (customer_id)
            REFERENCES account (id),

    CONSTRAINT fk_reservation_table
        FOREIGN KEY (table_id)
            REFERENCES restaurant_table (id),

    CONSTRAINT fk_reservation_requested_area
        FOREIGN KEY (requested_area_id)
            REFERENCES area (id),

    CONSTRAINT fk_reservation_created_by
        FOREIGN KEY (created_by)
            REFERENCES account (id),

    CONSTRAINT chk_reservation_party_size
        CHECK (party_size BETWEEN 1 AND 12),

    CONSTRAINT chk_reservation_time
        CHECK (end_time > start_time),

    CONSTRAINT chk_reservation_status
        CHECK (
            status IN (
                       'PENDING',
                       'CONFIRMED',
                       'ARRIVED',
                       'COMPLETED',
                       'NO_SHOW',
                       'CANCELLED',
                       'REJECTED'
                )
            ),

    CONSTRAINT chk_reservation_reschedule_count
        CHECK (reschedule_count <= 2),

    CONSTRAINT chk_customer_or_guest
        CHECK (
            customer_id IS NOT NULL
                OR (
                guest_name IS NOT NULL
                    AND guest_phone IS NOT NULL
                    AND created_by IS NOT NULL
                )
            )
);


-- =========================================================
-- 14. RESERVATION RANGE SYNC TRIGGER
-- =========================================================

CREATE OR REPLACE FUNCTION sync_reservation_ranges()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.reservation_range :=
        tstzrange(
            NEW.start_time,
            NEW.end_time,
            '[)'
        );

    NEW.occupied_range :=
        tstzrange(
            NEW.start_time,
            NEW.end_time + (NEW.buffer_minutes * INTERVAL '1 minute'),
            '[)'
        );

RETURN NEW;
END;
$$;

CREATE TRIGGER trg_sync_reservation_ranges
    BEFORE INSERT OR UPDATE OF start_time, end_time, buffer_minutes
                     ON reservation
                         FOR EACH ROW
                         EXECUTE FUNCTION sync_reservation_ranges();


-- =========================================================
-- 15. RESERVATION STATUS LOG
-- =========================================================

CREATE TABLE reservation_status_log
(
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reservation_id BIGINT      NOT NULL,
    event_type     VARCHAR(30) NOT NULL,
    old_value      JSONB,
    new_value      JSONB,
    reason         TEXT,
    changed_by     BIGINT,
    changed_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_reservation_status_log_reservation
        FOREIGN KEY (reservation_id)
            REFERENCES reservation (id),

    CONSTRAINT fk_reservation_status_log_changed_by
        FOREIGN KEY (changed_by)
            REFERENCES account (id),

    CONSTRAINT chk_reservation_status_log_event_type
        CHECK (
            event_type IN (
                           'CREATED',
                           'STATUS_CHANGED',
                           'TABLE_CHANGED',
                           'TIME_CHANGED',
                           'RESCHEDULED'
                )
            )
);

CREATE INDEX idx_reservation_status_log_reservation_changed
    ON reservation_status_log (reservation_id, changed_at);


-- =========================================================
-- 16. FOOD ORDER
-- =========================================================

CREATE TABLE food_order
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id      BIGINT      NOT NULL,
    delivery_phone   VARCHAR(30) NOT NULL,
    delivery_address TEXT        NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PLACED',
    payment_method   VARCHAR(20) NOT NULL DEFAULT 'COD',
    cancelled_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT fk_food_order_customer
        FOREIGN KEY (customer_id)
            REFERENCES account (id),

    CONSTRAINT chk_food_order_status
        CHECK (status IN ('PLACED', 'CANCELLED')),

    CONSTRAINT chk_food_order_payment_method
        CHECK (payment_method = 'COD'),

    CONSTRAINT chk_food_order_cancelled_at
        CHECK (
            (status = 'PLACED' AND cancelled_at IS NULL)
                OR
            (status = 'CANCELLED' AND cancelled_at IS NOT NULL)
            )
);

CREATE INDEX idx_food_order_customer_created_at
    ON food_order (customer_id, created_at DESC);

CREATE INDEX idx_food_order_created_at
    ON food_order (created_at);


-- =========================================================
-- 17. FOOD ORDER ITEM
-- =========================================================

CREATE TABLE food_order_item
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    food_order_id BIGINT NOT NULL,
    dish_id       BIGINT NOT NULL,
    quantity      INT    NOT NULL,
    unit_price    INT    NOT NULL,

    CONSTRAINT fk_food_order_item_order
        FOREIGN KEY (food_order_id)
            REFERENCES food_order (id),

    CONSTRAINT fk_food_order_item_dish
        FOREIGN KEY (dish_id)
            REFERENCES dish (id),

    CONSTRAINT chk_food_order_item_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_food_order_item_unit_price
        CHECK (unit_price > 0)
);

CREATE INDEX idx_food_order_item_food_order
    ON food_order_item (food_order_id);


-- =========================================================
-- 18. RESERVATION INDEXES
-- =========================================================

CREATE INDEX idx_reservation_start_time
    ON reservation (start_time);

CREATE INDEX idx_reservation_customer_status
    ON reservation (customer_id, status);


-- =========================================================
-- 19. RESERVATION OVERLAP CONSTRAINTS
-- =========================================================

-- No two active reservations may occupy the same table
-- during overlapping occupied ranges.
ALTER TABLE reservation
    ADD CONSTRAINT reservation_table_no_overlap
    EXCLUDE USING gist
    (
        table_id WITH =,
        occupied_range WITH &&
    )
    WHERE (
        status IN (
            'PENDING',
            'CONFIRMED',
            'ARRIVED',
            'COMPLETED'
        )
    );


-- A customer cannot have two actual reservation periods overlapping.
-- Cleaning buffer is intentionally not used for this constraint.
ALTER TABLE reservation
    ADD CONSTRAINT reservation_customer_no_overlap
    EXCLUDE USING gist
    (
        customer_id WITH =,
        reservation_range WITH &&
    )
    WHERE (
        status IN (
            'PENDING',
            'CONFIRMED',
            'ARRIVED'
        )
        AND customer_id IS NOT NULL
    );


-- =========================================================
-- 20. INITIAL BUSINESS CONFIG
-- =========================================================

INSERT INTO business_config
(config_key, config_value, description)
VALUES
    ('buffer_minutes', '15',
     'Buffer time after reservation'),

    ('min_advance_hours', '2',
     'Minimum booking advance time'),

    ('min_advance_hours_large_group', '4',
     'Minimum advance time for large groups'),

    ('large_group_threshold', '7',
     'Party size requiring large-group rules'),

    ('max_advance_days', '30',
     'Maximum number of days a reservation can be booked in advance'),

    ('max_party_size', '12',
     'Maximum party size'),

    ('auto_confirm_threshold', '6',
     'Maximum party size eligible for automatic confirmation'),

    ('cancel_change_deadline_hours', '2',
     'Cancellation and reschedule deadline'),

    ('max_reschedule_count', '2',
     'Maximum reschedule attempts'),

    ('no_show_grace_minutes', '15',
     'Grace period before customer can be marked as no-show'),

    ('pending_auto_cancel_hours', '3',
     'Pending reservation auto-cancel threshold before start time'),

    ('reminder_hours_before', '3',
     'Reservation reminder time'),

    ('max_active_reservations_per_customer', '3',
     'Maximum active reservations per customer'),

    ('time_slot_step_minutes', '15',
     'Reservation time slot step'),

    ('default_duration_minutes', '90',
     'Default reservation duration'),

    ('min_duration_minutes', '60',
     'Minimum reservation duration'),

    ('max_duration_minutes', '180',
     'Maximum reservation duration');

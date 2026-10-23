package com.sagares.saga_res_api.reservation.specification;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.reservation.entity.Reservation;
import com.sagares.saga_res_api.reservation.entity.ReservationStatus;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;

public final class ReservationSpecifications {

    private ReservationSpecifications() {
    }

    /**
     * Lọc giờ bắt đầu trong khoảng [from, to).
     * Bao gồm from, không bao gồm to.
     */
    public static Specification<Reservation> startsWithin(Instant from, Instant to) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(
                        root.<Instant>get("startTime"),
                        from
                ),
                cb.lessThan(
                        root.<Instant>get("startTime"),
                        to
                )
        );
    }

    public static Specification<Reservation> hasStatus(
            ReservationStatus status
    ) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }

            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Reservation> inArea(Long areaId) {
        return (root, query, cb) -> {
            if (areaId == null) {
                return cb.conjunction();
            }

            // Khu vực của bàn đang được xếp.
            return cb.equal(
                    root.join("table")
                            .join("area")
                            .get("id"),
                    areaId
            );
        };
    }

    public static Specification<Reservation> matchesSearch(
            String search
    ) {
        if (search == null || search.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }

        String keyword = search.strip().toLowerCase(Locale.ROOT);
        String pattern = "%" + escapeLike(keyword) + "%";

        return (root, query, cb) -> {
            // LEFT JOIN để giữ lại đơn khách vãng lai:
            // những đơn này có customer = null.
            Join<Reservation, Account> customer =
                    root.join("customer", JoinType.LEFT);

            // Account.fullName và Account.phone là NOT NULL.
            // Không có customer thì dùng thông tin khách vãng lai.
            Expression<String> customerName = cb.coalesce(
                    customer.<String>get("fullName"),
                    root.<String>get("guestName")
            );

            Expression<String> customerPhone = cb.coalesce(
                    customer.<String>get("phone"),
                    root.<String>get("guestPhone")
            );

            return cb.or(
                    cb.like(
                            cb.lower(root.<String>get("code")),
                            pattern,
                            '!'
                    ),
                    cb.like(
                            cb.lower(customerName),
                            pattern,
                            '!'
                    ),
                    cb.like(
                            customerPhone,
                            pattern,
                            '!'
                    )
            );
        };
    }

    /**
     * %, _ là wildcard của LIKE.
     * Escape để tìm chúng như ký tự bình thường khi người dùng nhập.
     * Dấu ! được dùng làm escape character.
     */
    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
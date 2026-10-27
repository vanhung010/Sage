package com.sagares.saga_res_api.order.specification;

import com.sagares.saga_res_api.account.entity.Account;
import com.sagares.saga_res_api.order.entity.FoodOrder;
import com.sagares.saga_res_api.order.entity.FoodOrderStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor
public final class FoodOrderSpecifications {

    private static final ZoneId RESTAURANT_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static Specification<FoodOrder> createdOn(
            LocalDate date
    ) {
        if (date == null) {
            return (root, query, cb) -> cb.conjunction();
        }

        // Ngày địa phương được đổi thành khoảng Instant [from, to).
        Instant from = date
                .atStartOfDay(RESTAURANT_ZONE)
                .toInstant();

        Instant to = date
                .plusDays(1)
                .atStartOfDay(RESTAURANT_ZONE)
                .toInstant();

        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(
                        root.<Instant>get("createdAt"),
                        from
                ),
                cb.lessThan(
                        root.<Instant>get("createdAt"),
                        to
                )
        );
    }

    public static Specification<FoodOrder> hasStatus(
            FoodOrderStatus status
    ) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<FoodOrder> matchesSearch(
            String search
    ) {
        if (search == null || search.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }

        String keyword = search
                .strip()
                .toLowerCase(Locale.ROOT);

        String namePattern =
                "%" + escapeLike(keyword) + "%";

        // Cho phép tìm "0901 234 567" khi DB lưu "0901234567".
        String compactPhone =
                keyword.replaceAll("[\\s()+.\\-]", "");

        String phoneKeyword = compactPhone.matches("[0-9]+")
                ? compactPhone
                : keyword;

        String phonePattern =
                "%" + escapeLike(phoneKeyword) + "%";

        Long orderId = parseOrderId(keyword);

        return (root, query, cb) -> {
            // FoodOrder.customer là quan hệ bắt buộc.
            Join<FoodOrder, Account> customer =
                    root.join("customer", JoinType.INNER);

            List<Predicate> matches = new ArrayList<>();

            matches.add(cb.like(
                    cb.lower(customer.<String>get("fullName")),
                    namePattern,
                    '!'
            ));

            matches.add(cb.like(
                    customer.<String>get("phone"),
                    phonePattern,
                    '!'
            ));

            matches.add(cb.like(
                    root.<String>get("deliveryPhone"),
                    phonePattern,
                    '!'
            ));

            if (orderId != null) {
                matches.add(cb.equal(
                        root.<Long>get("id"),
                        orderId
                ));
            }

            return cb.or(matches.toArray(Predicate[]::new));
        };
    }

    private static Long parseOrderId(String keyword) {
        String value = keyword.startsWith("#")
                ? keyword.substring(1)
                : keyword;

        if (!value.matches("[0-9]+")) {
            return null;
        }

        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException exception) {
            // search là từ khóa tự do, không bắt buộc là Long hợp lệ.
            return null;
        }
    }

    private static String escapeLike(String value) {
        return value
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
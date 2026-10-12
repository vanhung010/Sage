package com.sagares.saga_res_api.restaurant.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.restaurant.dto.admin.CreateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.dto.admin.UpdateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.repository.OpeningHourRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Opt in against a disposable PostgreSQL instance; each run uses a fresh isolated schema.
@EnabledIfSystemProperty(named = "opening-hour.integration", matches = "true")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OpeningHourService.class)
class OpeningHourPersistenceTest {

    private static final LocalTime OPEN = LocalTime.of(10, 0);
    private static final LocalTime CLOSE = LocalTime.of(22, 0);
    private static final String SCHEMA = "opening_hour_it_" + UUID.randomUUID().toString().replace("-", "");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getProperty("opening-hour.test-url",
                "jdbc:postgresql://127.0.0.1:55439/postgres"));
        properties.add("spring.datasource.username", () -> System.getProperty("opening-hour.test-user", "opening_hour_test"));
        properties.add("spring.datasource.password", () -> System.getProperty("opening-hour.test-password", ""));
        properties.add("spring.flyway.default-schema", () -> SCHEMA);
        properties.add("spring.jpa.properties.hibernate.default_schema", () -> SCHEMA);
        properties.add("spring.datasource.hikari.connection-init-sql", () -> "SET search_path TO " + SCHEMA + ", public");
    }

    @Autowired
    private OpeningHourService service;
    @Autowired
    private OpeningHourRepository repository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createAndPartialPatchPersistLocalTimes() {
        service.createOpeningHour(new CreateOpeningHourRequest(DayOfWeek.MONDAY, OPEN, CLOSE));
        service.updateOpeningHour((short) 1, new UpdateOpeningHourRequest(null, LocalTime.of(9, 30), null));
        assertThat(jdbc.queryForObject("SELECT open_time FROM opening_hour WHERE day_of_week = 1", LocalTime.class))
                .isEqualTo(LocalTime.of(9, 30));
        assertThat(jdbc.queryForObject("SELECT close_time FROM opening_hour WHERE day_of_week = 1", LocalTime.class))
                .isEqualTo(CLOSE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void outerFailureRollsBackInsertedConfiguration() {
        var transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.createOpeningHour(new CreateOpeningHourRequest(DayOfWeek.WEDNESDAY, OPEN, CLOSE));
            throw new IllegalStateException("Force rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(repository.existsById((short) 3)).isFalse();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousCreatesHaveExactlyOneWinnerWithoutOverwritingIt() throws Exception {
        try {
            List<Boolean> results = race(
                    () -> createSunday(OPEN),
                    () -> createSunday(LocalTime.of(9, 0)));
            assertThat(results).containsExactlyInAnyOrder(true, false);
            assertThat(repository.count()).isEqualTo(1);
            assertThat(repository.findById((short) 0).orElseThrow().getOpenTime())
                    .isEqualTo(results.getFirst() ? OPEN : LocalTime.of(9, 0));
        } finally {
            repository.deleteById((short) 0);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void simultaneousPartialPatchesPreserveBothChanges() throws Exception {
        service.createOpeningHour(new CreateOpeningHourRequest(DayOfWeek.TUESDAY, OPEN, CLOSE));
        try {
            race(
                    () -> service.updateOpeningHour((short) 2,
                            new UpdateOpeningHourRequest(null, LocalTime.of(9, 0), null)),
                    () -> service.updateOpeningHour((short) 2,
                            new UpdateOpeningHourRequest(null, null, LocalTime.of(23, 0))));
            var stored = repository.findById((short) 2).orElseThrow();
            assertThat(stored.getOpenTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(stored.getCloseTime()).isEqualTo(LocalTime.of(23, 0));
        } finally {
            repository.deleteById((short) 2);
        }
    }

    @Test
    void impactedReservationQueryUsesVietnameseWeekdayAndInclusiveClosingBoundary() {
        LocalDate monday = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        Long boundary = reservation("boundary", monday, "20:30", "22:00", "CONFIRMED");
        Long late = reservation("late", monday, "21:00", "22:30", "PENDING");
        Long early = reservation("early", monday, "09:30", "11:00", "ARRIVED");
        reservation("cancelled", monday, "21:00", "22:30", "CANCELLED");
        reservation("completed", monday, "21:00", "22:30", "COMPLETED");
        reservation("other-day", monday.plusDays(1), "21:00", "22:30", "CONFIRMED");
        reservation("past", monday.minusWeeks(2), "21:00", "22:30", "CONFIRMED");

        assertThat(reservationRepository.findIdsOutsideOpeningHours((short) 1, OPEN, CLOSE, false))
                .containsExactly(early, late).doesNotContain(boundary);
        // Monday 00:30 in Vietnam is still Sunday in UTC.
        Long localMonday = reservation("local-monday", monday, "00:30", "02:00", "CONFIRMED");
        assertThat(reservationRepository.findIdsOutsideOpeningHours((short) 1, OPEN, CLOSE, false))
                .containsExactly(localMonday, early, late);
        assertThat(reservationRepository.findIdsOutsideOpeningHours((short) 1, OPEN, CLOSE, true))
                .contains(boundary);
    }

    private boolean createSunday(LocalTime openTime) {
        try {
            service.createOpeningHour(new CreateOpeningHourRequest(DayOfWeek.SUNDAY, openTime, CLOSE));
            return true;
        } catch (BusinessException exception) {
            assertThat(exception.getCode()).isEqualTo("OPENING_HOUR_ALREADY_EXISTS");
            return false;
        }
    }

    private <T> List<T> race(Callable<T> first, Callable<T> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var futures = List.of(first, second).stream().map(operation -> executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for concurrent test");
                }
                return operation.call();
            })).toList();
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        }
    }

    private Long reservation(String code, LocalDate date, String start, String end, String status) {
        Long customer = jdbc.queryForObject("""
                INSERT INTO account (email, password_hash, full_name, phone, role)
                VALUES (?, 'test-hash', 'Test', '0901234567', 'CUSTOMER') RETURNING id
                """, Long.class, code + "@test.invalid");
        Long area = jdbc.queryForObject("INSERT INTO area (name) VALUES (?) RETURNING id", Long.class, code);
        Long table = jdbc.queryForObject("""
                INSERT INTO restaurant_table (code, area_id, min_capacity, max_capacity)
                VALUES (?, ?, 1, 4) RETURNING id
                """, Long.class, code, area);
        return jdbc.queryForObject("""
                INSERT INTO reservation (code, customer_id, table_id, party_size, start_time, end_time, buffer_minutes, status)
                VALUES (?, ?, ?, 2, CAST(? AS timestamptz), CAST(? AS timestamptz), 15, ?) RETURNING id
                """, Long.class, code, customer, table, date + "T" + start + "+07:00", date + "T" + end + "+07:00", status);
    }
}

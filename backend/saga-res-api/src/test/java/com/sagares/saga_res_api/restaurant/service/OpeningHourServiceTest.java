package com.sagares.saga_res_api.restaurant.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.restaurant.dto.admin.CreateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.dto.admin.UpdateOpeningHourRequest;
import com.sagares.saga_res_api.restaurant.entity.OpeningHour;
import com.sagares.saga_res_api.restaurant.repository.OpeningHourRepository;
import com.sagares.saga_res_api.reservation.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpeningHourServiceTest {

    private static final LocalTime OPEN = LocalTime.of(10, 0);
    private static final LocalTime CLOSE = LocalTime.of(22, 0);

    @Mock
    private OpeningHourRepository repository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private OpeningHourService service;

    @Test
    void listsEveryConfiguredDayInCalendarOrderIncludingClosedDays() {
        OpeningHour sunday = hour(0);
        sunday.setClosed(true);
        sunday.setOpenTime(null);
        sunday.setCloseTime(null);
        when(repository.findAll()).thenReturn(List.of(sunday, hour(5), hour(3), hour(1), hour(6), hour(2), hour(4)));

        assertThat(service.getPublicOpeningHours()).extracting("dayOfWeek")
                .containsExactly((Object[]) DayOfWeek.values());
        assertThat(service.listAdminOpeningHours()).extracting("id")
                .containsExactly((short) 1, (short) 2, (short) 3, (short) 4, (short) 5, (short) 6, (short) 0);
        var closedDay = service.getPublicOpeningHours().getLast();
        assertThat(closedDay.closed()).isTrue();
        assertThat(closedDay.openTime()).isNull();
        assertThat(closedDay.closeTime()).isNull();
    }

    @Test
    void missingConfigurationReturnsEmptyListsWithoutInventingDefaults() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.getPublicOpeningHours()).isEmpty();
        assertThat(service.listAdminOpeningHours()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(DayOfWeek.class)
    void createsEachWeekdayWithTheExistingDatabaseKey(DayOfWeek day) {
        short id = (short) (day.getValue() % 7);
        when(repository.insertIfAbsent(id, OPEN, CLOSE)).thenReturn(1);

        var response = service.createOpeningHour(new CreateOpeningHourRequest(day, OPEN, CLOSE));

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.dayOfWeek()).isEqualTo(day);
        assertThat(response.openTime()).isEqualTo(OPEN);
        assertThat(response.closeTime()).isEqualTo(CLOSE);
        assertThat(response.closed()).isFalse();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateIncludingConcurrentInsertBecomesVietnameseConflict() {
        when(repository.insertIfAbsent((short) 1, OPEN, CLOSE)).thenReturn(0);
        assertBusiness(() -> service.createOpeningHour(new CreateOpeningHourRequest(DayOfWeek.MONDAY, OPEN, CLOSE)),
                HttpStatus.CONFLICT, "OPENING_HOUR_ALREADY_EXISTS", "Giờ mở cửa của ngày này đã được cấu hình");
        verify(repository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"10:00,10:00", "22:00,10:00"})
    void createRejectsEqualOrReversedTimes(String open, String close) {
        assertInvalidTime(() -> service.createOpeningHour(new CreateOpeningHourRequest(
                DayOfWeek.MONDAY, LocalTime.parse(open), LocalTime.parse(close))));
        verifyNoInteractions(repository);
    }

    @Test
    void createRequiresDayAndBothTimesAtServiceBoundary() {
        assertBusiness(() -> service.createOpeningHour(new CreateOpeningHourRequest(null, OPEN, CLOSE)),
                HttpStatus.BAD_REQUEST, "INVALID_DAY_OF_WEEK", "Ngày trong tuần là bắt buộc");
        assertBusiness(() -> service.createOpeningHour(new CreateOpeningHourRequest(DayOfWeek.MONDAY, null, CLOSE)),
                HttpStatus.BAD_REQUEST, "OPENING_HOUR_TIME_REQUIRED", "Giờ mở cửa và giờ đóng cửa là bắt buộc");
        assertBusiness(() -> service.createOpeningHour(new CreateOpeningHourRequest(DayOfWeek.MONDAY, OPEN, null)),
                HttpStatus.BAD_REQUEST, "OPENING_HOUR_TIME_REQUIRED", "Giờ mở cửa và giờ đóng cửa là bắt buộc");
        verifyNoInteractions(repository);
    }

    @ParameterizedTest
    @CsvSource({"09:30,", ",22:30", "09:30,22:30"})
    void partialUpdatePreservesOmittedTimeAndClosedState(String open, String close) {
        OpeningHour hour = hour(1);
        hour.setClosed(true);
        when(repository.findByIdForUpdate((short) 1)).thenReturn(Optional.of(hour));
        when(repository.saveAndFlush(hour)).thenReturn(hour);
        LocalTime newOpen = open == null ? null : LocalTime.parse(open);
        LocalTime newClose = close == null ? null : LocalTime.parse(close);

        var response = service.updateOpeningHour((short) 1, new UpdateOpeningHourRequest(null, newOpen, newClose));

        assertThat(response.id()).isEqualTo((short) 1);
        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.openTime()).isEqualTo(newOpen == null ? OPEN : newOpen);
        assertThat(response.closeTime()).isEqualTo(newClose == null ? CLOSE : newClose);
        assertThat(response.closed()).isTrue();
    }

    @Test
    void echoingOwnWeekdayIsNotADuplicate() {
        OpeningHour hour = hour(0);
        when(repository.findByIdForUpdate((short) 0)).thenReturn(Optional.of(hour));
        when(repository.saveAndFlush(hour)).thenReturn(hour);
        assertThat(service.updateOpeningHour((short) 0,
                new UpdateOpeningHourRequest(DayOfWeek.SUNDAY, OPEN, CLOSE)).dayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
    }

    @Test
    void weekdayCannotBeChangedBecauseItIsThePrimaryKey() {
        OpeningHour hour = hour(1);
        when(repository.findByIdForUpdate((short) 1)).thenReturn(Optional.of(hour));
        assertBusiness(() -> service.updateOpeningHour((short) 1,
                        new UpdateOpeningHourRequest(DayOfWeek.TUESDAY, OPEN, CLOSE)),
                HttpStatus.BAD_REQUEST, "OPENING_HOUR_DAY_IMMUTABLE",
                "Không thể thay đổi ngày trong tuần của cấu hình giờ mở cửa");
        assertThat(hour.getDayOfWeek()).isEqualTo((short) 1);
        verify(repository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"22:00,", "23:00,", ",10:00", ",09:00", "22:00,10:00"})
    void invalidPatchIsRejectedBeforeMutatingManagedEntity(String open, String close) {
        OpeningHour hour = hour(1);
        when(repository.findByIdForUpdate((short) 1)).thenReturn(Optional.of(hour));
        assertInvalidTime(() -> service.updateOpeningHour((short) 1, new UpdateOpeningHourRequest(null,
                open == null ? null : LocalTime.parse(open), close == null ? null : LocalTime.parse(close))));
        assertThat(hour.getOpenTime()).isEqualTo(OPEN);
        assertThat(hour.getCloseTime()).isEqualTo(CLOSE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void updateMissingDayReturnsNotFound() {
        assertBusiness(() -> service.updateOpeningHour((short) 1, new UpdateOpeningHourRequest(null, OPEN, null)),
                HttpStatus.NOT_FOUND, "OPENING_HOUR_NOT_FOUND", "Cấu hình giờ mở cửa không tồn tại");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void emptyPatchIsRejected() {
        when(repository.findByIdForUpdate((short) 1)).thenReturn(Optional.of(hour(1)));
        assertBusiness(() -> service.updateOpeningHour((short) 1, new UpdateOpeningHourRequest(null, null, null)),
                HttpStatus.BAD_REQUEST, "EMPTY_PATCH", "Ít nhất một trường giờ mở cửa phải được cung cấp");
        verify(repository, never()).saveAndFlush(any());
    }

    private void assertInvalidTime(Runnable action) {
        assertBusiness(action, HttpStatus.BAD_REQUEST, "INVALID_OPENING_HOUR_TIME", "Giờ đóng cửa phải sau giờ mở cửa");
    }

    @Test
    void updateListsImpactedReservationsWithoutModifyingThem() {
        OpeningHour hour = hour(1);
        LocalTime earlyClose = LocalTime.of(21, 0);
        when(repository.findByIdForUpdate((short) 1)).thenReturn(Optional.of(hour));
        when(repository.saveAndFlush(hour)).thenReturn(hour);
        when(reservationRepository.findIdsOutsideOpeningHours((short) 1, OPEN, earlyClose, false))
                .thenReturn(List.of(12L, 34L));

        var response = service.updateOpeningHour((short) 1, new UpdateOpeningHourRequest(null, null, earlyClose));

        assertThat(response.impactedReservationIds()).containsExactly(12L, 34L);
        verify(reservationRepository).findIdsOutsideOpeningHours((short) 1, OPEN, earlyClose, false);
        org.mockito.Mockito.verifyNoMoreInteractions(reservationRepository);
    }

    private void assertBusiness(Runnable action, HttpStatus status, String code, String message) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(status);
            assertThat(exception.getCode()).isEqualTo(code);
            assertThat(exception.getMessage()).isEqualTo(message);
        });
    }

    private OpeningHour hour(int id) {
        OpeningHour hour = new OpeningHour();
        hour.setDayOfWeek((short) id);
        hour.setOpenTime(OPEN);
        hour.setCloseTime(CLOSE);
        return hour;
    }
}

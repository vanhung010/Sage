package com.sagares.saga_res_api.table.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.table.dto.admin.CreateRestaurantTableRequest;
import com.sagares.saga_res_api.table.dto.admin.UpdateRestaurantTableRequest;
import com.sagares.saga_res_api.table.entity.Area;
import com.sagares.saga_res_api.table.entity.RestaurantTable;
import com.sagares.saga_res_api.table.repository.AreaRepository;
import com.sagares.saga_res_api.table.repository.RestaurantTableRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRestaurantTableServiceTest {

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @Mock
    private AreaRepository areaRepository;

    @InjectMocks
    private AdminRestaurantTableService service;

    @Test
    void listIncludesInactiveTablesWithTheirAreasInRepositoryOrder() {
        RestaurantTable garden = table(2L, "T02", area(2L, "Garden", false), false);
        RestaurantTable indoor = table(1L, "T01", area(1L, "Indoor", true), true);
        when(restaurantTableRepository.findAllByOrderByArea_NameAscCodeAsc()).thenReturn(List.of(garden, indoor));

        var responses = service.listTables();

        assertThat(responses).extracting("id").containsExactly(2L, 1L);
        assertThat(responses).extracting("active").containsExactly(false, true);
        assertThat(responses.getFirst().area().id()).isEqualTo(2L);
        assertThat(responses.getFirst().area().name()).isEqualTo("Garden");
        assertThat(responses.getFirst().minCapacity()).isEqualTo((short) 1);
        assertThat(responses.getFirst().maxCapacity()).isEqualTo((short) 2);
        verify(restaurantTableRepository).findAllByOrderByArea_NameAscCodeAsc();
    }

    @ParameterizedTest
    @CsvSource({"1,2", "2,2", "1,32767"})
    void createTrimsCodeAndDefaultsActiveWithValidCapacity(short min, short max) {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", true)));
        when(restaurantTableRepository.saveAndFlush(any(RestaurantTable.class))).thenAnswer(invocation -> {
            RestaurantTable saved = invocation.getArgument(0);
            assertThat(saved.getId()).isNull();
            saved.setId(1L);
            return saved;
        });

        var response = service.createTable(new CreateRestaurantTableRequest(" T01 ", 1L, min, max));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("T01");
        assertThat(response.area().name()).isEqualTo("Indoor");
        assertThat(response.minCapacity()).isEqualTo(min);
        assertThat(response.maxCapacity()).isEqualTo(max);
        assertThat(response.active()).isTrue();
        verify(restaurantTableRepository).existsByCode("T01");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "123456789012345678901"})
    void createRejectsInvalidCode(String code) {
        assertError(() -> service.createTable(new CreateRestaurantTableRequest(code, 1L, (short) 1, (short) 2)),
                HttpStatus.BAD_REQUEST, "INVALID_TABLE_CODE");
        verifyNoInteractions(restaurantTableRepository, areaRepository);
    }

    @Test
    void createRejectsDuplicateCodeIncludingInactiveTableCode() {
        when(restaurantTableRepository.existsByCode("T01")).thenReturn(true);

        assertError(() -> service.createTable(createRequest()), HttpStatus.CONFLICT, "TABLE_CODE_ALREADY_EXISTS");

        verifyNoInteractions(areaRepository);
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsUnknownArea() {
        assertError(() -> service.createTable(createRequest()), HttpStatus.NOT_FOUND, "AREA_NOT_FOUND");
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @Test
    void createRejectsMissingAreaId() {
        assertError(() -> service.createTable(new CreateRestaurantTableRequest("T01", null, (short) 1, (short) 2)),
                HttpStatus.BAD_REQUEST, "INVALID_AREA_ID");
        verifyNoInteractions(areaRepository);
    }

    @Test
    void createRejectsInactiveArea() {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", false)));

        assertError(() -> service.createTable(createRequest()), HttpStatus.CONFLICT, "AREA_INACTIVE");
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"0,2", "-1,2", "3,2", "1,0"})
    void createRejectsInvalidCapacity(short min, short max) {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", true)));

        assertError(() -> service.createTable(new CreateRestaurantTableRequest("T01", 1L, min, max)),
                HttpStatus.BAD_REQUEST, "INVALID_TABLE_CAPACITY");
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @Test
    void createMapsDatabaseDuplicateRaceToBusinessConflict() {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", true)));
        when(restaurantTableRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertError(() -> service.createTable(createRequest()), HttpStatus.CONFLICT, "TABLE_CODE_ALREADY_EXISTS");
    }

    @Test
    void updateChangesCodeAreaAndCapacityWhilePreservingIdAndActiveState() {
        RestaurantTable table = table(1L, "T01", area(1L, "Indoor", true), false);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(areaRepository.findById(2L)).thenReturn(Optional.of(area(2L, "Garden", true)));
        when(restaurantTableRepository.saveAndFlush(table)).thenReturn(table);

        var response = service.updateTable(1L, new UpdateRestaurantTableRequest(" T02 ", 2L, (short) 2, (short) 4));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo("T02");
        assertThat(response.area().id()).isEqualTo(2L);
        assertThat(response.area().name()).isEqualTo("Garden");
        assertThat(response.minCapacity()).isEqualTo((short) 2);
        assertThat(response.maxCapacity()).isEqualTo((short) 4);
        assertThat(response.active()).isFalse();
        verify(restaurantTableRepository).findByIdForUpdate(1L);
        verify(restaurantTableRepository).existsByCodeAndIdNot("T02", 1L);
    }

    @Test
    void updateKeepingOwnCodeAndAreaSucceedsEvenWhenCurrentAreaIsInactive() {
        RestaurantTable table = table(1L, "T01", area(1L, "Indoor", false), true);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(restaurantTableRepository.saveAndFlush(table)).thenReturn(table);

        var response = service.updateTable(1L, new UpdateRestaurantTableRequest(" T01 ", 1L, null, null));

        assertThat(response.code()).isEqualTo("T01");
        assertThat(response.area().id()).isEqualTo(1L);
        verifyNoInteractions(areaRepository);
        verify(restaurantTableRepository, never()).existsByCodeAndIdNot(any(), any());
    }

    @Test
    void updateRejectsAnotherTablesCodeBeforeMutatingFields() {
        RestaurantTable table = table(1L, "T01", area(1L, "Indoor", true), true);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(restaurantTableRepository.existsByCodeAndIdNot("T02", 1L)).thenReturn(true);

        assertError(() -> service.updateTable(1L, new UpdateRestaurantTableRequest("T02", 2L, (short) 2, (short) 4)),
                HttpStatus.CONFLICT, "TABLE_CODE_ALREADY_EXISTS");

        assertThat(table.getCode()).isEqualTo("T01");
        assertThat(table.getArea().getId()).isEqualTo(1L);
        assertThat(table.getMinCapacity()).isEqualTo((short) 1);
        verifyNoInteractions(areaRepository);
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void updateRejectsUnknownOrInactiveTargetArea(boolean missing) {
        RestaurantTable table = table(1L, "T01", area(1L, "Indoor", true), true);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(areaRepository.findById(2L)).thenReturn(missing ? Optional.empty() : Optional.of(area(2L, "Garden", false)));

        assertError(() -> service.updateTable(1L, new UpdateRestaurantTableRequest("T02", 2L, null, null)),
                missing ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT, missing ? "AREA_NOT_FOUND" : "AREA_INACTIVE");

        assertThat(table.getCode()).isEqualTo("T01");
        assertThat(table.getArea().getId()).isEqualTo(1L);
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @CsvSource({"0,", "3,", ",0", ", -1"})
    void partialCapacityUpdateRejectsInvalidMergedValues(Short min, Short max) {
        RestaurantTable table = table(1L, "T01", area(1L, "Indoor", true), true);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));

        assertError(() -> service.updateTable(1L, new UpdateRestaurantTableRequest("T02", null, min, max)),
                HttpStatus.BAD_REQUEST, "INVALID_TABLE_CAPACITY");
        assertThat(table.getCode()).isEqualTo("T01");
        assertThat(table.getMinCapacity()).isEqualTo((short) 1);
        assertThat(table.getMaxCapacity()).isEqualTo((short) 2);
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @Test
    void partialCapacityUpdateUsesExistingMinimumAndAllowsEqualValues() {
        RestaurantTable table = table(1L, "T01", area(1L, "Indoor", true), true);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(restaurantTableRepository.saveAndFlush(table)).thenReturn(table);

        var response = service.updateTable(1L, new UpdateRestaurantTableRequest(null, null, null, (short) 1));

        assertThat(response.code()).isEqualTo("T01");
        assertThat(response.minCapacity()).isEqualTo((short) 1);
        assertThat(response.maxCapacity()).isEqualTo((short) 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "123456789012345678901"})
    void updateRejectsInvalidCode(String code) {
        when(restaurantTableRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(table(1L, "T01", area(1L, "Indoor", true), true)));

        assertError(() -> service.updateTable(1L, new UpdateRestaurantTableRequest(code, null, null, null)),
                HttpStatus.BAD_REQUEST, "INVALID_TABLE_CODE");
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateMapsDatabaseDuplicateRaceToConflict() {
        RestaurantTable table = table(1L, "T01", area(1L, "Indoor", true), true);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(restaurantTableRepository.saveAndFlush(table)).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertError(() -> service.updateTable(1L, new UpdateRestaurantTableRequest("T02", null, null, null)),
                HttpStatus.CONFLICT, "TABLE_CODE_ALREADY_EXISTS");
    }

    @Test
    void updateRejectsEmptyPatch() {
        assertError(() -> service.updateTable(1L, new UpdateRestaurantTableRequest(null, null, null, null)),
                HttpStatus.BAD_REQUEST, "EMPTY_PATCH");
        verifyNoInteractions(restaurantTableRepository, areaRepository);
    }

    @Test
    void unknownTableIsRejectedOnUpdateAndDeactivation() {
        assertError(() -> service.updateTable(999L, new UpdateRestaurantTableRequest("T02", null, null, null)),
                HttpStatus.NOT_FOUND, "TABLE_NOT_FOUND");
        assertError(() -> service.deactivateTable(999L), HttpStatus.NOT_FOUND, "TABLE_NOT_FOUND");
        verify(restaurantTableRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deactivationIsIdempotentRetainsRowAndAppearsInAdminList(boolean active) {
        RestaurantTable table = table(1L, "T01", area(1L, "Indoor", true), active);
        when(restaurantTableRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(table));
        when(restaurantTableRepository.saveAndFlush(table)).thenReturn(table);
        when(restaurantTableRepository.findAllByOrderByArea_NameAscCodeAsc()).thenReturn(List.of(table));

        var response = service.deactivateTable(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.active()).isFalse();
        assertThat(service.listTables()).containsExactly(response);
        verify(restaurantTableRepository).findByIdForUpdate(1L);
        verify(restaurantTableRepository).saveAndFlush(table);
        verify(restaurantTableRepository).findAllByOrderByArea_NameAscCodeAsc();
        verifyNoMoreInteractions(restaurantTableRepository);
        verifyNoInteractions(areaRepository);
    }

    private void assertError(Runnable operation, HttpStatus status, String code) {
        assertThatThrownBy(operation::run).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getStatus()).isEqualTo(status);
            assertThat(exception.getCode()).isEqualTo(code);
        });
    }

    private CreateRestaurantTableRequest createRequest() {
        return new CreateRestaurantTableRequest("T01", 1L, (short) 1, (short) 2);
    }

    private Area area(Long id, String name, boolean active) {
        Area area = new Area();
        area.setId(id);
        area.setName(name);
        area.setActive(active);
        return area;
    }

    private RestaurantTable table(Long id, String code, Area area, boolean active) {
        RestaurantTable table = new RestaurantTable();
        table.setId(id);
        table.setCode(code);
        table.setArea(area);
        table.setMinCapacity((short) 1);
        table.setMaxCapacity((short) 2);
        table.setActive(active);
        return table;
    }
}

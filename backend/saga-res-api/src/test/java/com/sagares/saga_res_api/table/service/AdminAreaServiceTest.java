package com.sagares.saga_res_api.table.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.table.dto.admin.CreateAreaRequest;
import com.sagares.saga_res_api.table.dto.admin.UpdateAreaRequest;
import com.sagares.saga_res_api.table.entity.Area;
import com.sagares.saga_res_api.table.repository.AreaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
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
class AdminAreaServiceTest {

    @Mock
    private AreaRepository areaRepository;

    @InjectMocks
    private AdminAreaService service;

    @Test
    void listAreasUsesNameOrderingAndIncludesInactiveRows() {
        Area garden = area(2L, "Garden", "Outdoor seating", false);
        Area indoor = area(1L, "Indoor", "Main dining room", true);
        when(areaRepository.findAllByOrderByNameAsc()).thenReturn(List.of(garden, indoor));

        var responses = service.listAreas();

        assertThat(responses).extracting("id").containsExactly(2L, 1L);
        assertThat(responses).extracting("name").containsExactly("Garden", "Indoor");
        assertThat(responses).extracting("description").containsExactly("Outdoor seating", "Main dining room");
        assertThat(responses).extracting("active").containsExactly(false, true);
        verify(areaRepository).findAllByOrderByNameAsc();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Indoor", "Garden"})
    void createAreaTrimsTextAndDefaultsActiveToTrue(String name) {
        when(areaRepository.saveAndFlush(any(Area.class))).thenAnswer(invocation -> {
            Area saved = invocation.getArgument(0);
            assertThat(saved.getId()).isNull();
            saved.setId(1L);
            return saved;
        });

        var response = service.createArea(new CreateAreaRequest("  " + name + "  ", "  Dining area  "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo(name);
        assertThat(response.description()).isEqualTo("Dining area");
        assertThat(response.active()).isTrue();
        verify(areaRepository).existsByName(name);
    }

    @Test
    void createAreaAllowsOptionalDescriptionAndMaximumLengthName() {
        when(areaRepository.saveAndFlush(any(Area.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.createArea(new CreateAreaRequest("a".repeat(100), null));

        assertThat(response.name()).hasSize(100);
        assertThat(response.description()).isNull();
        assertThat(response.active()).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void createAreaRejectsBlankName(String name) {
        assertThatThrownBy(() -> service.createArea(new CreateAreaRequest(name, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("INVALID_AREA_NAME");
                });
        verifyNoInteractions(areaRepository);
    }

    @Test
    void createAreaRejectsTooLongName() {
        assertThatThrownBy(() -> service.createArea(new CreateAreaRequest("a".repeat(101), null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verifyNoInteractions(areaRepository);
    }

    @Test
    void createAreaRejectsDuplicateNameIncludingInactiveAreaName() {
        when(areaRepository.existsByName("Indoor")).thenReturn(true);

        assertDuplicateName(() -> service.createArea(new CreateAreaRequest(" Indoor ", null)));

        verify(areaRepository, never()).saveAndFlush(any(Area.class));
    }

    @Test
    void createAreaMapsDatabaseUniqueRaceToConflict() {
        when(areaRepository.saveAndFlush(any(Area.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertDuplicateName(() -> service.createArea(new CreateAreaRequest("Indoor", null)));
    }

    @Test
    void updateAreaTrimsNameAndDescriptionAndPreservesIdentityAndActiveState() {
        Area area = area(1L, "Indoor", "Old description", false);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.saveAndFlush(area)).thenReturn(area);

        var response = service.updateArea(1L, new UpdateAreaRequest(" Main Indoor ", " Main dining room "));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Main Indoor");
        assertThat(response.description()).isEqualTo("Main dining room");
        assertThat(response.active()).isFalse();
        verify(areaRepository).existsByNameAndIdNot("Main Indoor", 1L);
    }

    @Test
    void updateAreaWithOwnNameSucceeds() {
        Area area = area(1L, "Indoor", "Original", true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.saveAndFlush(area)).thenReturn(area);

        var response = service.updateArea(1L, new UpdateAreaRequest(" Indoor ", null));

        assertThat(response.name()).isEqualTo("Indoor");
        assertThat(response.description()).isEqualTo("Original");
        verify(areaRepository, never()).existsByNameAndIdNot(any(), any());
    }

    @Test
    void updateAreaCanChangeOnlyDescriptionAndClearItWithEmptyText() {
        Area area = area(1L, "Indoor", "Original", true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.saveAndFlush(area)).thenReturn(area);

        var response = service.updateArea(1L, new UpdateAreaRequest(null, "  "));

        assertThat(response.name()).isEqualTo("Indoor");
        assertThat(response.description()).isEmpty();
        assertThat(response.active()).isTrue();
    }

    @Test
    void updateAreaRejectsAnotherAreasNameBeforeChangingAnyFields() {
        Area area = area(1L, "Indoor", "Original", true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.existsByNameAndIdNot("Garden", 1L)).thenReturn(true);

        assertDuplicateName(() -> service.updateArea(1L, new UpdateAreaRequest(" Garden ", "Changed")));

        assertThat(area.getName()).isEqualTo("Indoor");
        assertThat(area.getDescription()).isEqualTo("Original");
        verify(areaRepository, never()).saveAndFlush(any(Area.class));
    }

    @Test
    void updateAreaMapsDatabaseUniqueRaceToConflict() {
        Area area = area(1L, "Indoor", null, true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.saveAndFlush(area)).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertDuplicateName(() -> service.updateArea(1L, new UpdateAreaRequest("Garden", null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void updateAreaRejectsBlankSuppliedName(String name) {
        Area area = area(1L, "Indoor", "Original", true);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));

        assertThatThrownBy(() -> service.updateArea(1L, new UpdateAreaRequest(name, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(areaRepository, never()).saveAndFlush(any(Area.class));
    }

    @Test
    void updateAreaRejectsTooLongName() {
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area(1L, "Indoor", null, true)));

        assertThatThrownBy(() -> service.updateArea(1L, new UpdateAreaRequest("a".repeat(101), null)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(areaRepository, never()).saveAndFlush(any(Area.class));
    }

    @Test
    void updateAreaRejectsEmptyPatch() {
        assertThatThrownBy(() -> service.updateArea(1L, new UpdateAreaRequest(null, null)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getCode()).isEqualTo("EMPTY_PATCH");
                });
        verifyNoInteractions(areaRepository);
    }

    @Test
    void updateAreaRejectsUnknownId() {
        assertNotFound(() -> service.updateArea(999L, new UpdateAreaRequest("Indoor", null)));
    }

    @Test
    void deactivateAreaRejectsUnknownId() {
        assertNotFound(() -> service.deactivateArea(999L));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void deactivateAreaRetainsRowAndAppearsInactiveInAdminList(boolean active) {
        Area area = area(1L, "Indoor", "Main dining room", active);
        when(areaRepository.findById(1L)).thenReturn(Optional.of(area));
        when(areaRepository.saveAndFlush(area)).thenReturn(area);
        when(areaRepository.findAllByOrderByNameAsc()).thenReturn(List.of(area));

        var response = service.deactivateArea(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Indoor");
        assertThat(response.description()).isEqualTo("Main dining room");
        assertThat(response.active()).isFalse();
        assertThat(service.listAreas()).containsExactly(response);
        verify(areaRepository).findById(1L);
        verify(areaRepository).saveAndFlush(area);
        verify(areaRepository).findAllByOrderByNameAsc();
        verifyNoMoreInteractions(areaRepository);
    }

    private void assertDuplicateName(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getCode()).isEqualTo("AREA_NAME_ALREADY_EXISTS");
                });
    }

    private void assertNotFound(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getCode()).isEqualTo("AREA_NOT_FOUND");
                });
        verify(areaRepository, never()).saveAndFlush(any(Area.class));
    }

    private Area area(Long id, String name, String description, boolean active) {
        Area area = new Area();
        area.setId(id);
        area.setName(name);
        area.setDescription(description);
        area.setActive(active);
        return area;
    }
}

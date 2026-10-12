package com.sagares.saga_res_api.menu.service;

import com.sagares.saga_res_api.common.exception.BusinessException;
import com.sagares.saga_res_api.menu.dto.admin.CreateTagRequest;
import com.sagares.saga_res_api.menu.dto.admin.UpdateTagRequest;
import com.sagares.saga_res_api.menu.entity.Tag;
import com.sagares.saga_res_api.menu.repository.DishRepository;
import com.sagares.saga_res_api.menu.repository.TagRepository;
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
class AdminMenuTagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private DishRepository dishRepository;

    @InjectMocks
    private AdminMenuTagService service;

    @Test
    void listTagsUsesNameAscendingRepositoryOrder() {
        when(tagRepository.findAllByOrderByNameAsc()).thenReturn(List.of(
                tag(2L, "Spicy"),
                tag(1L, "Vegetarian")
        ));

        var result = service.listTags();

        assertThat(result).extracting("id").containsExactly(2L, 1L);
        assertThat(result).extracting("name").containsExactly("Spicy", "Vegetarian");
        verify(tagRepository).findAllByOrderByNameAsc();
        verifyNoInteractions(dishRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Spicy", "Vegetarian"})
    void createTagTrimsAndSavesName(String name) {
        when(tagRepository.saveAndFlush(any(Tag.class))).thenAnswer(invocation -> {
            Tag saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        var result = service.createTag(new CreateTagRequest("  " + name + "  "));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo(name);
        verify(tagRepository).existsByName(name);
        verify(tagRepository).saveAndFlush(any(Tag.class));
        verifyNoInteractions(dishRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void createTagRejectsBlankName(String name) {
        assertBusiness(
                () -> service.createTag(new CreateTagRequest(name)),
                HttpStatus.BAD_REQUEST,
                "INVALID_TAG_NAME",
                "Tên nhãn không được để trống"
        );
        verifyNoInteractions(tagRepository, dishRepository);
    }

    @Test
    void createTagRejectsNameLongerThanFiftyCharacters() {
        assertBusiness(
                () -> service.createTag(new CreateTagRequest("a".repeat(51))),
                HttpStatus.BAD_REQUEST,
                "INVALID_TAG_NAME",
                "Tên nhãn không được vượt quá 50 ký tự"
        );
        verifyNoInteractions(tagRepository, dishRepository);
    }

    @Test
    void createTagAcceptsExactlyFiftyCharacters() {
        String name = "a".repeat(50);
        when(tagRepository.saveAndFlush(any(Tag.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.createTag(new CreateTagRequest(name)).name()).isEqualTo(name);

        verify(tagRepository).existsByName(name);
    }

    @Test
    void createTagRejectsExistingName() {
        when(tagRepository.existsByName("Spicy")).thenReturn(true);

        assertDuplicate(() -> service.createTag(new CreateTagRequest(" Spicy ")));

        verify(tagRepository, never()).saveAndFlush(any(Tag.class));
        verifyNoInteractions(dishRepository);
    }

    @Test
    void createTagMapsDatabaseUniqueRaceToDuplicateConflict() {
        when(tagRepository.saveAndFlush(any(Tag.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertDuplicate(() -> service.createTag(new CreateTagRequest("Spicy")));
    }

    @Test
    void updateTagTrimsAndSavesNewName() {
        Tag tag = tag(1L, "Spicy");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.saveAndFlush(tag)).thenReturn(tag);

        var result = service.updateTag(1L, new UpdateTagRequest("  Very Spicy  "));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Very Spicy");
        verify(tagRepository).existsByNameAndIdNot("Very Spicy", 1L);
        verifyNoInteractions(dishRepository);
    }

    @Test
    void updateTagWithOwnNameDoesNotReportDuplicate() {
        Tag tag = tag(1L, "Spicy");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.saveAndFlush(tag)).thenReturn(tag);

        assertThat(service.updateTag(1L, new UpdateTagRequest(" Spicy ")).name()).isEqualTo("Spicy");

        verify(tagRepository, never()).existsByNameAndIdNot(any(), any());
    }

    @Test
    void updateTagRejectsAnotherTagsNameWithoutMutatingEntity() {
        Tag tag = tag(1L, "Spicy");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByNameAndIdNot("Vegan", 1L)).thenReturn(true);

        assertDuplicate(() -> service.updateTag(1L, new UpdateTagRequest(" Vegan ")));

        assertThat(tag.getName()).isEqualTo("Spicy");
        verify(tagRepository, never()).saveAndFlush(any(Tag.class));
    }

    @Test
    void updateTagRejectsUnknownId() {
        assertBusiness(
                () -> service.updateTag(999L, new UpdateTagRequest("Spicy")),
                HttpStatus.NOT_FOUND,
                "TAG_NOT_FOUND",
                "Nhãn không tồn tại"
        );
        verify(tagRepository, never()).saveAndFlush(any(Tag.class));
        verifyNoInteractions(dishRepository);
    }

    @Test
    void updateTagRejectsEmptyPatchBeforeRepositoryAccess() {
        assertBusiness(
                () -> service.updateTag(1L, new UpdateTagRequest(null)),
                HttpStatus.BAD_REQUEST,
                "EMPTY_PATCH",
                "Phải cung cấp ít nhất một trường để cập nhật"
        );
        verifyNoInteractions(tagRepository, dishRepository);
    }

    @Test
    void updateTagRejectsBlankName() {
        Tag tag = tag(1L, "Spicy");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        assertBusiness(
                () -> service.updateTag(1L, new UpdateTagRequest("   ")),
                HttpStatus.BAD_REQUEST,
                "INVALID_TAG_NAME",
                "Tên nhãn không được để trống"
        );
        assertThat(tag.getName()).isEqualTo("Spicy");
        verify(tagRepository, never()).saveAndFlush(any(Tag.class));
    }

    @Test
    void updateTagMapsDatabaseUniqueRaceToDuplicateConflict() {
        Tag tag = tag(1L, "Spicy");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.saveAndFlush(tag))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertDuplicate(() -> service.updateTag(1L, new UpdateTagRequest("Vegan")));
    }

    @Test
    void deleteUnusedTagDeletesAndFlushes() {
        Tag tag = tag(1L, "Spicy");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        service.deleteTag(1L);

        verify(dishRepository).existsByTags_Id(1L);
        verify(tagRepository).delete(tag);
        verify(tagRepository).flush();
        verifyNoMoreInteractions(dishRepository);
    }

    @Test
    void deleteTagInUseRejectsBeforeDeleteAndPreservesJoinRows() {
        Tag tag = tag(1L, "Spicy");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(dishRepository.existsByTags_Id(1L)).thenReturn(true);

        assertTagInUse(() -> service.deleteTag(1L));

        verify(tagRepository, never()).delete(any(Tag.class));
        verify(tagRepository, never()).flush();
        verifyNoMoreInteractions(dishRepository);
    }

    @Test
    void deleteTagMapsForeignKeyRaceToTagInUse() {
        Tag tag = tag(1L, "Spicy");
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("foreign key"))
                .when(tagRepository).flush();

        assertTagInUse(() -> service.deleteTag(1L));

        verify(tagRepository).delete(tag);
    }

    @Test
    void deleteUnknownTagReturnsNotFoundWithoutDishQuery() {
        assertBusiness(
                () -> service.deleteTag(999L),
                HttpStatus.NOT_FOUND,
                "TAG_NOT_FOUND",
                "Nhãn không tồn tại"
        );
        verifyNoInteractions(dishRepository);
        verify(tagRepository, never()).delete(any(Tag.class));
    }

    private void assertDuplicate(Runnable action) {
        assertBusiness(action, HttpStatus.CONFLICT, "DUPLICATE_TAG_NAME", "Tên nhãn đã tồn tại");
    }

    private void assertTagInUse(Runnable action) {
        assertBusiness(action, HttpStatus.CONFLICT, "TAG_IN_USE",
                "Không thể xóa nhãn đang được gán cho món ăn");
    }

    private void assertBusiness(
            Runnable action,
            HttpStatus status,
            String code,
            String message
    ) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(status);
                    assertThat(exception.getCode()).isEqualTo(code);
                    assertThat(exception.getMessage()).isEqualTo(message);
                });
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}

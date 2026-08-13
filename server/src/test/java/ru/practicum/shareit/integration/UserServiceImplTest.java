package ru.practicum.shareit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.model.ConflictException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.user.dto.CreateUserRequest;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserResponse;
import ru.practicum.shareit.user.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceImplTest {

    @Autowired
    private UserService userService;

    @Test
    void create_savesAndReturnsUser() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("John")
                .email("john@test.ru")
                .build();

        UserResponse response = userService.create(request);

        assertThat(response.getId()).isPositive();
        assertThat(response.getName()).isEqualTo("John");
        assertThat(response.getEmail()).isEqualTo("john@test.ru");
    }

    @Test
    void create_throwsWhenEmailAlreadyInUse() {
        CreateUserRequest first = CreateUserRequest.builder()
                .name("John")
                .email("dup@test.ru")
                .build();
        userService.create(first);
        CreateUserRequest second = CreateUserRequest.builder()
                .name("Jane")
                .email("dup@test.ru")
                .build();

        assertThatThrownBy(() -> userService.create(second))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_changesFields() {
        UserResponse created = userService.create(CreateUserRequest.builder()
                .name("John")
                .email("john@test.ru")
                .build());
        UpdateUserRequest update = UpdateUserRequest.builder()
                .id(created.getId())
                .name("Johnny")
                .email("johnny@test.ru")
                .build();

        UserResponse updated = userService.update(update);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getName()).isEqualTo("Johnny");
        assertThat(updated.getEmail()).isEqualTo("johnny@test.ru");
    }

    @Test
    void update_throwsWhenUserMissing() {
        UpdateUserRequest update = UpdateUserRequest.builder()
                .id(999L)
                .name("Nobody")
                .build();

        assertThatThrownBy(() -> userService.update(update))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_removesUser() {
        UserResponse created = userService.create(CreateUserRequest.builder()
                .name("John")
                .email("john@test.ru")
                .build());

        userService.delete(created.getId());

        assertThatThrownBy(() -> userService.findById(created.getId()))
                .isInstanceOf(NotFoundException.class);
    }
}
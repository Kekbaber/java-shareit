package ru.practicum.shareit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.exception.model.ConflictException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.user.dto.CreateUserRequest;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserResponse;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.impl.UserServiceImpl;
import ru.practicum.shareit.user.storage.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("John").email("john@test.ru").build();
    }

    @Test
    void findById_returnsUserWhenFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.findById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("John");
        assertThat(response.getEmail()).isEqualTo("john@test.ru");
    }

    @Test
    void findById_throwsWhenMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_savesUser() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("John")
                .email("john@test.ru")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponse response = userService.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("john@test.ru");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void create_withoutEmail_skipsDuplicateCheck() {
        CreateUserRequest request = CreateUserRequest.builder().name("John").build();
        when(userRepository.save(any(User.class))).thenReturn(user);

        userService.create(request);

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void create_throwsWhenEmailAlreadyInUse() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("John")
                .email("john@test.ru")
                .build();
        when(userRepository.findByEmail("john@test.ru")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_changesOnlyProvidedFields() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .id(1L)
                .name("Johnny")
                .email("johnny@test.ru")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.update(request);

        assertThat(response.getName()).isEqualTo("Johnny");
        assertThat(response.getEmail()).isEqualTo("johnny@test.ru");
        verify(userRepository).save(user);
    }

    @Test
    void update_throwsWhenUserMissing() {
        UpdateUserRequest request = UpdateUserRequest.builder().id(999L).name("Nobody").build();
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_throwsWhenEmailTakenByAnotherUser() {
        User other = User.builder().id(2L).name("Jane").email("johnny@test.ru").build();
        UpdateUserRequest request = UpdateUserRequest.builder()
                .id(1L)
                .name("Johnny")
                .email("johnny@test.ru")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("johnny@test.ru")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> userService.update(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_withSameEmail_doesNotThrow() {
        UpdateUserRequest request = UpdateUserRequest.builder()
                .id(1L)
                .email("john@test.ru")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("john@test.ru")).thenReturn(Optional.of(user));

        UserResponse response = userService.update(request);

        assertThat(response.getEmail()).isEqualTo("john@test.ru");
    }

    @Test
    void delete_deletesById() {
        userService.delete(1L);

        verify(userRepository).deleteById(1L);
    }
}
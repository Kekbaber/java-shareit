package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.exception.model.ConflictException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.dto.CreateUserRequest;
import ru.practicum.shareit.user.dto.UpdateUserRequest;
import ru.practicum.shareit.user.dto.UserResponse;
import ru.practicum.shareit.user.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void getUser_returnsUser() throws Exception {
        when(userService.findById(1L)).thenReturn(UserResponse.builder()
                .id(1L)
                .name("John")
                .email("john@test.ru")
                .build());

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    void createUser_returnsCreated() throws Exception {
        when(userService.create(any(CreateUserRequest.class))).thenReturn(UserResponse.builder()
                .id(2L)
                .name("John")
                .email("john@test.ru")
                .build());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateUserRequest.builder()
                                .name("John")
                                .email("john@test.ru")
                                .build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void patchUser_returnsUpdated() throws Exception {
        when(userService.update(any(UpdateUserRequest.class))).thenReturn(UserResponse.builder()
                .id(1L)
                .name("Johnny")
                .email("johnny@test.ru")
                .build());

        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(UpdateUserRequest.builder().name("Johnny").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Johnny"));
    }

    @Test
    void deleteUser_returnsNoContent() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getUser_returns404_whenUserMissing() throws Exception {
        when(userService.findById(1L)).thenThrow(new NotFoundException("User not found"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void createUser_returns409_whenEmailInUse() throws Exception {
        when(userService.create(any(CreateUserRequest.class))).thenThrow(new ConflictException("Email already in use"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void getUser_returns500_onUnexpectedError() throws Exception {
        when(userService.findById(1L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}
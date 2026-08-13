package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.dto.CommentResponse;
import ru.practicum.shareit.item.dto.CreateCommentRequest;
import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemResponse;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void create_returnsItem() throws Exception {
        when(itemService.create(any(CreateItemRequest.class), eq(1L))).thenReturn(ItemResponse.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .requestId(5L)
                .build());

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(CreateItemRequest.builder()
                                .name("Hammer")
                                .description("iron hammer")
                                .available(true)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Hammer"));
    }

    @Test
    void update_returnsUpdatedItem() throws Exception {
        when(itemService.update(eq(1L), any(UpdateItemRequest.class), eq(2L))).thenReturn(ItemResponse.builder()
                .id(1L)
                .name("Sledgehammer")
                .description("heavy")
                .available(false)
                .build());

        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateItemRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sledgehammer"));
    }

    @Test
    void getItem_returnsItem() throws Exception {
        when(itemService.findById(1L, 3L)).thenReturn(ItemResponse.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .build());

        mockMvc.perform(get("/items/1").header("X-Sharer-User-Id", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hammer"));
    }

    @Test
    void getAll_returnsListOfItems() throws Exception {
        when(itemService.findAllOwnerItems(4L)).thenReturn(List.of(ItemResponse.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .build()));

        mockMvc.perform(get("/items").header("X-Sharer-User-Id", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Hammer"));
    }

    @Test
    void search_returnsFoundItems() throws Exception {
        when(itemService.search("hammer")).thenReturn(List.of(ItemResponse.builder()
                .id(1L)
                .name("Hammer")
                .build()));

        mockMvc.perform(get("/items/search").param("text", "hammer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void addComment_returnsComment() throws Exception {
        when(itemService.addComment(eq(1L), any(CreateCommentRequest.class), eq(2L)))
                .thenReturn(CommentResponse.builder()
                        .id(1L)
                        .text("great")
                        .authorName("John")
                        .build());
        CreateCommentRequest request = new CreateCommentRequest();
        request.setText("great");

        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("great"));
    }
}
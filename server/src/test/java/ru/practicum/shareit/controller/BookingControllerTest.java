package ru.practicum.shareit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.dto.BookingResponse;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    @Test
    void create_returnsBooking() throws Exception {
        when(bookingService.create(any(CreateBookingRequest.class), eq(1L))).thenReturn(bookingResponse());

        CreateBookingRequest request = new CreateBookingRequest();
        request.setItemId(1L);
        request.setStart(LocalDateTime.now().plusDays(1));
        request.setEnd(LocalDateTime.now().plusDays(2));

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    void approve_returnsUpdatedBooking() throws Exception {
        when(bookingService.approve(1L, true, 2L)).thenReturn(BookingResponse.builder()
                .id(1L)
                .status(BookingStatus.APPROVED)
                .build());

        mockMvc.perform(patch("/bookings/1")
                        .header("X-Sharer-User-Id", 2)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void getBooking_returnsBooking() throws Exception {
        when(bookingService.findById(1L, 3L)).thenReturn(bookingResponse());

        mockMvc.perform(get("/bookings/1").header("X-Sharer-User-Id", 3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getAll_returnsBookings() throws Exception {
        when(bookingService.findAllByBooker("ALL", 4L)).thenReturn(List.of(bookingResponse()));

        mockMvc.perform(get("/bookings").header("X-Sharer-User-Id", 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getAllByOwner_returnsBookings() throws Exception {
        when(bookingService.findAllByOwner("ALL", 5L)).thenReturn(List.of(bookingResponse()));

        mockMvc.perform(get("/bookings/owner").header("X-Sharer-User-Id", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].item.name").value("Hammer"));
    }

    @Test
    void getBooking_returns404_whenBookingMissing() throws Exception {
        when(bookingService.findById(1L, 3L)).thenThrow(new NotFoundException("Booking not found"));

        mockMvc.perform(get("/bookings/1").header("X-Sharer-User-Id", 3))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void approve_returns403_whenNotOwner() throws Exception {
        when(bookingService.approve(1L, true, 2L)).thenThrow(new ForbiddenException("Only owner can approve"));

        mockMvc.perform(patch("/bookings/1")
                        .header("X-Sharer-User-Id", 2)
                        .param("approved", "true"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void create_returns400_whenAlreadyApproved() throws Exception {
        when(bookingService.approve(1L, true, 2L)).thenThrow(new IllegalArgumentException("Booking already approved"));

        mockMvc.perform(patch("/bookings/1")
                        .header("X-Sharer-User-Id", 2)
                        .param("approved", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void getBooking_returns500_onUnexpectedError() throws Exception {
        when(bookingService.findById(1L, 3L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/bookings/1").header("X-Sharer-User-Id", 3))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    private BookingResponse bookingResponse() {
        return BookingResponse.builder()
                .id(1L)
                .start(LocalDateTime.now().plusDays(1))
                .end(LocalDateTime.now().plusDays(2))
                .item(BookingResponse.ItemInfo.builder().id(1L).name("Hammer").build())
                .booker(BookingResponse.BookerInfo.builder().id(1L).name("John").build())
                .status(BookingStatus.WAITING)
                .build();
    }
}
package ru.practicum.shareit.booking.mapper;

import lombok.NoArgsConstructor;
import ru.practicum.shareit.booking.dto.BookingResponse;
import ru.practicum.shareit.booking.model.Booking;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class BookingMapper {

    public static BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .start(booking.getStart())
                .end(booking.getEnd())
                .item(BookingResponse.ItemInfo.builder()
                        .id(booking.getItem().getId())
                        .name(booking.getItem().getName())
                        .build())
                .booker(BookingResponse.BookerInfo.builder()
                        .id(booking.getBooker().getId())
                        .name(booking.getBooker().getName())
                        .build())
                .status(booking.getStatus())
                .build();
    }
}
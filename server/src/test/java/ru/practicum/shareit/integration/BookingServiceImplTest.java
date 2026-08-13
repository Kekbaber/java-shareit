package ru.practicum.shareit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingResponse;
import ru.practicum.shareit.booking.dto.CreateBookingRequest;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceImplTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void create_savesWaitingBooking() {
        User owner = saveUser("owner", "owner@test.ru");
        User booker = saveUser("booker", "booker@test.ru");
        Item item = saveItem(owner, true);
        LocalDateTime now = LocalDateTime.now();

        BookingResponse response = bookingService.create(bookingRequest(item.getId(), now.plusHours(1), now.plusHours(2)),
                booker.getId());

        assertThat(response.getId()).isPositive();
        assertThat(response.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(response.getItem().getId()).isEqualTo(item.getId());
        assertThat(response.getBooker().getId()).isEqualTo(booker.getId());
    }

    @Test
    void create_throwsWhenItemUnavailable() {
        User owner = saveUser("owner", "owner@test.ru");
        User booker = saveUser("booker", "booker@test.ru");
        Item item = saveItem(owner, false);

        assertThatThrownBy(() -> bookingService.create(bookingRequest(item.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_throwsWhenBookingOwnItem() {
        User owner = saveUser("owner", "owner@test.ru");
        Item item = saveItem(owner, true);

        assertThatThrownBy(() -> bookingService.create(bookingRequest(item.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), owner.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approve_byOwnerChangesStatus() {
        User owner = saveUser("owner", "owner@test.ru");
        User booker = saveUser("booker", "booker@test.ru");
        Item item = saveItem(owner, true);
        BookingResponse created = bookingService.create(bookingRequest(item.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        BookingResponse approved = bookingService.approve(created.getId(), true, owner.getId());

        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void approve_throwsWhenNotOwner() {
        User owner = saveUser("owner", "owner@test.ru");
        User booker = saveUser("booker", "booker@test.ru");
        Item item = saveItem(owner, true);
        BookingResponse created = bookingService.create(bookingRequest(item.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        assertThatThrownBy(() -> bookingService.approve(created.getId(), true, booker.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAllByBooker_filtersByState() {
        User owner = saveUser("owner", "owner@test.ru");
        User booker = saveUser("booker", "booker@test.ru");
        Item item = saveItem(owner, true);
        LocalDateTime now = LocalDateTime.now();
        bookingService.create(bookingRequest(item.getId(), now.minusHours(3), now.minusHours(2)), booker.getId());
        bookingService.create(bookingRequest(item.getId(), now.plusHours(1), now.plusHours(2)), booker.getId());

        List<BookingResponse> all = bookingService.findAllByBooker("ALL", booker.getId());
        List<BookingResponse> past = bookingService.findAllByBooker("PAST", booker.getId());
        List<BookingResponse> future = bookingService.findAllByBooker("FUTURE", booker.getId());

        assertThat(all).hasSize(2);
        assertThat(past).hasSize(1);
        assertThat(future).hasSize(1);
    }

    @Test
    void findAllByOwner_returnsOwnerItemsBookings() {
        User owner = saveUser("owner", "owner@test.ru");
        User booker = saveUser("booker", "booker@test.ru");
        Item item = saveItem(owner, true);
        bookingService.create(bookingRequest(item.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        List<BookingResponse> responses = bookingService.findAllByOwner("ALL", owner.getId());

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getItem().getId()).isEqualTo(item.getId());
    }

    @Test
    void findById_throwsWhenAccessDenied() {
        User owner = saveUser("owner", "owner@test.ru");
        User booker = saveUser("booker", "booker@test.ru");
        User stranger = saveUser("stranger", "stranger@test.ru");
        Item item = saveItem(owner, true);
        BookingResponse created = bookingService.create(bookingRequest(item.getId(),
                LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)), booker.getId());

        assertThatThrownBy(() -> bookingService.findById(created.getId(), stranger.getId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> bookingService.findById(999L, booker.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    private User saveUser(String name, String email) {
        return userRepository.save(User.builder().name(name).email(email).build());
    }

    private Item saveItem(User owner, boolean available) {
        return itemRepository.save(Item.builder()
                .name("Hammer")
                .description("iron hammer")
                .available(available)
                .owner(owner)
                .build());
    }

    private CreateBookingRequest bookingRequest(long itemId, LocalDateTime start, LocalDateTime end) {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setItemId(itemId);
        request.setStart(start);
        request.setEnd(end);
        return request;
    }
}
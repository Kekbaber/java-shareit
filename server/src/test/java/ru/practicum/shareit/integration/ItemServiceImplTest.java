package ru.practicum.shareit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.dto.CommentResponse;
import ru.practicum.shareit.item.dto.CreateCommentRequest;
import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemResponse;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.item.storage.CommentRepository;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.storage.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceImplTest {

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Test
    void findAllOwnerItems_returnsItemsWithBookingsAndComments() {
        User owner = userRepository.save(User.builder().name("owner").email("owner@test.ru").build());
        User booker = userRepository.save(User.builder().name("booker").email("booker@test.ru").build());
        Item item = itemRepository.save(Item.builder()
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .owner(owner)
                .build());

        LocalDateTime now = LocalDateTime.now();
        bookingRepository.save(Booking.builder()
                .start(now.minusDays(2))
                .end(now.minusDays(1))
                .item(item)
                .booker(booker)
                .status(BookingStatus.APPROVED)
                .build());
        bookingRepository.save(Booking.builder()
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .item(item)
                .booker(booker)
                .status(BookingStatus.APPROVED)
                .build());
        commentRepository.save(Comment.builder()
                .text("good tool")
                .item(item)
                .author(booker)
                .created(now.minusDays(1))
                .build());

        List<ItemResponse> responses = itemService.findAllOwnerItems(owner.getId());

        assertThat(responses).hasSize(1);
        ItemResponse response = responses.get(0);
        assertThat(response.getLastBooking()).isNotNull();
        assertThat(response.getLastBooking().getBookerId()).isEqualTo(booker.getId());
        assertThat(response.getNextBooking()).isNotNull();
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getComments().get(0).getText()).isEqualTo("good tool");
    }

    @Test
    void create_withRequestId_bindsRequest() {
        User owner = userRepository.save(User.builder().name("owner").email("owner@test.ru").build());
        User requester = userRepository.save(User.builder().name("req").email("req@test.ru").build());
        ItemRequest request = itemRequestRepository.save(ItemRequest.builder()
                .description("need drill")
                .requestor(requester)
                .created(LocalDateTime.now())
                .build());

        ItemResponse response = itemService.create(CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .requestId(request.getId())
                .build(), owner.getId());

        assertThat(response.getRequestId()).isEqualTo(request.getId());
        Item saved = itemRepository.findById(response.getId()).orElseThrow();
        assertThat(saved.getRequest().getId()).isEqualTo(request.getId());
    }

    @Test
    void create_withoutRequestId_savesPlainItem() {
        User owner = userRepository.save(User.builder().name("owner").email("owner@test.ru").build());

        ItemResponse response = itemService.create(CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .build(), owner.getId());

        assertThat(response.getRequestId()).isNull();
    }

    @Test
    void create_withUnknownRequestId_throws() {
        User owner = userRepository.save(User.builder().name("owner").email("owner@test.ru").build());

        assertThatThrownBy(() -> itemService.create(CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .requestId(999L)
                .build(), owner.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addComment_allowedForFinishedBooking() {
        User owner = userRepository.save(User.builder().name("owner").email("owner@test.ru").build());
        User booker = userRepository.save(User.builder().name("booker").email("booker@test.ru").build());
        Item item = itemRepository.save(Item.builder()
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .owner(owner)
                .build());
        LocalDateTime now = LocalDateTime.now();
        bookingRepository.save(Booking.builder()
                .start(now.minusDays(2))
                .end(now.minusDays(1))
                .item(item)
                .booker(booker)
                .status(BookingStatus.APPROVED)
                .build());

        CreateCommentRequest comment = new CreateCommentRequest();
        comment.setText("great");
        CommentResponse response = itemService.addComment(item.getId(), comment, booker.getId());

        assertThat(response.getText()).isEqualTo("great");
        assertThat(response.getAuthorName()).isEqualTo(booker.getName());
    }
}
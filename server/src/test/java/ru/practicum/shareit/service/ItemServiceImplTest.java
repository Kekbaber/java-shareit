package ru.practicum.shareit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.storage.BookingRepository;
import ru.practicum.shareit.exception.model.ForbiddenException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.dto.CommentResponse;
import ru.practicum.shareit.item.dto.CreateCommentRequest;
import ru.practicum.shareit.item.dto.CreateItemRequest;
import ru.practicum.shareit.item.dto.ItemResponse;
import ru.practicum.shareit.item.dto.UpdateItemRequest;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.service.impl.ItemServiceImpl;
import ru.practicum.shareit.item.storage.CommentRepository;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.storage.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.storage.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private User owner;
    private User booker;
    private Item item;
    private Comment comment;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).name("owner").email("owner@test.ru").build();
        booker = User.builder().id(2L).name("booker").email("booker@test.ru").build();
        item = Item.builder()
                .id(1L)
                .name("Hammer")
                .description("iron hammer")
                .available(true)
                .owner(owner)
                .build();
        comment = Comment.builder()
                .id(1L)
                .text("good tool")
                .item(item)
                .author(booker)
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    void create_withRequestId_bindsRequest() {
        ItemRequest request = ItemRequest.builder().id(5L).description("need drill").build();
        CreateItemRequest createRequest = CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .requestId(5L)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(5L)).thenReturn(Optional.of(request));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        ItemResponse response = itemService.create(createRequest, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        verify(itemRequestRepository).findById(5L);
    }

    @Test
    void create_withoutRequestId_skipsRequestLookup() {
        CreateItemRequest createRequest = CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        itemService.create(createRequest, 1L);

        verify(itemRequestRepository, never()).findById(any());
    }

    @Test
    void create_throwsWhenUserMissing() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.create(CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .build(), 9L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_throwsWhenRequestMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.create(CreateItemRequest.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .requestId(5L)
                .build(), 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_changesOnlyProvidedFields() {
        UpdateItemRequest update = new UpdateItemRequest();
        update.setName("Sledgehammer");
        update.setAvailable(false);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        ItemResponse response = itemService.update(1L, update, 1L);

        assertThat(response.getName()).isEqualTo("Sledgehammer");
        assertThat(response.getAvailable()).isFalse();
        assertThat(response.getDescription()).isEqualTo("iron hammer");
    }

    @Test
    void update_throwsWhenNotOwner() {
        UpdateItemRequest update = new UpdateItemRequest();
        update.setName("Sledgehammer");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.update(1L, update, 2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void update_throwsWhenItemMissing() {
        UpdateItemRequest update = new UpdateItemRequest();
        update.setName("Ghost");
        when(itemRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.update(9L, update, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findById_returnsBookingsAndCommentsForOwner() {
        LocalDateTime now = LocalDateTime.now();
        Booking past = booking(now.minusDays(2), now.minusDays(1));
        Booking next = booking(now.plusDays(1), now.plusDays(2));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(commentRepository.findByItemId(1L)).thenReturn(List.of(comment));
        when(bookingRepository.findAllApprovedForItems(anyList())).thenReturn(List.of(past, next));

        ItemResponse response = itemService.findById(1L, 1L);

        assertThat(response.getLastBooking()).isNotNull();
        assertThat(response.getNextBooking()).isNotNull();
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getComments().get(0).getText()).isEqualTo("good tool");
    }

    @Test
    void findById_returnsNullBookingsForOtherUser() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(commentRepository.findByItemId(1L)).thenReturn(List.of());

        ItemResponse response = itemService.findById(1L, 2L);

        assertThat(response.getLastBooking()).isNull();
        assertThat(response.getNextBooking()).isNull();
        assertThat(response.getComments()).isEmpty();
        verify(bookingRepository, never()).findAllApprovedForItems(anyList());
    }

    @Test
    void findById_throwsWhenItemMissing() {
        when(itemRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.findById(9L, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findAllOwnerItems_returnsEmptyWhenNoItems() {
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of());

        assertThat(itemService.findAllOwnerItems(1L)).isEmpty();
    }

    @Test
    void findAllOwnerItems_returnsItemsWithBookingsAndComments() {
        LocalDateTime now = LocalDateTime.now();
        Booking past = booking(now.minusDays(2), now.minusDays(1));
        Booking next = booking(now.plusDays(1), now.plusDays(2));
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of(item));
        when(bookingRepository.findAllApprovedForItems(anyList())).thenReturn(List.of(past, next));
        when(commentRepository.findByItemIdIn(anyList())).thenReturn(List.of(comment));

        List<ItemResponse> responses = itemService.findAllOwnerItems(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getLastBooking()).isNotNull();
        assertThat(responses.get(0).getNextBooking()).isNotNull();
        assertThat(responses.get(0).getComments()).hasSize(1);
    }

    @Test
    void findAllOwnerItems_withoutBookings() {
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of(item));
        when(bookingRepository.findAllApprovedForItems(anyList())).thenReturn(List.of());
        when(commentRepository.findByItemIdIn(anyList())).thenReturn(List.of());

        List<ItemResponse> responses = itemService.findAllOwnerItems(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getLastBooking()).isNull();
        assertThat(responses.get(0).getNextBooking()).isNull();
    }

    @Test
    void findAllOwnerItems_withOnlyPastBooking() {
        LocalDateTime now = LocalDateTime.now();
        Booking past = booking(now.minusDays(2), now.minusDays(1));
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of(item));
        when(bookingRepository.findAllApprovedForItems(anyList())).thenReturn(List.of(past));
        when(commentRepository.findByItemIdIn(anyList())).thenReturn(List.of());

        List<ItemResponse> responses = itemService.findAllOwnerItems(1L);

        assertThat(responses.get(0).getLastBooking()).isNotNull();
        assertThat(responses.get(0).getNextBooking()).isNull();
    }

    @Test
    void search_returnsMatchingItems() {
        when(itemRepository.search("drill")).thenReturn(List.of(item));

        List<ItemResponse> responses = itemService.search("drill");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Hammer");
        verify(itemRepository).search("drill");
    }

    @Test
    void addComment_addsForBookedUser() {
        CreateCommentRequest createComment = new CreateCommentRequest();
        createComment.setText("good tool");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.existsByItemIdAndBookerIdAndApprovedAndEndBefore(1L, 2L)).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentResponse response = itemService.addComment(1L, createComment, 2L);

        assertThat(response.getText()).isEqualTo("good tool");
        assertThat(response.getAuthorName()).isEqualTo("booker");
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_throwsWhenUserNotBooked() {
        CreateCommentRequest createComment = new CreateCommentRequest();
        createComment.setText("good tool");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.findById(2L)).thenReturn(Optional.of(booker));
        when(bookingRepository.existsByItemIdAndBookerIdAndApprovedAndEndBefore(1L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> itemService.addComment(1L, createComment, 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addComment_throwsWhenItemMissing() {
        CreateCommentRequest createComment = new CreateCommentRequest();
        createComment.setText("good tool");
        when(itemRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.addComment(9L, createComment, 2L))
                .isInstanceOf(NotFoundException.class);
    }

    private Booking booking(LocalDateTime start, LocalDateTime end) {
        return Booking.builder()
                .id(1L)
                .start(start)
                .end(end)
                .item(item)
                .booker(booker)
                .status(BookingStatus.APPROVED)
                .build();
    }
}
package ru.practicum.shareit.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.storage.ItemRepository;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.dto.CreateItemRequest;
import ru.practicum.shareit.request.dto.ItemRequestResponse;
import ru.practicum.shareit.request.service.ItemRequestService;
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
class ItemRequestServiceImplTest {

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void create_savesRequestForExistingUser() {
        User requester = userRepository.save(User.builder().name("req").email("req@test.ru").build());
        CreateItemRequest request = new CreateItemRequest();
        request.setDescription("Need a drill");

        ItemRequestResponse response = itemRequestService.create(request, requester.getId());

        assertThat(response.getId()).isPositive();
        assertThat(response.getDescription()).isEqualTo("Need a drill");
        assertThat(response.getCreated()).isNotNull();
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void create_throwsWhenUserMissing() {
        CreateItemRequest request = new CreateItemRequest();
        request.setDescription("Need a drill");

        assertThatThrownBy(() -> itemRequestService.create(request, 999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOwnRequests_returnsOwnRequestsNewestFirstWithItems() {
        User requester = userRepository.save(User.builder().name("req").email("req@test.ru").build());
        User owner = userRepository.save(User.builder().name("owner").email("owner@test.ru").build());
        ItemRequest older = itemRequestRepository.save(ItemRequest.builder()
                .description("older")
                .requestor(requester)
                .created(LocalDateTime.now().minusHours(2))
                .build());
        itemRequestRepository.save(ItemRequest.builder()
                .description("newer")
                .requestor(requester)
                .created(LocalDateTime.now())
                .build());
        itemRepository.save(Item.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .owner(owner)
                .request(older)
                .build());

        List<ItemRequestResponse> responses = itemRequestService.getOwnRequests(requester.getId());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDescription()).isEqualTo("newer");
        assertThat(responses.get(1).getDescription()).isEqualTo("older");
        assertThat(responses.get(1).getItems()).hasSize(1);
        ItemRequestResponse.ItemSummary summary = responses.get(1).getItems().get(0);
        assertThat(summary.getName()).isEqualTo("Drill");
        assertThat(summary.getOwnerId()).isEqualTo(owner.getId());
    }

    @Test
    void getAllOtherRequests_returnsOnlyRequestsOfOthers() {
        User me = userRepository.save(User.builder().name("me").email("me@test.ru").build());
        User other = userRepository.save(User.builder().name("other").email("other@test.ru").build());
        itemRequestRepository.save(ItemRequest.builder()
                .description("mine")
                .requestor(me)
                .created(LocalDateTime.now().minusHours(1))
                .build());
        itemRequestRepository.save(ItemRequest.builder()
                .description("older other")
                .requestor(other)
                .created(LocalDateTime.now().minusHours(3))
                .build());
        itemRequestRepository.save(ItemRequest.builder()
                .description("newer other")
                .requestor(other)
                .created(LocalDateTime.now().minusHours(2))
                .build());

        List<ItemRequestResponse> responses = itemRequestService.getAllOtherRequests(me.getId());

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getDescription()).isEqualTo("newer other");
        assertThat(responses.get(1).getDescription()).isEqualTo("older other");
    }

    @Test
    void getById_returnsRequestWithItems() {
        User requester = userRepository.save(User.builder().name("req").email("req@test.ru").build());
        User owner = userRepository.save(User.builder().name("owner").email("owner@test.ru").build());
        ItemRequest request = itemRequestRepository.save(ItemRequest.builder()
                .description("need drill")
                .requestor(requester)
                .created(LocalDateTime.now())
                .build());
        itemRepository.save(Item.builder()
                .name("Drill")
                .description("power drill")
                .available(true)
                .owner(owner)
                .request(request)
                .build());

        ItemRequestResponse response = itemRequestService.getById(request.getId());

        assertThat(response.getDescription()).isEqualTo("need drill");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getId()).isPositive();
    }

    @Test
    void getById_throwsWhenRequestMissing() {
        assertThatThrownBy(() -> itemRequestService.getById(999L))
                .isInstanceOf(NotFoundException.class);
    }
}
package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public ItemDto create(@Valid @RequestBody ItemDto itemDto,
                          @RequestHeader("X-Sharer-User-Id") long userId) {
        ItemDto created = itemService.create(itemDto, userId);
        log.info("Создана вещь: itemId={} userId={}", created.getId(), userId);
        return created;
    }

    @PatchMapping("/{itemId}")
    public ItemDto update(@PathVariable long itemId,
                          @RequestBody ItemDto itemDto,
                          @RequestHeader("X-Sharer-User-Id") long userId) {
        ItemDto updated = itemService.update(itemId, itemDto, userId);
        log.info("Обновлена вещь: itemId={} userId={}", updated.getId(), userId);
        return updated;
    }

    @GetMapping("/{itemId}")
    public ItemDto getItem(@PathVariable long itemId) {
        log.debug("Получение вещи: id={}", itemId);
        return itemService.findById(itemId);
    }

    @GetMapping
    public List<ItemDto> getAll(@RequestHeader("X-Sharer-User-Id") long userId) {
        log.debug("Получение всех вещей пользователя: userId={}", userId);
        return itemService.findAllOwnerItems(userId);
    }

    @GetMapping("/search")
    public List<ItemDto> search(@RequestParam String text) {
        log.info("Поиск вещей: text='{}'", text);
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemService.search(text);
    }

}

package ru.practicum.shareit.item.service;

import ru.practicum.shareit.item.dto.ItemDto;

import java.util.List;

public interface ItemService {

    ItemDto create(ItemDto item, long userId);

    ItemDto update(long itemId, ItemDto itemDto, long userId);

    ItemDto findById(long id);

    List<ItemDto> findAllOwnerItems(long ownerId);

    List<ItemDto> search(String text);
}

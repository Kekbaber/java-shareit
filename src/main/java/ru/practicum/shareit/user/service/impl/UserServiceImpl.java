package ru.practicum.shareit.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.model.ConflictException;
import ru.practicum.shareit.exception.model.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.service.UserService;
import ru.practicum.shareit.user.storage.UserStorage;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserStorage userStorage;

    @Override
    public UserDto findById(long id) {
        return userStorage.findById(id)
                .map(UserMapper::toDto)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public UserDto create(UserDto userDto) {
        if (userDto.getEmail() != null) {
            userStorage.findByEmail(userDto.getEmail())
                    .ifPresent(user -> {
                        log.warn("User with email {} already exists", user.getEmail());
                        throw new ConflictException("Email already in use");
                    });
        }
        User user = UserMapper.toEntity(userDto);
        User created = userStorage.create(user);
        return UserMapper.toDto(created);
    }

    @Override
    public UserDto update(UserDto userDto) {
        User user = userStorage.findById(userDto.getId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }
        if (userDto.getEmail() != null) {
            userStorage.findByEmail(userDto.getEmail())
                    .filter(existing -> existing.getId() != user.getId())
                    .ifPresent(existing -> {
                        throw new ConflictException("Email already in use");
                    });
            user.setEmail(userDto.getEmail());
        }

        userStorage.update(user);
        return UserMapper.toDto(user);
    }

    @Override
    public void delete(long id) {
        userStorage.delete(id);
    }
}

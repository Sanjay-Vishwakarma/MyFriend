package com.realtime.myfriend.service;


import com.realtime.myfriend.dtos.UserDTO;
import com.realtime.myfriend.entity.User;
import com.realtime.myfriend.entity.User.Role;
import com.realtime.myfriend.exception.UserNotFoundException;
import com.realtime.myfriend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PresenceService presenceService;

    public CompletableFuture<User> getUserById(String userId) {
        return CompletableFuture.supplyAsync(() ->
                userRepository.findById(userId)
                        .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId))
        );
    }

    public CompletableFuture<User> getUserByUsername(String username) {
        return CompletableFuture.supplyAsync(() -> {
            logger.debug("Looking up user by username: {}", username);

            // First try to find by username
            Optional<User> userByUsername = userRepository.findByUsername(username);
            if (userByUsername.isPresent()) {
                return userByUsername.get();
            }

            // If not found by username, try by ID
            Optional<User> userById = userRepository.findById(username);
            if (userById.isPresent()) {
                return userById.get();
            }

            throw new UserNotFoundException("User not found with identifier: " + username);
        });
    }

    public CompletableFuture<List<UserDTO>> getAllUsers() {
        return CompletableFuture.supplyAsync(() ->
                userRepository.findAll()
                        .stream()
                        .map(user -> new UserDTO(
                                user.getId(),
                                user.getName(),
                                user.getEmail(),
                                user.getUsername(),
                                user.isOnline(),
                                user.getLastSeen()
                        ))
                        .collect(Collectors.toList())
        );
    }

    public CompletableFuture<List<User>> getOnlineUsers() {
        return CompletableFuture.supplyAsync(userRepository::findAllOnlineUsers);
    }

    @Transactional
    public CompletableFuture<User> updateUser(String userId, User updatedUser) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

            user.setName(updatedUser.getName());
            user.setEmail(updatedUser.getEmail());
            user.setDob(updatedUser.getDob());
            user.setGender(updatedUser.getGender());
            user.setUpdatedAt(LocalDateTime.now());

            return userRepository.save(user);
        });
    }

    @Transactional
    public CompletableFuture<Void> deleteUser(String userId) {
        return CompletableFuture.runAsync(() -> {
            if (!userRepository.existsById(userId)) {
                throw new UserNotFoundException("User not found with id: " + userId);
            }
            userRepository.deleteById(userId);
            presenceService.userDisconnected(userId);
        });
    }

    @Transactional
    public CompletableFuture<User> promoteToAdmin(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

            user.setRole(Role.ADMIN);
            user.setUpdatedAt(LocalDateTime.now());

            return userRepository.save(user);
        });
    }

    public String findIdByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


    public String findUsernameById(String senderId) {
        return userRepository.findById(senderId)
                .map(User::getUsername)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + senderId));
    }

}
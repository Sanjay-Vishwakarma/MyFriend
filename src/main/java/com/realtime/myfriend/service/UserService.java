package com.realtime.myfriend.service;


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
import java.util.concurrent.CompletableFuture;

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
        return CompletableFuture.supplyAsync(() -> 
            userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username))
        );
    }

    public CompletableFuture<List<User>> getAllUsers() {
        return CompletableFuture.supplyAsync(userRepository::findAll);
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




}
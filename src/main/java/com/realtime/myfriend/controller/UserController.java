package com.realtime.myfriend.controller;


import com.realtime.myfriend.dtos.UserDTO;
import com.realtime.myfriend.entity.User;
import com.realtime.myfriend.exception.UserNotFoundException;
import com.realtime.myfriend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Users", description = "User management API")
@CrossOrigin("http://localhost:3000")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;

//    @GetMapping
//    @Operation(summary = "Get all users")
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    public CompletableFuture<ResponseEntity<List<UserDTO>>> getAllUsers() {
//        return userService.getAllUsers()
//                .thenApply(users -> {
//                    logger.info("Successfully fetched {} users", users.size());
//                    System.out.println(users);
//                    return ResponseEntity.ok(users);
//                })
//                .exceptionally(e -> {
//                    logger.error("Failed to get users: {}", e.getMessage(), e);
//                    return ResponseEntity.internalServerError().build();
//                });
//    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers().join(); // block just for testing
        System.out.println("users = " + users);
        return ResponseEntity.ok(users);
    }


    @GetMapping("/online")
    @Operation(summary = "Get online users")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<List<User>>> getOnlineUsers() {
        return userService.getOnlineUsers()
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    logger.error("Failed to get online users: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<User>> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    if (e.getCause() instanceof UserNotFoundException) {
                        return ResponseEntity.notFound().build();
                    }
                    logger.error("Failed to get user: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<User>> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    if (e.getCause() instanceof UserNotFoundException) {
                        return ResponseEntity.notFound().build();
                    }
                    logger.error("Failed to get user: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    @PreAuthorize("hasRole('USER') and #id == principal.id or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<User>> updateUser(
            @PathVariable String id,
            @RequestBody User updatedUser
    ) {
        return userService.updateUser(id, updatedUser)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    if (e.getCause() instanceof UserNotFoundException) {
                        return ResponseEntity.notFound().build();
                    }
                    logger.error("Failed to update user: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user")
    @PreAuthorize("hasRole('USER') and #id == principal.id or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<Void>> deleteUser(@PathVariable String id) {
        return userService.deleteUser(id)
                .thenApplyAsync(unused -> ResponseEntity.noContent().<Void>build())
                .exceptionally(throwable -> {
                    Throwable cause = throwable.getCause();
                    if (cause instanceof UserNotFoundException) {
                        return ResponseEntity.notFound().build();
                    } else {
                        logger.error("Failed to delete user: {}", cause.getMessage());
                        return ResponseEntity.internalServerError().build();
                    }
                });
    }
}
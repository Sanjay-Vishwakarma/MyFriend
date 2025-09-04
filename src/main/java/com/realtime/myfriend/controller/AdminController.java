package com.realtime.myfriend.controller;


import com.realtime.myfriend.dtos.UserDTO;
import com.realtime.myfriend.entity.User;
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
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin management API")
@PreAuthorize("hasRole('ADMIN')") // All endpoints in this controller require ADMIN role
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get all users (Admin only)")
    public CompletableFuture<ResponseEntity<List<UserDTO>>> getAllUsersAdmin() {
        return userService.getAllUsers()
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    logger.error("Failed to get users: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

//    @DeleteMapping("/{id}")
//    @Operation(summary = "Delete any user (Admin only)")
//    public CompletableFuture<ResponseEntity<Void>> deleteUserAdmin(@PathVariable String id) {
//        return userService.deleteUser(id)
//                .thenApply(v -> ResponseEntity.noContent().build())
//                .exceptionally(e -> {
//                    logger.error("Failed to delete user: {}", e.getMessage());
//                    return ResponseEntity.internalServerError().build();
//                });
//    }
//
//    @PostMapping("/{id}/promote")
//    @Operation(summary = "Promote user to admin")
//    public CompletableFuture<ResponseEntity<User>> promoteToAdmin(@PathVariable String id) {
//        return userService.promoteToAdmin(id)
//                .thenApply(ResponseEntity::ok)
//                .exceptionally(e -> {
//                    logger.error("Failed to promote user: {}", e.getMessage());
//                    return ResponseEntity.internalServerError().build();
//                });
//    }
}
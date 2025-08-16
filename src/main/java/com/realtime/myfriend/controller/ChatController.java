package com.realtime.myfriend.controller;


import com.realtime.myfriend.entity.ChatMessage;
import com.realtime.myfriend.exception.UserNotFoundException;
import com.realtime.myfriend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chat", description = "Chat API")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;

    @PostMapping("/send")
    @Operation(summary = "Send a message")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<ChatMessage>> sendMessage(
            @RequestParam String senderId,
            @RequestParam String receiverId,
            @RequestBody String content
    ) {
        return chatService.sendMessage(senderId, receiverId, content)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    if (e.getCause() instanceof UserNotFoundException) {
                        return ResponseEntity.notFound().build();
                    }
                    logger.error("Failed to send message: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/conversation")
    @Operation(summary = "Get conversation between two users")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<List<ChatMessage>>> getConversation(
            @RequestParam String user1Id,
            @RequestParam String user2Id
    ) {
        return chatService.getConversation(user1Id, user2Id)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    logger.error("Failed to get conversation: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/mark-read")
    @Operation(summary = "Mark messages as read")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<Void>> markMessagesAsRead(
            @RequestParam String senderId,
            @RequestParam String receiverId
    ) {
        return chatService.markMessagesAsRead(senderId, receiverId)
                .thenApply(unused -> ResponseEntity.noContent().<Void>build())
                .exceptionally(ex -> {
                    if (ex.getCause() instanceof ChangeSetPersister.NotFoundException) {
                        return ResponseEntity.notFound().<Void>build();
                    }
                    logger.error("Failed to mark messages as read: {}", ex.getMessage());
                    return ResponseEntity.internalServerError().<Void>build();
                });
    }
}
package com.realtime.myfriend.controller;


import com.realtime.myfriend.entity.ChatMessage;
import com.realtime.myfriend.exception.UserNotFoundException;
import com.realtime.myfriend.service.ChatService;
import com.realtime.myfriend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chat", description = "Chat API")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;

    private final UserService userService;

    @PostMapping("/send")
    @Operation(summary = "Send a message")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<ChatMessage>> sendMessage(
            @RequestParam String receiverUsername,
            @RequestBody String content,
            Principal principal // current user
    ) {
        String senderUsername = principal.getName(); // get sender from logged-in user

        return userService.getUserByUsername(senderUsername)
                .thenCombine(userService.getUserByUsername(receiverUsername),
                        (sender, receiver) -> {
                            if (sender == null || receiver == null) {
                                throw new UserNotFoundException("Sender or receiver not found");
                            }
                            return chatService.sendMessage(sender.getId(), receiver.getId(), content)
                                    .join();
                        })
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    logger.error("Failed to send message: {}", ex.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/conversation")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<List<ChatMessage>>> getConversation(
            @RequestParam String user2Id,
            Principal principal
    ) {
        try {
            String currentUsername = principal.getName();
            logger.debug("Current username: {}", currentUsername);

            // Get current user ID from username
            return userService.getUserByUsername(currentUsername)
                    .thenApply(currentUser -> {
                        String currentUserId = currentUser.getId();
                        logger.debug("Current user ID: {}, Other user ID: {}", currentUserId, user2Id);

                        List<ChatMessage> conversation = chatService.findConversation(currentUserId, user2Id);
                        logger.debug("Found {} messages in conversation", conversation.size());

                        return ResponseEntity.ok(conversation);
                    })
                    .exceptionally(ex -> {
                        logger.error("Failed to get conversation: {}", ex.getMessage());
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    });
        } catch (Exception e) {
            logger.error("Error in getConversation: {}", e.getMessage());
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    @PostMapping("/mark-read")
    @Operation(summary = "Mark messages as read")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<Void>> markMessagesAsRead(
            @RequestParam String senderId,
            @RequestParam String receiverId
    ) {
        return chatService.markMessagesAsRead(senderId, receiverId, null) // ✅ null = mark all
                .thenApply(unused -> ResponseEntity.noContent().<Void>build())
                .exceptionally(ex -> {
                    logger.error("Failed to mark messages as read: {}", ex.getMessage());
                    return ResponseEntity.internalServerError().<Void>build();
                });
    }


    @GetMapping("/testAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> testAuth(Principal principal) {
        return ResponseEntity.ok("Authenticated as: " + principal.getName());
    }

}
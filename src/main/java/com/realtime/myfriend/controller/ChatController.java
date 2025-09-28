package com.realtime.myfriend.controller;

import com.realtime.myfriend.entity.ChatMessage;
import com.realtime.myfriend.exception.UserNotFoundException;
import com.realtime.myfriend.repository.UserRepository;
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
    private final UserRepository userRepository;

    @PostMapping("/send")
    @Operation(summary = "Send a message")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<ChatMessage>> sendMessage(
            @RequestParam String receiverUsername,
            @RequestBody String content,
            Principal principal
    ) {
        String senderUsername = principal.getName();

        return userService.getUserByUsername(senderUsername)
                .thenCompose(sender -> userService.getUserByUsername(receiverUsername)
                        .thenCompose(receiver -> {
                            if (sender == null || receiver == null) {
                                return CompletableFuture.failedFuture(new UserNotFoundException("Sender or receiver not found"));
                            }
                            return chatService.sendMessage(sender.getId(), receiver.getId(), content);
                        }))
                .thenApply(ResponseEntity::ok)
                .handle((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send message: {}", ex.getMessage());
                        if (ex.getCause() instanceof UserNotFoundException) {
                            return ResponseEntity.notFound().build();
                        }
                        return ResponseEntity.internalServerError().build();
                    }
                    return result;
                });
    }

    @GetMapping("/conversation")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<List<ChatMessage>>> getConversation(
            @RequestParam String user2Id,
            Principal principal
    ) {
        return userService.getUserByUsername(principal.getName())
                .thenCompose(user -> {
                    if (user == null) {
                        return CompletableFuture.failedFuture(new UserNotFoundException("User not found"));
                    }
                    return chatService.getConversation(user.getId(), user2Id);
                })
                .thenApply(ResponseEntity::ok)
                .handle((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to get conversation: {}", ex.getMessage());
                        if (ex.getCause() instanceof UserNotFoundException) {
                            return ResponseEntity.notFound().build();
                        }
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                    return result;
                });
    }

    @PostMapping("/mark-read")
    @Operation(summary = "Mark messages as read")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<Void>> markMessagesAsRead(
            @RequestParam String senderId,
            @RequestParam String receiverId
    ) {
        return chatService.markMessagesAsRead(senderId, receiverId, null)
                .thenApply(this::toVoidResponse)
                .exceptionally(this::handleMarkReadError);
    }

    private ResponseEntity<Void> toVoidResponse(Void unused) {
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Void> handleMarkReadError(Throwable ex) {
        logger.error("Failed to mark messages as read: {}", ex.getMessage());
        return ResponseEntity.internalServerError().build();
    }

    @GetMapping("/testAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> testAuth(Principal principal) {
        return ResponseEntity.ok("Authenticated as: " + principal.getName());
    }
}
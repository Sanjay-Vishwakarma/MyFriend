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
import org.springframework.data.crossstore.ChangeSetPersister;
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
@CrossOrigin("http://localhost:3000")
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
        String currentUserId = principal.getName();

        System.out.println("currentUserId = " + currentUserId);
        System.out.println("user2Id = " + user2Id);
        List<ChatMessage> conversation = chatService.findConversation(currentUserId, user2Id);
        System.out.println("conversation = " + conversation);
        System.out.println("conversation size = " + conversation.size());
        return CompletableFuture.completedFuture(ResponseEntity.ok(conversation));

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

}
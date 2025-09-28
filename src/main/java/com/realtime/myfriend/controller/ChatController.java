package com.realtime.myfriend.controller;

import com.realtime.myfriend.entity.ChatMessage;
import com.realtime.myfriend.entity.User;
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
    public ResponseEntity<ChatMessage> sendMessage(
            @RequestParam String receiverUsername,
            @RequestBody String content,
            Principal principal
    ) {
        try {
            String senderUsername = principal.getName();

            User sender = userRepository.findByUsername(senderUsername)
                    .orElseThrow(() -> new UserNotFoundException("Sender not found"));
            User receiver = userRepository.findByUsername(receiverUsername)
                    .orElseThrow(() -> new UserNotFoundException("Receiver not found"));

            ChatMessage message = chatService.sendMessage(sender.getId(), receiver.getId(), content);
            return ResponseEntity.ok(message);
        } catch (UserNotFoundException ex) {
            logger.error("User not found: {}", ex.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            logger.error("Failed to send message: {}", ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/conversation")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ChatMessage>> getConversation(
            @RequestParam String user2Id,
            Principal principal
    ) {
        try {
            User currentUser = userRepository.findByUsername(principal.getName())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            List<ChatMessage> messages = chatService.getConversation(currentUser.getId(), user2Id);
            return ResponseEntity.ok(messages);
        } catch (UserNotFoundException ex) {
            logger.error("User not found: {}", ex.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            logger.error("Failed to get conversation: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/mark-read")
    @Operation(summary = "Mark messages as read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> markMessagesAsRead(
            @RequestParam String senderId,
            @RequestParam String receiverId
    ) {
        try {
            chatService.markMessagesAsRead(senderId, receiverId, null);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            logger.error("Failed to mark messages as read: {}", ex.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/testAuth")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<String> testAuth(Principal principal) {
        return ResponseEntity.ok("Authenticated as: " + principal.getName());
    }
}
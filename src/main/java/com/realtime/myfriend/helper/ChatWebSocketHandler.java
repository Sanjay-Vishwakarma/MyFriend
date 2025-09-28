package com.realtime.myfriend.helper;

import com.realtime.myfriend.dtos.ReadReceipt;
import com.realtime.myfriend.entity.ChatMessage;
import com.realtime.myfriend.service.ChatService;
import com.realtime.myfriend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserService userService;

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            logger.warn("Unauthorized message sending attempt");
            return;
        }

        String username = principal.getName();
        String senderId = userService.findIdByUsername(username);
        chatMessage.setSenderId(senderId);

        // ✅ Proper async handling
        chatService.saveMessage(senderId, chatMessage.getReceiverId(), chatMessage.getContent())
                .whenComplete((savedMessage, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to save message: {}", ex.getMessage());
                        return;
                    }

                    try {
                        String senderUsername = userService.findUsernameById(savedMessage.getSenderId());
                        String receiverUsername = userService.findUsernameById(savedMessage.getReceiverId());

                        messagingTemplate.convertAndSendToUser(
                                senderUsername, "/queue/messages", savedMessage
                        );
                        messagingTemplate.convertAndSendToUser(
                                receiverUsername, "/queue/messages", savedMessage
                        );

                        logger.info("Message [{}] sent from {} → {}", savedMessage.getId(), senderUsername, receiverUsername);
                    } catch (Exception e) {
                        logger.error("Error sending WebSocket message: {}", e.getMessage());
                    }
                });
    }

    @MessageMapping("/chat.read")
    public void markAsRead(
            @Payload ReadReceipt receipt,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            logger.warn("Unauthorized read attempt");
            return;
        }

        try {
            String username = principal.getName();
            String readerId = userService.findIdByUsername(username);

            if (!readerId.equals(receipt.getReceiverId())) {
                logger.warn("User {} tried to spoof read receipts for receiver {}", username, receipt.getReceiverId());
                return;
            }

            // ✅ Async mark as read with comprehensive error handling
            chatService.markMessagesAsRead(receipt.getSenderId(), receipt.getReceiverId(), receipt.getMessageIds())
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            handleMarkReadError(ex, receipt);
                            return;
                        }
                        handleMarkReadSuccess(receipt);
                    });

        } catch (Exception ex) {
            logger.error("Error processing read receipt: {}", ex.getMessage());
        }
    }

    private void handleMarkReadSuccess(ReadReceipt receipt) {
        try {
            // Convert sender ID to username for WebSocket routing
            String senderUsername = userService.findUsernameById(receipt.getSenderId());

            messagingTemplate.convertAndSendToUser(
                    senderUsername, "/queue/read", receipt
            );

            logger.info("User {} marked {} messages as read from {}",
                    receipt.getReceiverId(),
                    receipt.getMessageIds() != null ? receipt.getMessageIds().size() : "all",
                    receipt.getSenderId());

        } catch (Exception e) {
            logger.error("Error sending read receipt confirmation: {}", e.getMessage());
        }
    }

    private void handleMarkReadError(Throwable ex, ReadReceipt receipt) {
        logger.error("Failed to mark messages as read from {} to {}: {}",
                receipt.getSenderId(), receipt.getReceiverId(), ex.getMessage());

        // Optionally send error back to the client who tried to mark as read
        try {
            String receiverUsername = userService.findUsernameById(receipt.getReceiverId());
            Map<String, Object> errorResponse = Map.of(
                    "error", "Failed to mark messages as read",
                    "timestamp", System.currentTimeMillis()
            );
            messagingTemplate.convertAndSendToUser(
                    receiverUsername, "/queue/errors", errorResponse
            );
        } catch (Exception e) {
            logger.error("Failed to send error response to client: {}", e.getMessage());
        }
    }

}
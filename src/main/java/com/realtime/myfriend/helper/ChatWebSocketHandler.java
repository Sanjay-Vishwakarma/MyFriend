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

        try {
            String username = principal.getName();
            String senderId = userService.findIdByUsername(username);
            chatMessage.setSenderId(senderId);

            // Synchronous call
            ChatMessage savedMessage = chatService.saveMessage(senderId, chatMessage.getReceiverId(), chatMessage.getContent());

            String senderUsername = userService.findUsernameById(savedMessage.getSenderId());
            String receiverUsername = userService.findUsernameById(savedMessage.getReceiverId());

            // Send back to both users by username (Principal name)
            messagingTemplate.convertAndSendToUser(
                    senderUsername, "/queue/messages", savedMessage
            );
            messagingTemplate.convertAndSendToUser(
                    receiverUsername, "/queue/messages", savedMessage
            );

            logger.info("Message [{}] sent from {} → {}", savedMessage.getId(), senderUsername, receiverUsername);
        } catch (Exception ex) {
            logger.error("Failed to send message via WebSocket: {}", ex.getMessage());
        }
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
                logger.warn("User {} tried to spoof read receipts!", username);
                return;
            }

            // Synchronous call
            chatService.markMessagesAsRead(receipt.getSenderId(), receipt.getReceiverId(), receipt.getMessageIds());

            messagingTemplate.convertAndSendToUser(
                    receipt.getSenderId(), "/queue/read", receipt
            );

            logger.info("User {} marked messages as read from {}", receipt.getReceiverId(), receipt.getSenderId());
        } catch (Exception ex) {
            logger.error("Failed to mark messages as read via WebSocket: {}", ex.getMessage());
        }
    }
}
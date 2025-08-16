package com.realtime.myfriend.helper;


import com.realtime.myfriend.entity.ChatMessage;
import com.realtime.myfriend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    
    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public CompletableFuture<Void> sendMessage(
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            logger.warn("Unauthorized message sending attempt");
            return CompletableFuture.completedFuture(null);
        }
        
        return chatService.sendMessage(
                chatMessage.getSenderId(),
                chatMessage.getReceiverId(),
                chatMessage.getContent()
        ).thenAccept(msg -> logger.info("Message sent from {} to {}", msg.getSenderId(), msg.getReceiverId()));
    }
}
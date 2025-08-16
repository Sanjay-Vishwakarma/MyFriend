package com.realtime.myfriend.helper;


import com.realtime.myfriend.entity.User;
import com.realtime.myfriend.service.PresenceService;
import com.realtime.myfriend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    private final SimpMessageSendingOperations messagingTemplate;
    private final PresenceService presenceService;
    private final UserService userService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        
        CompletableFuture.runAsync(() -> {
            try {
                User user = userService.getUserByUsername(username).join();
                presenceService.userConnected(user.getId());
                logger.info("User connected: {}", username);
            } catch (Exception e) {
                logger.error("Error handling connection for user {}: {}", username, e.getMessage());
            }
        });
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = Objects.requireNonNull(headerAccessor.getUser()).getName();
        
        CompletableFuture.runAsync(() -> {
            try {
                User user = userService.getUserByUsername(username).join();
                presenceService.userDisconnected(user.getId());
                logger.info("User disconnected: {}", username);
            } catch (Exception e) {
                logger.error("Error handling disconnection for user {}: {}", username, e.getMessage());
            }
        });
    }
}
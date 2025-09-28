package com.realtime.myfriend.helper;


import com.realtime.myfriend.entity.User;
import com.realtime.myfriend.security.JwtService;
import com.realtime.myfriend.service.PresenceService;
import com.realtime.myfriend.service.UserService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    
    private final SimpMessageSendingOperations messagingTemplate;
    private final PresenceService presenceService;
    private final UserService userService;
    private final JwtService jwtService;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        Principal user = headerAccessor.getUser();
        if (user == null) {
            logger.error("No user found in disconnect event, sessionId={}", headerAccessor.getSessionId());
            return;
        }

        String username = user.getName();

        CompletableFuture.runAsync(() -> {
            try {
                User dbUser = userService.getUserByUsername(username).join();
                presenceService.userDisconnected(dbUser.getId());
                logger.info("User disconnected: {}", username);
            } catch (Exception e) {
                logger.error("Error handling disconnect for user {}: {}", username, e.getMessage());
            }
        });
    }


    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser(); // 👈 Get the principal set by AuthChannelInterceptor

        if (principal == null) {
            logger.error("WebSocket connection attempt without a valid principal. SessionId: {}", headerAccessor.getSessionId());
            return;
        }

        String username = principal.getName();

        userService.getUserByUsername(username)
                .thenAccept(user -> {
                    if (user != null) {
                        presenceService.userConnected(user.getId());
                        logger.info("User connected successfully: {}", username);
                    } else {
                        logger.error("User not found in DB for authenticated username: {}", username);
                    }
                })
                .exceptionally(ex -> {
                    logger.error("Error processing user connection for {}: {}", username, ex.getMessage(), ex);
                    return null;
                });
    }

}
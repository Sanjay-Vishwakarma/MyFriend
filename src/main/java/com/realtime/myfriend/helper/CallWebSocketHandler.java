package com.realtime.myfriend.helper;


import com.realtime.myfriend.entity.CallHistory;
import com.realtime.myfriend.service.CallService;
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
public class CallWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(CallWebSocketHandler.class);
    
    private final CallService callService;

    @MessageMapping("/call.initiate")
    public CompletableFuture<Void> initiateCall(
            @Payload CallHistory callHistory,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            logger.warn("Unauthorized call initiation attempt");
            return CompletableFuture.completedFuture(null);
        }
        
        return callService.initiateCall(
                callHistory.getCallerId(),
                callHistory.getReceiverId(),
                callHistory.getCallType()
        ).thenAccept(call -> logger.info("Call initiated from {} to {}", call.getCallerId(), call.getReceiverId()));
    }

    @MessageMapping("/call.end")
    public CompletableFuture<Void> endCall(
            @Payload CallHistory callHistory,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        Principal principal = headerAccessor.getUser();
        if (principal == null) {
            logger.warn("Unauthorized call end attempt");
            return CompletableFuture.completedFuture(null);
        }
        
        return callService.endCall(
                callHistory.getId(),
                callHistory.getStatus()
        ).thenAccept(call -> logger.info("Call ended: {}", call.getId()));
    }
}
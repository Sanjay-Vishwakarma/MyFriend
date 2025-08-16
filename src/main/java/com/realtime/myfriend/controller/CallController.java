package com.realtime.myfriend.controller;


import com.realtime.myfriend.entity.CallHistory;
import com.realtime.myfriend.exception.UserNotFoundException;
import com.realtime.myfriend.service.CallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/calls")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Calls", description = "Call management API")
public class CallController {
    private static final Logger logger = LoggerFactory.getLogger(CallController.class);
    
    private final CallService callService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a call")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<CallHistory>> initiateCall(
            @RequestParam String callerId,
            @RequestParam String receiverId,
            @RequestParam CallHistory.CallType callType
    ) {
        return callService.initiateCall(callerId, receiverId, callType)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    if (e.getCause() instanceof UserNotFoundException) {
                        return ResponseEntity.notFound().build();
                    }
                    logger.error("Failed to initiate call: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @PostMapping("/end/{callId}")
    @Operation(summary = "End a call")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<CallHistory>> endCall(
            @PathVariable String callId,
            @RequestParam CallHistory.CallStatus status
    ) {
        return callService.endCall(callId, status)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    logger.error("Failed to end call: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get user's call history")
    @PreAuthorize("hasRole('USER') and #userId == principal.id or hasRole('ADMIN')")
    public CompletableFuture<ResponseEntity<List<CallHistory>>> getUserCallHistory(
            @PathVariable String userId
    ) {
        return callService.getUserCallHistory(userId)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    logger.error("Failed to get call history: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }

    @GetMapping("/history/between")
    @Operation(summary = "Get call history between two users")
    @PreAuthorize("hasRole('USER')")
    public CompletableFuture<ResponseEntity<List<CallHistory>>> getCallHistoryBetweenUsers(
            @RequestParam String user1Id,
            @RequestParam String user2Id
    ) {
        return callService.getCallHistoryBetweenUsers(user1Id, user2Id)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    logger.error("Failed to get call history between users: {}", e.getMessage());
                    return ResponseEntity.internalServerError().build();
                });
    }
}
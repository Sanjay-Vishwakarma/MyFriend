package com.realtime.myfriend.service;


import com.realtime.myfriend.entity.CallHistory;
import com.realtime.myfriend.exception.UserNotFoundException;
import com.realtime.myfriend.repository.CallHistoryRepository;
import com.realtime.myfriend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class CallService {
    private static final Logger logger = LoggerFactory.getLogger(CallService.class);
    
    private final CallHistoryRepository callHistoryRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;

    @Transactional
    public CompletableFuture<CallHistory> initiateCall(String callerId, String receiverId, CallHistory.CallType callType) {
        return CompletableFuture.supplyAsync(() -> {
            if (!userRepository.existsById(receiverId)) {
                throw new UserNotFoundException("Receiver not found with id: " + receiverId);
            }
            
            CallHistory call = CallHistory.builder()
                    .callerId(callerId)
                    .receiverId(receiverId)
                    .callType(callType)
                    .startTime(LocalDateTime.now())
                    .status(CallHistory.CallStatus.MISSED) // Default status
                    .build();
            
            CallHistory savedCall = callHistoryRepository.save(call);
            
            // Notify receiver if online
            if (presenceService.isUserOnline(receiverId)) {
                messagingTemplate.convertAndSendToUser(
                        receiverId,
                        "/queue/call",
                        savedCall
                );
            }
            
            return savedCall;
        });
    }

    @Transactional
    public CompletableFuture<CallHistory> endCall(String callId, CallHistory.CallStatus status) {
        return CompletableFuture.supplyAsync(() -> {
            CallHistory call = callHistoryRepository.findById(callId)
                    .orElseThrow(() -> new RuntimeException("Call not found with id: " + callId));
            
            call.setEndTime(LocalDateTime.now());
            call.setDuration(Duration.between(call.getStartTime(), call.getEndTime()));
            call.setStatus(status);
            
            return callHistoryRepository.save(call);
        });
    }

    public CompletableFuture<List<CallHistory>> getUserCallHistory(String userId) {
        return CompletableFuture.supplyAsync(() -> 
            callHistoryRepository.findByUserId(userId)
        );
    }

    public CompletableFuture<List<CallHistory>> getCallHistoryBetweenUsers(String user1Id, String user2Id) {
        return CompletableFuture.supplyAsync(() -> 
            callHistoryRepository.findCallHistoryBetweenUsers(user1Id, user2Id)
        );
    }
}
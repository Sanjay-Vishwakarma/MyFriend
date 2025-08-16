package com.realtime.myfriend.service;

import com.realtime.myfriend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private static final Logger logger = LoggerFactory.getLogger(PresenceService.class);
    
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Track active users in memory for faster access
    private final ConcurrentMap<String, LocalDateTime> activeUsers = new ConcurrentHashMap<>();

    @Transactional
    public void userConnected(String userId) {
        activeUsers.put(userId, LocalDateTime.now());
        updateUserPresence(userId, true);
        notifyPresenceChange(userId, true);
    }

    @Transactional
    public void userDisconnected(String userId) {
        activeUsers.remove(userId);
        updateUserPresence(userId, false);
        notifyPresenceChange(userId, false);
    }

    public boolean isUserOnline(String userId) {
        return activeUsers.containsKey(userId);
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    public void checkActiveUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);
        
        activeUsers.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(threshold)) {
                updateUserPresence(entry.getKey(), false);
                notifyPresenceChange(entry.getKey(), false);
                return true;
            }
            return false;
        });
    }

    private void updateUserPresence(String userId, boolean online) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setOnline(online);
            user.setLastSeen(online ? LocalDateTime.now() : user.getLastSeen());
            userRepository.save(user);
        });
    }

    private void notifyPresenceChange(String userId, boolean isOnline) {
        messagingTemplate.convertAndSend("/topic/presence", 
                new PresenceNotification(userId, isOnline));
    }

    public record PresenceNotification(String userId, boolean isOnline) {}
}
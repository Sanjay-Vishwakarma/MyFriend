package com.realtime.myfriend.service;


import com.realtime.myfriend.entity.ChatMessage;
import com.realtime.myfriend.exception.UserNotFoundException;
import com.realtime.myfriend.repository.ChatMessageRepository;
import com.realtime.myfriend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;

    @Transactional
    public CompletableFuture<ChatMessage> sendMessage(String senderId, String receiverId, String content) {
        return CompletableFuture.supplyAsync(() -> {
            if (!userRepository.existsById(receiverId)) {
                throw new UserNotFoundException("Receiver not found with id: " + receiverId);
            }
            
            ChatMessage message = ChatMessage.builder()
                    .senderId(senderId)
                    .receiverId(receiverId)
                    .content(content)
                    .timestamp(LocalDateTime.now())
                    .read(false)
                    .build();
            
            ChatMessage savedMessage = chatMessageRepository.save(message);
            
            // Send message to receiver if online
            if (presenceService.isUserOnline(receiverId)) {
                messagingTemplate.convertAndSendToUser(
                        receiverId,
                        "/queue/messages",
                        savedMessage
                );
            }
            
            return savedMessage;
        });
    }

    public CompletableFuture<List<ChatMessage>> getConversation(String user1Id, String user2Id) {
        return CompletableFuture.supplyAsync(() -> 
            chatMessageRepository.findConversation(user1Id, user2Id)
        );
    }

    @Transactional
    public CompletableFuture<Void> markMessagesAsRead(String senderId, String receiverId) {
        return CompletableFuture.supplyAsync(() -> {
            List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessages(
                    senderId,
                    receiverId,
                    LocalDateTime.now().minusMonths(1)
            );
            unreadMessages.forEach(msg -> msg.setRead(true));
            chatMessageRepository.saveAll(unreadMessages);
            return null;
        }, Executors.newVirtualThreadPerTaskExecutor());
    }
}
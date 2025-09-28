package com.realtime.myfriend.service;

import com.realtime.myfriend.entity.ChatMessage;
import com.realtime.myfriend.exception.UserNotFoundException;
import com.realtime.myfriend.repository.ChatMessageRepository;
import com.realtime.myfriend.repository.UserRepository;
import com.realtime.myfriend.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceService presenceService;
    private final MongoTemplate mongoTemplate;

    // ✅ Send message (synchronous)
    @Transactional
    public ChatMessage sendMessage(String senderId, String receiverId, String content) {
        if (!userRepository.existsById(senderId)) {
            throw new UserNotFoundException("Sender not found with id: " + senderId);
        }
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

        // ✅ Send via WebSocket if user is online
        if (presenceService.isUserOnline(receiverId)) {
            String receiverUsername = userRepository.findById(receiverId)
                    .map(user -> user.getUsername())
                    .orElseThrow(() -> new UserNotFoundException("Receiver not found"));
            messagingTemplate.convertAndSendToUser(
                    receiverUsername, "/queue/messages", savedMessage
            );
        }

        return savedMessage;
    }

    // ✅ Get conversation (synchronous)
    public List<ChatMessage> getConversation(String user1Id, String user2Id) {
        return chatMessageRepository.findConversation(user1Id, user2Id);
    }

    // ✅ Save message (synchronous)
    @Transactional
    public ChatMessage saveMessage(String senderId, String receiverId, String content) {
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

        return chatMessageRepository.save(message);
    }

    // ✅ Mark messages as read (synchronous)
    @Transactional
    public void markMessagesAsRead(String senderId, String receiverId, List<String> messageIds) {
        Query query = new Query();

        if (messageIds != null && !messageIds.isEmpty()) {
            query.addCriteria(Criteria.where("_id").in(messageIds));
        } else {
            query.addCriteria(Criteria.where("senderId").is(senderId)
                    .and("receiverId").is(receiverId)
                    .and("read").is(false));
        }

        Update update = new Update().set("read", true);
        mongoTemplate.updateMulti(query, update, ChatMessage.class);

        if (messageIds != null && !messageIds.isEmpty()) {
            logger.info("Marked {} specific messages as read from {} → {}", messageIds.size(), senderId, receiverId);
        } else {
            logger.info("Marked all unread messages as read from {} → {}", senderId, receiverId);
        }
    }

    // ✅ Update many messages as read (synchronous)
    public void updateManyAsRead(String senderId, String receiverId, List<String> messageIds) {
        Query query = new Query()
                .addCriteria(Criteria.where("_id").in(messageIds)
                        .and("senderId").is(senderId)
                        .and("receiverId").is(receiverId)
                        .and("read").is(false));

        Update update = new Update().set("read", true);

        mongoTemplate.updateMulti(query, update, ChatMessage.class);

        logger.info("Marked {} messages as read from {} → {}", messageIds.size(), senderId, receiverId);
    }
}
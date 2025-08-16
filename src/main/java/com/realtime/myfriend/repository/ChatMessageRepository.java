package com.realtime.myfriend.repository;


import com.realtime.myfriend.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    @Query("{ $or: [ { 'senderId': ?0, 'receiverId': ?1 }, { 'senderId': ?1, 'receiverId': ?0 } ] }")
    List<ChatMessage> findConversation(String user1Id, String user2Id);
    
    @Query("{ 'senderId': ?0, 'receiverId': ?1, 'timestamp': { $gte: ?2 } }")
    List<ChatMessage> findUnreadMessages(String senderId, String receiverId, LocalDateTime since);
    
    @Query(value = "{ 'senderId': ?0, 'receiverId': ?1 }", delete = true)
    void deleteConversation(String user1Id, String user2Id);
}
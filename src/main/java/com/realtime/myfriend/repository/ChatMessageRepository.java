package com.realtime.myfriend.repository;


import com.realtime.myfriend.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    @Query("{ $or: [ { 'senderId': ?0, 'receiverId': ?1 }, { 'senderId': ?1, 'receiverId': ?0 } ] }")
    List<ChatMessage> findConversation(String user1Id, String user2Id);


    @Query("{ 'senderId': ?0, 'receiverId': ?1, 'read': false }")
    List<ChatMessage> findUnreadMessages(String senderId, String receiverId);

    @Query(value = "{ 'senderId': ?0, 'receiverId': ?1 }", delete = true)
    void deleteConversation(String user1Id, String user2Id);

    @Query("{ $or: [ " +
            " { 'senderId': ?0, 'receiverId': ?1 }, " +
            " { 'senderId': ?1, 'receiverId': ?0 }, " +
            " { 'senderId': ?2, 'receiverId': ?3 }, " +
            " { 'senderId': ?3, 'receiverId': ?2 } " +
            "] }")
    List<ChatMessage> findConversationWithUsername(
            String user1Id, String user2Id,
            String user1Username, String user2Username
    );

    @Query("{ '_id': { $in: ?2 }, 'senderId': ?0, 'receiverId': ?1 }")
    void updateManyAsRead(String senderId, String receiverId, List<String> messageIds);

}
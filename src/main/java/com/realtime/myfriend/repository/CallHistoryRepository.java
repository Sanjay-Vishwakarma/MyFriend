package com.realtime.myfriend.repository;


import com.realtime.myfriend.entity.CallHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface CallHistoryRepository extends MongoRepository<CallHistory, String> {
    @Query("{ $or: [ { 'callerId': ?0 }, { 'receiverId': ?0 } ] }")
    List<CallHistory> findByUserId(String userId);
    
    @Query("{ $or: [ { 'callerId': ?0, 'receiverId': ?1 }, { 'callerId': ?1, 'receiverId': ?0 } ] }")
    List<CallHistory> findCallHistoryBetweenUsers(String user1Id, String user2Id);
    
    @Query("{ 'startTime': { $gte: ?0, $lte: ?1 } }")
    List<CallHistory> findCallsBetweenDates(LocalDateTime start, LocalDateTime end);
}
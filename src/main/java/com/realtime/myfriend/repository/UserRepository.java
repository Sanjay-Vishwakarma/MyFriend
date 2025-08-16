package com.realtime.myfriend.repository;


import com.realtime.myfriend.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    
    @Query("{ 'online': true }")
    List<User> findAllOnlineUsers();
    
    @Query("{ 'lastSeen': { $gte: ?0 } }")
    List<User> findRecentlyActiveUsers(LocalDateTime time);
}
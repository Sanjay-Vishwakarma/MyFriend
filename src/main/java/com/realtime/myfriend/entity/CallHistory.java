package com.realtime.myfriend.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.Duration;

@Document(collection = "call_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallHistory {
    @Id
    private String id;
    
    @Indexed
    private String callerId;
    
    @Indexed
    private String receiverId;
    
    private CallType callType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration duration;
    private CallStatus status;
    
    public enum CallType {
        VOICE, VIDEO
    }
    
    public enum CallStatus {
        COMPLETED, MISSED, REJECTED, FAILED
    }
}
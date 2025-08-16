package com.realtime.myfriend.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    private String id;
    
    @Indexed
    private String senderId;
    
    @Indexed
    private String receiverId;
    
    private String content;
    private LocalDateTime timestamp;
    private boolean read;
}
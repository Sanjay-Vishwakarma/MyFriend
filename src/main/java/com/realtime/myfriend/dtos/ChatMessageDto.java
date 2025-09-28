package com.realtime.myfriend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private String id;
    private String senderId;
    private String senderUsername;
    private String receiverId;
    private String receiverUsername;
    private String content;
    private LocalDateTime timestamp;
    private boolean read;
}

package com.realtime.myfriend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReadReceipt {
    private String senderId;    // The one who originally sent the messages
    private String receiverId;  // The one who read them
    private List<String> messageIds; // IDs of the messages being marked as read
}

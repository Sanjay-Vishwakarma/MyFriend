package com.realtime.myfriend.util;

import java.security.SecureRandom;
import java.util.Base64;

public class JwtSecretGenerator {
    public static void main(String[] args) {
        byte[] secretBytes = new byte[32]; // 256-bit
        SecureRandom random = new SecureRandom();
        random.nextBytes(secretBytes);
        
        String secret = Base64.getEncoder().encodeToString(secretBytes);
        System.out.println("Generated JWT Secret: " + secret);
    }
}

package com.realtime.myfriend.controller;


import com.realtime.myfriend.dtos.AuthenticationRequest;
import com.realtime.myfriend.dtos.AuthenticationResponse;
import com.realtime.myfriend.dtos.RegisterRequest;
import com.realtime.myfriend.security.JwtService;
import com.realtime.myfriend.service.AuthenticationService;
import com.realtime.myfriend.service.PresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthenticationController {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    private final AuthenticationService authenticationService;
    private final PresenceService presenceService;
    private final JwtService jwtService;


    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public CompletableFuture<ResponseEntity<AuthenticationResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authenticationService.register(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    logger.error("Registration failed: {}", e.getMessage());
                    return ResponseEntity.badRequest().build();
                });
    }

    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate user")
    public CompletableFuture<ResponseEntity<AuthenticationResponse>> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        return authenticationService.authenticate(request)
                .thenApply(ResponseEntity::ok)
                .exceptionally(e -> {
                    logger.error("Authentication failed: {}", e.getMessage());
                    return ResponseEntity.badRequest().build();
                });
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String userId = jwtService.extractUserId(token); // You’d need to add this method
        presenceService.userDisconnected(userId);
        // Optionally store token in a blacklist
        return ResponseEntity.ok().build();
    }

}
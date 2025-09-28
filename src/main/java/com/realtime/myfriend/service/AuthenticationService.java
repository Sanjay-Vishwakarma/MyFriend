package com.realtime.myfriend.service;


import com.realtime.myfriend.dtos.AuthenticationRequest;
import com.realtime.myfriend.dtos.AuthenticationResponse;
import com.realtime.myfriend.dtos.RegisterRequest;
import com.realtime.myfriend.entity.User;
import com.realtime.myfriend.entity.User.Role;
import com.realtime.myfriend.repository.UserRepository;
import com.realtime.myfriend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PresenceService presenceService;

    @Transactional
    public CompletableFuture<AuthenticationResponse> register(RegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var user = User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .dob(request.getDob())
                        .gender(request.getGender())
                        .username(request.getUsername())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .online(false)
                        .role(request.getRole() != null ? request.getRole() : Role.USER) // Default to USER
                        .build();

                User savedUser = userRepository.save(user);

                var jwtToken = jwtService.generateToken(user).join();
                return AuthenticationResponse.builder()
                        .token(jwtToken)
                        .username(savedUser.getUsername())
                        .userId(savedUser.getId())
                        .build();
            } catch (Exception e) {
                logger.error("Error during user registration: {}", e.getMessage());
                throw new RuntimeException("Registration failed", e);
            }
        });
    }

    public CompletableFuture<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );
                
                var user = userRepository.findByUsername(request.getUsername())
                        .orElseThrow();
                
                // Update user presence
                presenceService.userConnected(user.getId());
                
                var jwtToken = jwtService.generateToken(user).join();
                return AuthenticationResponse.builder()
                        .token(jwtToken)
                        .userId(user.getId())
                        .username(user.getUsername())
                        .build();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Authentication error for user {}: {}", request.getUsername(), e.getMessage());
                throw new RuntimeException("Authentication failed", e);
            }
        });
    }

    @Transactional
    public void logout(String userId) {
        presenceService.userDisconnected(userId);
    }
}
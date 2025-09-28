package com.realtime.myfriend.service;

import com.realtime.myfriend.dtos.AuthenticationRequest;
import com.realtime.myfriend.dtos.AuthenticationResponse;
import com.realtime.myfriend.dtos.RegisterRequest;
import com.realtime.myfriend.entity.User;
import com.realtime.myfriend.exception.EmailAlreadyExistsException;
import com.realtime.myfriend.exception.UsernameAlreadyExistsException;
import com.realtime.myfriend.repository.UserRepository;
import com.realtime.myfriend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public CompletableFuture<AuthenticationResponse> register(RegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (userRepository.findByUsername(request.getUsername()).isPresent()) {
                throw new UsernameAlreadyExistsException("Username already exists");
            }

            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new EmailAlreadyExistsException("Email already exists");
            }

            User user = User.builder()
                    .name(request.getName())
                    .email(request.getEmail())
                    .dob(request.getDob())
                    .gender(request.getGender())
                    .username(request.getUsername())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(User.Role.USER)
                    .build();

            userRepository.save(user);

            String jwtToken = jwtService.generateToken(user).join();

            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .build();
        });
    }

    public CompletableFuture<AuthenticationResponse> authenticate(AuthenticationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            String jwtToken = jwtService.generateToken(user).join();

            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .userId(user.getId())
                    .username(user.getUsername())
                    .build();
        });
    }
}
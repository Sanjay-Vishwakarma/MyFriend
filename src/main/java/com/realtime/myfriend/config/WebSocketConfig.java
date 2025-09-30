package com.realtime.myfriend.config;

import com.realtime.myfriend.helper.AuthChannelInterceptorAdapter;
import com.realtime.myfriend.helper.HttpHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptorAdapter authChannelInterceptor;

    // Active profile (default to dev if not set)
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");

        // Enable simple broker only in dev
        if (!"prod".equalsIgnoreCase(activeProfile)) {
            config.enableSimpleBroker("/topic", "/queue");
        }
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new HttpHandshakeInterceptor())

                .setAllowedOrigins("http://localhost:3000","http://192.168.0.105:9177","https://myfriend-frontend-eight.vercel.app") // allow all in prod/dev

                .withSockJS();

        // Skip broker in prod: log info
        if ("prod".equalsIgnoreCase(activeProfile)) {
            System.out.println("[WebSocketConfig] Simple broker skipped in PROD to speed up startup.");
        }
    }
}

package com.realtime.myfriend.config;

import com.realtime.myfriend.helper.AuthChannelInterceptorAdapter;
import com.realtime.myfriend.helper.HttpHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    private final AuthChannelInterceptorAdapter authChannelInterceptor;

    public WebSocketConfig(AuthChannelInterceptorAdapter authChannelInterceptor) {
        this.authChannelInterceptor = authChannelInterceptor;
    }
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor); // ✅ register it
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new HttpHandshakeInterceptor()) // 👈 add here
//                .setAllowedOriginPatterns("http://localhost:3000")
                .setAllowedOrigins("http://localhost:3000")
                .withSockJS();
    }
}
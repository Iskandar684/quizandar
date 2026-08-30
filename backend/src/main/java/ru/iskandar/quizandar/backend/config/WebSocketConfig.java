package ru.iskandar.quizandar.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Префикс для исходящих сообщений (сервер -> клиент)
        registry.enableSimpleBroker("/topic", "/queue");
        // Префикс для входящих сообщений (клиент -> сервер)
        registry.setApplicationDestinationPrefixes("/app");
        // Персональные очереди пользователей
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // для dev; в prod лучше ограничить
                .withSockJS();
    }
}

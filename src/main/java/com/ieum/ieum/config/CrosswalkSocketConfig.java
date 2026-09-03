package com.ieum.ieum.config;

import com.ieum.ieum.websocket.CrosswalkSimulationHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CrosswalkSocketConfig implements WebSocketConfigurer {

    private final CrosswalkSimulationHandler handler;

    public CrosswalkSocketConfig(CrosswalkSimulationHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/simulate").setAllowedOrigins("*");
    }
}
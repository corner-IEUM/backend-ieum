package com.ieum.ieum.client;

import com.ieum.ieum.dto.GuidanceMessage;
import com.ieum.ieum.service.GuidanceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletionStage;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiWebSocketClient {

    private final GuidanceService guidanceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.server.url:ws://localhost:8000/ws/detect}")
    private String aiServerUrl;

    @PostConstruct
    public void connect() {
        try {
            HttpClient.newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(aiServerUrl), new Listener())
                    .thenRun(() -> log.info("[AI 서버] 연결 성공: {}", aiServerUrl))
                    .exceptionally(e -> {
                        log.warn("[AI 서버] 연결 실패: {}", e.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.warn("[AI 서버] 연결 시도 중 오류: {}", e.getMessage());
        }
    }

    private class Listener implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("[AI 서버] 소켓 열림");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);

            if (last) {
                handleMessage(buffer.toString());
                buffer.setLength(0);
            }

            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("[AI 서버] 오류 발생: {}", error.getMessage());
        }
    }

    private void handleMessage(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);

            String kind = node.path("kind").asString();
            int priority = node.path("priority").asInt(3);   // 없으면 일반(3)
            String message = node.path("message").asString();

            @SuppressWarnings("unchecked")
            Map<String, Object> meta = node.has("meta")
                    ? objectMapper.convertValue(node.path("meta"), Map.class)
                    : Map.of();

            guidanceService.send(new GuidanceMessage(kind, priority, message, meta));

        } catch (Exception e) {
            log.error("[파싱 오류] {}", e.getMessage());
        }
    }
}
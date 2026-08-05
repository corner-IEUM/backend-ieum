package com.ieum.ieum.service;

import com.ieum.ieum.dto.ObstacleMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 장애물 감지 결과를 구독 중인 모든 앱에 전송
     */
    public void broadcastObstacle(ObstacleMessage message) {
        log.info("[중계] {}", message.getMessage());
        messagingTemplate.convertAndSend("/topic/obstacle", message);
    }
}
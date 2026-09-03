package com.ieum.ieum.service;

import com.ieum.ieum.dto.GuidanceMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class GuidanceService {

    private static final long REPEAT_MS      = 2500;  // 같은 문구 반복 억제
    private static final long URGENT_QUIET_MS = 1200; // 급한 안내 직후, 덜 급한 안내 잠깐 보류

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Long> lastSent = new ConcurrentHashMap<>();
    private volatile long lastUrgentAt = 0;

    public GuidanceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** 모든 안내(장애물·횡단보도)가 이 메서드를 거쳐 하나의 채널로 나간다 */
    public synchronized void send(GuidanceMessage msg) {
        long now = System.currentTimeMillis();

        // 1) 같은 문구를 짧은 간격 내 반복하면 스킵
        Long prev = lastSent.get(msg.message());
        if (prev != null && now - prev < REPEAT_MS) return;

        // 2) 급한 안내(≤2) 직후엔 덜 급한 안내(3)를 잠깐 보류 (겹침·잡음 방지)
        if (msg.priority() >= 3 && now - lastUrgentAt < URGENT_QUIET_MS) return;

        lastSent.put(msg.message(), now);
        if (msg.priority() <= 2) lastUrgentAt = now;

        log.info("[안내] p{} [{}] {}", msg.priority(), msg.kind(), msg.message());
        messagingTemplate.convertAndSend("/topic/guidance", msg);
    }
}
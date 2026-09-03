package com.ieum.ieum.controller;

import com.ieum.ieum.dto.WalkCompleteRequest;
import com.ieum.ieum.dto.WalkLogResponse;
import com.ieum.ieum.service.WalkLogService;
import com.ieum.ieum.security.JwtTokenProvider; // 프로젝트에서 쓰는 실제 패키지 경로로 맞춰줘
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// @RestController: 이 클래스의 메서드가 반환하는 값이 그대로 JSON 응답이 됨
// @RequestMapping: 이 컨트롤러의 모든 URL 앞에 공통으로 붙는 경로
@RestController
@RequestMapping("/api/walk")
@RequiredArgsConstructor
public class WalkLogController {

    private final WalkLogService walkLogService;
    private final JwtTokenProvider tokenProvider; // 토큰에서 userId 뽑아내는 데 사용

    // 보행 완료 시 프론트가 호출하는 API
    // POST /api/walk/complete
    @PostMapping("/complete")
    public ResponseEntity<WalkLogResponse> completeWalk(
            @Valid @RequestBody WalkCompleteRequest request,
            HttpServletRequest httpReq
    ) {
        Long userId = extractUserId(httpReq); // 토큰에서 누가 보낸 요청인지 확인
        WalkLogResponse response = walkLogService.saveWalkLog(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // "내 경로" 탭에서 프론트가 호출하는 API
    // GET /api/walk/logs
    @GetMapping("/logs")
    public ResponseEntity<List<WalkLogResponse>> getWalkLogs(HttpServletRequest httpReq) {
        Long userId = extractUserId(httpReq);
        List<WalkLogResponse> logs = walkLogService.getWalkLogsByUserId(userId);
        return ResponseEntity.ok(logs);
    }

    // 요청 헤더의 토큰에서 userId를 꺼내는 공통 로직
    // 프론트가 userId를 직접 보내게 하면 아무나 남의 userId로 요청 가능 -> 보안 문제
    // 그래서 로그인 시 발급된 토큰 안에 심어둔 userId를 서버가 직접 꺼내서 사용
    private Long extractUserId(HttpServletRequest httpReq) {
        String header = httpReq.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new IllegalArgumentException("인증 토큰이 없습니다.");
        }
        String token = header.substring(7); // "Bearer " 7글자 잘라내고 순수 토큰만 추출
        if (!tokenProvider.validate(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
        return tokenProvider.getUserId(token);
    }
}
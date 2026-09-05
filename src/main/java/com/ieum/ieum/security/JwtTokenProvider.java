package com.ieum.ieum.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HexFormat;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey key;
    private final long validityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long validityMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMs = validityMs;
        // 로그인 세션이 예기치 않게 끊기는 문제를 재현/추적하기 위한 진단용 로그.
        // secret 원문은 절대 남기지 않고, 짧은 해시(fingerprint)만 남겨서 서버가 재시작되거나
        // 인스턴스가 바뀌었을 때 "같은 secret을 쓰고 있는지"를 로그만으로 비교할 수 있게 한다
        // (fingerprint가 재시작 전후로 달라지면 그게 곧 원인 — secret이 안정적이지 않다는 뜻).
        log.info("[JWT] secret fingerprint={}, access-token-validity={}ms(~{}일)",
                fingerprint(secret), validityMs, validityMs / 86_400_000.0);
    }

    private static String fingerprint(String secret) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 4); // 앞 4바이트만 — 식별용, 역산 불가
        } catch (Exception e) {
            return "unknown";
        }
    }

    public String createToken(Long userId, String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("[JWT] 만료된 토큰 — exp={}, now={}", e.getClaims().getExpiration(), new Date());
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치(다른 secret으로 발급된 토큰)·형식 오류 등 — 어떤 경우인지 예외 종류로 구분해서 남긴다.
            // 로그인이 반복적으로 풀리는 문제를 다음에 또 겪게 되면, 여기 로그의 예외 종류가
            // "만료"가 아니라 "서명 불일치"로 찍히는지부터 확인할 것 — 그렇다면 이 서버가 아닌
            // 다른 secret으로 발급된 토큰(예: 다른 백엔드 인스턴스)이라는 뜻이다.
            log.warn("[JWT] 유효하지 않은 토큰 — {}: {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
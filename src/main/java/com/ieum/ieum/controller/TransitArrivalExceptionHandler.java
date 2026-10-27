package com.ieum.ieum.controller;

import com.ieum.ieum.service.ArrivalUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = TransitArrivalController.class)
public class TransitArrivalExceptionHandler {

    @ExceptionHandler(ArrivalUnavailableException.class)
    public ResponseEntity<Map<String, String>> handle(ArrivalUnavailableException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("code", "ARRIVAL_UNAVAILABLE", "message", "버스 도착 정보를 불러올 수 없어요"));
    }
}
package com.ieum.ieum.service;

public class ArrivalUnavailableException extends RuntimeException {
    public ArrivalUnavailableException(String message) {
        super(message);
    }
    public ArrivalUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

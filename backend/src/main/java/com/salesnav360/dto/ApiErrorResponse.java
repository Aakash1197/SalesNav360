package com.salesnav360.dto;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String message,
        boolean status,
        Instant timestamp,
        String errorCode,
        Map<String, String> errors) {

    public ApiErrorResponse(String message, int errorCode, Map<String, String> errors) {
        this(message, false, Instant.now(), Integer.toString(errorCode), errors);
    }

    public ApiErrorResponse(String message, int errorCode) {
        this(message, errorCode, null);
    }
}


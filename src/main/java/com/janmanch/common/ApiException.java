package com.janmanch.common;

import org.springframework.http.HttpStatus;

/** Thrown by services when a request cannot be fulfilled; mapped to an HTTP status. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

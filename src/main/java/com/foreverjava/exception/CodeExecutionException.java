package com.foreverjava.exception;

import org.springframework.http.HttpStatus;

public class CodeExecutionException extends RuntimeException {

    private final HttpStatus status;

    public CodeExecutionException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public CodeExecutionException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}

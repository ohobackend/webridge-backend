package com.webridge.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, Object> body;

    public ApiException(HttpStatus status, String error) {
        super(error);
        this.status = status;
        this.body = Map.of("error", error);
    }

    public ApiException(HttpStatus status, Map<String, Object> body) {
        super(body.toString());
        this.status = status;
        this.body = body;
    }
}

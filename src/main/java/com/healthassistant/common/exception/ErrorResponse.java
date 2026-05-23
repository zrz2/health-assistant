package com.healthassistant.common.exception;

import java.time.LocalDateTime;

public class ErrorResponse {
    private int code;
    private String message;
    private String path;
    private LocalDateTime timestamp;

    public ErrorResponse(int code, String message, String path) {
        this.code = code;
        this.message = message;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public LocalDateTime getTimestamp() { return timestamp; }
}

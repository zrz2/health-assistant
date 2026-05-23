package com.healthassistant.module.admin.dto;

import com.healthassistant.module.admin.entity.OperationLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String operation;
    private String method;
    private String params;
    private String ip;
    private Long executionTime;
    private String result;
    private LocalDateTime createdAt;

    public static OperationLogResponse from(OperationLog log) {
        return OperationLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .operation(log.getOperation())
                .method(log.getMethod())
                .params(log.getParams())
                .ip(log.getIp())
                .executionTime(log.getExecutionTime())
                .result(log.getResult())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

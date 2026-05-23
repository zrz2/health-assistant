package com.healthassistant.module.knowledge.dto;

import com.healthassistant.module.knowledge.entity.SyncTask;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SyncTaskResponse {

    private Long id;
    private String taskId;
    private String sourceName;
    private String syncType;
    private Integer status;
    private Integer totalItems;
    private Integer successItems;
    private Integer failedItems;
    private String errorLog;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;

    public static SyncTaskResponse from(SyncTask task) {
        return SyncTaskResponse.builder()
                .id(task.getId())
                .taskId(task.getTaskId())
                .sourceName(task.getSourceName())
                .syncType(task.getSyncType())
                .status(task.getStatus())
                .totalItems(task.getTotalItems())
                .successItems(task.getSuccessItems())
                .failedItems(task.getFailedItems())
                .errorLog(task.getErrorLog())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .build();
    }
}

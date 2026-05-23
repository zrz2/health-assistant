package com.healthassistant.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalUsers;
    private long totalSessions;
    private long totalMessages;
    private long totalKnowledgeItems;
    private long todayActiveSessions;
    private long indexedItems;
    private double positiveRate;
}

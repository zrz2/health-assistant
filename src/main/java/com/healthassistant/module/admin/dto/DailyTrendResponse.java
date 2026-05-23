package com.healthassistant.module.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendResponse {
    private List<TrendPoint> newUsers;
    private List<TrendPoint> messages;
    private List<TrendPoint> positiveRates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private String date;
        private long value;
    }
}

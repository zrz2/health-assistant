package com.healthassistant.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private String avatarUrl;
    private Integer userType;
    private LocalDateTime lastLoginTime;

    private HealthRecordDTO healthRecord;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthRecordDTO {
        private Integer age;
        private Integer gender;
        private BigDecimal height;
        private BigDecimal weight;
        private String bloodType;
        private String medicalHistory;
        private String allergies;
        private String chronicDiseases;
        private String currentMedications;
        private String lifestyle;
    }
}

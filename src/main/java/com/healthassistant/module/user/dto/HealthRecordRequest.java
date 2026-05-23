package com.healthassistant.module.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HealthRecordRequest {

    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能超过150")
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

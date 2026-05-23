package com.healthassistant.module.user.service;

import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.module.user.dto.HealthRecordRequest;
import com.healthassistant.module.user.dto.UserProfileDTO;
import com.healthassistant.module.user.entity.HealthRecord;
import com.healthassistant.module.user.entity.User;
import com.healthassistant.module.user.repository.HealthRecordRepository;
import com.healthassistant.module.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final HealthRecordRepository healthRecordRepository;

    public UserService(UserRepository userRepository, HealthRecordRepository healthRecordRepository) {
        this.userRepository = userRepository;
        this.healthRecordRepository = healthRecordRepository;
    }

    public UserProfileDTO getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        HealthRecord record = healthRecordRepository.findByUserId(userId).orElse(null);

        return UserProfileDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .userType(user.getUserType())
                .lastLoginTime(user.getLastLoginTime())
                .healthRecord(toHealthRecordDTO(record))
                .build();
    }

    @Transactional
    public UserProfileDTO updateProfile(Long userId, UserProfileDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAvatarUrl() != null) {
            user.setAvatarUrl(dto.getAvatarUrl());
        }
        userRepository.save(user);

        return getProfile(userId);
    }

    public UserProfileDTO.HealthRecordDTO getHealthRecord(Long userId) {
        HealthRecord record = healthRecordRepository.findByUserId(userId).orElse(null);
        return toHealthRecordDTO(record);
    }

    @Transactional
    public UserProfileDTO.HealthRecordDTO updateHealthRecord(Long userId, HealthRecordRequest request) {
        HealthRecord record = healthRecordRepository.findByUserId(userId)
                .orElseGet(() -> {
                    HealthRecord r = new HealthRecord();
                    r.setUserId(userId);
                    return r;
                });

        if (request.getAge() != null) record.setAge(request.getAge());
        if (request.getGender() != null) record.setGender(request.getGender());
        if (request.getHeight() != null) record.setHeight(request.getHeight());
        if (request.getWeight() != null) record.setWeight(request.getWeight());
        if (request.getBloodType() != null) record.setBloodType(request.getBloodType());
        if (request.getMedicalHistory() != null) record.setMedicalHistory(request.getMedicalHistory());
        if (request.getAllergies() != null) record.setAllergies(request.getAllergies());
        if (request.getChronicDiseases() != null) record.setChronicDiseases(request.getChronicDiseases());
        if (request.getCurrentMedications() != null) record.setCurrentMedications(request.getCurrentMedications());
        if (request.getLifestyle() != null) record.setLifestyle(request.getLifestyle());

        record.setUpdatedAt(LocalDateTime.now());
        healthRecordRepository.save(record);

        return toHealthRecordDTO(record);
    }

    private UserProfileDTO.HealthRecordDTO toHealthRecordDTO(HealthRecord record) {
        if (record == null) return null;
        return UserProfileDTO.HealthRecordDTO.builder()
                .age(record.getAge())
                .gender(record.getGender())
                .height(record.getHeight())
                .weight(record.getWeight())
                .bloodType(record.getBloodType())
                .medicalHistory(record.getMedicalHistory())
                .allergies(record.getAllergies())
                .chronicDiseases(record.getChronicDiseases())
                .currentMedications(record.getCurrentMedications())
                .lifestyle(record.getLifestyle())
                .build();
    }
}

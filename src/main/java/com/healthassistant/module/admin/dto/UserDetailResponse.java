package com.healthassistant.module.admin.dto;

import com.healthassistant.module.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    private Long id;
    private String username;
    private String email;
    private String phone;
    private String nickname;
    private Integer status;
    private Integer userType;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;
    private long sessionCount;
    private long messageCount;

    public static UserDetailResponse from(User user, long sessionCount, long messageCount) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .userType(user.getUserType())
                .lastLoginTime(user.getLastLoginTime())
                .createdAt(user.getCreatedAt())
                .sessionCount(sessionCount)
                .messageCount(messageCount)
                .build();
    }
}

package com.healthassistant.module.user.controller;

import com.healthassistant.common.result.Result;
import com.healthassistant.module.user.dto.HealthRecordRequest;
import com.healthassistant.module.user.dto.UserProfileDTO;
import com.healthassistant.module.user.entity.User;
import com.healthassistant.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public Result<UserProfileDTO> getProfile(@AuthenticationPrincipal User user) {
        return Result.success(userService.getProfile(user.getId()));
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result<UserProfileDTO> updateProfile(@AuthenticationPrincipal User user,
                                                 @Valid @RequestBody UserProfileDTO dto) {
        return Result.success("更新成功", userService.updateProfile(user.getId(), dto));
    }

    @Operation(summary = "获取健康档案")
    @GetMapping("/health-record")
    public Result<UserProfileDTO.HealthRecordDTO> getHealthRecord(@AuthenticationPrincipal User user) {
        return Result.success(userService.getHealthRecord(user.getId()));
    }

    @Operation(summary = "更新健康档案")
    @PutMapping("/health-record")
    public Result<UserProfileDTO.HealthRecordDTO> updateHealthRecord(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody HealthRecordRequest request) {
        return Result.success("保存成功", userService.updateHealthRecord(user.getId(), request));
    }
}

package com.healthassistant.module.user.service;

import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.module.user.dto.LoginRequest;
import com.healthassistant.module.user.dto.LoginResponse;
import com.healthassistant.module.user.dto.RegisterRequest;
import com.healthassistant.module.user.entity.User;
import com.healthassistant.module.user.entity.UserPreference;
import com.healthassistant.module.user.repository.UserPreferenceRepository;
import com.healthassistant.module.user.repository.UserRepository;
import com.healthassistant.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final long expiration;

    public AuthService(UserRepository userRepository,
                       UserPreferenceRepository userPreferenceRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       @Value("${app.security.jwt.expiration}") long expiration) {
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.expiration = expiration;
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS, "用户名已存在");
        }
        if (request.getEmail() != null && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        userRepository.save(user);

        UserPreference preference = new UserPreference();
        preference.setUserId(user.getId());
        userPreferenceRepository.save(preference);

        log.info("User registered: {}", user.getUsername());
        return buildLoginResponse(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String ip) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR, "密码错误");
        }

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }

        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(ip);
        userRepository.save(user);

        log.info("User logged in: {}", user.getUsername());
        return buildLoginResponse(user);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "刷新令牌无效或已过期");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getUsername(), user.getId(), user.getUserType());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiration)
                .userInfo(buildUserInfo(user))
                .build();
    }

    public void logout(String accessToken) {
        jwtTokenProvider.blacklistToken(accessToken);
        log.debug("Token blacklisted");
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getUsername(), user.getId(), user.getUserType());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiration)
                .userInfo(buildUserInfo(user))
                .build();
    }

    private LoginResponse.UserInfo buildUserInfo(User user) {
        return LoginResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .userType(user.getUserType())
                .build();
    }
}

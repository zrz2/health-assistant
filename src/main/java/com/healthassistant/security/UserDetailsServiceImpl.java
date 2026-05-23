package com.healthassistant.security;

import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.module.user.entity.User;
import com.healthassistant.module.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在: " + username));

        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.USER_DISABLED, "用户已被禁用");
        }

        return user;
    }
}

package com.healthassistant.module.admin.service;

import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.module.admin.dto.UserDetailResponse;
import com.healthassistant.module.chat.repository.ChatMessageRepository;
import com.healthassistant.module.chat.repository.ChatSessionRepository;
import com.healthassistant.module.user.entity.User;
import com.healthassistant.module.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public AdminUserService(UserRepository userRepository,
                             ChatSessionRepository sessionRepository,
                             ChatMessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    public Page<User> listUsers(String keyword, Integer userType, Integer status, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(root.get("username"), pattern),
                        cb.like(root.get("email"), pattern),
                        cb.like(root.get("nickname"), pattern)
                ));
            }
            if (userType != null) {
                predicates.add(cb.equal(root.get("userType"), userType));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return userRepository.findAll(spec, pageable);
    }

    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        long sessionCount = sessionRepository.countByUserId(userId);
        long messageCount = messageRepository.countBySessionId(null); // placeholder
        return UserDetailResponse.from(user, sessionCount, 0);
    }

    @Transactional
    public void updateStatus(Long userId, Integer status, Long currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_DISABLE_SELF, "不能禁用自己");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional
    public void updateRole(Long userId, Integer userType, Long currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_CHANGE_OWN_ROLE, "不能修改自己的角色");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        user.setUserType(userType);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId, Long currentUserId) {
        if (userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_DELETE_SELF, "不能删除自己");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        user.setDeleted(1);
        userRepository.save(user);
    }
}

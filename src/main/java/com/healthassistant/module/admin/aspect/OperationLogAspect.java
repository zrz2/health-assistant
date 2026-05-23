package com.healthassistant.module.admin.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthassistant.module.admin.repository.OperationLogRepository;
import com.healthassistant.module.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final OperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper;

    public OperationLogAspect(OperationLogRepository operationLogRepository,
                               ObjectMapper objectMapper) {
        this.operationLogRepository = operationLogRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(opLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, OperationLog opLog) throws Throwable {
        com.healthassistant.module.admin.entity.OperationLog entity =
                new com.healthassistant.module.admin.entity.OperationLog();
        entity.setOperation(opLog.value());
        entity.setMethod(joinPoint.getSignature().toShortString());

        // Current user
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            entity.setUserId(user.getId());
            entity.setUsername(user.getUsername());
        }

        // Client IP
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isBlank()) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (ip == null || ip.isBlank()) {
                    ip = request.getRemoteAddr();
                }
                entity.setIp(ip);
            }
        } catch (Exception e) {
            // Ignore
        }

        // Params
        try {
            entity.setParams(objectMapper.writeValueAsString(joinPoint.getArgs()));
        } catch (JsonProcessingException e) {
            entity.setParams("[serialization error]");
        }

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            entity.setExecutionTime(System.currentTimeMillis() - start);
            entity.setResult("SUCCESS");
            return result;
        } catch (Throwable t) {
            entity.setExecutionTime(System.currentTimeMillis() - start);
            entity.setResult("FAILED: " + t.getMessage());
            throw t;
        } finally {
            try {
                operationLogRepository.save(entity);
            } catch (Exception e) {
                log.error("Failed to save operation log", e);
            }
        }
    }
}

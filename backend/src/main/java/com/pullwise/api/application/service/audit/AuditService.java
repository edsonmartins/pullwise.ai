package com.pullwise.api.application.service.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.domain.model.AuditLog;
import com.pullwise.api.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Serviço de auditoria para registrar ações no sistema.
 * Grava assincronamente na tabela audit_logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Registra uma ação de auditoria de forma assíncrona.
     */
    @Async
    public void logAction(String action, Long userId, Long organizationId,
                          String entityType, Long entityId,
                          Object oldValues, Object newValues,
                          String ipAddress, String userAgent) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .userId(userId)
                    .organizationId(organizationId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValues(oldValues != null ? objectMapper.writeValueAsString(oldValues) : null)
                    .newValues(newValues != null ? objectMapper.writeValueAsString(newValues) : null)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log: {} by user {} on {}:{}", action, userId, entityType, entityId);

        } catch (Exception e) {
            log.error("Failed to write audit log for action '{}': {}", action, e.getMessage());
        }
    }

    /**
     * Registra uma ação simples (sem detalhes de request).
     */
    public void logAction(String action, Long userId, Long organizationId,
                          String entityType, Long entityId) {
        logAction(action, userId, organizationId, entityType, entityId, null, null, null, null);
    }
}

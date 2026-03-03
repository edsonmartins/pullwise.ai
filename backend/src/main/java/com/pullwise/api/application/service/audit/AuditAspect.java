package com.pullwise.api.application.service.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.Principal;

/**
 * Aspecto AOP que intercepta métodos anotados com @Auditable
 * e registra a ação no sistema de auditoria.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String action = auditable.action();
        String entityType = auditable.entityType();

        // Extrair informações do request HTTP
        Long userId = null;
        String ipAddress = null;
        String userAgent = null;

        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ipAddress = request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");

                Principal principal = request.getUserPrincipal();
                if (principal != null) {
                    try {
                        userId = Long.parseLong(principal.getName());
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract request info for audit: {}", e.getMessage());
        }

        // Extrair entityId dos argumentos (primeiro Long encontrado)
        Long entityId = extractEntityId(joinPoint.getArgs());

        // Executar o método original
        Object result = joinPoint.proceed();

        // Registrar auditoria (assíncrono)
        auditService.logAction(action, userId, null, entityType, entityId,
                null, null, ipAddress, userAgent);

        return result;
    }

    private Long extractEntityId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }
}

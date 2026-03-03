package com.pullwise.api.application.service.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação para marcar métodos de controller que devem ser auditados.
 * O aspecto AuditAspect intercepta métodos anotados e registra a ação.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /**
     * Nome da ação (ex: "CREATE_REVIEW", "UPDATE_SETTINGS", "CANCEL_SUBSCRIPTION").
     */
    String action();

    /**
     * Tipo da entidade (ex: "Review", "Project", "Subscription").
     */
    String entityType() default "";
}

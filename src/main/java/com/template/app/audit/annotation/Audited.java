package com.template.app.audit.annotation;

import com.template.app.audit.domain.entity.AuditLog;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited.
 * Use this on service methods to automatically log audit events.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * The action type being performed
     */
    AuditLog.AuditAction action() default AuditLog.AuditAction.CUSTOM;

    /**
     * The entity type being operated on
     */
    String entityType();

    /**
     * Description of the action
     */
    String description() default "";

    /**
     * SpEL expression to extract entity ID from method parameters
     * e.g., "#id" or "#request.id"
     */
    String entityIdExpression() default "";

    /**
     * Whether to log asynchronously
     */
    boolean async() default false;
}

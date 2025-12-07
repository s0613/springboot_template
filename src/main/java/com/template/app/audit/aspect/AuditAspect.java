package com.template.app.audit.aspect;

import com.template.app.audit.annotation.Audited;
import com.template.app.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        Long actorId = getCurrentUserId();
        Long entityId = extractEntityId(joinPoint, audited);
        String description = audited.description().isEmpty()
                ? audited.action().name() + " " + audited.entityType()
                : audited.description();

        Object result;
        try {
            result = joinPoint.proceed();

            // Log successful action
            if (audited.async()) {
                auditService.logAsync(audited.action(), audited.entityType(), entityId, actorId, description);
            } else {
                auditService.log(audited.action(), audited.entityType(), entityId, actorId, description);
            }

        } catch (Exception e) {
            // Log failed action
            String failDescription = description + " (FAILED: " + e.getMessage() + ")";
            if (audited.async()) {
                auditService.logAsync(audited.action(), audited.entityType(), entityId, actorId, failDescription);
            } else {
                auditService.log(audited.action(), audited.entityType(), entityId, actorId, failDescription);
            }
            throw e;
        }

        return result;
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof String userIdStr) {
                return Long.parseLong(userIdStr);
            }
        } catch (Exception e) {
            log.debug("Could not get current user ID", e);
        }
        return null;
    }

    private Long extractEntityId(ProceedingJoinPoint joinPoint, Audited audited) {
        String expression = audited.entityIdExpression();
        if (expression.isEmpty()) {
            return null;
        }

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = joinPoint.getArgs();

            StandardEvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < parameters.length; i++) {
                context.setVariable(parameters[i].getName(), args[i]);
            }

            Object result = expressionParser.parseExpression(expression).getValue(context);
            if (result instanceof Long) {
                return (Long) result;
            } else if (result instanceof Number) {
                return ((Number) result).longValue();
            } else if (result instanceof String) {
                return Long.parseLong((String) result);
            }
        } catch (Exception e) {
            log.debug("Could not extract entity ID from expression: {}", expression, e);
        }

        return null;
    }
}

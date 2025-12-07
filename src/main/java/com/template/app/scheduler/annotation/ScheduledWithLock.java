package com.template.app.scheduler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for scheduled methods that should use distributed locking.
 * This ensures only one instance executes the job in a clustered environment.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledWithLock {

    /**
     * The lock key. If not specified, the method name will be used.
     */
    String lockKey() default "";

    /**
     * Lock duration in seconds. Should be longer than the expected job execution time.
     */
    long lockDurationSeconds() default 300; // 5 minutes default

    /**
     * The job group for categorization in history.
     */
    String jobGroup() default "default";

    /**
     * Whether to record job execution history.
     */
    boolean recordHistory() default true;

    /**
     * Whether to skip if the job is already running (vs waiting for lock).
     */
    boolean skipIfLocked() default true;
}

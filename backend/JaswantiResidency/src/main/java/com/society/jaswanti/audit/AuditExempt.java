package com.society.jaswanti.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicitly opts a state-changing endpoint out of the system log. The reason is mandatory so that every
 * exemption is a deliberate, reviewable decision rather than an oversight.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditExempt {
    String reason();
}

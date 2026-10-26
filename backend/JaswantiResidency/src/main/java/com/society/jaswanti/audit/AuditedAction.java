package com.society.jaswanti.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller endpoint as an auditable action. {@link AuditInterceptor} records every call to it in the
 * system log, including failed and denied attempts. Every POST/PUT/PATCH/DELETE endpoint must carry either this
 * or {@link AuditExempt}; AuditCoverageTests fails the build otherwise.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditedAction {

    /** Stable machine name, e.g. APPROVE_PAYMENT_SUBMISSION. */
    String action();

    /** Area of the system, e.g. PAYMENTS, BILLING, ANNOUNCEMENTS. */
    String module();

    /** Human-readable default description, used when the endpoint does not supply a more specific one. */
    String summary();

    /** Kind of record acted on (see {@link AuditTargetResolver}); empty when there is no single target. */
    String target() default "";

    /** Path variable holding the target id; defaults to the first path variable. */
    String targetParam() default "";
}

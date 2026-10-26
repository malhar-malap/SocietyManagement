package com.society.jaswanti.audit;

import com.society.jaswanti.entity.ActivityLog;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.ActivityLogRepository;
import com.society.jaswanti.service.ActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;
import java.util.Map;

/**
 * Writes one system-log entry for every call to an {@link AuditedAction} endpoint, whatever the outcome.
 *
 * If the endpoint's service already wrote its own entry (ledger services do, inside their transaction) and that
 * entry was committed, nothing more is written. If the action failed or was denied, the service's entry (if any)
 * was rolled back with it, so a FAILED / DENIED entry is written here instead.
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuditInterceptor.class);

    /** Query/form parameters safe to show in the log. Anything else (passwords, tokens, OTPs) is never recorded. */
    private static final List<String> LOGGED_PARAMS = List.of(
            "status", "role", "billingMonth", "amount", "dueDate", "chargeType", "endDate",
            "paymentMode", "category", "submissionType", "certificateNumber", "issuedDate");

    private static final String ACTOR_AT_START = AuditInterceptor.class.getName() + ".actorAtStart";

    private final ActivityLogService activityLogService;
    private final ActivityLogRepository activityLogRepository;
    private final AuditTargetResolver targetResolver;

    public AuditInterceptor(ActivityLogService activityLogService,
                            ActivityLogRepository activityLogRepository,
                            AuditTargetResolver targetResolver) {
        this.activityLogService = activityLogService;
        this.activityLogRepository = activityLogRepository;
        this.targetResolver = targetResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        AuditedAction audited = auditedAction(handler);
        if (audited == null) {
            return true;
        }
        request.setAttribute(ACTOR_AT_START, currentUser());

        // Resolve the target before the action runs, so a deleted record still gets a readable label
        if (!audited.target().isEmpty()) {
            String id = targetId(request, audited);
            if (id != null) {
                AuditTrail.set(AuditTrail.TARGET_TYPE, audited.target());
                AuditTrail.set(AuditTrail.TARGET_ID, id);
                AuditTrail.set(AuditTrail.TARGET_LABEL, targetResolver.resolve(audited.target(), id));
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuditedAction audited = auditedAction(handler);
        if (audited == null) {
            return;
        }
        try {
            Object recordedId = AuditTrail.get(AuditTrail.RECORDED_LOG_ID);
            if (recordedId instanceof Long id && activityLogRepository.existsById(id)) {
                return;
            }

            int status = response.getStatus();
            String outcome = outcome(status, ex);

            ActivityLog log = new ActivityLog();
            log.setAction(firstNonBlank(AuditTrail.getString(AuditTrail.ACTION), audited.action()));
            log.setModule(audited.module());
            log.setOutcome(outcome);

            User actor = actor(request);
            if (actor != null) {
                log.setUser(actor);
                log.setActorName(actor.getName());
                log.setActorEmail(actor.getEmail());
                log.setActorRole(actor.getRole() != null ? actor.getRole().name() : null);
            } else {
                log.setActorEmail(AuditTrail.getString(AuditTrail.ACTOR_LABEL));
            }

            // Targets named by the handler itself (creates) are resolved now that the record exists
            if (AuditTrail.get(AuditTrail.TARGET_ID) != null && AuditTrail.get(AuditTrail.TARGET_LABEL) == null) {
                AuditTrail.set(AuditTrail.TARGET_LABEL, targetResolver.resolve(
                        AuditTrail.getString(AuditTrail.TARGET_TYPE), AuditTrail.getString(AuditTrail.TARGET_ID)));
            }

            log.setDescription(AuditTrail.truncate(description(request, audited, outcome, status), 2000));
            activityLogService.record(log);
        } catch (RuntimeException e) {
            // The response has already been sent; never let audit trouble surface to the user
            logger.error("Failed to write system log entry for {} {}", request.getMethod(), request.getRequestURI(), e);
        }
    }

    private String description(HttpServletRequest request, AuditedAction audited, String outcome, int status) {
        String detail = AuditTrail.getString(AuditTrail.DESCRIPTION);
        StringBuilder sb = new StringBuilder(detail != null ? detail : audited.summary());
        if (detail == null) {
            String params = loggedParams(request);
            if (!params.isEmpty()) {
                sb.append(" (").append(params).append(")");
            }
        }
        if (ActivityLog.OUTCOME_DENIED.equals(outcome)) {
            sb.append(" — denied (HTTP ").append(status).append(")");
        } else if (ActivityLog.OUTCOME_FAILED.equals(outcome)) {
            sb.append(" — failed (HTTP ").append(status).append(")");
        }
        return sb.toString();
    }

    private static String loggedParams(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        for (String name : LOGGED_PARAMS) {
            String value = request.getParameter(name);
            if (value != null && !value.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(", ");
                }
                sb.append(name).append("=").append(AuditTrail.truncate(value, 100));
            }
        }
        return sb.toString();
    }

    static String outcome(int status, Exception ex) {
        if (status == 401 || status == 403) {
            return ActivityLog.OUTCOME_DENIED;
        }
        if (status >= 400 || ex != null) {
            return ActivityLog.OUTCOME_FAILED;
        }
        return ActivityLog.OUTCOME_SUCCESS;
    }

    private static User actor(HttpServletRequest request) {
        Object explicit = AuditTrail.get(AuditTrail.ACTOR);
        if (explicit instanceof User user) {
            return user;
        }
        Object atStart = request.getAttribute(ACTOR_AT_START);
        return atStart instanceof User user ? user : currentUser();
    }

    private static User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof User user ? user : null;
    }

    @SuppressWarnings("unchecked")
    private static String targetId(HttpServletRequest request, AuditedAction audited) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(vars instanceof Map<?, ?> map) || map.isEmpty()) {
            return null;
        }
        Map<String, String> pathVars = (Map<String, String>) map;
        return audited.targetParam().isEmpty() ? pathVars.values().iterator().next() : pathVars.get(audited.targetParam());
    }

    private static AuditedAction auditedAction(Object handler) {
        return handler instanceof HandlerMethod method ? method.getMethodAnnotation(AuditedAction.class) : null;
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}

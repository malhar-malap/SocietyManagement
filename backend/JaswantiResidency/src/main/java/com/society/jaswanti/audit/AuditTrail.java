package com.society.jaswanti.audit;

import com.society.jaswanti.entity.ActivityLog;
import com.society.jaswanti.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Request-scoped hints that let controllers and services enrich the audit entry for the current request.
 * Every method is a no-op outside an HTTP request, so services stay safe to call from anywhere.
 */
public final class AuditTrail {

    static final String DESCRIPTION = AuditTrail.class.getName() + ".description";
    static final String ACTION = AuditTrail.class.getName() + ".action";
    static final String ACTOR = AuditTrail.class.getName() + ".actor";
    static final String ACTOR_LABEL = AuditTrail.class.getName() + ".actorLabel";
    static final String TARGET_TYPE = AuditTrail.class.getName() + ".targetType";
    static final String TARGET_ID = AuditTrail.class.getName() + ".targetId";
    static final String TARGET_LABEL = AuditTrail.class.getName() + ".targetLabel";
    static final String RECORDED_LOG_ID = AuditTrail.class.getName() + ".recordedLogId";

    private AuditTrail() {}

    /** Replaces the endpoint's default summary with a more specific description. */
    public static void describe(String description) {
        set(DESCRIPTION, description);
    }

    /** Overrides the endpoint's action name, e.g. when one endpoint can perform two different actions. */
    public static void action(String action) {
        set(ACTION, action);
    }

    /** Sets the actor when nobody is signed in yet (e.g. a successful sign-in). */
    public static void actor(User user) {
        set(ACTOR, user);
    }

    /** Records who attempted an action when there is no matching account (e.g. a failed sign-in identifier). */
    public static void actorLabel(String label) {
        set(ACTOR_LABEL, label);
    }

    /** Names the record acted on, for endpoints that create it (the id is not in the URL yet). */
    public static void target(String type, Object id) {
        if (type != null && id != null) {
            set(TARGET_TYPE, type);
            set(TARGET_ID, String.valueOf(id));
            set(TARGET_LABEL, null);
        }
    }

    /**
     * Fills in where the action came from (method, path, client IP) and what it targeted, from the current request.
     * The IP is the socket address: X-Forwarded-For is client-controlled and is not trusted for the audit trail.
     */
    public static void applyRequestContext(ActivityLog log) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            HttpServletRequest request = servletAttributes.getRequest();
            if (log.getIpAddress() == null) {
                log.setIpAddress(request.getRemoteAddr());
            }
            if (log.getHttpMethod() == null) {
                log.setHttpMethod(request.getMethod());
            }
            if (log.getRequestPath() == null) {
                log.setRequestPath(truncate(request.getRequestURI(), 500));
            }
        }
        if (log.getTargetType() == null) {
            log.setTargetType(getString(TARGET_TYPE));
            log.setTargetId(getString(TARGET_ID));
            log.setTargetLabel(truncate(getString(TARGET_LABEL), 500));
        }
    }

    /** Remembers that an entry for this request has been written, so the interceptor does not write a duplicate. */
    public static void markRecorded(Long logId) {
        set(RECORDED_LOG_ID, logId);
    }

    static String truncate(String value, int max) {
        return value != null && value.length() > max ? value.substring(0, max - 1) + "…" : value;
    }

    static Object get(String key) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getAttribute(key, RequestAttributes.SCOPE_REQUEST) : null;
    }

    static String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : null;
    }

    static void set(String key, Object value) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        if (value == null) {
            attributes.removeAttribute(key, RequestAttributes.SCOPE_REQUEST);
        } else {
            attributes.setAttribute(key, value, RequestAttributes.SCOPE_REQUEST);
        }
    }
}

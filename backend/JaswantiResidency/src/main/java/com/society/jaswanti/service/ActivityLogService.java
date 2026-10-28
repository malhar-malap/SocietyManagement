package com.society.jaswanti.service;

import com.society.jaswanti.audit.AuditPolicy;
import com.society.jaswanti.audit.AuditTrail;
import com.society.jaswanti.entity.ActivityLog;
import com.society.jaswanti.entity.User;
import com.society.jaswanti.repository.ActivityLogRepository;
import org.springframework.stereotype.Service;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityLogService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    /**
     * Records an action performed by {@code user}. Runs inside the caller's transaction, so ledger operations stay
     * atomic with their audit entry: if this write fails, the operation rolls back, and vice versa.
     */
    public void logActivity(User user, String action, String module, String description) {
        ActivityLog log = new ActivityLog(user, action, module, description, null);
        applyActor(log, user);
        save(log);
    }

    /** Records an event with no matching account, e.g. a lockout for an identifier that does not exist. */
    public void logAnonymous(String actorLabel, String action, String module, String description) {
        ActivityLog log = new ActivityLog(null, action, module, description, null);
        log.setActorEmail(actorLabel);
        save(log);
    }

    /** Saves a fully built entry (used by the audit interceptor). */
    public void record(ActivityLog log) {
        save(log);
    }

    private void save(ActivityLog log) {
        AuditTrail.applyRequestContext(log);
        // Routine activity (sign-ins, account setup, residents' own changes) is not security-relevant; see AuditPolicy
        if (!AuditPolicy.shouldRecord(log)) {
            return;
        }
        ActivityLog saved = activityLogRepository.save(log);
        AuditTrail.markRecorded(saved.getId());
    }

    static void applyActor(ActivityLog log, User user) {
        if (user != null) {
            log.setActorName(user.getName());
            log.setActorEmail(user.getEmail());
            log.setActorRole(user.getRole() != null ? user.getRole().name() : null);
        }
    }
}

package com.society.jaswanti.audit;

import com.society.jaswanti.entity.ActivityLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Set;

/**
 * Decides which events belong in the system log. The log exists to catch misuse (wrong approvals, payment and
 * account changes made by staff), so routine activity such as successful sign-ins, account setup and residents
 * editing their own details is left out. Refused requests (DENIED) and failed sign-ins are always kept.
 */
public final class AuditPolicy {

    static final Set<String> ROUTINE_ACTIONS = Set.of(
            "LOGIN", "REQUEST_PASSWORD_RESET", "RESET_PASSWORD", "SETUP_PROFILE", "VERIFY_SETUP_EMAIL");

    static final Set<String> RESIDENT_SELF_SERVICE_ACTIONS = Set.of(
            "CHANGE_PASSWORD", "UPDATE_PROFILE", "CREATE_SUBMISSION");

    private AuditPolicy() {
    }

    public static boolean shouldRecord(String action, String outcome, String actorRole) {
        if (ActivityLog.OUTCOME_DENIED.equals(outcome)) {
            return true;
        }
        if ("LOGIN".equals(action)) {
            return !ActivityLog.OUTCOME_SUCCESS.equals(outcome);
        }
        if (ROUTINE_ACTIONS.contains(action)) {
            return false;
        }
        return !(RESIDENT_SELF_SERVICE_ACTIONS.contains(action) && "RESIDENT".equals(actorRole));
    }

    public static boolean shouldRecord(ActivityLog log) {
        return shouldRecord(log.getAction(), log.getOutcome(), log.getActorRole());
    }

    /** The same rule as {@link #shouldRecord}, for hiding routine entries written before this policy existed. */
    public static Specification<ActivityLog> recordedOnly() {
        return (root, query, cb) -> {
            Predicate denied = cb.equal(root.get("outcome"), ActivityLog.OUTCOME_DENIED);
            Predicate successfulLogin = cb.and(
                    cb.equal(root.get("action"), "LOGIN"),
                    cb.equal(root.get("outcome"), ActivityLog.OUTCOME_SUCCESS));
            Predicate otherRoutine = cb.and(
                    root.get("action").in(ROUTINE_ACTIONS),
                    cb.notEqual(root.get("action"), "LOGIN"));
            Predicate residentSelfService = cb.and(
                    root.get("action").in(RESIDENT_SELF_SERVICE_ACTIONS),
                    cb.equal(root.get("actorRole"), "RESIDENT"));
            return cb.or(denied, cb.not(cb.or(successfulLogin, otherRoutine, residentSelfService)));
        };
    }
}

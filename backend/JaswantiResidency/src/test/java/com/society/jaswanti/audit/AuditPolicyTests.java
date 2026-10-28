package com.society.jaswanti.audit;

import com.society.jaswanti.entity.ActivityLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditPolicyTests {

    @Test
    @DisplayName("Successful sign-ins and account setup are not recorded; failed sign-ins are")
    void signInsAndSetup() {
        assertThat(AuditPolicy.shouldRecord("LOGIN", ActivityLog.OUTCOME_SUCCESS, "ADMIN")).isFalse();
        assertThat(AuditPolicy.shouldRecord("LOGIN", ActivityLog.OUTCOME_DENIED, null)).isTrue();
        assertThat(AuditPolicy.shouldRecord("LOGIN", ActivityLog.OUTCOME_FAILED, null)).isTrue();
        assertThat(AuditPolicy.shouldRecord("SETUP_PROFILE", ActivityLog.OUTCOME_SUCCESS, "RESIDENT")).isFalse();
        assertThat(AuditPolicy.shouldRecord("VERIFY_SETUP_EMAIL", ActivityLog.OUTCOME_SUCCESS, "ADMIN")).isFalse();
        assertThat(AuditPolicy.shouldRecord("RESET_PASSWORD", ActivityLog.OUTCOME_SUCCESS, null)).isFalse();
        assertThat(AuditPolicy.shouldRecord("LOGIN_LOCKOUT", ActivityLog.OUTCOME_SUCCESS, null)).isTrue();
    }

    @Test
    @DisplayName("Residents' own edits are skipped, but the same action by staff and every refusal is kept")
    void selfServiceVersusStaff() {
        assertThat(AuditPolicy.shouldRecord("UPDATE_PROFILE", ActivityLog.OUTCOME_SUCCESS, "RESIDENT")).isFalse();
        assertThat(AuditPolicy.shouldRecord("CREATE_SUBMISSION", ActivityLog.OUTCOME_SUCCESS, "RESIDENT")).isFalse();
        assertThat(AuditPolicy.shouldRecord("UPDATE_PROFILE", ActivityLog.OUTCOME_SUCCESS, "ADMIN")).isTrue();
        assertThat(AuditPolicy.shouldRecord("CHANGE_PASSWORD", ActivityLog.OUTCOME_DENIED, "RESIDENT")).isTrue();
    }

    @Test
    @DisplayName("Payment and admin operations are always recorded")
    void paymentsAndAdminActions() {
        for (String action : new String[]{"APPROVE_PAYMENT_SUBMISSION", "REJECT_PAYMENT_SUBMISSION", "RECORD_PAYMENT",
                "MARK_BILL_PAID_CASH", "REVERSE_TRANSACTION", "UPDATE_USER_ROLE", "GENERATE_MONTHLY_BILLS"}) {
            assertThat(AuditPolicy.shouldRecord(action, ActivityLog.OUTCOME_SUCCESS, "ADMIN")).as(action).isTrue();
            assertThat(AuditPolicy.shouldRecord(action, ActivityLog.OUTCOME_FAILED, "ADMIN")).as(action).isTrue();
        }
        assertThat(AuditPolicy.shouldRecord("SUBMIT_PAYMENT", ActivityLog.OUTCOME_SUCCESS, "RESIDENT")).isTrue();
    }
}

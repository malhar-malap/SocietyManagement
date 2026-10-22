package com.society.jaswanti.exception;

public class LastMasterAdminProtectionException extends RuntimeException {
    public LastMasterAdminProtectionException() {
        super("The last active Master Admin cannot be demoted, deactivated, or blocked.");
    }
}

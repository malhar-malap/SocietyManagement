package com.society.jaswanti.exception;

public class PasswordResetCooldownException extends RuntimeException {
    public PasswordResetCooldownException(String message) {
        super(message);
    }
}

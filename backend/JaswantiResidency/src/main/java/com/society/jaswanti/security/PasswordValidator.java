package com.society.jaswanti.security;

import com.society.jaswanti.exception.InvalidPasswordException;

public final class PasswordValidator {

    private PasswordValidator() {}

    public static void validate(String password) {
        if (password == null || password.trim().length() < 8) {
            throw new InvalidPasswordException("Password must be at least 8 characters long.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new InvalidPasswordException("Password must contain at least one letter and one digit.");
        }
    }
}

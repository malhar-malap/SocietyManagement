package com.society.jaswanti.exception;

public class LoginLockoutException extends RuntimeException {
    public LoginLockoutException(String message) {
        super(message);
    }
}

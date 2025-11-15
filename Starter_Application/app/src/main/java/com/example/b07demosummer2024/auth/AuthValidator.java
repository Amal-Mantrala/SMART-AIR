package com.example.b07demosummer2024.auth;

public class AuthValidator {
    public static final int MIN_PASSWORD_LENGTH = 6;

    public static boolean isEmailEmpty(String email) {
        return email == null || email.trim().isEmpty();
    }

    public static boolean isPasswordEmpty(String password) {
        return password == null || password.trim().isEmpty();
    }

    public static boolean isEmailValidFormat(String email) {
        if (email == null) return false;
        email = email.trim();
        if (email.isEmpty()) return false;
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public static boolean isPasswordStrongEnough(String password) {
        return password != null && password.length() >= MIN_PASSWORD_LENGTH;
    }
}

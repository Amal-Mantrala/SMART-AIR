package com.example.b07demosummer2024.auth;

import org.junit.Test;

import static org.junit.Assert.*;


public class AuthValidatorTest {
    @Test
    public void email_isValid() {
        assertTrue(AuthValidator.isEmailValidFormat("test@gmail.com"));
        assertTrue(AuthValidator.isEmailValidFormat("user123@mail.co"));
        assertTrue(AuthValidator.isEmailValidFormat("a.b-c_d+e@domain.org"));
    }

    @Test
    public void email_isInvalid() {
        assertFalse(AuthValidator.isEmailValidFormat("suuu@gmai"));
        assertFalse(AuthValidator.isEmailValidFormat("invalid"));
        assertFalse(AuthValidator.isEmailValidFormat("hello@"));
        assertFalse(AuthValidator.isEmailValidFormat("@gmail.com"));
        assertFalse(AuthValidator.isEmailValidFormat("test@gmail"));
        assertFalse(AuthValidator.isEmailValidFormat("test@gmail.c"));
        assertFalse(AuthValidator.isEmailValidFormat(""));
        assertFalse(AuthValidator.isEmailValidFormat(null));
    }

    @Test
    public void password_isValid() {
        assertTrue(AuthValidator.isPasswordStrongEnough("abcdef"));
        assertTrue(AuthValidator.isPasswordStrongEnough("12345678"));
        assertTrue(AuthValidator.isPasswordStrongEnough("passWORD123"));
    }

    @Test
    public void password_isInvalid() {
        assertFalse(AuthValidator.isPasswordStrongEnough(""));
        assertFalse(AuthValidator.isPasswordStrongEnough("123"));
    }

}
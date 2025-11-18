package com.example.b07demosummer2024.auth;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.lang.reflect.Method;

public class AuthServiceTest {

    @Test
    public void testAuthServiceClassExists() {
        Class<?> authServiceClass = AuthService.class;
        assertNotNull(authServiceClass);
    }

    @Test
    public void testIsSignedInMethodExists() {
        try {
            Method method = AuthService.class.getMethod("isSignedIn");
            assertNotNull(method);
        } catch (NoSuchMethodException e) {
            assertNotNull("isSignedIn method should exist", null);
        }
    }
}

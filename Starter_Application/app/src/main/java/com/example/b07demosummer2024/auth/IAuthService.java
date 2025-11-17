package com.example.b07demosummer2024.auth;

public interface IAuthService {
        void signIn(String email, String password, AuthService.AuthCallback callback);
        void resetPassword(String email, AuthService.AuthCallback callback);
        void signOut();
}

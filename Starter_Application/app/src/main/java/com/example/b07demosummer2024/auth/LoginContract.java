package com.example.b07demosummer2024.auth;

public interface LoginContract {
        interface View {
            void showEmailError(String message);
            void showPasswordError(String message);
            void showLoginError(String message);
            void navigateToHome();
        }

        interface Presenter {
            void onLoginClicked(String email, String password);
            void onForgotPasswordClicked(String email);
            void onDestroy(); // optional, in case you ever need cleanup
        }
}

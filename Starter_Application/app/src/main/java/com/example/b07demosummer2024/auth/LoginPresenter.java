package com.example.b07demosummer2024.auth;

import android.widget.Toast;

public class LoginPresenter implements LoginContract.Presenter {
    private final LoginContract.View view;
    private final IAuthService authService;

    public LoginPresenter(LoginContract.View view, IAuthService authService) {
        this.view = view;
        this.authService = authService;
    }

    @Override
    public void onLoginClicked(String email, String password) {
        // Clear previous errors
        view.showEmailError(null);
        view.showPasswordError(null);

        if (email.isEmpty()) {
            view.showEmailError("Email is required");
            return;
        }
        
        if (password.isEmpty()) {
            view.showPasswordError("Password is required");
            return;
        }

        if (!(email.contains("@") && email.contains("."))) {
            email = email + "@smart-air-child.com";
        }
        if (!AuthValidator.isEmailValidFormat(email)) {
            view.showEmailError("Please enter a valid email");
            return;
        }

        if (!AuthValidator.isPasswordStrongEnough(password)) {
            view.showPasswordError("Password must be at least 6 characters");
            return;
        }

        // Show loading feedback
        view.showLoginError("Signing in...");

        authService.signIn(email, password, (success, message) -> {
            if (success) {
                view.showLoginError("Login successful!");
                view.navigateToHome();
            } else {
                view.showLoginError("Login failed: " + message);
            }
        });
    }

    @Override
    public void onForgotPasswordClicked(String email) {
        if (!AuthValidator.isEmailValidFormat(email)) {
            view.showEmailError("Enter a valid email to reset password");
            return;
        }
        authService.resetPassword(email, (success, message) -> {
            view.showLoginError(message);
        });
    }

    @Override
    public void onDestroy() {
    }
}

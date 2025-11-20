package com.example.b07demosummer2024.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import com.example.b07demosummer2024.R;

import javax.annotation.Nullable;
import com.example.b07demosummer2024.auth.AuthValidator;
import com.example.b07demosummer2024.auth.AuthService;


public class SignupFragment extends Fragment {

    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private AuthService authService;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailInput = view.findViewById(R.id.signupEmail);
        passwordInput = view.findViewById(R.id.signupPassword);
        confirmPasswordInput = view.findViewById(R.id.signupConfirmPassword);
        Button createAccountButton = view.findViewById(R.id.buttonCreateAccount);
        TextView goToLoginText = view.findViewById(R.id.textGoToLogin);

        authService = new AuthService();

        createAccountButton.setOnClickListener(v -> handleCreateAccount());
        goToLoginText.setOnClickListener(v -> navigateToLogin());
    }

    private void handleCreateAccount() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirm = confirmPasswordInput.getText().toString().trim();

        if (!AuthValidator.isEmailValidFormat(email)) {
            emailInput.setError("Please enter a valid email");
            return;
        }

        if (!AuthValidator.isPasswordStrongEnough(password)) {
            passwordInput.setError("Password must be at least 6 characters");
            return;
        }

        if (!password.equals(confirm)) {
            confirmPasswordInput.setError("Passwords do not match");
            return;
        }

        authService.createUser(email, password, (success, message) -> {
            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Account created. Please log in.", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                } else {
                    Toast.makeText(getContext(), "Signup failed: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void navigateToLogin() {
        requireActivity().runOnUiThread(() -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}



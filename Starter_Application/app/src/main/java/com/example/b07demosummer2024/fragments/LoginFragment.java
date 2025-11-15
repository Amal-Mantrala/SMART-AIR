package com.example.b07demosummer2024.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.AuthService;
import com.example.b07demosummer2024.auth.AuthValidator;

public class LoginFragment extends Fragment {

    private EditText emailInput;
    private EditText passwordInput;

    public LoginFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailInput = view.findViewById(R.id.editTextEmail);
        passwordInput = view.findViewById(R.id.editTextPassword);
        Button loginButton = view.findViewById(R.id.buttonSignIn);

        AuthService authService = new AuthService();

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (!AuthValidator.isEmailValidFormat(email)) {
                emailInput.setError("Invalid email");
                return;
            }
            if (!AuthValidator.isPasswordStrongEnough(password)) {
                passwordInput.setError("Password too short");
                return;
            }

            authService.signIn(email, password, (success, message) -> requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                    // code for navigating to HomeFragment later (for person implementing going to homepage)
                } else {
                    Toast.makeText(getContext(), "Login failed: " + message, Toast.LENGTH_LONG).show();
                }
            }));
        });
    }
}

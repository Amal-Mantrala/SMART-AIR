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
import com.example.b07demosummer2024.auth.AuthValidator;
import com.example.b07demosummer2024.auth.AuthService;

public class SignupFragment extends Fragment {

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private AuthService authService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_signup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nameInput = view.findViewById(R.id.signupName);
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
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirm = confirmPasswordInput.getText().toString().trim();

        if (name.isEmpty()) {
            nameInput.setError("Please enter your name");
            return;
        }

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
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(getContext(), "Account created!", Toast.LENGTH_SHORT).show();

                    com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
                    if (auth.getCurrentUser() != null) {
                        String uid = auth.getCurrentUser().getUid();
                        com.google.firebase.firestore.FirebaseFirestore db =
                                com.google.firebase.firestore.FirebaseFirestore.getInstance();

                        java.util.Map<String, Object> user = new java.util.HashMap<>();
                        user.put("name", name);
                        user.put("email", email);

                        db.collection("users").document(uid)
                                .set(user, com.google.firebase.firestore.SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded()) {
                                        showRoleDialog(name, uid);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded()) {
                                        Toast.makeText(getContext(), "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                } else {
                    Toast.makeText(getContext(), "Signup failed: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private void showRoleDialog(String name, String uid) {
        String[] roles = new String[]{"Parent", "Provider"};

        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Choose Role")
                .setItems(roles, (dialog, which) -> {

                    String role = (which == 0) ? "parent" : "provider";

                    java.util.Map<String, Object> userData = new java.util.HashMap<>();
                    userData.put("role", role);
                    userData.put("name", name);

                    com.google.firebase.firestore.FirebaseFirestore db =
                            com.google.firebase.firestore.FirebaseFirestore.getInstance();

                    db.collection("users").document(uid)
                            .set(userData, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Role saved: " + role, Toast.LENGTH_SHORT).show();
                                Toast.makeText(getContext(), "Please log in now to continue.", Toast.LENGTH_LONG).show();

                                com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                                
                                requireActivity().getSupportFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.fragment_container, new LoginFragment())
                                        .commit();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                })
                .setCancelable(false)
                .show();
    }

    private void navigateToLogin() {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}

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
import android.widget.TextView;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.AuthService;
import com.example.b07demosummer2024.auth.LoginContract;
import com.example.b07demosummer2024.auth.LoginPresenter;
import com.example.b07demosummer2024.auth.AuthValidator;

public class LoginFragment extends Fragment implements LoginContract.View {

    private EditText emailInput;
    private EditText passwordInput;

    private LoginContract.Presenter presenter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailInput = view.findViewById(R.id.editTextEmail);
        passwordInput = view.findViewById(R.id.editTextPassword);
        Button loginButton = view.findViewById(R.id.buttonSignIn);
        TextView forgotPassword = view.findViewById(R.id.textForgotPassword);
        TextView goToSignup = view.findViewById(R.id.textGoToSignup);

        presenter = new LoginPresenter(this, new AuthService());

        forgotPassword.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            presenter.onForgotPasswordClicked(email);
        });

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            
            presenter.onLoginClicked(email, password);
        });

        goToSignup.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SignupFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }


    @Override
    public void showEmailError(String message) {
        emailInput.setError(message);
        if (message != null) {
            emailInput.requestFocus();
        }
    }

    @Override
    public void showPasswordError(String message) {
        passwordInput.setError(message);
        if (message != null) {
            passwordInput.requestFocus();
        }
    }

    @Override
    public void showLoginError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void navigateToHome() {
        // After successful login, fetch user's role and name from Firebase
        requireActivity().runOnUiThread(() -> {
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                // Authentication failed - return to login
                Toast.makeText(getContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = auth.getCurrentUser().getUid();
            
            // Always fetch user data from Firestore (no local storage check)
            com.google.firebase.firestore.FirebaseFirestore db = com.google.firebase.firestore.FirebaseFirestore.getInstance();
            
            db.collection("users").document(uid).get().addOnCompleteListener(task -> {
                if (!isAdded()) return; // Fragment no longer attached
                
                String role = null;
                String name = null;
                
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    com.google.firebase.firestore.DocumentSnapshot document = task.getResult();
                    
                    // Fetch role and name
                    role = document.getString("role");
                    name = document.getString("name");
                }

                if (role != null) {
                    // Navigate to the appropriate role-specific homepage
                    navigateToRoleHome(role);
                } else {
                    // User doesn't have a role yet - show role selection
                    RoleSelectionFragment roleDialog = RoleSelectionFragment.newInstance(name != null ? name : "");
                    roleDialog.show(getParentFragmentManager(), "roleSelection");
                }
            }).addOnFailureListener(e -> {
                if (!isAdded()) return;
                // Handle Firebase fetch error
                Toast.makeText(getContext(), "Error loading user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // Stay on current fragment - user can try again
            });
        });
    }

    private void navigateToRoleHome(String role) {
        androidx.fragment.app.Fragment target;
        switch (role) {
            case "child":
                target = new ChildHomeFragment();
                break;
            case "parent":
                target = new ParentHomeFragment();
                break;
            case "provider":
                target = new ProviderHomeFragment();
                break;
            default:
                // If role is unrecognized, show role selection again
                RoleSelectionFragment roleDialog = RoleSelectionFragment.newInstance("");
                roleDialog.show(getParentFragmentManager(), "roleSelection");
                return;
        }
        requireActivity().runOnUiThread(() -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, target)
                    .commitNow(); // Use commitNow for immediate execution
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDestroy();
    }
}
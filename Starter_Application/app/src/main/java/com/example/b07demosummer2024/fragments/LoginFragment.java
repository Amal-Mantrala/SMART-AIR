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
            
            // Add debug toast to confirm button click is working
            Toast.makeText(getContext(), "Login button clicked", Toast.LENGTH_SHORT).show();
            
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
        // After successful login, fetch user's role and navigate accordingly
        requireActivity().runOnUiThread(() -> {
            com.google.firebase.auth.FirebaseAuth auth = com.google.firebase.auth.FirebaseAuth.getInstance();
            if (auth.getCurrentUser() == null) {
                // Fallback
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
                return;
            }

            String uid = auth.getCurrentUser().getUid();
            
            // Try cached role first for immediate navigation
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
            String cachedRole = prefs.getString("user_role_" + uid, null);
            
            if (cachedRole != null) {
                navigateToRoleHome(cachedRole);
                return;
            }
            
            // If no cached role, fetch from Firebase
            com.google.firebase.database.DatabaseReference ref = com.google.firebase.database.FirebaseDatabase.getInstance("https://b07-demo-summer-2024-default-rtdb.firebaseio.com/")
                    .getReference("users").child(uid).child("role");
            ref.get().addOnCompleteListener(task -> {
                if (!isAdded()) return;
                String role = null;
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getValue() != null) {
                    role = String.valueOf(task.getResult().getValue());
                    // Cache the role for next time
                    prefs.edit().putString("user_role_" + uid, role).apply();
                }

                if (role != null) {
                    navigateToRoleHome(role);
                } else {
                    // No role saved yet — show role selection
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new HomeFragment())
                            .commit();
                    new RoleSelectionFragment().show(getParentFragmentManager(), "roleSelection");
                }
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
                target = new HomeFragment();
        }
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, target)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDestroy();
    }
}
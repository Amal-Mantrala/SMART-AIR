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
    }

    @Override
    public void showPasswordError(String message) {
        passwordInput.setError(message);
    }

    @Override
    public void showLoginError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void navigateToHome() {
        requireActivity().runOnUiThread(() -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDestroy();
    }
}
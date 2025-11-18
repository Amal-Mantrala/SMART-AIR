package com.example.b07demosummer2024.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.AuthService;

public abstract class ProtectedFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isNotSignedIn()) {
            redirectToLogin();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isNotSignedIn()) {
            redirectToLogin();
        }
    }

    private boolean isNotSignedIn() {
        AuthService authService = new AuthService();
        return !authService.isSignedIn();
    }

    private void redirectToLogin() {
        getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
    }
}


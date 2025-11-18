package com.example.b07demosummer2024.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.AuthService;

/**
 * Base class for fragments that require authentication.
 * Automatically redirects to LoginFragment if user is not signed in.
 */
public abstract class ProtectedFragment extends Fragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check auth before view creation to prevent any content flash
        if (isNotSignedIn()) {
            redirectToLogin();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Also check on resume for cases where auth state changes while fragment is visible
        if (isNotSignedIn()) {
            redirectToLogin();
        }
    }

    /**
     * Checks if the user is not authenticated.
     * @return true if user is not signed in, false otherwise
     */
    private boolean isNotSignedIn() {
        AuthService authService = new AuthService();
        return !authService.isSignedIn();
    }

    /**
     * Redirects the user to the LoginFragment.
     * Clears the back stack to prevent navigation back to protected screens.
     */
    private void redirectToLogin() {
        // Clear back stack to prevent navigation back to protected screens
        getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        
        // Replace current fragment with LoginFragment
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
    }
}


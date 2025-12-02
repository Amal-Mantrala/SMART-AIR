package com.example.b07demosummer2024.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;

public class HomePageFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_homepage, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button signInBtn = view.findViewById(R.id.btn_sign_in);
        Button signUpBtn = view.findViewById(R.id.btn_sign_up);

        signInBtn.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .addToBackStack(null)
                    .commit();
        });

        signUpBtn.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SignupFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}


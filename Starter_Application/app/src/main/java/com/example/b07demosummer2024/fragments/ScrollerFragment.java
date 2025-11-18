package com.example.b07demosummer2024.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.R;

public class ScrollerFragment extends ProtectedFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scroller, container, false);

        TextView textView = view.findViewById(R.id.textView);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            text.append("Line ").append(i + 1).append("\n");
        }
        textView.setText(text.toString());

        return view;
    }
}

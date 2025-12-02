package com.example.b07demosummer2024.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.b07demosummer2024.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class WeekScheduleDialogFragment extends DialogFragment {

    private static final String ARG_CHILD_ID = "childId";

    public static WeekScheduleDialogFragment newInstance(String childId) {
        WeekScheduleDialogFragment f = new WeekScheduleDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHILD_ID, childId);
        f.setArguments(args);
        return f;
    }

    private String childId;
    private CheckBox checkSun, checkMon, checkTue, checkWed, checkThu, checkFri, checkSat;
    private Button saveButton, cancelButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_week_schedule_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            childId = getArguments().getString(ARG_CHILD_ID);
        }

        checkSun = view.findViewById(R.id.checkSun);
        checkMon = view.findViewById(R.id.checkMon);
        checkTue = view.findViewById(R.id.checkTue);
        checkWed = view.findViewById(R.id.checkWed);
        checkThu = view.findViewById(R.id.checkThu);
        checkFri = view.findViewById(R.id.checkFri);
        checkSat = view.findViewById(R.id.checkSat);

        saveButton = view.findViewById(R.id.buttonSaveSchedule);
        cancelButton = view.findViewById(R.id.buttonCancelSchedule);

        // Load existing schedule from Firestore (users/{childId}.weeklySchedule)
        if (childId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(childId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot doc = task.getResult();
                    Map<String, Object> schedule = (Map<String, Object>) doc.get("weeklySchedule");
                    if (schedule == null) {
                        // default: all days selected
                        checkSun.setChecked(true);
                        checkMon.setChecked(true);
                        checkTue.setChecked(true);
                        checkWed.setChecked(true);
                        checkThu.setChecked(true);
                        checkFri.setChecked(true);
                        checkSat.setChecked(true);
                    } else {
                        // Keys are stored as numbers "1".."7" matching Calendar.DAY_OF_WEEK
                        checkSun.setChecked(Boolean.TRUE.equals(schedule.get("1")));
                        checkMon.setChecked(Boolean.TRUE.equals(schedule.get("2")));
                        checkTue.setChecked(Boolean.TRUE.equals(schedule.get("3")));
                        checkWed.setChecked(Boolean.TRUE.equals(schedule.get("4")));
                        checkThu.setChecked(Boolean.TRUE.equals(schedule.get("5")));
                        checkFri.setChecked(Boolean.TRUE.equals(schedule.get("6")));
                        checkSat.setChecked(Boolean.TRUE.equals(schedule.get("7")));
                    }
                }
            });
        }

        saveButton.setOnClickListener(v -> {
            Map<String, Object> scheduleMap = new HashMap<>();
            scheduleMap.put("1", checkSun.isChecked());
            scheduleMap.put("2", checkMon.isChecked());
            scheduleMap.put("3", checkTue.isChecked());
            scheduleMap.put("4", checkWed.isChecked());
            scheduleMap.put("5", checkThu.isChecked());
            scheduleMap.put("6", checkFri.isChecked());
            scheduleMap.put("7", checkSat.isChecked());

            if (childId != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> update = new HashMap<>();
                update.put("weeklySchedule", scheduleMap);
                db.collection("users").document(childId).set(update, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        dismiss();
                    })
                    .addOnFailureListener(e -> {
                        // keep dialog open on failure
                    });
            } else {
                dismiss();
            }
        });

        cancelButton.setOnClickListener(v -> dismiss());
    }
}

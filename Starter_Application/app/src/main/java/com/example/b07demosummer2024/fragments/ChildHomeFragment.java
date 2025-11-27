package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.AuthService;
import com.example.b07demosummer2024.models.MedicineLog;
import com.example.b07demosummer2024.models.SymptomLog;
import com.example.b07demosummer2024.models.DailyWellnessLog;
import com.example.b07demosummer2024.services.ChildHealthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ChildHomeFragment extends ProtectedFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_child_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        TextView greetingText = view.findViewById(R.id.textGreeting);

        Button signOut = view.findViewById(R.id.buttonSignOut);
        Button detailsButton = view.findViewById(R.id.buttonDetails);
        Button informationButton = view.findViewById(R.id.buttonInformation);
        Button logMedicineButton = view.findViewById(R.id.buttonLogMedicine);
        Button logSymptomsButton = view.findViewById(R.id.buttonLogSymptoms);
        Button dailyCheckInButton = view.findViewById(R.id.buttonDailyCheckIn);
        Button viewHistoryButton = view.findViewById(R.id.buttonViewHistory);
        
        // Load user name and set greeting
        loadUserNameAndSetGreeting(greetingText);
        
        signOut.setOnClickListener(v -> {
            // Clear cached role data before signing out
            SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                prefs.edit().remove("user_role_" + auth.getCurrentUser().getUid()).apply();
            }
            
            new AuthService().signOut();
            FragmentManager fm = getParentFragmentManager();
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fm.beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
        });

        detailsButton.setOnClickListener(v -> showUserDetailsDialog());
        informationButton.setOnClickListener(v -> showTutorial());
        logMedicineButton.setOnClickListener(v -> showLogMedicineDialog());
        logSymptomsButton.setOnClickListener(v -> showLogSymptomsDialog());
        dailyCheckInButton.setOnClickListener(v -> showDailyCheckInDialog());
        viewHistoryButton.setOnClickListener(v -> showHealthHistoryDialog());

        showTutorialIfFirstTime();
        checkForPendingInvitations();
    }

    private void checkForPendingInvitations() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String currentUserEmail = auth.getCurrentUser().getEmail();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("invitations")
                .whereEqualTo("childEmail", currentUserEmail)
                .whereEqualTo("status", "pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String parentName = document.getString("parentName");
                            String parentUid = document.getString("parentUid");
                            showInvitationDialog(parentName, parentUid, document.getId());
                            break; // Show one invitation at a time
                        }
                    }
                });
    }

    private void showInvitationDialog(String parentName, String parentUid, String invitationId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Family Invitation")
                .setMessage(parentName + " has invited you to join their family account.")
                .setPositiveButton("Accept", (dialog, which) -> {
                    acceptInvitation(parentUid, invitationId);
                })
                .setNegativeButton("Decline", (dialog, which) -> {
                    declineInvitation(invitationId);
                })
                .setCancelable(false)
                .show();
    }

    private void acceptInvitation(String parentUid, String invitationId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String childId = auth.getCurrentUser().getUid();

        // Update child document
        db.collection("users").document(childId).update("parentId", parentUid);

        // Update parent document
        db.collection("users").document(parentUid).update("children", FieldValue.arrayUnion(childId));

        // Update invitation status
        db.collection("invitations").document(invitationId).update("status", "accepted");

        Toast.makeText(getContext(), "Invitation accepted!", Toast.LENGTH_SHORT).show();
    }

    private void declineInvitation(String invitationId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("invitations").document(invitationId).update("status", "declined");
        Toast.makeText(getContext(), "Invitation declined.", Toast.LENGTH_SHORT).show();
    }


    private void loadUserNameAndSetGreeting(TextView greetingText) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists() && isAdded()) {
                            String name = document.getString("name");
                            if (name != null && !name.isEmpty()) {
                                String greeting = getString(R.string.child_greeting, name);
                                greetingText.setText(greeting);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Keep default greeting if Firestore fails
                    });
        }
    }

    private void showTutorialIfFirstTime() {
        SharedPreferences prefs = requireContext().getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE);
        String key = "tutorial_seen_child";
        if (!prefs.getBoolean(key, false)) {
            showTutorial();
            prefs.edit().putBoolean(key, true).apply();
        }
    }

    private void showTutorial() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.tutorial_title)
                .setMessage(R.string.child_tutorial_content)
                .setPositiveButton(R.string.tutorial_got_it, null)
                .setCancelable(true)
                .show();
    }

    private void showUserDetailsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_details, null);
        TextView emailText = dialogView.findViewById(R.id.textUserEmail);
        EditText nameEdit = dialogView.findViewById(R.id.editUserName);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            // Set email
            emailText.setText(auth.getCurrentUser().getEmail());
            
            // Load name from Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(auth.getCurrentUser().getUid())
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String name = document.getString("name");
                            if (name != null) {
                                nameEdit.setText(name);
                            }
                        }
                    });
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        saveButton.setOnClickListener(v -> {
            String name = nameEdit.getText().toString().trim();
            if (!name.isEmpty()) {
                // Save name to Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users")
                        .document(auth.getCurrentUser().getUid())
                        .update("name", name)
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), R.string.name_saved, Toast.LENGTH_SHORT).show();
                                // Refresh the greeting with new name
                                TextView greetingText = getView().findViewById(R.id.textGreeting);
                                if (greetingText != null) {
                                    String greeting = getString(R.string.child_greeting, name);
                                    greetingText.setText(greeting);
                                }
                                dialog.dismiss();
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), "Failed to save name", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                nameEdit.setError("Name cannot be empty");
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showLogMedicineDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_log_medicine, null);
        RadioGroup medicineTypeGroup = dialogView.findViewById(R.id.radioGroupMedicineType);
        RadioButton radioRescue = dialogView.findViewById(R.id.radioRescue);
        RadioButton radioController = dialogView.findViewById(R.id.radioController);
        EditText medicineNameEdit = dialogView.findViewById(R.id.editMedicineName);
        EditText dosageEdit = dialogView.findViewById(R.id.editDosage);
        RadioGroup effectivenessGroup = dialogView.findViewById(R.id.radioGroupEffectiveness);
        RadioButton radioBetter = dialogView.findViewById(R.id.radioBetter);
        RadioButton radioSame = dialogView.findViewById(R.id.radioSame);
        RadioButton radioWorse = dialogView.findViewById(R.id.radioWorse);
        LinearLayout rescueDetailsLayout = dialogView.findViewById(R.id.layoutRescueDetails);
        CheckBox checkWheezing = dialogView.findViewById(R.id.checkWheezing);
        CheckBox checkCoughing = dialogView.findViewById(R.id.checkCoughing);
        CheckBox checkShortBreath = dialogView.findViewById(R.id.checkShortBreath);
        CheckBox checkChestTight = dialogView.findViewById(R.id.checkChestTight);
        SeekBar severitySeekBar = dialogView.findViewById(R.id.seekBarSeverity);
        TextView severityText = dialogView.findViewById(R.id.textSeverityValue);
        Spinner locationSpinner = dialogView.findViewById(R.id.spinnerLocation);
        EditText notesEdit = dialogView.findViewById(R.id.editNotes);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        // Setup location spinner
        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.location_array, android.R.layout.simple_spinner_item);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        // Setup severity seekbar to show 1-10
        severityText.setText(String.valueOf(severitySeekBar.getProgress() + 1));
        severitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                severityText.setText(String.valueOf(progress + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Show/hide rescue details based on medicine type
        medicineTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioRescue) {
                rescueDetailsLayout.setVisibility(View.VISIBLE);
            } else {
                rescueDetailsLayout.setVisibility(View.GONE);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        saveButton.setOnClickListener(v -> {
            String medicineName = medicineNameEdit.getText().toString().trim();
            String dosage = dosageEdit.getText().toString().trim();
            String notes = notesEdit.getText().toString().trim();

            if (medicineName.isEmpty() || dosage.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in medicine name and dosage", Toast.LENGTH_SHORT).show();
                return;
            }

            String medicineType = radioRescue.isChecked() ? "rescue" : "controller";
            
            // Get effectiveness rating
            String effectiveness = "";
            int effectivenessId = effectivenessGroup.getCheckedRadioButtonId();
            if (effectivenessId == R.id.radioBetter) {
                effectiveness = "Better";
            } else if (effectivenessId == R.id.radioSame) {
                effectiveness = "Same";
            } else if (effectivenessId == R.id.radioWorse) {
                effectiveness = "Worse";
            }
            
            MedicineLog medicineLog = new MedicineLog();
            medicineLog.setChildId(FirebaseAuth.getInstance().getCurrentUser().getUid());
            medicineLog.setMedicineType(medicineType);
            medicineLog.setMedicineName(medicineName);
            medicineLog.setDosage(dosage);
            medicineLog.setNotes(notes + (effectiveness.isEmpty() ? "" : "\nEffectiveness: " + effectiveness));
            medicineLog.setLocation(locationSpinner.getSelectedItem().toString());

            if ("rescue".equals(medicineType)) {
                List<String> symptoms = new ArrayList<>();
                if (checkWheezing.isChecked()) symptoms.add("Wheezing");
                if (checkCoughing.isChecked()) symptoms.add("Coughing");
                if (checkShortBreath.isChecked()) symptoms.add("Short of Breath");
                if (checkChestTight.isChecked()) symptoms.add("Chest Tightness");
                
                medicineLog.setSymptoms(symptoms);
                medicineLog.setSeverityLevel(severitySeekBar.getProgress() + 1);
            }

            ChildHealthService healthService = new ChildHealthService();
            healthService.saveMedicineLog(medicineLog, new ChildHealthService.SaveCallback() {
                @Override
                public void onSuccess(String documentId) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Medicine log saved successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error saving log: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showLogSymptomsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_log_symptoms, null);
        CheckBox checkWheezing = dialogView.findViewById(R.id.checkWheezing);
        CheckBox checkCoughing = dialogView.findViewById(R.id.checkCoughing);
        CheckBox checkShortBreath = dialogView.findViewById(R.id.checkShortBreath);
        CheckBox checkChestTight = dialogView.findViewById(R.id.checkChestTight);
        CheckBox checkFatigue = dialogView.findViewById(R.id.checkFatigue);
        CheckBox checkNightSymptoms = dialogView.findViewById(R.id.checkNightSymptoms);
        SeekBar severitySeekBar = dialogView.findViewById(R.id.seekBarSeverity);
        TextView severityText = dialogView.findViewById(R.id.textSeverityValue);
        CheckBox checkDust = dialogView.findViewById(R.id.checkDust);
        CheckBox checkPollen = dialogView.findViewById(R.id.checkPollen);
        CheckBox checkExercise = dialogView.findViewById(R.id.checkExercise);
        CheckBox checkWeather = dialogView.findViewById(R.id.checkWeather);
        CheckBox checkSmoke = dialogView.findViewById(R.id.checkSmoke);
        CheckBox checkStress = dialogView.findViewById(R.id.checkStress);
        Spinner locationSpinner = dialogView.findViewById(R.id.spinnerLocation);
        Spinner activitySpinner = dialogView.findViewById(R.id.spinnerActivity);
        CheckBox checkRescueUsed = dialogView.findViewById(R.id.checkRescueUsed);
        EditText peakFlowEdit = dialogView.findViewById(R.id.editPeakFlow);
        EditText notesEdit = dialogView.findViewById(R.id.editNotes);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        // Setup severity seekbar to show 1-10
        severityText.setText(String.valueOf(severitySeekBar.getProgress() + 1));
        severitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                severityText.setText(String.valueOf(progress + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        saveButton.setOnClickListener(v -> {
            List<String> symptoms = new ArrayList<>();
            if (checkWheezing.isChecked()) symptoms.add("Wheezing");
            if (checkCoughing.isChecked()) symptoms.add("Coughing");
            if (checkShortBreath.isChecked()) symptoms.add("Short of Breath");
            if (checkChestTight.isChecked()) symptoms.add("Chest Tightness");
            if (checkFatigue.isChecked()) symptoms.add("Fatigue");
            if (checkNightSymptoms.isChecked()) symptoms.add("Night Symptoms");

            if (symptoms.isEmpty()) {
                Toast.makeText(requireContext(), "Please select at least one symptom", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> triggers = new ArrayList<>();
            if (checkDust.isChecked()) triggers.add("Dust");
            if (checkPollen.isChecked()) triggers.add("Pollen");
            if (checkExercise.isChecked()) triggers.add("Exercise");
            if (checkWeather.isChecked()) triggers.add("Weather");
            if (checkSmoke.isChecked()) triggers.add("Smoke/Fumes");
            if (checkStress.isChecked()) triggers.add("Stress");

            SymptomLog symptomLog = new SymptomLog();
            symptomLog.setChildId(FirebaseAuth.getInstance().getCurrentUser().getUid());
            symptomLog.setSymptoms(symptoms);
            symptomLog.setOverallSeverity(severitySeekBar.getProgress() + 1);
            symptomLog.setTriggers(triggers);
            symptomLog.setLocation(locationSpinner.getSelectedItem().toString());
            symptomLog.setActivityLevel(activitySpinner.getSelectedItem().toString());
            symptomLog.setRescueInhalerUsed(checkRescueUsed.isChecked());
            symptomLog.setPeakFlowReading(peakFlowEdit.getText().toString().trim());
            symptomLog.setNotes(notesEdit.getText().toString().trim());

            ChildHealthService healthService = new ChildHealthService();
            healthService.saveSymptomLog(symptomLog, new ChildHealthService.SaveCallback() {
                @Override
                public void onSuccess(String documentId) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Symptom log saved successfully!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error saving log: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDailyCheckInDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_daily_checkin, null);
        
        SeekBar energySeekBar = dialogView.findViewById(R.id.seekBarEnergy);
        TextView energyText = dialogView.findViewById(R.id.textEnergyValue);
        SeekBar breathingSeekBar = dialogView.findViewById(R.id.seekBarBreathing);
        TextView breathingText = dialogView.findViewById(R.id.textBreathingValue);
        SeekBar sleepSeekBar = dialogView.findViewById(R.id.seekBarSleep);
        TextView sleepText = dialogView.findViewById(R.id.textSleepValue);
        EditText nightWakingEdit = dialogView.findViewById(R.id.editNightWaking);
        EditText rescueUsesEdit = dialogView.findViewById(R.id.editRescueUses);
        Spinner moodSpinner = dialogView.findViewById(R.id.spinnerMood);
        EditText notesEdit = dialogView.findViewById(R.id.editNotes);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        // Setup mood spinner
        ArrayAdapter<CharSequence> moodAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.mood_array, android.R.layout.simple_spinner_item);
        moodAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(moodAdapter);

        // Initialize text values to show 1-10 scale
        energyText.setText(String.valueOf(energySeekBar.getProgress() + 1));
        breathingText.setText(String.valueOf(breathingSeekBar.getProgress() + 1));
        sleepText.setText(String.valueOf(sleepSeekBar.getProgress() + 1));
        
        energySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                energyText.setText(String.valueOf(progress + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        breathingSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                breathingText.setText(String.valueOf(progress + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        sleepSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sleepText.setText(String.valueOf(progress + 1));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        saveButton.setOnClickListener(v -> {
            String rescueUsesStr = rescueUsesEdit.getText().toString().trim();
            if (rescueUsesStr.isEmpty()) rescueUsesStr = "0";
            
            String nightWakingStr = nightWakingEdit.getText().toString().trim();
            if (nightWakingStr.isEmpty()) nightWakingStr = "0";
            
            try {
                int rescueUses = Integer.parseInt(rescueUsesStr);
                int nightWaking = Integer.parseInt(nightWakingStr);
                
                DailyWellnessLog wellnessLog = new DailyWellnessLog();
                wellnessLog.setChildId(FirebaseAuth.getInstance().getCurrentUser().getUid());
                wellnessLog.setEnergyLevel(energySeekBar.getProgress() + 1);
                wellnessLog.setBreathingEase(breathingSeekBar.getProgress() + 1);
                wellnessLog.setSleepQuality(sleepSeekBar.getProgress() + 1);
                wellnessLog.setRescueInhalerUses(rescueUses);
                wellnessLog.setMood(moodSpinner.getSelectedItem().toString());
                String notes = notesEdit.getText().toString().trim();
                if (nightWaking > 0) {
                    notes += (notes.isEmpty() ? "" : "\n") + "Night wakings: " + nightWaking;
                }
                wellnessLog.setNotes(notes);

                ChildHealthService healthService = new ChildHealthService();
                healthService.saveDailyWellnessLog(wellnessLog, new ChildHealthService.SaveCallback() {
                    @Override
                    public void onSuccess(String documentId) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Daily check-in saved successfully!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), "Error saving check-in: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Please enter valid numbers for rescue inhaler uses and night waking", Toast.LENGTH_SHORT).show();
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showHealthHistoryDialog() {
        String childId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, HealthHistoryFragment.newInstance(childId))
                .addToBackStack(null)
                .commit();
    }
}

package com.example.b07demosummer2024.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.auth.AuthService;
import com.example.b07demosummer2024.logic.ZoneCalculator;
import com.example.b07demosummer2024.models.MedicineLog;
import com.example.b07demosummer2024.models.SymptomLog;
import com.example.b07demosummer2024.models.DailyWellnessLog;
import com.example.b07demosummer2024.models.TriageIncident;
import com.example.b07demosummer2024.models.Streak;
import com.example.b07demosummer2024.models.Badge;
import com.example.b07demosummer2024.models.MotivationSettings;
import com.example.b07demosummer2024.models.ZoneLog;
import com.example.b07demosummer2024.services.ChildHealthService;
import com.example.b07demosummer2024.services.TriageService;
import com.example.b07demosummer2024.services.MotivationService;
import com.google.firebase.auth.FirebaseAuth;
import com.example.b07demosummer2024.auth.ImpersonationService;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChildHomeFragment extends ProtectedFragment {

    private TextView zoneText;
    private MotivationService motivationService;
    private int pbValue = 0;
    private boolean isCalculatingMotivation = false;
    private long lastMotivationCalculation = 0;
    private static final long MOTIVATION_CALCULATION_COOLDOWN = 5000; // 5 seconds

    private ListenerRegistration userListener;

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
        Button motivationButton = view.findViewById(R.id.buttonMotivation);
        Button triageButton = view.findViewById(R.id.buttonTriage);
        Button pefButton = view.findViewById(R.id.buttonEnterPef);
        Button techniqueHelperButton = view.findViewById(R.id.buttonTechniqueHelper);
        Button myInventoryButton = view.findViewById(R.id.buttonMyInventory);
        zoneText = view.findViewById(R.id.textZoneDisplay);

        // Initialize motivation service safely
        try {
            motivationService = new MotivationService();
        } catch (Exception e) {
            // Fallback - disable motivation features if service fails
            motivationService = null;
        }

        // Load Zone and PB
        startUserListener();
        
        // Load user name and set greeting
        loadUserNameAndSetGreeting(greetingText);
        
        signOut.setOnClickListener(v -> {
            // If we are impersonating a child (parent viewing child), clear impersonation
            if (ImpersonationService.isImpersonating(requireContext())) {
                ImpersonationService.clearImpersonation(requireContext());
                // return to parent home
                getParentFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ParentHomeFragment())
                        .commit();
                return;
            }

            // Normal sign out flow for a real child account
            SharedPreferences prefs = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                prefs.edit().remove("user_role_" + auth.getCurrentUser().getUid()).apply();
            }

            new AuthService().signOut();
            FragmentManager fm = getParentFragmentManager();
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            fm.beginTransaction()
                    .replace(R.id.fragment_container, new HomePageFragment())
                    .commit();
        });

        detailsButton.setOnClickListener(v -> showUserDetailsDialog());
        informationButton.setOnClickListener(v -> showTutorial());
        logMedicineButton.setOnClickListener(v -> showLogMedicineDialog());
        logSymptomsButton.setOnClickListener(v -> showLogSymptomsDialog());
        dailyCheckInButton.setOnClickListener(v -> showDailyCheckInDialog());
        viewHistoryButton.setOnClickListener(v -> showHealthHistoryDialog());
        motivationButton.setOnClickListener(v -> {
            if (motivationService != null) {
                showMotivationDialog();
            } else {
                Toast.makeText(requireContext(), "Motivation system temporarily unavailable", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Long press on motivation button to create test data
        motivationButton.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Test Motivation System");
            builder.setMessage("This will create sample medicine logs for the past 5 days to test streaks and badges. Continue?");
            builder.setPositiveButton("Yes", (dialog, which) -> createTestMotivationData());
            builder.setNegativeButton("No", null);
            builder.show();
            return true;
        });
        triageButton.setOnClickListener(v -> showTriageDialog());
        pefButton.setOnClickListener(v -> showPefDialog());
        techniqueHelperButton.setOnClickListener(v -> {
            new TechniqueHelperFragment().show(getParentFragmentManager(), "TechniqueHelper");
        });
        myInventoryButton.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ChildInventoryFragment())
                    .addToBackStack(null)
                    .commit();
        });

        showTutorialIfFirstTime();
    }

    private void loadUserNameAndSetGreeting(TextView greetingText) {
        String childId = ImpersonationService.getActiveChildId(requireContext());
        if (childId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(childId)
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
            
            // Initialize motivation system for first-time users
            initializeMotivationForNewUser();
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

        String childId = ImpersonationService.getActiveChildId(requireContext());
        if (childId != null) {
            // Try to read stored email from users/{childId} (child profiles may not have email)
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(childId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String name = document.getString("name");
                            String email = document.getString("email");
                            if (email != null) emailText.setText(email);
                            if (name != null) nameEdit.setText(name);
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
                    .document(childId)
                        .update("name", name)
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded()) {
                                Toast.makeText(requireContext(), R.string.name_saved, Toast.LENGTH_SHORT).show();
                                // Refresh the greeting with new name
                                View fragmentView = getView();
                                if (fragmentView != null) {
                                    TextView greetingText = fragmentView.findViewById(R.id.textGreeting);
                                    if (greetingText != null) {
                                        String greeting = getString(R.string.child_greeting, name);
                                        greetingText.setText(greeting);
                                    }
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
            medicineLog.setChildId(ImpersonationService.getActiveChildId(requireContext()));
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
                        
                        // Trigger motivation update: record controller medicine completion (one-per-day)
                        if (motivationService != null && "controller".equals(medicineType)) {
                            updateStreaksBasedOnActivity("controller_medicine", true);
                        }
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
        EditText tagsEdit = dialogView.findViewById(R.id.editTags);
        EditText notesEdit = dialogView.findViewById(R.id.editNotes);
        Button saveButton = dialogView.findViewById(R.id.buttonSave);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.location_array, android.R.layout.simple_spinner_item);
        locationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        ArrayAdapter<CharSequence> activityAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.activity_array, android.R.layout.simple_spinner_item);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(activityAdapter);

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

            List<String> tags = new ArrayList<>();
            String tagsInput = tagsEdit.getText().toString().trim();
            if (!tagsInput.isEmpty()) {
                String[] tagArray = tagsInput.split(",");
                for (String tag : tagArray) {
                    String trimmedTag = tag.trim();
                    if (!trimmedTag.isEmpty()) {
                        tags.add(trimmedTag);
                    }
                }
            }

            SymptomLog symptomLog = new SymptomLog();
            symptomLog.setChildId(ImpersonationService.getActiveChildId(requireContext()));
            symptomLog.setSymptoms(symptoms);
            symptomLog.setOverallSeverity(severitySeekBar.getProgress() + 1);
            symptomLog.setTriggers(triggers);
            symptomLog.setTags(tags);
            Object locationItem = locationSpinner.getSelectedItem();
            symptomLog.setLocation(locationItem != null ? locationItem.toString() : "");
            Object activityItem = activitySpinner.getSelectedItem();
            symptomLog.setActivityLevel(activityItem != null ? activityItem.toString() : "");
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
                wellnessLog.setChildId(ImpersonationService.getActiveChildId(requireContext()));
                wellnessLog.setEnergyLevel(energySeekBar.getProgress() + 1);
                wellnessLog.setBreathingEase(breathingSeekBar.getProgress() + 1);
                wellnessLog.setSleepQuality(sleepSeekBar.getProgress() + 1);
                wellnessLog.setRescueInhalerUses(rescueUses);
                wellnessLog.setMood(moodSpinner.getSelectedItem().toString());
                wellnessLog.setEnteredBy("child");
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
        String childId = ImpersonationService.getActiveChildId(requireContext());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, HealthHistoryFragment.newInstance(childId))
                .addToBackStack(null)
                .commit();
    }

    private void showTriageDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_triage, null);
        
        CheckBox checkCannotSpeak = dialogView.findViewById(R.id.checkCannotSpeak);
        CheckBox checkChestRetractions = dialogView.findViewById(R.id.checkChestRetractions);
        CheckBox checkBlueGrayLips = dialogView.findViewById(R.id.checkBlueGrayLips);
        EditText editRescueAttempts = dialogView.findViewById(R.id.editRescueAttempts);
        EditText editPEF = dialogView.findViewById(R.id.editPEF);
        
        LinearLayout layoutDecisionCard = dialogView.findViewById(R.id.layoutDecisionCard);
        TextView textDecision = dialogView.findViewById(R.id.textDecision);
        Button buttonEmergency = dialogView.findViewById(R.id.buttonEmergency);
        Button buttonHomeSteps = dialogView.findViewById(R.id.buttonHomeSteps);
        LinearLayout layoutHomeSteps = dialogView.findViewById(R.id.layoutHomeSteps);
        TextView textTimer = dialogView.findViewById(R.id.textTimer);
        Button buttonRecheck = dialogView.findViewById(R.id.buttonRecheck);
        Button buttonNotImproving = dialogView.findViewById(R.id.buttonNotImproving);
        
        Button buttonCheck = dialogView.findViewById(R.id.buttonCheck);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        String childId = ImpersonationService.getActiveChildId(requireContext());
        TriageService triageService = new TriageService();
        triageService.getRecentRescueAttempts(childId, new TriageService.RescueAttemptsCallback() {
            @Override
            public void onSuccess(int count) {
                if (isAdded() && count > 0) {
                    editRescueAttempts.setText(String.valueOf(count));
                }
            }

            @Override
            public void onError(String error) {
            }
        });

        final boolean[] storedRedFlags = {false, false, false};
        final int[] storedRescueAttempts = {0};
        final String[] storedPEF = {""};
        
        android.os.Handler timerHandler = new android.os.Handler();
        Runnable timerRunnable = new Runnable() {
            int minutesRemaining = 10;

            @Override
            public void run() {
                if (minutesRemaining > 0 && isAdded()) {
                    textTimer.setText(getString(R.string.timer_remaining, minutesRemaining));
                    textTimer.setVisibility(View.VISIBLE);
                    minutesRemaining--;
                    timerHandler.postDelayed(this, 60000);
                } else {
                    if (isAdded()) {
                        textTimer.setText("Time's up! Auto-escalating...");
                        autoEscalate(childId, storedRedFlags[0], storedRedFlags[1], storedRedFlags[2], 
                                    storedRescueAttempts[0], storedPEF[0], triageService, dialog);
                    }
                }
            }
        };

        buttonCheck.setOnClickListener(v -> {
            final boolean cannotSpeak = checkCannotSpeak.isChecked();
            final boolean chestRetractions = checkChestRetractions.isChecked();
            final boolean blueGrayLips = checkBlueGrayLips.isChecked();
            boolean hasRedFlags = cannotSpeak || chestRetractions || blueGrayLips;
            
            storedRedFlags[0] = cannotSpeak;
            storedRedFlags[1] = chestRetractions;
            storedRedFlags[2] = blueGrayLips;
            
            String rescueAttemptsStr = editRescueAttempts.getText().toString().trim();
            int rescueAttemptsValue = 0;
            if (!rescueAttemptsStr.isEmpty()) {
                try {
                    rescueAttemptsValue = Integer.parseInt(rescueAttemptsStr);
                } catch (NumberFormatException e) {
                    rescueAttemptsValue = 0;
                }
            }
            final int rescueAttempts = rescueAttemptsValue;
            storedRescueAttempts[0] = rescueAttempts;
            
            final String pef = editPEF.getText().toString().trim();
            storedPEF[0] = pef;
            
            layoutDecisionCard.setVisibility(View.VISIBLE);
            buttonCheck.setVisibility(View.GONE);
            
            if (hasRedFlags) {
                textDecision.setText(getString(R.string.triage_decision_emergency));
                buttonEmergency.setVisibility(View.VISIBLE);
                buttonHomeSteps.setVisibility(View.GONE);
                layoutHomeSteps.setVisibility(View.GONE);
                
                buttonEmergency.setOnClickListener(emergencyV -> {
                    saveTriageIncident(childId, cannotSpeak, chestRetractions, blueGrayLips, 
                                     rescueAttempts, pef, "emergency", "called_emergency");
                    dialog.dismiss();
                });
            } else {
                textDecision.setText(getString(R.string.triage_decision_home));
                buttonEmergency.setVisibility(View.GONE);
                buttonHomeSteps.setVisibility(View.VISIBLE);
                layoutHomeSteps.setVisibility(View.VISIBLE);
                
                timerHandler.post(timerRunnable);
                
                buttonHomeSteps.setOnClickListener(homeV -> {
                    saveTriageIncident(childId, cannotSpeak, chestRetractions, blueGrayLips, 
                                     rescueAttempts, pef, "home_steps", "started_home_steps");
                });
                
                buttonRecheck.setOnClickListener(recheckV -> {
                    boolean newCannotSpeak = checkCannotSpeak.isChecked();
                    boolean newChestRetractions = checkChestRetractions.isChecked();
                    boolean newBlueGrayLips = checkBlueGrayLips.isChecked();
                    boolean hasNewRedFlags = newCannotSpeak || newChestRetractions || newBlueGrayLips;
                    
                    String updatedRescueAttemptsStr = editRescueAttempts.getText().toString().trim();
                    int updatedRescueAttempts = 0;
                    if (!updatedRescueAttemptsStr.isEmpty()) {
                        try {
                            updatedRescueAttempts = Integer.parseInt(updatedRescueAttemptsStr);
                        } catch (NumberFormatException e) {
                            updatedRescueAttempts = 0;
                        }
                    }
                    String updatedPEF = editPEF.getText().toString().trim();
                    
                    if (hasNewRedFlags) {
                        timerHandler.removeCallbacks(timerRunnable);
                        autoEscalate(childId, newCannotSpeak, newChestRetractions, newBlueGrayLips,
                                    updatedRescueAttempts, updatedPEF, triageService, dialog);
                        return;
                    }
                    
                    timerHandler.removeCallbacks(timerRunnable);
                    dialog.dismiss();
                    showTriageDialog();
                });
                
                buttonNotImproving.setOnClickListener(notImprovingV -> {
                    timerHandler.removeCallbacks(timerRunnable);
                    autoEscalate(childId, cannotSpeak, chestRetractions, blueGrayLips,
                                rescueAttempts, pef, triageService, dialog);
                });
            }
        });

        buttonCancel.setOnClickListener(v -> {
            timerHandler.removeCallbacks(timerRunnable);
            dialog.dismiss();
        });

        dialog.setOnDismissListener(dialogInterface -> {
            timerHandler.removeCallbacks(timerRunnable);
        });

        dialog.show();
    }

    private void autoEscalate(String childId, boolean cannotSpeak, boolean chestRetractions, 
                             boolean blueGrayLips, int rescueAttempts, String pef,
                             TriageService triageService, AlertDialog dialog) {
        triageService.alertParentEscalation(childId, "Symptoms not improving or new red flags appeared", 
            new TriageService.ParentAlertCallback() {
                @Override
                public void onSuccess() {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), getString(R.string.escalation_message), Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(String error) {
                    // Don't show duplicate toast on error - the success case already shows the message
                }
            });
        
        TriageIncident incident = new TriageIncident();
        incident.setChildId(childId);
        incident.setCannotSpeakFullSentences(cannotSpeak);
        incident.setChestRetractions(chestRetractions);
        incident.setBlueGrayLipsNails(blueGrayLips);
        incident.setRecentRescueAttempts(rescueAttempts);
        incident.setPeakFlowReading(pef.isEmpty() ? null : pef);
        incident.setDecision("home_steps");
        incident.setEscalated(true);
        incident.setEscalationTimestamp(System.currentTimeMillis());
        incident.setEscalationReason("Auto-escalated: Timer expired or new red flags appeared");
        incident.setUserResponse("auto_escalated_to_emergency");
        
        triageService.saveTriageIncident(incident, new TriageService.SaveCallback() {
            @Override
            public void onSuccess(String documentId) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.triage_saved), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
            }
        });
        
        dialog.dismiss();
    }

    private void saveTriageIncident(String childId, boolean cannotSpeak, boolean chestRetractions, 
                                   boolean blueGrayLips, int rescueAttempts, String pef, 
                                   String decision, String userResponse) {
        TriageIncident incident = new TriageIncident();
        incident.setChildId(childId);
        incident.setCannotSpeakFullSentences(cannotSpeak);
        incident.setChestRetractions(chestRetractions);
        incident.setBlueGrayLipsNails(blueGrayLips);
        incident.setRecentRescueAttempts(rescueAttempts);
        incident.setPeakFlowReading(pef.isEmpty() ? null : pef);
        incident.setDecision(decision);
        incident.setEscalated(false);
        incident.setUserResponse(userResponse);

        TriageService triageService = new TriageService();
        triageService.saveTriageIncident(incident, new TriageService.SaveCallback() {
            @Override
            public void onSuccess(String documentId) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.triage_saved), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.triage_error), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showPefDialog() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_pef_entry, null);

        EditText pefInput = dialogView.findViewById(R.id.editTextPef);
        RadioGroup medGroup = dialogView.findViewById(R.id.radioGroupMedType);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add PEF")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String pefText = pefInput.getText().toString().trim();

                    if (pefText.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter a PEF value", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int selectedId = medGroup.getCheckedRadioButtonId();
                    if (selectedId == -1) {
                        Toast.makeText(getContext(), "Please select pre-med or post-med", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String medType = (selectedId == R.id.radioPreMed) ? "pre" : "post";

                    int pefValue;
                    try {
                        pefValue = Integer.parseInt(pefText);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getContext(), "PEF must be a number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (pefValue <=0 || pefValue > 800) {
                        Toast.makeText(getContext(), "PEF must be a number between 0 and 800", Toast.LENGTH_SHORT).show();
                        return;
                    }



                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String uid = ImpersonationService.getActiveChildId(requireContext());

                    String timestamp = String.valueOf(System.currentTimeMillis());
                    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                    String time = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                    String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    db.collection("users")
                            .document(uid)
                            .collection("pefLogs")
                            .document(timestamp)
                            .set(new HashMap<String, Object>() {{
                                put("pef", pefValue);
                                put("medType", medType);
                                put("date", date);
                                put("time", time);
                            }})
                            .addOnSuccessListener(a -> {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(),
                                                "PEF saved (" + medType + "-med)",
                                                Toast.LENGTH_SHORT).show();
                                    }
                            })
                            .addOnFailureListener(e -> {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(),
                                                "Error saving PEF",
                                                Toast.LENGTH_SHORT).show();
                                    }
                            });

                    db.collection("users")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(snapshot -> {

                                Long existingPb = snapshot.getLong("pb");
                                long newPb = (existingPb == null) ? pefValue :
                                        Math.max(existingPb, pefValue);

                                // First update PB
                                db.collection("users")
                                        .document(uid)
                                        .update("pb", newPb)
                                        .addOnSuccessListener(unused -> {

                                            // Compute correct zone using updated PB
                                            String zone = ZoneCalculator.computeZone(pefValue, (int) newPb);

                                            String childId = uid;
                                            String newZone = zone;
                                            int pef = (int) pefValue;
                                            long now = System.currentTimeMillis();

                                            ZoneLog newLog = new ZoneLog(childId, newZone, pef, now);

                                            db.collection("zoneLogs")
                                                    .orderBy("timestamp", Query.Direction.DESCENDING)
                                                    .limit(1)
                                                    .get()
                                                    .addOnSuccessListener(docsnapshot -> {

                                                        // No previous log - always save the first one
                                                        if (docsnapshot.isEmpty()) {
                                                            db.collection("zoneLogs")
                                                                    .add(newLog);
                                                            return;
                                                        }

                                                        ZoneLog last = docsnapshot.getDocuments().get(0).toObject(ZoneLog.class);

                                                        assert last != null;
                                                        if (last.getZone() != null &&
                                                                last.getZone().equals(newZone)) {
                                                            return;
                                                        }

                                                        // Zone changed - SAVE new log
                                                        db.collection("zoneLogs")
                                                                .add(newLog);
                                                    });


                                            // Save zone and last zone date
                                            db.collection("users")
                                                    .document(uid)
                                                    .update("zone", zone, "lastZoneDate", today);

                                            // Update UI
                                            zoneText.setText("Zone: " + zone);
                                            updateZoneUI(zone);
                                        });
                            });

                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateZoneUI(String zone) {
        switch (zone) {
            case "Green":
                zoneText.setTextColor(Color.parseColor("#2ecc71")); // green
                break;
            case "Yellow":
                zoneText.setTextColor(Color.parseColor("#f1c40f")); // yellow
                break;
            case "Red":
                zoneText.setTextColor(Color.parseColor("#e74c3c")); // red
                break;
        }
    }

    private void startUserListener() {
        // Use impersonation-aware active child id so parents viewing a child
        // see the child's zone/PEF data rather than the signed-in parent's document.
        String uid = ImpersonationService.getActiveChildId(requireContext());
        if (uid == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        userListener = db.collection("users")
                .document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {

                        String zone = snapshot.getString("zone");
                        String lastZoneDate = snapshot.getString("lastZoneDate");

                        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                .format(new Date());

                        if (lastZoneDate == null || !lastZoneDate.equals(today)) {
                            // New day — reset zone
                            zoneText.setText("Zone: --");
                            updateZoneUI("none");

                            // reset value in Firestore too
                            db.collection("users")
                                    .document(uid)
                                    .update("zone", null,
                                            "lastZoneDate", today);
                        } else {
                            // Same day — show stored zone
                            if (zone != null) {
                                zoneText.setText("Zone: " + zone);
                                updateZoneUI(zone);
                            } else {
                                zoneText.setText("Zone: --");
                                updateZoneUI("none");
                            }
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (userListener != null) {
            userListener.remove();
        }
    }

    // Motivation System Methods
    private void showMotivationDialog() {
        try {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_motivation_progress, null);
            
            TextView controllerStreakCount = dialogView.findViewById(R.id.textControllerStreakCount);
            TextView controllerBest = dialogView.findViewById(R.id.textControllerBest);
            TextView techniqueStreakCount = dialogView.findViewById(R.id.textTechniqueStreakCount);
            TextView techniqueBest = dialogView.findViewById(R.id.textTechniqueBest);
            LinearLayout badgesLayout = dialogView.findViewById(R.id.layoutBadges);
            Button settingsButton = dialogView.findViewById(R.id.buttonMotivationSettings);
            Button closeButton = dialogView.findViewById(R.id.buttonClose);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            // Set loading values first
            controllerStreakCount.setText("Calculating...");
            controllerBest.setText("Calculating...");
            techniqueStreakCount.setText("Calculating...");
            techniqueBest.setText("Calculating...");

            // Calculate and load data from actual health logs if service is available
            if (motivationService != null && ImpersonationService.getActiveChildId(requireContext()) != null) {
                String childId = ImpersonationService.getActiveChildId(requireContext());
                
                // Only calculate if enough time has passed since last calculation
                long currentTime = System.currentTimeMillis();
                if (!isCalculatingMotivation && (currentTime - lastMotivationCalculation) > MOTIVATION_CALCULATION_COOLDOWN) {
                    isCalculatingMotivation = true;
                    lastMotivationCalculation = currentTime;
                    
                    // Calculate streaks and badges from actual health logs
                    motivationService.calculateStreaksFromLogs(childId);
                    motivationService.calculateBadgesFromLogs(childId);
                    
                    // Load data immediately first, then refresh after calculations
                    loadStreaksData(controllerStreakCount, controllerBest, techniqueStreakCount, techniqueBest);
                    loadBadgesData(badgesLayout);
                    
                    // Refresh data after calculations complete (only once)
                    new android.os.Handler().postDelayed(() -> {
                        if (isAdded()) {
                            loadStreaksData(controllerStreakCount, controllerBest, techniqueStreakCount, techniqueBest);
                            loadBadgesData(badgesLayout);
                        }
                        isCalculatingMotivation = false; // Reset flag
                    }, 2000); // 2 second delay to allow calculations to complete
                } else {
                    // Just load existing data without recalculating
                    loadStreaksData(controllerStreakCount, controllerBest, techniqueStreakCount, techniqueBest);
                    loadBadgesData(badgesLayout);
                }
            } else {
                // Set default values if service is not available
                controllerStreakCount.setText("0 days");
                controllerBest.setText("Best: 0");
                techniqueStreakCount.setText("0 days");
                techniqueBest.setText("Best: 0");
            }

            settingsButton.setOnClickListener(v -> {
                dialog.dismiss();
                if (motivationService != null) {
                    showMotivationSettingsDialog();
                } else {
                    Toast.makeText(requireContext(), "Settings temporarily unavailable", Toast.LENGTH_SHORT).show();
                }
            });

            closeButton.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open motivation dialog", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadStreaksData(TextView controllerCount, TextView controllerBest, 
                                TextView techniqueCount, TextView techniqueBest) {
        try {
            if (motivationService == null || ImpersonationService.getActiveChildId(requireContext()) == null) {
                return;
            }
            String childId = ImpersonationService.getActiveChildId(requireContext());
            
            motivationService.getChildStreaks(childId, new MotivationService.StreakCallback() {
                @Override
                public void onStreaksLoaded(List<Streak> streaks) {
                    if (isAdded()) {
                        // Set default values first
                        controllerCount.setText("0 days");
                        controllerBest.setText("Best: 0");
                        techniqueCount.setText("0 days");
                        techniqueBest.setText("Best: 0");
                        
                        // Update with actual data if available
                        if (streaks != null) {
                            for (Streak streak : streaks) {
                                if (streak != null) {
                                    if ("controller_planned".equals(streak.getStreakType())) {
                                        controllerCount.setText(streak.getCurrentCount() + " days");
                                        controllerBest.setText("Best: " + streak.getBestCount());
                                    } else if ("technique_completed".equals(streak.getStreakType())) {
                                        techniqueCount.setText(streak.getCurrentCount() + " days");
                                        techniqueBest.setText("Best: " + streak.getBestCount());
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        // Set default values on error
                        controllerCount.setText("0 days");
                        controllerBest.setText("Best: 0");
                        techniqueCount.setText("0 days");
                        techniqueBest.setText("Best: 0");
                    }
                }
            });
        } catch (Exception e) {
            // Silent error - keep default values
        }
    }

    private void loadBadgesData(LinearLayout badgesLayout) {
        try {
            if (motivationService == null || ImpersonationService.getActiveChildId(requireContext()) == null) {
                return;
            }
            
            String childId = ImpersonationService.getActiveChildId(requireContext());
            
            motivationService.getChildBadges(childId, new MotivationService.BadgeCallback() {
                @Override
                public void onBadgesLoaded(List<Badge> badges) {
                    if (isAdded() && badgesLayout != null) {
                        badgesLayout.removeAllViews();
                        if (badges != null) {
                            for (Badge badge : badges) {
                                if (badge != null) {
                                    try {
                                        View badgeView = createBadgeView(badge);
                                        if (badgeView != null) {
                                            badgesLayout.addView(badgeView);
                                        }
                                    } catch (Exception e) {
                                        // Skip this badge if there's an error
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    // Silent error - don't interrupt user experience
                }
            });
        } catch (Exception e) {
            // Silent error - keep empty badges layout
        }
    }

    private View createBadgeView(Badge badge) {
        try {
            View badgeView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
            TextView titleText = badgeView.findViewById(android.R.id.text1);
            TextView descriptionText = badgeView.findViewById(android.R.id.text2);
            
            if (titleText != null && descriptionText != null) {
                String title = badge.getTitle() != null ? badge.getTitle() : "Badge";
                String description = badge.getDescription() != null ? badge.getDescription() : "Achievement";
                
                titleText.setText((badge.isUnlocked() ? "🏆 " : "🔒 ") + title);
                
                if (badge.isUnlocked()) {
                    descriptionText.setText("Earned! " + description);
                    try {
                        titleText.setTextColor(getResources().getColor(R.color.badge_earned));
                    } catch (Exception e) {
                        // Fallback color
                        titleText.setTextColor(0xFFD700);
                    }
                } else {
                    int progress = Math.min(badge.getProgress(), badge.getTargetValue());
                    descriptionText.setText("Progress: " + progress + "/" + badge.getTargetValue() + " - " + description);
                    try {
                        titleText.setTextColor(getResources().getColor(R.color.badge_locked));
                    } catch (Exception e) {
                        // Fallback color
                        titleText.setTextColor(0xCCCCCC);
                    }
                }
            }
            
            return badgeView;
        } catch (Exception e) {
            // Return null if badge creation fails
            return null;
        }
    }

    private void showMotivationSettingsDialog() {
        try {
            if (motivationService == null) {
                Toast.makeText(requireContext(), "Settings unavailable", Toast.LENGTH_SHORT).show();
                return;
            }
            
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_motivation_settings, null);
            
            EditText controllerThreshold = dialogView.findViewById(R.id.editControllerStreakThreshold);
            EditText techniqueThreshold = dialogView.findViewById(R.id.editTechniqueStreakThreshold);
            EditText perfectWeekDays = dialogView.findViewById(R.id.editPerfectWeekDays);
            EditText techniqueSessions = dialogView.findViewById(R.id.editTechniqueSessions);
            EditText lowRescueLimit = dialogView.findViewById(R.id.editLowRescueLimit);
            EditText lowRescuePeriod = dialogView.findViewById(R.id.editLowRescuePeriod);
            CheckBox streakNotifications = dialogView.findViewById(R.id.checkStreakNotifications);
            CheckBox badgeNotifications = dialogView.findViewById(R.id.checkBadgeNotifications);
            CheckBox weeklyProgress = dialogView.findViewById(R.id.checkWeeklyProgress);
            Button saveButton = dialogView.findViewById(R.id.buttonSave);
            Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            // Set default values
            setDefaultSettingsValues(controllerThreshold, techniqueThreshold, perfectWeekDays, 
                                   techniqueSessions, lowRescueLimit, lowRescuePeriod,
                                   streakNotifications, badgeNotifications, weeklyProgress);

            // Load current settings if possible
            if (ImpersonationService.getActiveChildId(requireContext()) != null) {
                String childId = ImpersonationService.getActiveChildId(requireContext());
                loadCurrentSettings(childId, controllerThreshold, techniqueThreshold, perfectWeekDays,
                                  techniqueSessions, lowRescueLimit, lowRescuePeriod,
                                  streakNotifications, badgeNotifications, weeklyProgress);
            }

            saveButton.setOnClickListener(v -> saveSettings(dialog, controllerThreshold, techniqueThreshold, 
                                                           perfectWeekDays, techniqueSessions, lowRescueLimit, 
                                                           lowRescuePeriod, streakNotifications, badgeNotifications, weeklyProgress));

            cancelButton.setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStreaksBasedOnActivity(String activityType, boolean successful) {
        try {
            if (motivationService == null || ImpersonationService.getActiveChildId(requireContext()) == null) {
                return;
            }
            
            String childId = ImpersonationService.getActiveChildId(requireContext());
            
            if ("controller_medicine".equals(activityType)) {
                motivationService.updateControllerStreak(childId, successful, new MotivationService.MotivationCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (isAdded() && message.contains("🎉") && !message.isEmpty()) {
                            // Only show streak celebration toasts, not regular updates
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                        // Don't check for badge updates here to prevent repeated calls
                    }

                    @Override
                    public void onError(String error) {
                        // Silent error
                    }
                });
            } else if ("breathing_technique".equals(activityType)) {
                motivationService.updateTechniqueStreak(childId, successful, new MotivationService.MotivationCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (isAdded() && message.contains("🎉") && !message.isEmpty()) {
                            // Only show streak celebration toasts, not regular updates
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                        // Don't check for badge updates here to prevent repeated calls
                    }

                    @Override
                    public void onError(String error) {
                        // Silent error
                    }
                });
            }
        } catch (Exception e) {
            // Silent error - don't interrupt user flow
        }
    }

    private void checkForBadgeUpdates(String childId) {
        try {
            if (motivationService != null && !isCalculatingMotivation) {
                motivationService.checkBadgeProgress(childId, new MotivationService.MotivationCallback() {
                    @Override
                    public void onSuccess(String message) {
                        if (isAdded() && message.contains("Badge earned") && !message.isEmpty()) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        // Silent error
                    }
                });
            }
        } catch (Exception e) {
            // Silent error
        }
    }

    private void initializeMotivationForNewUser() {
        try {
            if (motivationService != null && ImpersonationService.getActiveChildId(requireContext()) != null) {
                String childId = ImpersonationService.getActiveChildId(requireContext());
                motivationService.initializeMotivationForChild(childId, new MotivationService.MotivationCallback() {
                    @Override
                    public void onSuccess(String message) {
                        // Silent success
                    }

                    @Override
                    public void onError(String error) {
                        // Silent error
                    }
                });
            }
        } catch (Exception e) {
            // Silent error
        }
    }

    private void setDefaultSettingsValues(EditText controllerThreshold, EditText techniqueThreshold, EditText perfectWeekDays,
                                        EditText techniqueSessions, EditText lowRescueLimit, EditText lowRescuePeriod,
                                        CheckBox streakNotifications, CheckBox badgeNotifications, CheckBox weeklyProgress) {
        try {
            controllerThreshold.setText("7");
            techniqueThreshold.setText("7");
            perfectWeekDays.setText("7");
            techniqueSessions.setText("10");
            lowRescueLimit.setText("4");
            lowRescuePeriod.setText("30");
            streakNotifications.setChecked(true);
            badgeNotifications.setChecked(true);
            weeklyProgress.setChecked(true);
        } catch (Exception e) {
            // Ignore errors in setting defaults
        }
    }

    private void loadCurrentSettings(String childId, EditText controllerThreshold, EditText techniqueThreshold, EditText perfectWeekDays,
                                   EditText techniqueSessions, EditText lowRescueLimit, EditText lowRescuePeriod,
                                   CheckBox streakNotifications, CheckBox badgeNotifications, CheckBox weeklyProgress) {
        try {
            motivationService.getMotivationSettings(childId, new MotivationService.SettingsCallback() {
                @Override
                public void onSettingsLoaded(MotivationSettings settings) {
                    if (isAdded() && settings != null) {
                        try {
                            controllerThreshold.setText(String.valueOf(settings.getControllerStreakThreshold()));
                            techniqueThreshold.setText(String.valueOf(settings.getTechniqueStreakThreshold()));
                            perfectWeekDays.setText(String.valueOf(settings.getPerfectControllerWeekDays()));
                            techniqueSessions.setText(String.valueOf(settings.getTechniqueMasterSessions()));
                            lowRescueLimit.setText(String.valueOf(settings.getLowRescueMonthLimit()));
                            lowRescuePeriod.setText(String.valueOf(settings.getLowRescueMonthDays()));
                            streakNotifications.setChecked(settings.isStreakNotificationsEnabled());
                            badgeNotifications.setChecked(settings.isBadgeNotificationsEnabled());
                            weeklyProgress.setChecked(settings.isWeeklyProgressEnabled());
                        } catch (Exception e) {
                            // Keep default values if there's an error
                        }
                    }
                }

                @Override
                public void onError(String error) {
                    // Keep default values on error
                }
            });
        } catch (Exception e) {
            // Keep default values if loading fails
        }
    }

    private void saveSettings(AlertDialog dialog, EditText controllerThreshold, EditText techniqueThreshold, EditText perfectWeekDays,
                            EditText techniqueSessions, EditText lowRescueLimit, EditText lowRescuePeriod,
                            CheckBox streakNotifications, CheckBox badgeNotifications, CheckBox weeklyProgress) {
        try {
            String childId = ImpersonationService.getActiveChildId(requireContext());
            if (childId == null) {
                Toast.makeText(requireContext(), "Please select a child", Toast.LENGTH_SHORT).show();
                return;
            }
            MotivationSettings settings = new MotivationSettings(childId);
            
            // Parse values with defaults
            settings.setControllerStreakThreshold(parseIntWithDefault(controllerThreshold.getText().toString(), 7));
            settings.setTechniqueStreakThreshold(parseIntWithDefault(techniqueThreshold.getText().toString(), 7));
            settings.setPerfectControllerWeekDays(parseIntWithDefault(perfectWeekDays.getText().toString(), 7));
            settings.setTechniqueMasterSessions(parseIntWithDefault(techniqueSessions.getText().toString(), 10));
            settings.setLowRescueMonthLimit(parseIntWithDefault(lowRescueLimit.getText().toString(), 4));
            settings.setLowRescueMonthDays(parseIntWithDefault(lowRescuePeriod.getText().toString(), 30));
            settings.setStreakNotificationsEnabled(streakNotifications.isChecked());
            settings.setBadgeNotificationsEnabled(badgeNotifications.isChecked());
            settings.setWeeklyProgressEnabled(weeklyProgress.isChecked());

            motivationService.saveMotivationSettings(settings, new MotivationService.MotivationCallback() {
                @Override
                public void onSuccess(String message) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Settings saved!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }

                @Override
                public void onError(String error) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "Error saving settings", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Please check your input values", Toast.LENGTH_SHORT).show();
        }
    }

    private int parseIntWithDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // Test method to create sample motivation data - you can call this from a button
    private void createTestMotivationData() {
        if (motivationService == null || ImpersonationService.getActiveChildId(requireContext()) == null) {
            Toast.makeText(requireContext(), "Service not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String childId = ImpersonationService.getActiveChildId(requireContext());
        
        // Initialize motivation system for child
        motivationService.initializeMotivationForChild(childId, new MotivationService.MotivationCallback() {
            @Override
            public void onSuccess(String message) {
                // Create some test medicine logs for the past few days
                createTestMedicineLogs(childId);
                Toast.makeText(requireContext(), "Test data created! Try View Progress now.", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(requireContext(), "Error creating test data: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createTestMedicineLogs(String childId) {
        // Create medicine logs for the past 5 days to test streaks
        long currentTime = System.currentTimeMillis();
        long oneDayMs = 24 * 60 * 60 * 1000;

        for (int i = 0; i < 5; i++) {
            long timestamp = currentTime - (i * oneDayMs);
            
            // Create controller medicine log for test collection
            MedicineLog controllerLog = new MedicineLog();
            controllerLog.setChildId(childId);
            controllerLog.setMedicineType("Controller");
            controllerLog.setMedicineName("Test Controller Medicine");
            controllerLog.setDosage("1 puff");
            controllerLog.setTimestamp(timestamp);
            controllerLog.setNotes("Test data");

            // Save to Firestore test collection
            FirebaseFirestore.getInstance().collection("medicineLogs").add(controllerLog);
            
            // Also create data in the real collection for streaks
            MedicineLog realControllerLog = new MedicineLog();
            realControllerLog.setChildId(childId);
            realControllerLog.setMedicineType("controller"); // lowercase for real system
            realControllerLog.setMedicineName("Test Controller Medicine");
            realControllerLog.setDosage("1 puff");
            realControllerLog.setTimestamp(timestamp);
            realControllerLog.setNotes("Test controller data for streaks");
            
            FirebaseFirestore.getInstance().collection("medicineLog").add(realControllerLog);

            // Create rescue medicine log for some days (to test badge)
            if (i < 2) { // Only for first 2 days
                MedicineLog rescueLog = new MedicineLog();
                rescueLog.setChildId(childId);
                rescueLog.setMedicineType("Rescue");
                rescueLog.setMedicineName("Test Rescue Medicine");
                rescueLog.setDosage("2 puffs");
                rescueLog.setTimestamp(timestamp + 3600000); // 1 hour later
                rescueLog.setNotes("Test rescue data");

                FirebaseFirestore.getInstance().collection("medicineLogs").add(rescueLog);
                
                // Also add to the real medicineLog collection for dashboard testing
                MedicineLog realRescueLog = new MedicineLog();
                realRescueLog.setChildId(childId);
                realRescueLog.setMedicineType("rescue"); // lowercase for real system
                realRescueLog.setMedicineName("Test Rescue Medicine");
                realRescueLog.setDosage("2 puffs");
                realRescueLog.setTimestamp(timestamp + 3600000); // 1 hour later
                realRescueLog.setNotes("Test rescue data for dashboard");
                
                FirebaseFirestore.getInstance().collection("medicineLog").add(realRescueLog);
            }
        }
    }
}

package com.example.b07demosummer2024.fragments;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.b07demosummer2024.R;
import com.example.b07demosummer2024.models.TechniqueLog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TechniqueHelperFragment extends DialogFragment {

    private int currentStep = 0;
    private TextView stepTextView;
    private Button nextButton;
    private VideoView videoView;

    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable videoRunnable;

    private final String[] steps = {
        "1. Remove the cap from your inhaler.",
        "2. If this is your first time using the inhaler:\nSpray the medicine into the air 2-4 times.",
        "3. Attach to the spacer if you use one.",
        "4. Shake the inhaler.\nGive it a good shake for 5-10 seconds.",
        "5. Breathe out gently.\nA small breath out before you start.",
        "6. If you use a mask, place it gently over your nose and mouth. If not, put your lips around the mouthpiece and make a tight seal.",
        "7. Press the inhaler once.\nThis gives one puff of medicine. Take a slow, deep breath in.",
        "8. Hold your breath for about 10 seconds.\nOr as long as you comfortably can.",
        "9. Breathe out softly.",
        "10. Wait 30–60 seconds before the next puff if needed.\nYour asthma plan or doctor will say how many puffs to take."
    };

    // Timestamps for each step in milliseconds [start, end]
    private final int[][] timestamps = {
            {58000, 60000},   // 1. Cap
            {63000, 67000},   // 2. First time using
            {71500, 74000},   // 3. Attach to spacer
            {76000, 78000},   // 4. Shake
            {29000, 32000},   // 5. Breathe out (recheck timestamps)
            {81000, 83000},   // 6. Seal lips
            {83000, 88000},   // 7. Press and breathe in
            {101000, 107000}, // 8. Hold breath
            {108000, 110000}, // 9. Breathe out softly (recheck timestamps)
            {111000, 114000}  // 10. Wait (this one should be fine)
    };

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_technique_helper, null);

        stepTextView = view.findViewById(R.id.text_technique_step);
        nextButton = view.findViewById(R.id.button_technique_next);
        videoView = view.findViewById(R.id.video_view_technique);

        // Set up the video path
        String videoPath = "android.resource://" + requireActivity().getPackageName() + "/" + R.raw.inhaler_technique;
        Uri uri = Uri.parse(videoPath);
        videoView.setVideoURI(uri);

        // Wait for the video to be ready before playing the first segment
        videoView.setOnPreparedListener(mp -> {
            updateStep();
        });

        nextButton.setOnClickListener(v -> {
            currentStep++;
            if (currentStep < steps.length) {
                updateStep();
            } else {
                logTechnique();
                dismiss();
            }
        });

        builder.setView(view)
               .setTitle("Technique Helper");

        return builder.create();
    }

    private void updateStep() {
        stepTextView.setText(steps[currentStep]);
        if (currentStep < timestamps.length) {
            playVideoSegment(timestamps[currentStep][0], timestamps[currentStep][1]);
        }
        if (currentStep == steps.length - 1) {
            nextButton.setText("Finish");
        }
    }

    private void playVideoSegment(int startTime, int endTime) {
        // Stop any previous looping task
        if (videoRunnable != null) {
            handler.removeCallbacks(videoRunnable);
        }

        videoView.seekTo(startTime);
        videoView.start();

        videoRunnable = new Runnable() {
            @Override
            public void run() {
                if (videoView.getCurrentPosition() >= endTime) {
                    videoView.seekTo(startTime); // Loop back to the start
                }
                handler.postDelayed(this, 100); // Check again in 100ms
            }
        };

        handler.post(videoRunnable);
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // Clean up the handler when the dialog is dismissed to prevent memory leaks
        if (handler != null && videoRunnable != null) {
            handler.removeCallbacks(videoRunnable);
        }
    }

    private void logTechnique() {
        // Use impersonation helper so parents viewing a child's profile log technique for the child
        String userId = com.example.b07demosummer2024.auth.ImpersonationService.getActiveChildId(getContext());
        if (userId == null) return; // Not available
        long timestamp = System.currentTimeMillis();

        TechniqueLog log = new TechniqueLog(timestamp, userId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("techniqueLogs").add(log)
            .addOnSuccessListener(documentReference -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Technique practice logged!", Toast.LENGTH_SHORT).show();
                    
                    // Trigger motivation streak update for technique completion (counts once per day)
                    try {
                        com.example.b07demosummer2024.services.MotivationService motivationService =
                                new com.example.b07demosummer2024.services.MotivationService();
                        motivationService.updateTechniqueStreak(userId, true, new com.example.b07demosummer2024.services.MotivationService.MotivationCallback() {
                            @Override
                            public void onSuccess(String message) {
                                // optional: show celebration toast if present
                            }

                            @Override
                            public void onError(String error) {
                                android.util.Log.w("TechniqueHelper", "Could not update technique streak: " + error);
                            }
                        });
                    } catch (Exception e) {
                        // Ignore motivation service errors
                        android.util.Log.w("TechniqueHelper", "Could not update motivation streaks", e);
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Failed to log technique.", Toast.LENGTH_SHORT).show();
                }
            });
    }
}

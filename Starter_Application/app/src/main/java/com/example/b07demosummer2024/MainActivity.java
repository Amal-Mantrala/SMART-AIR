package com.example.b07demosummer2024;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.auth.AuthService;
import com.example.b07demosummer2024.fragments.ChildHomeFragment;
import com.example.b07demosummer2024.fragments.ParentHomeFragment;
import com.example.b07demosummer2024.fragments.ProviderHomeFragment;
import com.example.b07demosummer2024.fragments.RoleSelectionFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.b07demosummer2024.fragments.LoginFragment;
import com.example.b07demosummer2024.fragments.SignupFragment;
import com.example.b07demosummer2024.services.NotificationService;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseDatabase.getInstance("https://b07-demo-summer-2024-default-rtdb.firebaseio.com/");
        DatabaseReference myRef = db.getReference("testDemo");
        myRef.child("movies").setValue("B07 Demo!");

        NotificationService.getInstance().initialize(this);

        if (savedInstanceState == null) {
            checkAuthAndLoadFragment();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null && !(currentFragment instanceof LoginFragment) && !(currentFragment instanceof SignupFragment)) {
            AuthService authService = new AuthService();
            if (!authService.isSignedIn()) {
                // User is not signed in - redirect to login
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commit();
            }
        }
    }

    private void checkAuthAndLoadFragment() {
        AuthService authService = new AuthService();
        if (!authService.isSignedIn()) {
            // User is not signed in - show login fragment
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commitNow(); // Use commitNow for immediate execution
            return;
        }

        // User is signed in: fetch their role from Firebase and navigate accordingly
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commitNow(); // Use commitNow for immediate execution
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        // Show LoginFragment as placeholder while fetching from Firebase
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commitNow(); // Use commitNow for immediate execution

        // Always fetch latest data from Firestore
        com.google.firebase.firestore.FirebaseFirestore firestoreDb = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        firestoreDb.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        // Show role selector if fetch failed
                        RoleSelectionFragment roleDialog = RoleSelectionFragment.newInstance("");
                        roleDialog.show(getSupportFragmentManager(), "roleSelection");
                        return;
                    }
                    
                    com.google.firebase.firestore.DocumentSnapshot document = task.getResult();
                    if (document == null || !document.exists()) {
                        // No user data yet — prompt selection
                        RoleSelectionFragment roleDialog = RoleSelectionFragment.newInstance("");
                        roleDialog.show(getSupportFragmentManager(), "roleSelection");
                        return;
                    }
                    
                    String role = document.getString("role");
                    String name = document.getString("name");
                    
                    if (role == null) {
                        // No role yet — prompt selection
                        RoleSelectionFragment roleDialog = RoleSelectionFragment.newInstance(name != null ? name : "");
                        roleDialog.show(getSupportFragmentManager(), "roleSelection");
                        return;
                    }
                    
                    // Navigate to appropriate role homepage
                    navigateToRoleHome(role);
                });
    }

    private void navigateToRoleHome(String role) {
        Fragment target;
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
                // Unknown role - show role selection
                RoleSelectionFragment roleDialog = RoleSelectionFragment.newInstance("");
                roleDialog.show(getSupportFragmentManager(), "roleSelection");
                return;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, target)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
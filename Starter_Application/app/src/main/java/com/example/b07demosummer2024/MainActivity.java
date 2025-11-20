package com.example.b07demosummer2024;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.auth.AuthService;
import com.example.b07demosummer2024.fragments.HomeFragment;
import com.example.b07demosummer2024.fragments.ChildHomeFragment;
import com.example.b07demosummer2024.fragments.ParentHomeFragment;
import com.example.b07demosummer2024.fragments.ProviderHomeFragment;
import com.example.b07demosummer2024.fragments.RoleSelectionFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.b07demosummer2024.fragments.LoginFragment;

public class MainActivity extends AppCompatActivity {

    FirebaseDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseDatabase.getInstance("https://b07-demo-summer-2024-default-rtdb.firebaseio.com/");
        DatabaseReference myRef = db.getReference("testDemo");
        myRef.child("movies").setValue("B07 Demo!");

        if (savedInstanceState == null) {
            checkAuthAndLoadFragment();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (currentFragment != null && !(currentFragment instanceof LoginFragment)) {
            AuthService authService = new AuthService();
            if (!authService.isSignedIn()) {
                // Clear any cached role data when user is not signed in
                android.content.SharedPreferences prefs = getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
                prefs.edit().clear().apply();
                
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commit();
            }
        }
    }

    private void checkAuthAndLoadFragment() {
        AuthService authService = new AuthService();
        if (!authService.isSignedIn()) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
            return;
        }

        // User is signed in: fetch their role and navigate accordingly
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
            return;
        }

        String uid = auth.getCurrentUser().getUid();

        // Try cached role first for instant routing
        android.content.SharedPreferences prefs = getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE);
        String cachedRole = prefs.getString("user_role_" + uid, null);
        if (cachedRole != null) {
            navigateToRoleHome(cachedRole);
        } else {
            // No cached role - show temporary home while fetching
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // Always also fetch latest role from remote and correct if needed
        db.getReference("users").child(uid).child("role").get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        if (cachedRole == null) {
                            // Show role selector if nothing cached
                            new RoleSelectionFragment().show(getSupportFragmentManager(), "roleSelection");
                        }
                        return;
                    }
                    Object value = task.getResult() != null ? task.getResult().getValue() : null;
                    if (value == null) {
                        // No role yet — prompt selection
                        new RoleSelectionFragment().show(getSupportFragmentManager(), "roleSelection");
                        return;
                    }
                    String role = String.valueOf(value);
                    // If remote differs from cached, navigate to the correct one
                    if (!role.equals(cachedRole)) {
                        // Update cache
                        prefs.edit().putString("user_role_" + uid, role).apply();
                        navigateToRoleHome(role);
                    }
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
                target = new HomeFragment();
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
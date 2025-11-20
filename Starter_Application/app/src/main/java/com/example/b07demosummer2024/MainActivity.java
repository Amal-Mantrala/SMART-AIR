package com.example.b07demosummer2024;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.b07demosummer2024.auth.AuthService;
import com.example.b07demosummer2024.fragments.HomeFragment;
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
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commit();
            }
        }
    }

    private void checkAuthAndLoadFragment() {
        AuthService authService = new AuthService();
        Fragment fragment;
        if (authService.isSignedIn()) {
            fragment = new HomeFragment();
        } else {
            fragment = new LoginFragment();
        }
        
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
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
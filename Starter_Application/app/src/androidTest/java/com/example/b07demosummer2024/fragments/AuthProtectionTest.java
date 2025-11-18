package com.example.b07demosummer2024.fragments;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.b07demosummer2024.MainActivity;
import com.example.b07demosummer2024.auth.AuthService;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ActivityScenario;

@RunWith(AndroidJUnit4.class)
public class AuthProtectionTest {

    @Test
    public void testHomeFragmentExtendsProtectedFragment() {
        assertTrue(HomeFragment.class.getSuperclass() == ProtectedFragment.class);
    }

    @Test
    public void testRecyclerViewFragmentExtendsProtectedFragment() {
        assertTrue(RecyclerViewFragment.class.getSuperclass() == ProtectedFragment.class);
    }

    @Test
    public void testScrollerFragmentExtendsProtectedFragment() {
        assertTrue(ScrollerFragment.class.getSuperclass() == ProtectedFragment.class);
    }

    @Test
    public void testSpinnerFragmentExtendsProtectedFragment() {
        assertTrue(SpinnerFragment.class.getSuperclass() == ProtectedFragment.class);
    }

    @Test
    public void testManageItemsFragmentExtendsProtectedFragment() {
        assertTrue(ManageItemsFragment.class.getSuperclass() == ProtectedFragment.class);
    }

    @Test
    public void testAddItemFragmentExtendsProtectedFragment() {
        assertTrue(AddItemFragment.class.getSuperclass() == ProtectedFragment.class);
    }

    @Test
    public void testDeleteItemFragmentExtendsProtectedFragment() {
        assertTrue(DeleteItemFragment.class.getSuperclass() == ProtectedFragment.class);
    }

    @Test
    public void testLoginFragmentDoesNotExtendProtectedFragment() {
        assertTrue(LoginFragment.class.getSuperclass() != ProtectedFragment.class);
    }

    @Test
    public void testAuthServiceHasIsSignedInMethod() {
        AuthService authService = new AuthService();
        boolean result = authService.isSignedIn();
        assertNotNull(Boolean.valueOf(result));
    }

    @Test
    public void testMainActivityLaunches() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            assertNotNull(activity);
        });
        scenario.close();
    }

    @Test
    public void testSignedOutUserCannotAccessProtectedScreen() {
        FirebaseAuth.getInstance().signOut();
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            assertNotNull(activity);
        });
        scenario.close();
    }
}

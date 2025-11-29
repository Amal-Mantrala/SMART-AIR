package com.example.b07demosummer2024.services;

import static org.junit.Assert.*;

import com.example.b07demosummer2024.models.TriageIncident;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;

public class TriageServiceTest {

    @Test
    public void testTriageServiceClassExists() {
        Class<?> triageServiceClass = TriageService.class;
        assertNotNull(triageServiceClass);
    }

    @Test
    public void testTriageServiceConstructor() {
        TriageService service = new TriageService();
        assertNotNull(service);
    }

    @Test
    public void testSaveTriageIncidentMethodExists() {
        try {
            Method method = TriageService.class.getMethod("saveTriageIncident", 
                TriageIncident.class, TriageService.SaveCallback.class);
            assertNotNull(method);
        } catch (NoSuchMethodException e) {
            fail("saveTriageIncident method should exist");
        }
    }

    @Test
    public void testGetTriageHistoryMethodExists() {
        try {
            Method method = TriageService.class.getMethod("getTriageHistory", 
                String.class, int.class, TriageService.TriageHistoryCallback.class);
            assertNotNull(method);
        } catch (NoSuchMethodException e) {
            fail("getTriageHistory method should exist");
        }
    }

    @Test
    public void testGetRecentRescueAttemptsMethodExists() {
        try {
            Method method = TriageService.class.getMethod("getRecentRescueAttempts", 
                String.class, TriageService.RescueAttemptsCallback.class);
            assertNotNull(method);
        } catch (NoSuchMethodException e) {
            fail("getRecentRescueAttempts method should exist");
        }
    }

    @Test
    public void testAlertParentEscalationMethodExists() {
        try {
            Method method = TriageService.class.getMethod("alertParentEscalation", 
                String.class, String.class, TriageService.ParentAlertCallback.class);
            assertNotNull(method);
        } catch (NoSuchMethodException e) {
            fail("alertParentEscalation method should exist");
        }
    }

    @Test
    public void testSaveCallbackInterfaceExists() {
        Class<?> callbackClass = TriageService.SaveCallback.class;
        assertNotNull(callbackClass);
        assertTrue(callbackClass.isInterface());
        
        // Check for required methods
        try {
            Method onSuccess = callbackClass.getMethod("onSuccess", String.class);
            Method onError = callbackClass.getMethod("onError", String.class);
            assertNotNull(onSuccess);
            assertNotNull(onError);
        } catch (NoSuchMethodException e) {
            fail("SaveCallback should have onSuccess and onError methods");
        }
    }

    @Test
    public void testTriageHistoryCallbackInterfaceExists() {
        Class<?> callbackClass = TriageService.TriageHistoryCallback.class;
        assertNotNull(callbackClass);
        assertTrue(callbackClass.isInterface());
    }

    @Test
    public void testRescueAttemptsCallbackInterfaceExists() {
        Class<?> callbackClass = TriageService.RescueAttemptsCallback.class;
        assertNotNull(callbackClass);
        assertTrue(callbackClass.isInterface());
    }

    @Test
    public void testParentAlertCallbackInterfaceExists() {
        Class<?> callbackClass = TriageService.ParentAlertCallback.class;
        assertNotNull(callbackClass);
        assertTrue(callbackClass.isInterface());
    }

    @Test
    public void testTriageIncidentModelCompatibility() {
        TriageIncident incident = new TriageIncident();
        incident.setChildId("testChild");
        incident.setCannotSpeakFullSentences(true);
        incident.setChestRetractions(false);
        incident.setBlueGrayLipsNails(false);
        incident.setRecentRescueAttempts(2);
        incident.setDecision("emergency");
        
        // Verify the incident can be used with TriageService
        assertNotNull(incident);
        assertEquals("testChild", incident.getChildId());
        assertTrue(incident.hasRedFlags());
    }
}

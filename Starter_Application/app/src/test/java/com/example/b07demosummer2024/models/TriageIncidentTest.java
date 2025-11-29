package com.example.b07demosummer2024.models;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class TriageIncidentTest {
    private TriageIncident incident;

    @Before
    public void setUp() {
        incident = new TriageIncident();
    }

    @Test
    public void testTriageIncidentCreation() {
        assertNotNull(incident);
    }

    @Test
    public void testSetAndGetCannotSpeakFullSentences() {
        incident.setCannotSpeakFullSentences(true);
        assertTrue(incident.isCannotSpeakFullSentences());
        
        incident.setCannotSpeakFullSentences(false);
        assertFalse(incident.isCannotSpeakFullSentences());
    }

    @Test
    public void testSetAndGetChestRetractions() {
        incident.setChestRetractions(true);
        assertTrue(incident.isChestRetractions());
        
        incident.setChestRetractions(false);
        assertFalse(incident.isChestRetractions());
    }

    @Test
    public void testSetAndGetBlueGrayLipsNails() {
        incident.setBlueGrayLipsNails(true);
        assertTrue(incident.isBlueGrayLipsNails());
        
        incident.setBlueGrayLipsNails(false);
        assertFalse(incident.isBlueGrayLipsNails());
    }

    @Test
    public void testSetAndGetRecentRescueAttempts() {
        incident.setRecentRescueAttempts(3);
        assertEquals(3, incident.getRecentRescueAttempts());
        
        incident.setRecentRescueAttempts(0);
        assertEquals(0, incident.getRecentRescueAttempts());
    }

    @Test
    public void testSetAndGetPeakFlowReading() {
        incident.setPeakFlowReading("450");
        assertEquals("450", incident.getPeakFlowReading());
        
        incident.setPeakFlowReading(null);
        assertNull(incident.getPeakFlowReading());
    }

    @Test
    public void testSetAndGetDecision() {
        incident.setDecision("emergency");
        assertEquals("emergency", incident.getDecision());
        
        incident.setDecision("home_steps");
        assertEquals("home_steps", incident.getDecision());
    }

    @Test
    public void testSetAndGetEscalated() {
        incident.setEscalated(true);
        assertTrue(incident.isEscalated());
        
        incident.setEscalated(false);
        assertFalse(incident.isEscalated());
    }

    @Test
    public void testSetAndGetEscalationTimestamp() {
        long timestamp = System.currentTimeMillis();
        incident.setEscalationTimestamp(timestamp);
        assertEquals(timestamp, incident.getEscalationTimestamp());
    }

    @Test
    public void testSetAndGetUserResponse() {
        incident.setUserResponse("called_emergency");
        assertEquals("called_emergency", incident.getUserResponse());
        
        incident.setUserResponse("started_home_steps");
        assertEquals("started_home_steps", incident.getUserResponse());
    }

    @Test
    public void testHasRedFlags_NoFlags() {
        incident.setCannotSpeakFullSentences(false);
        incident.setChestRetractions(false);
        incident.setBlueGrayLipsNails(false);
        assertFalse(incident.hasRedFlags());
    }

    @Test
    public void testHasRedFlags_OneFlag() {
        incident.setCannotSpeakFullSentences(true);
        incident.setChestRetractions(false);
        incident.setBlueGrayLipsNails(false);
        assertTrue(incident.hasRedFlags());
    }

    @Test
    public void testHasRedFlags_AllFlags() {
        incident.setCannotSpeakFullSentences(true);
        incident.setChestRetractions(true);
        incident.setBlueGrayLipsNails(true);
        assertTrue(incident.hasRedFlags());
    }

    @Test
    public void testInheritedFields() {
        String childId = "child123";
        long timestamp = System.currentTimeMillis();
        String notes = "Test notes";
        
        incident.setChildId(childId);
        incident.setTimestamp(timestamp);
        incident.setNotes(notes);
        
        assertEquals(childId, incident.getChildId());
        assertEquals(timestamp, incident.getTimestamp());
        assertEquals(notes, incident.getNotes());
    }

    @Test
    public void testFullConstructor() {
        String logId = "log123";
        String childId = "child123";
        long timestamp = System.currentTimeMillis();
        String notes = "Test notes";
        
        TriageIncident fullIncident = new TriageIncident(
            logId, childId, timestamp, notes,
            true, true, false, 2, "450", "emergency", false
        );
        
        assertEquals(logId, fullIncident.getLogId());
        assertEquals(childId, fullIncident.getChildId());
        assertEquals(timestamp, fullIncident.getTimestamp());
        assertEquals(notes, fullIncident.getNotes());
        assertTrue(fullIncident.isCannotSpeakFullSentences());
        assertTrue(fullIncident.isChestRetractions());
        assertFalse(fullIncident.isBlueGrayLipsNails());
        assertEquals(2, fullIncident.getRecentRescueAttempts());
        assertEquals("450", fullIncident.getPeakFlowReading());
        assertEquals("emergency", fullIncident.getDecision());
        assertFalse(fullIncident.isEscalated());
    }
}

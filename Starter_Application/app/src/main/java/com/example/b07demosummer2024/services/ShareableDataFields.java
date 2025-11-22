package com.example.b07demosummer2024.services;

import java.util.Arrays;
import java.util.List;

/**
 * Defines the data fields that can be shared with providers.
 * Parents can selectively share these fields on a per-provider basis.
 */
public class ShareableDataFields {
    // Field identifiers
    public static final String FIELD_NAME = "name";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_BASIC_INFO = "basicInfo";
    public static final String FIELD_MEDICAL_HISTORY = "medicalHistory";
    public static final String FIELD_SYMPTOMS = "symptoms";
    public static final String FIELD_MEDICATIONS = "medications";
    public static final String FIELD_RESCUE_INHALER = "rescueInhaler";
    public static final String FIELD_CONTROLLER_MEDICINE = "controllerMedicine";
    public static final String FIELD_ADHERENCE = "adherence";
    public static final String FIELD_PEAK_FLOW = "peakFlow";
    public static final String FIELD_TRIGGERS = "triggers";

    // All available fields
    public static final List<String> ALL_FIELDS = Arrays.asList(
            FIELD_NAME,
            FIELD_EMAIL,
            FIELD_BASIC_INFO,
            FIELD_MEDICAL_HISTORY,
            FIELD_SYMPTOMS,
            FIELD_MEDICATIONS,
            FIELD_RESCUE_INHALER,
            FIELD_CONTROLLER_MEDICINE,
            FIELD_ADHERENCE,
            FIELD_PEAK_FLOW,
            FIELD_TRIGGERS
    );

    // Human-readable labels for UI
    public static String getFieldLabel(String field) {
        switch (field) {
            case FIELD_NAME:
                return "Name";
            case FIELD_EMAIL:
                return "Email";
            case FIELD_BASIC_INFO:
                return "Basic Information";
            case FIELD_MEDICAL_HISTORY:
                return "Medical History";
            case FIELD_SYMPTOMS:
                return "Symptoms";
            case FIELD_MEDICATIONS:
                return "Medications";
            case FIELD_RESCUE_INHALER:
                return "Rescue Inhaler Usage";
            case FIELD_CONTROLLER_MEDICINE:
                return "Controller Medicine Usage";
            case FIELD_ADHERENCE:
                return "Medication Adherence";
            case FIELD_PEAK_FLOW:
                return "Peak Flow Readings";
            case FIELD_TRIGGERS:
                return "Asthma Triggers";
            default:
                return field;
        }
    }

    // Description for each field
    public static String getFieldDescription(String field) {
        switch (field) {
            case FIELD_NAME:
                return "Patient's name";
            case FIELD_EMAIL:
                return "Patient's email address";
            case FIELD_BASIC_INFO:
                return "Age, date of birth, and other basic details";
            case FIELD_MEDICAL_HISTORY:
                return "Past medical history and diagnoses";
            case FIELD_SYMPTOMS:
                return "Symptom logs and patterns";
            case FIELD_MEDICATIONS:
                return "List of current medications";
            case FIELD_RESCUE_INHALER:
                return "Quick-relief inhaler usage logs";
            case FIELD_CONTROLLER_MEDICINE:
                return "Daily controller medicine usage";
            case FIELD_ADHERENCE:
                return "Medication adherence statistics";
            case FIELD_PEAK_FLOW:
                return "Peak flow meter readings";
            case FIELD_TRIGGERS:
                return "Identified asthma triggers";
            default:
                return "";
        }
    }
}

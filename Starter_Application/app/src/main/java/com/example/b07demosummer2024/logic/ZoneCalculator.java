package com.example.b07demosummer2024.logic;

public class ZoneCalculator {
        public static String computeZone(int pef, int pb) {
            if (pb <= 0) return "Unknown";

            double percent = (pef * 100.0) / pb;

            if (percent >= 80.0) return "Green";
            if (percent >= 50.0) return "Yellow";
            return "Red";
        }
}

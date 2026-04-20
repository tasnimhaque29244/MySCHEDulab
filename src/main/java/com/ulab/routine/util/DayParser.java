package com.ulab.routine.util;

import java.util.ArrayList;
import java.util.List;

public class DayParser {
    private DayParser() {
    }

    public static List<String> resolveDays(String raw) {
        List<String> days = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return days;
        }

        String v = raw.toUpperCase()
                .replace("(", "")
                .replace(")", "")
                .replaceAll("\\s+", "");

        if (v.contains("SUN") && v.contains("TUE")) {
            days.add("SUN");
            days.add("TUE");
            return days;
        }

        if (v.contains("MON") && v.contains("WED")) {
            days.add("MON");
            days.add("WED");
            return days;
        }

        if (v.contains("THU") && v.contains("SAT")) {
            days.add("THU");
            days.add("SAT");
            return days;
        }

        if (v.equals("THU")) {
            days.add("THU");
            return days;
        }

        if (v.equals("SAT")) {
            days.add("SAT");
            return days;
        }

        if (v.equals("SUN")) {
            days.add("SUN");
            return days;
        }

        if (v.equals("MON")) {
            days.add("MON");
            return days;
        }

        if (v.equals("TUE")) {
            days.add("TUE");
            return days;
        }

        if (v.equals("WED")) {
            days.add("WED");
            return days;
        }

        return days;
    }
}
package com.ulab.routine.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {
    private static final Pattern NORMAL_RANGE_PATTERN = Pattern.compile(
            "(\\d{1,2}\\s*:?\\s*\\d{2}\\s*(?:am|pm)?)\\s*-\\s*(\\d{1,2}\\s*:?\\s*\\d{2}\\s*(?:am|pm))",
            Pattern.CASE_INSENSITIVE
    );

    // Handles malformed headers like:
    // 12:15 : 1 : 35 pm
    // 1 : 40 : 3:00 pm
    private static final Pattern MALFORMED_RANGE_PATTERN = Pattern.compile(
            "(\\d{1,2}\\s*:?\\s*\\d{2})\\s*:\\s*(\\d{1,2}\\s*:?\\s*\\d{2}\\s*(?:am|pm))",
            Pattern.CASE_INSENSITIVE
    );

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private TimeParser() {
    }

    public static boolean isTimeRange(String text) {
        if (text == null) {
            return false;
        }

        String normalized = normalize(text);
        return NORMAL_RANGE_PATTERN.matcher(normalized).find()
                || MALFORMED_RANGE_PATTERN.matcher(normalized).find();
    }

    public static TimeRange parseRange(String text) {
        String normalized = normalize(text);

        Matcher normalMatcher = NORMAL_RANGE_PATTERN.matcher(normalized);
        if (normalMatcher.find()) {
            String left = cleanupClock(normalMatcher.group(1));
            String right = cleanupClock(normalMatcher.group(2));

            right = ensureMeridiem(right, null);
            left = inferLeftMeridiem(left, right);

            LocalTime start = LocalTime.parse(left.toUpperCase(), FORMATTER);
            LocalTime end = LocalTime.parse(right.toUpperCase(), FORMATTER);

            return new TimeRange(start, end);
        }

        Matcher malformedMatcher = MALFORMED_RANGE_PATTERN.matcher(normalized);
        if (malformedMatcher.find()) {
            String left = cleanupClock(malformedMatcher.group(1));
            String right = cleanupClock(malformedMatcher.group(2));

            right = ensureMeridiem(right, null);
            left = inferLeftMeridiem(left, right);

            LocalTime start = LocalTime.parse(left.toUpperCase(), FORMATTER);
            LocalTime end = LocalTime.parse(right.toUpperCase(), FORMATTER);

            return new TimeRange(start, end);
        }

        throw new IllegalArgumentException("Invalid time range: " + text);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private static String cleanupClock(String value) {
        String v = value.trim();

        // Turn "12 : 10 pm" -> "12:10 pm"
        v = v.replaceAll("\\s*:\\s*", ":");

        // normalize extra spaces
        v = v.replaceAll("\\s+", " ").trim();

        return v;
    }

    private static String ensureMeridiem(String value, String fallbackFrom) {
        String lower = value.toLowerCase();
        if (lower.endsWith("am") || lower.endsWith("pm")) {
            return value;
        }

        if (fallbackFrom != null) {
            String fallbackLower = fallbackFrom.toLowerCase();
            if (fallbackLower.endsWith("am")) {
                return value + " AM";
            }
            if (fallbackLower.endsWith("pm")) {
                return value + " PM";
            }
        }

        return value;
    }

    private static String inferLeftMeridiem(String left, String right) {
        String lower = left.toLowerCase();
        if (lower.endsWith("am") || lower.endsWith("pm")) {
            return left;
        }

        String rightLower = right.toLowerCase();
        String rightMeridiem = rightLower.endsWith("pm") ? "PM" : "AM";

        String sameMeridiem = left + " " + rightMeridiem;
        LocalTime sameTime = LocalTime.parse(sameMeridiem.toUpperCase(), FORMATTER);
        LocalTime rightTime = LocalTime.parse(right.toUpperCase(), FORMATTER);

        if (sameTime.isBefore(rightTime)) {
            return sameMeridiem;
        }

        String oppositeMeridiem = rightMeridiem.equals("PM") ? "AM" : "PM";
        return left + " " + oppositeMeridiem;
    }
}
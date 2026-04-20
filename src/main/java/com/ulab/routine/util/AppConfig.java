package com.ulab.routine.util;

import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
            if (in == null) {
                throw new RuntimeException("app.properties not found");
            }
            PROPS.load(in);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load app.properties", e);
        }
    }

    private AppConfig() {
    }

    public static String get(String key, String defaultValue) {
        return PROPS.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String value = PROPS.getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }
}
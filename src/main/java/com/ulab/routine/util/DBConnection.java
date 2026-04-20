package com.ulab.routine.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DBConnection {
    private static final Properties PROPS = new Properties();

    static {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in == null) {
                    throw new RuntimeException("db.properties not found");
                }
                PROPS.load(in);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DB config", e);
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.username"),
                PROPS.getProperty("db.password")
        );
    }
}
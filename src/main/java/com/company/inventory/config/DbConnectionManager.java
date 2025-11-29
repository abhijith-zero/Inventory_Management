package com.company.inventory.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public final class DbConnectionManager {
    private static final String URL = System.getProperty("db.url",
            "jdbc:h2:file:./data/inventory;MODE=MYSQL");
    private static final String USER = System.getProperty("db.user", "sa");
    private static final String PASSWORD = System.getProperty("db.password", "");

    private DbConnectionManager() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

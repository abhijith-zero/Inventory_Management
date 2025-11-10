package com.company.inventory.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A static utility class for managing the database connection pool (HikariCP).
 *
 * This class must be initialized by calling the init() method at the
 * start of the application.
 */
public class DbConnectionManager {

    // The single, static connection pool
    private static HikariDataSource dataSource;

    /**
     * Private constructor to prevent instantiation, as this is a utility class.
     */
    private DbConnectionManager() {
    }

    /**
     * Initializes the database connection pool.
     * This method MUST be called once when the application starts.
     */
    public static DataSource init() {
        try (InputStream input = DbConnectionManager.class.getClassLoader().getResourceAsStream("config/app.properties")) {
            if (input == null) {
                System.err.println("Sorry, unable to find db.properties");
                throw new RuntimeException("Could not find db.properties on the classpath");
            }

            Properties props = new Properties();
            props.load(input);

            // Configure HikariCP
            HikariConfig config = new HikariConfig();
            config.setDriverClassName(props.getProperty("jdbc.driver"));
            config.setJdbcUrl(props.getProperty("jdbc.url"));
            config.setUsername(props.getProperty("jdbc.username"));
            config.setPassword(props.getProperty("jdbc.password"));

            // Pool settings - these are good defaults
            config.setMaximumPoolSize(10); // Max number of connections
            config.setMinimumIdle(5);      // Min number of idle connections
            config.setIdleTimeout(600000); // 10 minutes
            config.setConnectionTimeout(30000); // 30 seconds

            // Initialize the static connection pool
            return dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            // We throw a RuntimeException because if the DB can't be set up,
            // the application is in an unusable state and should probably crash.
            throw new RuntimeException("Error initializing database connection pool", e);
        }
    }

    /**
     * Gets a database connection from the pool.
     * init() must have been called before this.
     *
     * @return A Connection object.
     * @throws SQLException if a database access error occurs or pool is not initialized.
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DatabaseConnectionManager has not been initialized. Call init() first.");
        }
        return dataSource.getConnection();
    }

    /**
     * Call this method when your application is shutting down.
     * It closes the entire connection pool.
     */
    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("HikariCP connection pool closed.");
        }
    }
}
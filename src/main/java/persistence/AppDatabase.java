package persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central database entry point for the local SQLite store.
 * This is the storage seam that later can be swapped for Firebase-backed repositories.
 */
public final class AppDatabase {

    private static final Path DATABASE_PATH = Paths.get("data", "coffee-cafe.db");
    private static final String JDBC_PREFIX = "jdbc:sqlite:";
    private static volatile boolean initialized;

    private AppDatabase() {
    }

    public static Connection openConnection() throws SQLException {
        ensureInitialized();
        return DriverManager.getConnection(JDBC_PREFIX + DATABASE_PATH.toAbsolutePath());
    }

    public static synchronized void ensureInitialized() throws SQLException {
        if (initialized) {
            return;
        }

        try {
            Path parent = DATABASE_PATH.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new SQLException("Unable to create database directory", e);
        }

        try (Connection connection = openRawConnection()) {
            SchemaInitializer.initialize(connection);
        }
        initialized = true;
    }

    private static Connection openRawConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_PREFIX + DATABASE_PATH.toAbsolutePath());
    }
}

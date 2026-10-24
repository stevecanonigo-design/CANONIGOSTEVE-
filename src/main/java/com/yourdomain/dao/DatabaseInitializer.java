package com.yourdomain.dao;

import com.yourdomain.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseInitializer {

    private DatabaseInitializer() {}

    private static final String CREATE_USERS = """
            CREATE TABLE IF NOT EXISTS users (
                user_id       INTEGER PRIMARY KEY AUTOINCREMENT,
                name          TEXT NOT NULL,
                username      TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                role          TEXT NOT NULL
                              CHECK (role IN ('Customer', 'Cafe Staff', 'Admin')),
                status        TEXT NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING', 'Active', 'Inactive'))
            )""";

    private static final String CREATE_COMPUTERS = """
            CREATE TABLE IF NOT EXISTS computers (
                computer_id     INTEGER PRIMARY KEY AUTOINCREMENT,
                computer_number TEXT NOT NULL UNIQUE,
                specifications  TEXT,
                status          TEXT NOT NULL DEFAULT 'Available',
                rate_per_hour   REAL NOT NULL DEFAULT 0
            )""";

    private static final String CREATE_SESSIONS = """
            CREATE TABLE IF NOT EXISTS sessions (
                session_id    INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id       INTEGER NOT NULL,
                computer_id   INTEGER NOT NULL,
                start_time    TEXT NOT NULL,
                end_time      TEXT,
                total_minutes INTEGER,
                total_fee     REAL,
                status        TEXT NOT NULL DEFAULT 'Active',
                FOREIGN KEY (user_id) REFERENCES users(user_id),
                FOREIGN KEY (computer_id) REFERENCES computers(computer_id)
            )""";

    private static final String CREATE_LOGS = """
            CREATE TABLE IF NOT EXISTS activity_logs (
                log_id        INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id       INTEGER NOT NULL,
                action        TEXT NOT NULL,
                log_timestamp TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            )""";

    /** Returns true if the database is ready. Never drops or deletes data. */
    public static boolean initialize() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement()) {

            st.execute(CREATE_USERS);
            st.execute(CREATE_COMPUTERS);
            st.execute(CREATE_SESSIONS);
            st.execute(CREATE_LOGS);

            warnIfOldStatusRule(conn);
            System.out.println("Database initialized successfully (students.db).");
        } catch (SQLException e) {
            System.out.println("Database initialization failed: " + e.getMessage());
            return false;
        }
        return seedDefaultAdmin();
    }

    // An older users table may only allow Active/Inactive, which would reject PENDING.
    private static void warnIfOldStatusRule(Connection conn) throws SQLException {
        String sql = "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'users'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String ddl = rs.getString(1);
                if (ddl != null && ddl.toUpperCase().contains("CHECK")
                        && !ddl.toUpperCase().contains("PENDING")) {
                    System.out.println("WARNING: your existing users table does not allow the "
                            + "PENDING status. Registration will fail until the schema is migrated.");
                }
            }
        }
    }

    // Needed so someone can approve PENDING accounts. Only runs if no Admin exists.
    private static boolean seedDefaultAdmin() {
        UserDao dao = new UserDao();
        int admins = dao.countByRole(User.ROLE_ADMIN);
        if (admins < 0) {
            return false;
        }
        if (admins == 0) {
            dao.addUser("System Administrator", "admin", "admin123",
                    User.ROLE_ADMIN, User.STATUS_ACTIVE);
            System.out.println("Default admin created: username 'admin', password 'admin123' "
                    + "(change this after setup).");
        }
        return true;
    }
}

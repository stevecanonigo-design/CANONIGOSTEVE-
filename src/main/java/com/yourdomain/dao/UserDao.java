package com.yourdomain.dao;

import com.yourdomain.model.User;
import com.yourdomain.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    public enum RegisterResult { SUCCESS, DUPLICATE_USERNAME, ERROR }

    /** Public registration: always starts as PENDING. */
    public RegisterResult register(String name, String username, String password, String role) {
        return addUser(name, username, password, role, User.STATUS_PENDING);
    }

    public RegisterResult addUser(String name, String username, String password,
                                  String role, String status) {
        if (findByUsername(username).isPresent()) {
            return RegisterResult.DUPLICATE_USERNAME;
        }
        String sql = "INSERT INTO users (name, username, password_hash, role, status) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, username);
            ps.setString(3, PasswordUtil.hash(password));
            ps.setString(4, role);
            ps.setString(5, status);
            ps.executeUpdate();
            return RegisterResult.SUCCESS;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                return RegisterResult.DUPLICATE_USERNAME;
            }
            System.out.println("Database error while registering: " + e.getMessage());
            return RegisterResult.ERROR;
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT user_id, name, username, password_hash, role, status "
                + "FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while finding user: " + e.getMessage());
        }
        return Optional.empty();
    }

    /** Checks username + password only. The caller must check status and role. */
    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = findByUsername(username);
        if (user.isPresent() && PasswordUtil.verify(password, user.get().passwordHash())) {
            return user;
        }
        return Optional.empty();
    }

    public List<User> findByStatus(String status) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, name, username, password_hash, role, status "
                + "FROM users WHERE status = ? ORDER BY user_id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("Database error while listing users: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatus(int userId, String newStatus) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Database error while updating status: " + e.getMessage());
            return false;
        }
    }

    /** Returns the number of users with this role, or -1 on error. */
    public int countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM users WHERE role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.out.println("Database error while counting users: " + e.getMessage());
            return -1;
        }
    }

    /** Writes a row to activity_logs (login, logout, session start, etc.). */
    public void logActivity(int userId, String action) {
        String sql = "INSERT INTO activity_logs (user_id, action) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, action);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Could not write activity log: " + e.getMessage());
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("name"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rs.getString("role"),
                rs.getString("status"));
    }
}

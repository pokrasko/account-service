package ru.pokrasko.accountservice;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DatabaseHelper implements AutoCloseable {
    private final String url;
    private final String username = "accountservice";
    private final String password = "1uGF2q5";

    private final Connection conn;
    private final PreparedStatement putStmt;
    private final PreparedStatement updateStmt;

    public DatabaseHelper(String url) throws SQLException {
        this.url = url;
        try {
            conn = DriverManager.getConnection(url, username, password);
            putStmt = conn.prepareStatement("INSERT INTO Accounts(Id, Value) VALUES(?, 0);");
            updateStmt = conn.prepareStatement("UPDATE Accounts SET Value = '?' WHERE Id = ?;");

            try (PreparedStatement createStmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " +
                    "Accounts(Id INT PRIMARY KEY, Value INT) ENGINE=InnoDB;")) {
                createStmt.executeUpdate();
            }
        } catch (SQLException e) {
            close();
            throw e;
        }
    }

    public void close() throws SQLException {
        if (updateStmt != null) {
            updateStmt.close();
        }
        if (putStmt != null) {
            putStmt.close();
        }
        if (conn != null) {
            conn.close();
        }
    }

    synchronized Map<Integer, AtomicLong> read() throws SQLException {
        Map<Integer, AtomicLong> result = new ConcurrentHashMap<>();
        try (PreparedStatement readStmt = conn.prepareStatement("SELECT * FROM Accounts")) {
            ResultSet set = readStmt.executeQuery();
            while (set.next()) {
                Integer id = set.getInt(1);
                AtomicLong value = new AtomicLong(set.getLong(2));
                result.put(id, value);
            }
        }
        return result;
    }

    synchronized void put(Integer id) throws SQLException {
        putStmt.setInt(1, id);
        putStmt.executeUpdate();
    }

    synchronized void update(Integer id, Long value) throws SQLException {
        updateStmt.setInt(1, id);
        updateStmt.setLong(2, value);
        updateStmt.executeUpdate();
    }
}

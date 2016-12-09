package ru.pokrasko.accountservice;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class DatabaseHelper implements AutoCloseable {
    private static final String username = "accountservice";
    private static final String password = "1uGF2q5";

    private static final String accountsTable = "accounts";
    private static final String idField = "id";
    private static final String valueField = "value";

    private final Connection conn;
    private final PreparedStatement getStmt;
    private final PreparedStatement putStmt;

    DatabaseHelper(String url) throws SQLException {
        try {
            conn = DriverManager.getConnection(url, username, password);
            getStmt = conn.prepareStatement("SELECT * FROM " + accountsTable
                    + " WHERE " + idField + " = ?;");
            putStmt = conn.prepareStatement("INSERT INTO " + accountsTable + " (" + idField + ", " + valueField + ")"
                    + " VALUES(?, ?) ON DUPLICATE KEY UPDATE " + valueField + " =?;");

            try (PreparedStatement createStmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + accountsTable
                    + " (" + idField + " INT PRIMARY KEY, " + valueField + " BIGINT) ENGINE=InnoDB;")) {
                createStmt.executeUpdate();
            }
        } catch (SQLException e) {
            close();
            throw e;
        }
    }

    public void close() throws SQLException {
        if (putStmt != null) {
            putStmt.close();
        }
        if (conn != null) {
            conn.close();
        }
    }

    synchronized ConcurrentMap<Integer, Long> read() throws SQLException {
        ConcurrentMap<Integer, Long> result = new ConcurrentHashMap<>();
        try (PreparedStatement readStmt = conn.prepareStatement("SELECT * FROM " + accountsTable)) {
            ResultSet set = readStmt.executeQuery();
            while (set.next()) {
                Integer id = set.getInt(1);
                Long value = set.getLong(2);
                result.put(id, value);
            }
        }
        return result;
    }

    synchronized Long get(Integer id) throws SQLException {
        getStmt.setInt(1, id);
        ResultSet set = getStmt.executeQuery();
        if (set.next()) {
            return set.getLong(1);
        } else {
            return null;
        }
    }

    synchronized void put(Integer id, Long value) throws SQLException {
        putStmt.setInt(1, id);
        putStmt.setLong(2, value);
        putStmt.setLong(3, value);
        putStmt.executeUpdate();
    }
}

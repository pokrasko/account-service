package ru.pokrasko.accountservice;

import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class DatabaseHelper implements AutoCloseable {
    private final String url;
    private static final String username = "accountservice";
    private static final String password = "1uGF2q5";

    private static final String accountsTable = "accounts";
    private static final String idField = "id";
    private static final String valueField = "value";

    private final Connection conn;
    private final PreparedStatement getStmt;
    private final PreparedStatement putStmt;
    private final PreparedStatement updateStmt;

    DatabaseHelper(String url) throws SQLException {
        this.url = url;
        try {
            conn = DriverManager.getConnection(this.url, username, password);
            getStmt = conn.prepareStatement("SELECT * FROM " + accountsTable
                    + " WHERE " + idField + " = ?;");
            putStmt = conn.prepareStatement("INSERT INTO " + accountsTable + " (" + idField + ", " + valueField + ")"
                    + " VALUES(?, 0);");
            updateStmt = conn.prepareStatement("UPDATE " + accountsTable + " SET " + valueField + " = ?"
                    + " WHERE " + idField + " = ?;");

            try (PreparedStatement createStmt = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " +
                    "Accounts(Id INT PRIMARY KEY, Value BIGINT) ENGINE=InnoDB;")) {
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

    synchronized void put(Integer id) throws SQLException {
        putStmt.setInt(1, id);
        putStmt.executeUpdate();
    }

    synchronized void update(Integer id, Long value) throws SQLException {
        updateStmt.setInt(2, id);
        updateStmt.setLong(1, value);
        updateStmt.executeUpdate();
    }
}

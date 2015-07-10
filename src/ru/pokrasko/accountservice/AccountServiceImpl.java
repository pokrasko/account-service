package ru.pokrasko.accountservice;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class AccountServiceImpl implements AccountService {
    private final Map<Integer, AtomicLong> accounts;
    private AtomicInteger running;
    private AtomicLong total;
    private DatabaseHelper helper;

    public AccountServiceImpl(DatabaseHelper helper) throws SQLException {
        this.helper = helper;
        accounts = helper.read();
        running = new AtomicInteger(0);
        total = new AtomicLong(0L);
    }

    public Long getAmount(Integer id) throws RemoteException {
        running.incrementAndGet();
        total.incrementAndGet();
        AtomicLong account = accounts.get(id);
        if (account == null) {
            return 0L;
        }
        Long result = account.get();
        running.decrementAndGet();
        return result;
    }

    public void addAmount(Integer id, Long value) throws RemoteException, SQLException {
        running.incrementAndGet();
        total.incrementAndGet();
        synchronized (this) {
            accounts.putIfAbsent(id, new AtomicLong(0L));
            helper.put(id);
        }
        if (value == 0L) {
            return;
        }
        synchronized (this) {
            helper.update(id, accounts.get(id).addAndGet(value));
        }
        running.decrementAndGet();
    }

    public int runningRequests() throws RemoteException {
        return running.get();
    }

    public long totalRequests() throws RemoteException {
        return total.get();
    }

    public void resetStats() throws RemoteException {
        total = new AtomicLong(0L);
    }
}

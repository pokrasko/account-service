package ru.pokrasko.accountservice;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class AccountServiceImpl implements AccountService {
    private AccountCache accounts;
    private AtomicInteger running;
    private AtomicLong total;

    AccountServiceImpl(DatabaseHelper helper) throws SQLException {
        accounts = new AccountCache(helper, helper.read());
        running = new AtomicInteger(0);
        total = new AtomicLong(0L);
    }

    public Long getAmount(Integer id) throws RemoteException {
        running.incrementAndGet();
        total.incrementAndGet();
        Long value = accounts.get(id);
        running.decrementAndGet();
        return value;
    }

    public void addAmount(Integer id, Long delta) throws RemoteException, SQLException {
        running.incrementAndGet();
        total.incrementAndGet();
        Long value = accounts.get(id);
        if (value == null) {
            value = 0L;
        }
        accounts.put(id, value + delta);
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

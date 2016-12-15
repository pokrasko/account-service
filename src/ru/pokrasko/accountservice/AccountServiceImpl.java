package ru.pokrasko.accountservice;

import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class AccountServiceImpl implements AccountService {
    private AccountCache accounts;
    private AtomicInteger running;
    private AtomicLong total;
    private Lock writeLock = new ReentrantLock();

    AccountServiceImpl(DatabaseHelper helper) throws SQLException {
        accounts = new AccountCache(helper, helper.read());
        running = new AtomicInteger(0);
        total = new AtomicLong(0L);
    }

    public Long getAmount(Integer id) throws RemoteException {
        running.getAndIncrement();
        total.getAndIncrement();
        Long value = accounts.get(id);
        running.getAndDecrement();
        return (value != null) ? value : 0L;
    }

    public void addAmount(Integer id, Long delta) throws RemoteException {
        running.getAndIncrement();
        total.getAndIncrement();
        writeLock.lock();
        try {
            Long value = accounts.get(id);
            if (value == null) {
                value = 0L;
            }
            accounts.put(id, value + delta);
        } finally {
            writeLock.unlock();
        }
        running.getAndDecrement();
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

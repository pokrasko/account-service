package ru.pokrasko.accountservice;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.sql.SQLException;

public interface AccountService extends Remote {
    /**
     * Retrieves current balance or zero if {@link #addAmount(Integer, Long)} was not called before for specified
     * id.
     *
     * @param id balance identifier
     * @return current balance for <code>id</code>
     * @throws RemoteException
     */
    Long getAmount(Integer id) throws RemoteException;

    /**
     * Increases balance or sets it if this method was called first time.
     *
     * @param id balance identifier
     * @param value positive or negative value, which must be added to current balance
     * @throws RemoteException
     */
    void addAmount(Integer id, Long value) throws RemoteException;

    /**
     * Gets stats for running requests.
     *
     * @return the number of requests for {@link #addAmount(Integer, Long)} and {@link #getAmount(Integer)}
     * requests running at this service at the moment.
     * @throws RemoteException
     */
    int runningRequests() throws RemoteException;

    /**
     * Gets stats for total requests.
     *
     * @return the total number of requests for {@link #addAmount(Integer, Long)} and {@link #getAmount(Integer)}
     * requests started since the moment when the service started or the last moment when stats have been reset
     * @throws RemoteException
     */
    long totalRequests() throws RemoteException;

    /**
     * Resets stats for total requests.
     *
     * @throws RemoteException
     */
    void resetStats() throws RemoteException;
}

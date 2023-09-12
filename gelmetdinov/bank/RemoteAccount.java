package info.kgeorgiy.ja.gelmetdinov.bank;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemoteAccount extends UnicastRemoteObject implements Account {
    private final String id;
    private BigDecimal amount;

    public RemoteAccount(String id, int port) throws RemoteException {
        super(port);
        this.id = id;
        this.amount = new BigDecimal(0);
    }
    @Override
    public String getId() throws RemoteException {
        return id;
    }

    @Override
    public synchronized BigDecimal getAmount() throws RemoteException {
        return amount;
    }

    @Override
    public synchronized void addAmount(BigDecimal amount) throws RemoteException {
        this.amount = this.amount.add(amount);
    }
}

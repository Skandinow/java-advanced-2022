package info.kgeorgiy.ja.gelmetdinov.bank;

import java.math.BigDecimal;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Account extends Remote {
    String getId() throws RemoteException;

    BigDecimal getAmount() throws RemoteException;

    void addAmount(BigDecimal amount) throws RemoteException;
}

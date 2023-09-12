package info.kgeorgiy.ja.gelmetdinov.bank;

import java.io.Serializable;
import java.math.BigDecimal;
import java.rmi.RemoteException;

public class LocalAccount implements Account, Serializable {
    private final String id;
    private BigDecimal amount;
    LocalAccount(RemoteAccount remoteAccount) throws RemoteException {
        this.id = remoteAccount.getId();
        this.amount = remoteAccount.getAmount();
    }

    LocalAccount(String accId) {
        this.id = accId;
        this.amount = BigDecimal.ZERO;
    }
    @Override
    public String getId() throws RemoteException {
        return id;
    }

    @Override
    public BigDecimal getAmount() throws RemoteException {
        return amount;
    }

    @Override
    public void addAmount(BigDecimal amount) throws RemoteException {
        this.amount = this.amount.add(amount);
    }
}

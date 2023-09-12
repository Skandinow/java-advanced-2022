package info.kgeorgiy.ja.gelmetdinov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Bank extends Remote {
    void createAccount(Person person, String accountID) throws RemoteException;

    Account getAccount(String accountID, Person person) throws RemoteException;

    void createPerson(String name, String surname, String passportID) throws RemoteException;

    Person getRemotePerson(String passportID) throws RemoteException;

    Person getLocalPerson(String passportID) throws RemoteException;

    Map<String, Map<String, Account>> getPassportIdToPersonsAccount() throws RemoteException;
}

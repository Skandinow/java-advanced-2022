package info.kgeorgiy.ja.gelmetdinov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RemotePerson extends UnicastRemoteObject implements Person {
    String name;
    String surname;
    String passportID;

    public RemotePerson(String name, String surname, String passportID, int port) throws RemoteException {
        super(port);
        this.name = name;
        this.surname = surname;
        this.passportID = passportID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public String getPassportID() {
        return passportID;
    }
}

package info.kgeorgiy.ja.gelmetdinov.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteBank extends UnicastRemoteObject implements Bank {
    private final int port;
    Map<String, Map<String, Account>> passportIdToPersonsAccount = new ConcurrentHashMap<>();
    Map<String, Person> passportIDToPerson = new ConcurrentHashMap<>();

    protected RemoteBank(int port) throws RemoteException {
        super(port);
        this.port = port;
    }


    public Map<String, Map<String, Account>> getPassportIdToPersonsAccount() throws RemoteException {
        return passportIdToPersonsAccount;
    }

    @Override
    public void createAccount(Person person, String accountID) throws RemoteException {
        if (person == null) return;

        String accId = person.getPassportID() + ":" + accountID;

        if (person instanceof LocalPerson) {
            ((LocalPerson) person).addAccount(accId, new LocalAccount(accountID));
        } else {

            passportIdToPersonsAccount.putIfAbsent(person.getPassportID(), new ConcurrentHashMap<>());
            passportIdToPersonsAccount.get(person.getPassportID()).putIfAbsent(accId, new RemoteAccount(accountID, port));
//            passportIdToPersonsAccount.computeIfAbsent(person.getPassportID(), string -> passportIdToPersonsAccount.putIfAbsent(string, new ConcurrentHashMap<>()))
//                    .putIfAbsent(accId, new RemoteAccount(accountID, port));

        }
    }

    @Override
    public Account getAccount(String accountID, Person person) throws RemoteException {
        if (person instanceof LocalPerson) {
            return ((LocalPerson) person).getAccounts().get(accountID);
        }
        return passportIdToPersonsAccount.get(person.getPassportID()).get(accountID);
    }

    public Person getRemotePerson(String passportId) throws RemoteException {
        return passportIDToPerson.getOrDefault(passportId, null);
    }

    public Person getLocalPerson(String passportId) throws RemoteException {
        Person person = passportIDToPerson.get(passportId);
        if (person == null) return null;

        ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();
        for (Account account : passportIdToPersonsAccount.get(passportId).values()) {
            accounts.put(passportId + ":" + account.getId(), new LocalAccount((RemoteAccount) account));
        }

        return new LocalPerson(person.getName(), person.getSurname(), person.getPassportID(),
                accounts);
    }

    public void createPerson(String name, String surname, String passportID) throws RemoteException {
        if (name == null && surname == null && passportID == null) return;

        passportIDToPerson.putIfAbsent(passportID, new RemotePerson(name, surname, passportID, port));
    }
}

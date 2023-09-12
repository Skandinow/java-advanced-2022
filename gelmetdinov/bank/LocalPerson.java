package info.kgeorgiy.ja.gelmetdinov.bank;

import java.io.Serializable;
import java.util.Map;

public class LocalPerson implements Serializable, Person {
    private final String name;
    private final String surname;
    private final String passwordID;
    private final Map<String, Account> accounts;

    public LocalPerson(String name, String surname, String passwordID, Map<String, Account> accounts) {
        this.name = name;
        this.surname = surname;
        this.passwordID = passwordID;
        this.accounts = accounts;
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
        return passwordID;
    }

    public Map<String, Account> getAccounts() {
        return accounts;
    }

    public void addAccount(String id, Account account) {
        accounts.put(id, account);
    }
}

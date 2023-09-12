package info.kgeorgiy.ja.gelmetdinov.bank;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.stream.IntStream;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class JUnitTest {
    private final static int PORT = 8888;
    private static Bank bank;

    @BeforeClass
    public static void beforeAll() throws Exception {
        Server.startServer(PORT);
        bank = (Bank) LocateRegistry.getRegistry(PORT).lookup("//localhost/bank");
        Assert.assertNotNull(bank);
    }

    private Person getPerson(int counter, boolean remote) {
        try {
            bank.createPerson("name" + counter, "surname" + counter,
                    "passportID" + counter);

            return searchForPerson(counter, remote);

        } catch (RemoteException e) {
            Assert.fail();
            throw new RuntimeException("Error: Remote exception");
        }
    }

    private Person searchForPerson(int counter, boolean remote) {
        try {
            return remote ? bank.getRemotePerson("passportID" + counter)
                    : bank.getLocalPerson("passportID" + counter);
        } catch (RemoteException e) {
            Assert.fail();
            throw new RuntimeException("Error: Remote exception");
        }
    }

    private Account getAccount(int counter, Person person) {
        try {
            // passportID + counter: counter
            bank.createAccount(person, String.valueOf(counter));
//            Map<String, Map<String, Account>> accs = bank.getPassportIdToPersonsAccount();

            return bank.getAccount(person.getPassportID() + ":" + counter, person);

        } catch (RemoteException e) {
            Assert.fail();
            throw new RuntimeException("Error: couldn't create account");
        }

    }

    private Account searchForAccount(int counter, Person person) {
        try {
            return bank.getAccount(person.getPassportID() + ":" + counter, person);
        } catch (RemoteException e) {
            throw new RuntimeException("Couldn't find account");
        }

    }

    private void addMoney(long amount, Account account) {
        try {
            account.addAmount(BigDecimal.valueOf(amount));
        } catch (RemoteException e) {
            throw new RuntimeException("Error: couldn't add money");
        }

    }

    private BigDecimal getMoney(Account account) {
        try {
            return account.getAmount();
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }


    private static void checkPersonData(Person firstRemotePerson, Person secondRemotePerson) throws RemoteException {
        Assert.assertEquals(firstRemotePerson.getName(), secondRemotePerson.getName());
        Assert.assertEquals(firstRemotePerson.getSurname(), secondRemotePerson.getSurname());
        Assert.assertEquals(firstRemotePerson.getPassportID(), secondRemotePerson.getPassportID());
    }

    private void assertEquals(int expected, Account remoteAccount) {
        Assert.assertEquals(expected, getMoney(remoteAccount).longValue());
    }

    @Test
    public void test1_fillingBank() {
        Person remotePerson, localPerson;
        Account remoteAccount, localAccount;
        for (int i = 0; i < 5; i++) {
            Assert.assertNull(searchForPerson(i, true));
            Assert.assertNull(searchForPerson(i, false));

            remotePerson = getPerson(i, true);
            Assert.assertNotNull(remotePerson);
            remoteAccount = getAccount(i, remotePerson);
            Assert.assertNotNull(remoteAccount);

            localPerson = getPerson(i, false);
            Assert.assertNotNull(localPerson);
            localAccount = getAccount(i, localPerson);
            Assert.assertNotNull(localAccount);
        }
    }

    @Test
    public void test2_creating2LocalsFromOneRemote() throws RemoteException {
        Person remotePerson = getPerson(0, true);
        Account remoteAccount = getAccount(0, remotePerson);
        for (int i = 0; i < 5; i++) {
            addMoney(100, remoteAccount);
        }
        assertEquals(500, remoteAccount);


        //Here created the first local person, remoted one didn't change and still have 500
        Person firstLocalPerson = getPerson(0, false);
        Account localAccountFirstPerson = getAccount(0, firstLocalPerson);
        for (int i = 0; i < 5; i++) {
            addMoney(100, localAccountFirstPerson);
        }
        assertEquals(500, remoteAccount);
        assertEquals(1000, localAccountFirstPerson);


        for (int i = 0; i < 5; i++) {
            addMoney(100, remoteAccount);
        }

        assertEquals(1000, localAccountFirstPerson);
        assertEquals(1000, remoteAccount);


        //Here created the second local person with same passport, name and surname as the first one
        Person secondLocalPerson = getPerson(0, false);
        Account localAccountSecondPerson = getAccount(0, secondLocalPerson);
        for (int i = 0; i < 5; i++) {
            addMoney(100, localAccountSecondPerson);
        }

        assertEquals(1500, localAccountSecondPerson);
        assertEquals(1000, localAccountFirstPerson);


        for (int i = 0; i < 5; i++) {
            addMoney(100, remoteAccount);
        }
        assertEquals(1500, remoteAccount);
        assertEquals(1500, localAccountSecondPerson);
        assertEquals(1000, localAccountFirstPerson);
        checkPersonData(firstLocalPerson,secondLocalPerson);
    }

    @Test
    public void test3_creating2RemotePersonsFromOne() throws RemoteException {
        Person firstRemotePerson = getPerson(10, true);
        Account firstRemoteAccount = getAccount(10, firstRemotePerson);
        for (int i = 0; i < 5; i++) {
            addMoney(100, firstRemoteAccount);
        }
        assertEquals(500, firstRemoteAccount);

        Person secondRemotePerson = getPerson(10, true);
        Account secondRemoteAccount = getAccount(10, firstRemotePerson);


        checkPersonData(firstRemotePerson, secondRemotePerson);
        Assert.assertEquals(secondRemoteAccount, firstRemoteAccount);
    }

    @Test
    public void test4_parallel() {
        int personId = 4;
        int numberOfStreams = 10;
        int[] accountId = new int[]{111, 222, 333};

        for (int j = 0; j < personId; j++) {
            int finalJ = j;
            for (int accId : accountId) {
                IntStream
                        .range(0, numberOfStreams)
                        .parallel()
                        .mapToObj(i -> getAccount(accId, getPerson(finalJ, true)))
                        .forEach(acc -> addMoney(50, acc));
            }
        }
        for (int i = 0; i < personId; i++) {
            for (int accId : accountId) {
                assertEquals(numberOfStreams * 50, searchForAccount(accId, searchForPerson(i, true)));
            }

        }
    }
}

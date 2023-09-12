package info.kgeorgiy.ja.gelmetdinov.bank;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public class Client {
    public static void main(String[] args) {
        if (!checkInput(args)) return;

        String name = args[0];
        String surname = args[1];
        String passportID = args[2];
        String accountID = args[3];
        int moneyChange = Integer.parseInt(args[4]);


        Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (MalformedURLException e) {
            System.out.println("Incorrect URL");
            return;
        } catch (NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (RemoteException e) {
            System.out.println("Remote exception: " + e.getMessage());
            return;
        }



        try {
            Person person = bank.getRemotePerson(passportID);
            if (person == null) {
                System.out.println("Creating person entity:");
                bank.createPerson(name, surname, passportID);
                person = bank.getRemotePerson(passportID);
                System.out.printf("%s %s with passportID : %s - was created%n",name ,surname, passportID);

                System.out.println("Creating account:");
                bank.createAccount(person, accountID);
            }
            Account account = bank.getAccount(passportID, person);
            if (account == null) {
                System.out.println("Creating account:");
                bank.createAccount(person, accountID);
                account = bank.getAccount(passportID, person);
                System.out.printf("Account with id: %s and passport: %s - was created%n", accountID, passportID);
            }
            account.addAmount(BigDecimal.valueOf(moneyChange));
            System.out.printf("%s's new balance: %s%n", name, account.getAmount().toPlainString());

        } catch (RemoteException e) {
            System.out.println("Remote exception: " + e.getMessage());
        }
    }

    private static boolean checkInput(String[] args) {
        String rightInput = "Invalid input: you should enter String: <name> String: <surname> String: <passportID>" +
                " String: <accountID>  int: <moneyChange>";

        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.out.println(rightInput);
            return false;
        }

        try {
            Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println(rightInput);
            return false;
        }
        return true;
    }
}

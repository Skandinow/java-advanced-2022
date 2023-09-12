package info.kgeorgiy.ja.gelmetdinov.bank;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Server {

    private static final int PORT = 8888;

    public static void main(String[] args) {
        startServer(PORT);
    }

    public static void startServer(int port) {
        Bank bank;
        try {
            bank = new RemoteBank(port);
            LocateRegistry.createRegistry(port).rebind("//localhost/bank", bank);
        } catch (RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
        }
    }
}

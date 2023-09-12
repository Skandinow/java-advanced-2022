package info.kgeorgiy.ja.gelmetdinov.hello;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import info.kgeorgiy.java.advanced.hello.HelloServer;

public class HelloUDPServer implements HelloServer {
    private ExecutorService executorService;
    private DatagramSocket socket;

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Error: Illegal arguments format.");
            return;
        }
        int port;
        int threads;
        try {
            port = Integer.parseInt(args[0]);
            threads = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid arguments: " + e.getMessage());
            return;
        }
        try (HelloServer helloServer = new HelloUDPServer()) {
            helloServer.start(port, threads);
            Scanner scanner = new Scanner(System.in);
            scanner.next();
        }

    }

    @Override
    public void start(int port, int threads) {
        try {
            executorService = Executors.newFixedThreadPool(threads);
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            System.err.println("Error: can't create or access socket from port");
            return;
        }
        for (int i = 0; i < threads; i++) {
            executorService.submit(receivePackage());
        }


    }

    private Runnable receivePackage() {
        return () -> {
            DatagramPacket received;
            while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                try {
                    int socketSize = socket.getReceiveBufferSize();
                    received = new DatagramPacket(new byte[socketSize], socketSize);
                    socket.receive(received);

                    try {
                        String s = Util.fromPacket(received);
                        byte[] bytes = ("Hello, " +
                                new String(received.getData(), received.getOffset(), received.getLength(),
                                        StandardCharsets.UTF_8))
                                .getBytes();
                        DatagramPacket response = new DatagramPacket(bytes, 0, bytes.length, received.getAddress(),
                                received.getPort());
                        socket.send(response);
                    } catch (IOException e) {
                        System.err.println("Error: Something went wrong while sending packet");
                    }


                } catch (SocketException e) {
                    System.err.println("Error: There are some troubles with underlying socket protocol");
                } catch (IOException e) {
                    System.err.println("Error: Something went wrong while receiving packet");
                }
            }

        };
    }


    @Override
    public void close() {
        executorService.shutdown();
        socket.close();
    }
}

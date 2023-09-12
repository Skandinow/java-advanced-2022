package info.kgeorgiy.ja.gelmetdinov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPClient implements HelloClient {
    //
    private static final int SO_TIMEOUT = 500;

    public static void main(String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Error: Illegal arguments format.");
            return;
        }
        String host;
        int port;
        String reqPref;
        int numberOfThreads;
        int numberOfRequests;
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
            reqPref = args[2];
            numberOfThreads = Integer.parseInt(args[3]);
            numberOfRequests = Integer.parseInt(args[4]);

        } catch (NumberFormatException e) {
            System.err.println("Error: Invalid arguments: " + e.getMessage());
            return;
        }
        HelloClient helloClient = new HelloUDPClient();
        helloClient.run(host, port, reqPref, numberOfThreads, numberOfRequests);
    }

    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        SocketAddress socket = new InetSocketAddress(host, port);
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        for (int i = 1; i < threads + 1; i++) {
            executorService.submit(request(i, socket, prefix, requests));
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(threads * requests * 500 * 10L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
    }

    private Runnable request(int threadId, SocketAddress socket, String prefix, int requests) {
        return () ->
        {
            try (final DatagramSocket datagramSocket = new DatagramSocket()) {
                datagramSocket.setSoTimeout(SO_TIMEOUT);

                final byte[] responseStorage = new byte[datagramSocket.getReceiveBufferSize()];
                for (int requestId = 1; requestId < requests + 1; requestId++) {
                    String request = prefix + threadId + "_" + requestId;

                    DatagramPacket response;
                    while (true) {
                        try {
                            DatagramPacket datagramPacket = new DatagramPacket(
                                    request.getBytes(StandardCharsets.UTF_8),
                                    request.length(),
                                    socket);
                            datagramSocket.send(datagramPacket);


                            response = new DatagramPacket(responseStorage, 0, responseStorage.length);
                            datagramSocket.receive(response);

                            if (isCorrect(request, response)) break;
                        } catch (final IOException ignored) {
                        }
                    }
                }

            } catch (final SocketException e) {
                System.err.println("Error: DataSocket can't be opened or can't be found");
            }
        };
    }

    private static boolean isCorrect(String request, DatagramPacket response) {
        return new String(response.getData(), 0, response.getLength(), StandardCharsets.UTF_8)
                .contains(request);
    }

}



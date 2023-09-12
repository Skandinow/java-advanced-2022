package info.kgeorgiy.ja.gelmetdinov.hello;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public class Util {
    private Util() {

    }

    public static String fromPacket(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public static DatagramPacket fromString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        return new DatagramPacket(bytes, 0, bytes.length);
    }
}

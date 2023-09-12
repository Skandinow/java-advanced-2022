package info.kgeorgiy.ja.gelmetdinov.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Walk {
    protected static MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Error: can't use SHA-256 ");
        }
    }

    protected static final int HASH_LENGTH = 2 * messageDigest.getDigestLength();
    protected static final String ERROR = "0".repeat(HASH_LENGTH);


    protected static class WriteHashException extends IOException {
        WriteHashException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("ERROR: Invalid input");
        } else if (messageDigest != null) {
            walk(args[0], args[1]);
        }
    }

    protected static void walk(String in, String out) {
        Path input;
        try {
            input = Path.of(in);
        } catch (InvalidPathException e) {
            System.err.println("Invalid input file's path: " + e.getMessage());
            return;
        }

        Path output;
        try {
            output = Path.of(out);
        } catch (InvalidPathException e) {
            System.err.println("Invalid output file's path: " + e.getMessage());
            return;
        }

        if (output.getParent() != null) {
            try {
                Files.createDirectories(output.getParent());
                System.err.println("New directory was created");
            } catch (IOException e) {
                System.out.println("ERROR: IOException can't create folder for output file: " + e.getMessage());
                return;
            }
        }
        try (BufferedWriter writer = Files.newBufferedWriter(output)) {
            try (BufferedReader reader = Files.newBufferedReader(input)) {
                String pathName;
                while ((pathName = reader.readLine()) != null) {
                    try {
                        Path path = Path.of(pathName);
                        if (Files.isRegularFile(path)) {
                            hashedSha256(writer, pathName, path);
                        } else {
                            writeHash(writer, ERROR, pathName);
                        }
                    } catch (InvalidPathException e) {
                        handleError("Error: file wasn't found ", writer, pathName, e);
                    } catch (WriteHashException e) {
                        System.err.println(e.getMessage());
                    }
                }
            } catch (IOException e) {
                // :NOTE: здесь ловится reader.readLine
                System.err.println("Error: cannot read input file " + e.getMessage());
            } catch (SecurityException e) {
                System.err.println("Error: security violation was detected while reading input file");
            }
        } catch (IOException e) {
            System.err.println("Error: cannot write into output file " + e.getMessage());
        } catch (SecurityException e) {
            System.err.println("Error: security violation was detected while writing into output file");
        }
    }

    protected static void hashedSha256(BufferedWriter writer, String pathName, Path path) throws WriteHashException {
        byte[] bytes = null;
        try (FileInputStream inputStream = new FileInputStream(path.toString())) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = inputStream.read(byteArray)) != -1) {
                messageDigest.update(byteArray, 0, bytesCount);
            }
            bytes = messageDigest.digest();
        } catch (FileNotFoundException e) {
            handleError("Error: file wasn't found ", writer, pathName, e);

        } catch (SecurityException e) {
            handleError("Error: security violation was detected while hashing ", writer, pathName, e);
        } catch (IOException e) {
            // :NOTE: здесь ловится WriteHashException fixed
            handleError("Error: can't read file ", writer, pathName, e);
        }
        if (bytes != null) {
            writeHash(writer, HexFormat.of().formatHex(bytes), pathName);
        }
    }

    protected static void writeHash(BufferedWriter writer, String first, String second) throws WriteHashException {
        try {
            writer.write(String.format("%s %s", first, second));
            writer.newLine();
        } catch (IOException e) {
            throw new WriteHashException("Error: occurred some problems while writing to output file" + e.getMessage());
        }
    }

    protected static void handleError(String message, BufferedWriter writer, String pathName,
                                      Throwable e) throws WriteHashException {
        System.err.println(message + pathName + " " + e.getMessage());
        writeHash(writer, ERROR, pathName);
    }
}


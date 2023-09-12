package info.kgeorgiy.ja.gelmetdinov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk extends Walk {
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
                        if (!Files.exists(path)) {
                            System.err.println("Error: file/directory doesn't exist");
                            writeHash(writer, ERROR, pathName);
                        } else if (Files.isRegularFile(path)) {
                            hashedSha256(writer, pathName, path);
                        } else {
                            handleDirectory(writer, pathName);
                        }
                    } catch (InvalidPathException e) {
                        handleError("Error: file wasn't found ", writer, pathName, e);
                    } catch (WriteHashException e) {
                        System.err.println(e.getMessage());
                    }
                }
            } catch (IOException e) {
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

    private static void handleDirectory(BufferedWriter writer, String pathName) {
        try {
            Walker walker = new Walker(writer);
            Files.walkFileTree(Path.of(pathName), walker);
        } catch (IOException e) {
            System.err.println("Error: cannot open directory " + e.getMessage());
        }
    }

    static class Walker extends SimpleFileVisitor<Path> {
        BufferedWriter writer;

        public Walker(BufferedWriter writer) {
            this.writer = writer;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            String pathName = path.toString();
            // NOTE: может убиться при обходе файлов
            try {
                hashedSha256(writer, pathName, Path.of(pathName));
            } catch (WriteHashException e) {
                System.out.println("Error: occurred some problems while writing to output file" + e.getMessage());
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
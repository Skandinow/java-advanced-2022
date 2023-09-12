package info.kgeorgiy.ja.gelmetdinov.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Create implementation for the {@link JarImpler} interface
 */
public class Implementor implements JarImpler {
    /**
     * Default constructor
     */
    public Implementor(){}

    /**
     * Line separator constant
     */
    private static final String NEW_LINE = System.lineSeparator();
    /**
     * Padding constant
     */
    private static final String TAB = "\t";

    /**
     * Main runs {@link Implementor}
     *     @param args arguments for running an application
     * It is possible to run {@link Implementor} in two different ways:
     *     2 arguments: {@code <className> <path>}
     *     then the following method will be called
     *     {@code  Implementor#implement(Class<?> class, Path root)},
     *     it will create compiled class {@code <className>} in {@code <path>}
     *      3 arguments: -jar {@code <className> <path>}
     *         then the following method will be called
     *     {@code Implementor#implementJar(Class<?> class, Path root)},
     *     it will create compiled class and *.jar file {@code <className>} in {@code <path>}.
     */
    public static void main(String[] args) {
        if (args == null || !(args.length == 2 || args.length == 3)) {
            System.out.println("Error: invalid number of parameters: should be 2 or 3");
            return;
        }
        if (Arrays.stream(args).filter(Objects::isNull).toArray().length > 0) {
            System.out.println("Error: parameters shouldn't be nulls");
            return;
        }
        Implementor implementor = new Implementor();
        try {
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            } else {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            }
        } catch (ImplerException e) {
            System.err.println("Error while implementation: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Error: invalid class name: " + e.getMessage());

        }
    }

    /**
     * Produces <var>.jar</var> file implementing class or interface specified by provided <var>token</var>.
     * Generated class' name should be the same as the class name of the type token with <var>Impl</var> suffix
     * added.
     *
     * @param token type token to create implementation for.
     * @param path  target <var>.jar</var> file.
     * @throws ImplerException when implementation cannot be generated.
     */
    @Override
    public void implementJar(Class<?> token, Path path) throws ImplerException {
        if (!token.isInterface() || Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("ERROR: inappropriate class");
        }
        Path tempPath = createDirectory(path);
        implement(token, tempPath);
        compileFile(token, tempPath);
        createJar(token, path, tempPath);
    }

    /**
     * Creates a temporary directory with a prefix of "jar" in the parent directory of the given path.
     *
     * @param path The path for which the parent directory will be used as the parent directory for the
     *             markdown
     *             Copy code
     *             newly created temporary directory.
     * @return A {@link Path} object representing the newly created temporary directory.
     * @throws ImplerException If an IO error occurs while creating the temporary directory.
     */
    private static Path createDirectory(Path path) throws ImplerException {
        Path tempPath;
        try {
            tempPath = Files.createTempDirectory(path.toAbsolutePath().getParent(), "jar");
        } catch (IOException e) {
            throw new ImplerException("Error: cannot create jar directory");
        }
        return tempPath;
    }

    /**
     * Compiles a file in a specified directory {@link Path} <var>tempPath</var>
     *
     * @param token {@link Class} to compile
     * @param tempPath specified {@link Path}
     * @throws ImplerException if it is unable to compile a file
     */
    private void compileFile(Class<?> token, Path tempPath) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        String[] args = new String[]{
                "-cp",
                getClassPath(token),
                "-encoding", "UTF-8",
                extractPath(token, tempPath).toString()
        };
        if (compiler.run(null, null, null, args ) != 0) {
            throw new ImplerException("Can't compile files");
        }
    }

    /**
     * Get class' path by its name
     *
     * @param token {@link Class} from that we will extract required data
     * @return {@link String} path of given class
     * @throws ImplerException if failed to convert URL to URI
     */
    private static String getClassPath(Class<?> token) throws ImplerException {
        try {
            return Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException("Failed to convert URL to URI");
        }
    }

    /**
     * Get {@link Path} to token
     *
     * @param path of parent class
     * @param token which paath we want to aquire
     * @return {@link Path} to token
     */
    private Path getPath(Path path, Class<?> token) {
        return Paths.get(path.toString(),
                token.getPackageName().replaceAll("\\.", "\\" + File.separator),
                token.getSimpleName() + "Impl" + ".class");
    }

    /**
     * Creates .jar file implementing class or interface specified by provided clazz.
     *
     * @param token given {@link Class} to extract required data
     * @param path specified {@link Path}
     * @param tempPath {@link Path} - temporary directory where implementation will be located.
     * @throws ImplerException throws if {@link JarOutputStream} failed to write implementation .jar.
     */
    private void createJar(Class<?> token, Path path, Path tempPath) throws ImplerException {
        try (JarOutputStream writerJar = new JarOutputStream(Files.newOutputStream(path))) {
            writerJar.putNextEntry(new ZipEntry(getLocalPath(token, "/", ".class")));
            Files.copy(getPath(tempPath, token), writerJar);
        } catch (IOException e) {
            throw new ImplerException("Can't write to .jar file", e);
        }
    }


    /**
     * Produces code implementing class or interface specified by provided {@code token}.
     * Generated class' name should be the same as the class name of the type token with {@code Impl} suffix
     * added. Generated source code should be placed in the correct subdirectory of the specified
     * {@code root} directory and have correct file name. For example, the implementation of the
     * interface {@link java.util.List} should go to {@code $root/java/util/ListImpl.java}
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws info.kgeorgiy.java.advanced.implementor.ImplerException when implementation cannot be
     *                                                                 generated.
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (!token.isInterface() || Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("ERROR: inappropriate class");
        }
        Path path = extractPath(token, root);
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {
            bufferedWriter.write(generateHeader(token));

            for (Method method : token.getMethods()) {
                if (!Modifier.isStatic(method.getModifiers())) {
                    bufferedWriter.write(generateMethod(method));
                }
            }
            bufferedWriter.write("}".concat(NEW_LINE));
        } catch (IOException e) {
            throw new ImplerException("Error: problems while implementing class");
        }
    }

    /**
     * Produces {@link String} implementing class' method

     * @param method of given class
     * @return generated String body of <var>method</var>
     */
    private String generateMethod(Method method) {
        return generateSignature(method) + generateBody(method);
    }


    /**
     * Produces {@link String} implementing method signature
     * @param method of given class
     * @return generated String of method signature
     */
    private String generateSignature(Method method) {
        StringBuilder stringBuilder = new StringBuilder();
        int modifiers = method.getModifiers();
        stringBuilder
                .append(TAB)
                .append(Modifier.toString(modifiers & ~Modifier.TRANSIENT & ~Modifier.NATIVE & ~Modifier.ABSTRACT))
                .append(" ")
                .append(method.getReturnType().getCanonicalName())
                .append(" ")
                .append(method.getName())
                .append(String.format("(%s)", generateParameteres(method)))
                .append(generateException(method));

        return stringBuilder.toString();
    }


    /**
     * Produces {@link String} implementing method body

     * @param method of given class
     * @return generated String of method body
     */
    private String generateBody(Method method) {
        StringBuilder stringBuilder = new StringBuilder(" {".concat(NEW_LINE).concat(TAB)
                .concat(TAB).concat("return "));

        if (Objects.equals(void.class, method.getReturnType())) {
            stringBuilder.append(";");
        } else if (Objects.equals(boolean.class, method.getReturnType())) {
            stringBuilder.append("false;");
        } else if (method.getReturnType().isPrimitive()) {
            stringBuilder.append("0;");
        } else {
            stringBuilder.append("null;");
        }
        return stringBuilder.append(NEW_LINE).append("}").append(NEW_LINE).toString();
    }


    /**
     * Produces {@link String} implementing method Exceptions

     * @param method of given class
     * @return generated String that contains Exceptions that belongs th given method
     */
    private String generateException(Method method) {
        Class<?>[] exception = method.getExceptionTypes();
        return (exception.length != 0) ? " throws " + Arrays.stream(exception)
                .map(Class::getCanonicalName).collect(Collectors.joining(", ")) : "";
    }//todo нормально вывод через стримы осознать

    /**
     * Produces {@link String} implementing method Parameters
     * @param method of given class
     * @return generated String that contains Exceptions that belongs th given method
     */
    private String generateParameteres(Method method) {
        return Arrays.stream(method.getParameters())
                .map(this::generateParameter).collect(Collectors.joining(", "));
    }

    /**
     * Produces {@link String} that contains Class and unique parameter name of a method parameter

     * @param parameter of given method
     * @return generated String that contains list of Exceptions that belongs th given method
     */
    private String generateParameter(Parameter parameter) {
        StringBuilder stringBuilder = new StringBuilder();
        String name = parameter.getType().getCanonicalName();
        stringBuilder.append(name)
                .append(" ")
                .append(parameter.getName());

        return stringBuilder.toString();
    }

    /**
     * Produces {@link String} that contains Class package, if it exists, all the modifiers,
     * name of a class and a name of implementable interface
     * @param token {@link Class} to extract all the data from
     * @return generated String that contains list of Exceptions that belongs th given method
     */
    private String generateHeader(Class<?> token) {
        StringBuilder header = new StringBuilder();
        String packageName = token.getPackage().getName();
        if (!packageName.isEmpty()) {
            header.append("package ").append(packageName).append(";");
        }

        header.append(NEW_LINE)
                .append("public class")
                .append(" ")
                .append(getImplClassName(token))
                .append(" implements ")
                .append(token.getCanonicalName())
                .append(" {")
                .append(NEW_LINE);

        return header.toString();
    }

    /**
     * Extracts the path to the implementation class.
     * @param token the class for which the implementation path is to be extracted
     * @param root  the root directory under which the implementation path is to be created
     * @return the path to the implementation class
     * @throws ImplerException if an error occurs while creating the directory for the implementation file
     */
    private Path extractPath(Class<?> token, Path root) throws ImplerException {
        Path path = root.resolve(getLocalPath(token, File.separator, ".java"));
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (IOException e) {
                throw new ImplerException("Error: cannot create directory for file");
            }
        }
        return path;
    }

    /**
     * Returns the local path for the given class.
     * @param token       the class for which the local path is to be returned
     * @param replacement the replacement string to be used for package name separators
     * @param format      the format string to be used for the implementation class file
     * @return the local path for the given class
     */
    private String getLocalPath(Class<?> token, String replacement, String format) {
        return token.getPackage().getName().replace(".", replacement)
                .concat("/" + getImplClassName(token) + format);
    }

    /**
     * Returns the name of the implementation class for the given class.
     * @param token the class for which the implementation class name is to be returned
     * @return the name of the implementation class for the given class
     */
    private String getImplClassName(Class<?> token) {
        return token.getSimpleName().concat("Impl");
    }

}

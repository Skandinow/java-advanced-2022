package info.kgeorgiy.ja.gelmetdinov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")

public class IterativeParallelism implements ScalarIP {
    @SuppressWarnings("unused")

    private ParallelMapper parallelMapper;

    /**
     * Default constructor
     */
    @SuppressWarnings("unused")
    public IterativeParallelism() {
    }

    /**
     * Constructor with {@code ParallelMapper}
     *
     * @param parallelMapper initialize {@code ParallelMapper} field
     */
    @SuppressWarnings("unused")
    public IterativeParallelism(ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Returns maximum value.
     *
     * @param numberOfThreads number of concurrent threads.
     * @param values          values to get maximum of.
     * @param comparator      value comparator.
     * @param <T>             value type.
     * @return maximum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T maximum(int numberOfThreads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return threadHandler(numberOfThreads, values,
                stream -> stream.max(comparator).orElseThrow(),
                stream -> stream.max(comparator).orElseThrow());
    }


    /**
     * Returns minimum value.
     *
     * @param numberOfThreads number of concurrent threads.
     * @param values          values to get minimum of.
     * @param comparator      value comparator.
     * @param <T>             value type.
     * @return minimum of given values
     * @throws InterruptedException             if executing thread was interrupted.
     * @throws java.util.NoSuchElementException if no values are given.
     */
    @Override
    public <T> T minimum(int numberOfThreads, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(numberOfThreads, values, Collections.reverseOrder(comparator));
    }

    /**
     * Returns whether all values satisfy predicate.
     *
     * @param numberOfThreads number of concurrent threads.
     * @param values          values to test.
     * @param predicate       test predicate.
     * @param <T>             value type.
     * @return whether all values satisfy predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int numberOfThreads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return threadHandler(numberOfThreads, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(it -> it));
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param numberOfThreads number of concurrent threads.
     * @param values          values to test.
     * @param predicate       test predicate.
     * @param <T>             value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int numberOfThreads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(numberOfThreads, values, predicate.negate());
    }

    /**
     * Returns number of values satisfying predicate.
     *
     * @param numberOfThreads number of concurrent threads.
     * @param values          values to test.
     * @param predicate       test predicate.
     * @param <T>             value type.
     * @return number of values satisfying predicate.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> int count(int numberOfThreads, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return Math.toIntExact(threadHandler(numberOfThreads, values,
                stream -> stream.filter(predicate).count(),
                stream -> stream.reduce(0L, Long::sum)));
    }

    private <T, R> R threadHandler(int numberOfThreads, List<? extends T> values,
                                   final Function<Stream<? extends T>, R> function,
                                   final Function<Stream<R>, R> resCol) throws InterruptedException {

        numberOfThreads = Math.min(numberOfThreads, values.size());
        int chunkSize = values.size() / numberOfThreads;
        int rest = values.size() % numberOfThreads;
        List<Stream<? extends T>> threadList = new ArrayList<>();

        List<R> result = new ArrayList<>(Collections.nCopies(numberOfThreads, null));

        List<Thread> threadsList = (parallelMapper == null) ? new ArrayList<>() : null;

        int second = 0;
        for (int i = 0; i < numberOfThreads; i++) {
            int first = second;
            second = first + chunkSize + Integer.signum(Integer.compare(rest, 0));
            if (rest > 0) {
                rest--;
            }
            int finalSecond = second;

            if (parallelMapper != null) {
                threadList.add(values.subList(first, finalSecond).stream());
            } else {
                int finalI = i;
                List<R> finalResult = result;
                Thread partThread = new Thread(() ->
                        finalResult.set(finalI,
                                function.apply(values.subList(first, finalSecond).stream())));

                partThread.start();
                threadsList.add(partThread);

            }
        }

        if (parallelMapper == null) {
            for (Thread thread : threadsList) {
                thread.join();
            }
        } else {
            result = parallelMapper.map(function, threadList);
        }

        return resCol.apply(result.stream());
    }
}

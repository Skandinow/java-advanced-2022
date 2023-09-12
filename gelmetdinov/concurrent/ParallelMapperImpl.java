package info.kgeorgiy.ja.gelmetdinov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;

/**
 * Implementation of {@link ParallelMapper} interface for parallel mapping.
 */
@SuppressWarnings("unused")
public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> threadList;
    private final Queue<Process> queue;


    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performed in parallel.
     *
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args)
            throws InterruptedException {


        List<R> result = new ArrayList<>(Collections.nCopies(args.size(), null));
        final IntShell counter = new IntShell(args.size());

        for (int i = 0; i < args.size(); ++i) {
            final int finalI = i;
            Process process = new Process(
                    () -> result.set(finalI, f.apply(args.get(finalI))), counter);

            synchronized (queue) {
                queue.add(process);
                queue.notifyAll();
            }
        }
        //
        synchronized (counter) {
            while (counter.tasksCounter > 0) {
                counter.wait();
            }
        }

        return result;
    }

    /** Stops all threads. All unfinished mappings are left in undefined state. */
    @Override
    public void close() {
        threadList.forEach(Thread::interrupt);

        threadList.forEach(thread -> {
                    try {
                        thread.join();
                    } catch (InterruptedException ignored) {
                    }
                }
        );
    }

    /**
     * Creates constructor that creates {@code ParallelMapperImpl} with {@code numberOfThreads} threads
     * @param numberOfThreads number of required threads to create
     */
    public ParallelMapperImpl(int numberOfThreads) {
        threadList = new ArrayList<>();
        queue = new LinkedBlockingDeque<>();

        for (int i = 0; i < numberOfThreads; i++) {
            threadList.add(new Thread(runProcess()));

            threadList.get(i).start();
        }

    }
//
    private Runnable runProcess() {
        return () -> {
            try {
                while (!Thread.interrupted()) {
                    Process process;
                    synchronized (queue) {
                        while (queue.isEmpty()) {
                            queue.wait();
                        }

                        process = queue.poll();

                    }

                    process.runnable.run();

                    synchronized (process.tasksCounter) {
                        process.tasksCounter.tasksCounter--;
                        if (process.tasksCounter.tasksCounter < 1) {
                            process.tasksCounter.notify();
                        }
                    }
                }
            } catch (InterruptedException ignored) {
            } finally {
                Thread.currentThread().interrupt();
            }
        };
    }


    private static class Process {
        Runnable runnable;
        private final IntShell tasksCounter;

        Process(Runnable runnable, IntShell tasks) {
            this.runnable = runnable;
            tasksCounter = tasks;
        }
    }

    private static class IntShell {
        private int tasksCounter;

        IntShell(int tasks) {
            tasksCounter = tasks;
        }

    }

}
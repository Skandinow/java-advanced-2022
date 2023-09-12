package info.kgeorgiy.ja.gelmetdinov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {

    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final Downloader downloader;
    private final Map<String, TaskQueue> hostMap = new ConcurrentHashMap<>();
    private final int perHost;


    public static void main(String[] args) {
        if (args == null || args.length > 5) {
            System.err.println("Error: Inappropriate arguments");
            return;
        }
        String url = args[0];
        int depth = getArgsI(args, 1);
        int downloaders = getArgsI(args, 2);
        int extractors = getArgsI(args, 3);
        int perhost = getArgsI(args, 4);
        try {
            Downloader downloader = new CachingDownloader(0.0);
            WebCrawler crawler = new WebCrawler(downloader,
                    downloaders, extractors, perhost);
            crawler.download(url, depth);
            crawler.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getArgsI(String[] args, int index) {
        return args.length > index ? Integer.parseInt(args[1]) : 1;
    }

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;

    }

    @Override
    public Result download(String url, int depth) {
        Phaser phaser = new Phaser(1);

        Set<String> used = new ConcurrentSkipListSet<>();
        Set<String> result = new ConcurrentSkipListSet<>();
        Map<String, IOException> errors = new ConcurrentHashMap<>();
        used.add(url);

        recursiveDownload(url, depth, phaser, used, result, errors);
        phaser.arriveAndAwaitAdvance();

        return new Result(new ArrayList<>(result), errors);
    }


    private void recursiveDownload(String link, int depth, Phaser phaser,
                                   Set<String> used,
                                   Set<String> result,
                                   Map<String, IOException> errors) {
        String host;
        try {
            host = URLUtils.getHost(link);
        } catch (MalformedURLException e) {
            errors.put(link, e);
            return;
        }


        TaskQueue queue = hostMap.computeIfAbsent(host, s -> new TaskQueue());
        phaser.register();
        queue.addTask(() -> {
            try {
                Document document = downloader.download(link);
                result.add(link);

                if (depth > 1) {
                    phaser.register();
                    extractors.submit(
                            () -> {
                                try {
                                    List<String> list = document.extractLinks();
                                    for (String url : list) {
                                        if (used.add(url)) {
                                            recursiveDownload(url, depth - 1, phaser, used, result, errors);
                                        }
                                    }
                                } catch (IOException ignored) {
                                } finally {
                                    phaser.arrive();
                                }
                            }
                    );
                }


            } catch (IOException e) {
                errors.put(link, e);
            } finally {
                phaser.arrive();

            }
        });
    }

    @Override
    public void close() {
        downloaders.shutdown();
        extractors.shutdown();
    }

    private class TaskQueue {
        private final Queue<Runnable> queue;
        private int counter;

        TaskQueue() {
            queue = new LinkedBlockingDeque<>();
        }

        private synchronized void decreaseCounter() {
            counter--;
        }

        private synchronized void addTask(Runnable task) {
            queue.add(task);
            runTask();
        }

        private synchronized void runTask() {
            if (!queue.isEmpty() || counter < perHost) {
                Runnable task = queue.poll();
                counter++;
                if (task != null) {
                    downloaders.submit(task);
                }
                decreaseCounter();
            }
        }
    }
}

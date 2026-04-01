/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094;

import static oeis.a136094.Bundle.parseBundles;
import static oeis.a136094.util.FileUtils.readLinesFromFile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

import oeis.a136094.key.Key;
import oeis.a136094.key.KeyBuilder;
import oeis.a136094.util.MemoryEfficientHashMap;

public class Checkpoint implements Runnable {
    
    private final File file;
    private final Thread thread;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final Map<Key, Integer> solutions = Collections.synchronizedMap(new MemoryEfficientHashMap<>());
    private volatile boolean shutdown;

    public Checkpoint(File file) {
        loadSolutions(file, solutions);
        this.file = file;
        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        if (!Main.NO_SAVE_FILES) {
            this.thread.start();
        }
    }

    private static void loadSolutions(File file, Map<Key, Integer> solutions) {
        File fileGz = new File(file.getPath() + ".gz");
        File sourceFile = fileGz.exists() ? fileGz : file;
        if (!sourceFile.exists()) return;
        System.out.println("Loading checkpoint ...");
        Map<Bundle[], Integer> answers = new ConcurrentHashMap<>();
        KeyBuilder.generateKeysInParallel((consumer) -> {
            loadSolutionsFromFile(sourceFile, (bundles, ans) -> {
                answers.put(bundles, ans.length());
                consumer.accept(bundles);
            });
        }, (bundles, key) -> {
            Integer ansLen = answers.get(bundles);
            solutions.put(key, ansLen);
        });
    }
    
    public boolean isEmpty() {
        return solutions.isEmpty();
    }
    
    public boolean containsKey(Key key) {
        return solutions.containsKey(key);
    }

    public Integer get(Key key) {
        return solutions.get(key);
    }
    
    public void put(Bundle[] problem, Key key, String ans) {
        try {
            solutions.put(key, ans.length());
            queue.put(Bundle.bundlesToString(problem) + " " + ans);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                if (!queue.isEmpty()) {
                    List<String> lines = new ArrayList<>();
                    queue.drainTo(lines);
                    if (!file.exists()) {
                        file.getAbsoluteFile().getParentFile().mkdirs();
                    }
                    try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true)))) {
                        for (String line : lines) {
                            writer.println(line);
                        }
                    }
                }
                if (shutdown) {
                    if (!queue.isEmpty()) continue; else break;
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void shutdown() {
        try {
            this.shutdown = true;
            this.thread.interrupt();
            this.thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void loadSolutionsFromFile(File file, BiConsumer<Bundle[], String> callback) {
        readLinesFromFile(file, (line) -> {
            int index = line.lastIndexOf(' ');
            String answer = line.substring(index+1);
            Bundle[] bundles = parseBundles(line.substring(0, index));
            callback.accept(bundles, answer);
        });
    }

}
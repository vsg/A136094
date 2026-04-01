/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.solver;

import static oeis.a136094.util.FileUtils.printWriteToFileGZ;
import static oeis.a136094.util.FileUtils.readLinesFromFile;
import static oeis.a136094.util.ParallelUtils.processInBatches;
import static oeis.a136094.util.ParallelUtils.processInParallel;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import com.linkedin.migz.MiGzOutputStream;

import oeis.a136094.Bundle;
import oeis.a136094.Main;
import oeis.a136094.key.Key;
import oeis.a136094.move.MoveBuilder;
import oeis.a136094.partials.Partials;
import oeis.a136094.util.MemoryEfficientHashSet;
import oeis.a136094.util.LogUtils;

public class DFSDiskSolver extends Solver {

    private static class Block {
        
        private final File blockFile;
        private final File tmpFile;
        private final PrintWriter writer;
        private int numPrefixesWritten;

        public Block(File blockFile) {
            this.blockFile = blockFile;
            this.tmpFile = new File(blockFile.getParent(), blockFile.getName() + ".tmp");
            this.writer = createWriter(tmpFile);
        }

        private PrintWriter createWriter(File file) {
            file.getAbsoluteFile().getParentFile().mkdirs();
            try {
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile), 65536);
                MiGzOutputStream gzOut = new MiGzOutputStream(out, Main.NUM_WORKER_THREADS, 65536);
                return new PrintWriter(gzOut);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        public void writePrefix(String prefix) {
            writer.println(prefix);
            numPrefixesWritten++;
        }

        public int size() {
            return numPrefixesWritten;
        }

        public void closeWriter() {
            writer.close();
        }

        public void commitBlockFile() {
            if (blockFile.exists() && !blockFile.delete()) {
                throw new RuntimeException();
            }
            if (!tmpFile.renameTo(blockFile)) {
                throw new RuntimeException();
            }
        }

    }

    private static class BlockReader implements Runnable {
        
        private final File prefixesFile;
        private final Bundle[] bundles0;
        private final int bestAnsLen;
        private final Consumer<Batch> batchCallback;

        public BlockReader(File prefixesFile, Bundle[] bundles0, int bestAnsLen, Consumer<Batch> batchCallback) {
            this.prefixesFile = prefixesFile;
            this.bundles0 = bundles0;
            this.bestAnsLen = bestAnsLen;
            this.batchCallback = batchCallback;
        }
        
        @Override
        public void run() {
            try {
                processInBatches((consumer) -> {
                    readLinesFromFile(prefixesFile, prefix -> {
                        consumer.accept(new Node(prefix, null, null));
                    });
                }, Main.DFS_DISK_BATCH_SIZE, (ArrayList<Node> nodes) -> {
                    processInParallel(nodes, Main.NUM_WORKER_THREADS, () -> {
                        final MoveBuilder moveBuilder = new MoveBuilder();
                        return (node) -> {
                            String prefix = node.prefix;
                            node.sortedBundles = moveBuilder.makeMovesAndSort(bundles0, prefix);
                        };
                    });
                    batchCallback.accept(new Batch(bestAnsLen, nodes));
                });
                batchCallback.accept(new Batch(-1, Collections.emptyList())); // signal end of input
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(-1);
            }
        }
        
    }

    private static class MultiBlockWriter {

        private final File bestAnsLenDir;
        private final List<Block> blocks = new ArrayList<>();
        private Block currentBlock;
        
        public MultiBlockWriter(File bestAnsLenDir) {
            this.bestAnsLenDir = bestAnsLenDir;
        }
        
        public void writePrefix(String prefix) {
            if (currentBlock == null) {
                startBlock(prefix);
            }

            currentBlock.writePrefix(prefix);

            if (currentBlock.size() == Main.DFS_DISK_BLOCK_SIZE) {
                closeBlock();
            }
        }

        private void startBlock(String prefix) {
            currentBlock = new Block(prefixesFile(bestAnsLenDir, prefix));
            blocks.add(currentBlock);
        }

        public void closeBlock() {
            if (currentBlock != null) {
                currentBlock.closeWriter();
                currentBlock = null;
            }
        }

    }
    
    private static class Progress {

        private final Bundle[] bundles0;
        private final int bestAnsLen;
        private final int level;
        private final String minPrefix;

        public Progress(Bundle[] bundles0, int bestAnsLen, int level, String minPrefix) {
            this.bundles0 = bundles0;
            this.bestAnsLen = bestAnsLen;
            this.level = level;
            this.minPrefix = minPrefix;
        }

        public void print(String message) {
            if (Main.PRINT_PROGRESS) {
                System.out.println(LogUtils.formatLog("%s %d %d %s: %s", 
                        Bundle.bundlesToString(bundles0), bestAnsLen, level, minPrefix, message));
            }
        }
        
    }
    
    public DFSDiskSolver(Partials partials) {
        super(partials);
    }

    private static final File problemDir(Bundle[] bundles) {
        return new File(Bundle.bundlesToString(bundles).replace(' ', '_').replace('/', '@'));
    }

    private static final File bestAnsLenDir(Bundle[] bundles, int bestAnsLen) {
        return new File(problemDir(bundles), String.format("%02d", bestAnsLen));
    }

    private static final File prefixesFile(File bestAnsLenDir, String minPrefix) {
        return new File(bestAnsLenDir, String.format("%s.txt.gz", minPrefix));
    }

    @Override
    public String solve(Bundle[] bundles0, int bestAnsLen) {
        if (findExistingBestAnsLenDir(bundles0) > bestAnsLen) {
            return null;
        }

        File bestAnsLenDir = bestAnsLenDir(bundles0, bestAnsLen);

        File prefixesFile = findMinPrefixesFile(bestAnsLenDir);
        if (prefixesFile == null) {
            prefixesFile = prefixesFile(bestAnsLenDir, "");
            printWriteToFileGZ(prefixesFile, pw -> {
                pw.println();
            });
        }

        while (prefixesFile != null) {
            String answer = processPrefixes(bundles0, bestAnsLen, prefixesFile);
            if (answer != null) {
                return answer;
            }
            prefixesFile = findMinPrefixesFile(bestAnsLenDir);
        }

        if (!bestAnsLenDir.delete()) {
            throw new RuntimeException();
        }

        return null;
    }

    private int findExistingBestAnsLenDir(Bundle[] bundles) {
        for (int len = 1; len <= 99; len++) {
            File dir = bestAnsLenDir(bundles, len);
            if (dir.exists())
                return len;
        }
        return -1;
    }

    private File findMinPrefixesFile(File dir) {
        return Optional.ofNullable(dir.listFiles((_, name) -> name.endsWith(".txt.gz")))
                .flatMap(files -> Arrays.stream(files).sorted().findFirst())
                .orElse(null);
    }

    private String processPrefixes(Bundle[] bundles0, int bestAnsLen, File prefixesFile) {
        long beginBlock = System.currentTimeMillis();
        
        File bestAnsLenDir = bestAnsLenDir(bundles0, bestAnsLen);
        MultiBlockWriter blockWriter = new MultiBlockWriter(bestAnsLenDir);

        String prefixesFileName = prefixesFile.getName();
        String minPrefix = prefixesFileName.substring(0, prefixesFileName.indexOf('.'));
        int level = minPrefix.length();

        Progress progress = new Progress(bundles0, bestAnsLen, level, minPrefix);
        
        AtomicLong numInNodes = new AtomicLong();
        AtomicLong numOutPrefixes = new AtomicLong();
        
        BlockingQueue<Batch> processorQueue = new LinkedBlockingQueue<>();
        BlockingQueue<Batch> orderingQueue = new ArrayBlockingQueue<>(5);
        
        BlockReader blockReader = new BlockReader(prefixesFile, bundles0, bestAnsLen, (batch) -> {
            try {
                processorQueue.add(batch);
                orderingQueue.add(batch);
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(-1);
            }
        });
        new Thread(blockReader).start();
        
        BatchNodeProcessor batchProcessor = new BatchNodeProcessor(partials, processorQueue);
        new Thread(batchProcessor).start();

        String answer = solveBlock(orderingQueue, bestAnsLen, blockWriter, numInNodes, numOutPrefixes, progress);
        
        batchProcessor.shutdown();
        
        blockWriter.closeBlock();

        if (answer != null) {
            return answer;
        }

        progress.print("next_prefixes = " + numOutPrefixes.get());

        for (Block block : blockWriter.blocks) {
            progress.print("save next prefixes " + block.size() + " into " + block.blockFile.getName());

            block.commitBlockFile();
        }

        if (!prefixesFile.delete()) {
            throw new RuntimeException();
        }

        long endBlock = System.currentTimeMillis();

        progress.print("block = " + LogUtils.timeStr(endBlock - beginBlock));

        return null;
    }

    private String solveBlock(BlockingQueue<Batch> queue, int bestAnsLen, MultiBlockWriter blockWriter,
            AtomicLong numInNodes, AtomicLong numOutPrefixes, Progress progress) {
        String answer = null;
        Set<Key> seenNodes = new MemoryEfficientHashSet<>();
        while (true) {
            Batch batch;
            try {
                batch = queue.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (batch.nodes.isEmpty()) break;
            
            batch.ensureProcessed();
            
            if (answer != null) continue; // make sure block reader completes
            
            answer = processBatch(batch, blockWriter, numInNodes, numOutPrefixes, seenNodes, progress);
        }
        return answer;
    }

    private String processBatch(Batch batch, MultiBlockWriter blockWriter, AtomicLong numInNodes,
            AtomicLong numOutPrefixes, Set<Key> seenNodes, Progress progress) {
        for (Node node : batch.nodes) {
            for (Node nextNode : node.nextNodes) {
                String prefix = nextNode.prefix;
                Bundle[] bundles = nextNode.sortedBundles;
                Key key = nextNode.key;

                if (bundles.length == 0) {
                    return prefix;
                }
                
                if (!seenNodes.add(key)) continue;
                
                blockWriter.writePrefix(prefix);
                numOutPrefixes.incrementAndGet();

                if (seenNodes.size() == Main.DFS_DISK_SEEN_SIZE) {
                    progress.print("trim");

                    trimSeenNodes(seenNodes);
                }
            }
            
            long numIn = numInNodes.incrementAndGet();
            if (numIn % 1000000 == 0) {
                long numOut = numOutPrefixes.get();
                progress.print(numIn + " -> " + numOut + " " + Bundle.bundlesToString(node.sortedBundles));
            }
        }
        return null;
    }

    private void trimSeenNodes(Set<Key> seenNodes) {
        Iterator<Key> iterator = seenNodes.iterator();
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
            if (count == 3) {
                iterator.remove(); // remove every third item
                count = 0;
            }
        }
    }

}
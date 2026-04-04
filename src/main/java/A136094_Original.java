/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
import static java.lang.Integer.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

//import org.openjdk.jol.info.GraphLayout;

import com.linkedin.migz.MiGzInputStream;
import com.linkedin.migz.MiGzOutputStream;

import oeis.a136094.key.Permutations;
import oeis.a136094.util.MemoryEfficientHashMap;
import oeis.a136094.util.MemoryEfficientHashSet;

/**
 * The original, unrefactored version of the program used to compute n=9.
 */
public class A136094_Original {

    public static class Bundle {
        
        private final int bundle;
        private final int shape;
        private final int index;

        //public static final int[] C_9 = new int[] {1, 9, 36, 84, 126, 126, 84, 36, 9, 1};
        //public static final int[] NUM_PIECES_BY_SIZE = new int[] {0, 9, 72, 252, 504, 630, 504, 252, 72, 9};// = i*C(9, i), for MAX_N=9
        //public static final int[] NUM_BUNDLES_BY_SIZE = new int[] {0, 9, 108, 588, 1890, 3906, 5292, 4572, 2295, 511};// for MAX_N=9
        private static final Bundle[] packedToBundle = new Bundle[1<<18];
        
        public static int numBundles;
        public static final int[] maxBundleIndexOfShape = new int[256];
        public static final Bundle[][] bundlesOfShape = new Bundle[256][];

        private static final String[] indexToString = new String[20000];
        private static final Map<String, Bundle> stringToBundle = new HashMap<>();

        static {
            Bundle[][] bundlesByShape = new Bundle[256][6000];
            int[] numBundlesByShape = new int[256];
            
            int numBundles = 0;
            
            for (int numDigits = 1; numDigits <= 9; numDigits++) {
                for (int digits = 1; digits <= MASK_9; digits++) {
                    if (bitCount(digits) != numDigits) continue;
                    
                    for (int numHeads = 1; numHeads <= numDigits; numHeads++) {
                        for (int heads = 1; heads <= digits; heads++) {
                            if (bitCount(heads) != numHeads) continue;
                            
                            if ((heads & ~digits) != 0) continue;

                            int bundleIndex = numBundles++;
                            String bundleStr = maskDigitsToString(heads) + "/" + maskDigitsToString(digits);
                            
                            Bundle bundle = new Bundle(heads, digits, bundleIndex);
                            int shape = bundle.shape();
                            int packed = bundle.pack();
                            
                            stringToBundle.put(bundleStr, bundle);
                            indexToString[bundleIndex] = bundleStr;
                            
                            packedToBundle[packed] = bundle;
                            
                            int numShapeBundles = numBundlesByShape[shape]++;
                            bundlesByShape[shape][numShapeBundles] = bundle;
                            
                            maxBundleIndexOfShape[shape] = bundleIndex;
                        }
                    }
                }
            }

            for (int shape = 0; shape < 256; shape++) {
                bundlesOfShape[shape] = Arrays.copyOf(bundlesByShape[shape], numBundlesByShape[shape]);
            }
            
            Bundle.numBundles = numBundles;
        }

        public Bundle(int heads, int digits, int index) {
            this.bundle = (heads << 9) | digits;
            this.shape = (bitCount(digits) << 4) | bitCount(heads);
            this.index = index;
        }
        
        public int index() {
            return index;
        }
        
        public int heads() {
            return bundle >> 9;
        }
        
        public int digits() {
            return bundle & MASK_9;
        }
        
        public int numHeads() {
            return shape & MASK_4;
        }
        
        public int numDigits() {
            return shape >> 4;
        }
        
        public int shape() {
            return shape;
        }
        
        public int pack() {
            return bundle;
        }
        
        public static Bundle unpack(int packed) {
            return packedToBundle[packed];
        }
        
        public int toSortable() {
            return (shape << 18) | bundle;
        }

        public Bundle swapBundleDigits(int[] swap) {
            int result = 0;
            for (int src = 0; src < 9; src++) {
                int dest = swap[src];
                result |= ((bundle >> (9 + src)) & 1) << (9 + dest);
                result |= ((bundle >> src) & 1) << dest;
            }
            return Bundle.unpack(result);
        }

        @Override
        public String toString() {
            return indexToString[index];
        }

        public static Bundle parse(String str) {
            return stringToBundle.get(str);
        }

        public static int[] packAll(Bundle[] bundles) {
            int[] result = new int[bundles.length];
            for (int i = 0; i < bundles.length; i++) {
                result[i] = bundles[i].pack();
            }
            return result;
        }
        
        public static Bundle[] unpackAll(int[] bundles) {
            Bundle[] result = new Bundle[bundles.length];
            for (int i = 0; i < bundles.length; i++) {
                result[i] = unpack(bundles[i]);
            }
            return result;
        }
        
        public static int swapPackedDigits(int bundle, int d1, int d2) {
            int heads = bundle >> 9;
            int digits = bundle & MASK_9;
            int hh = ((heads >> d1) ^ (heads >> d2)) & 1;
            int dd = ((digits >> d1) ^ (digits >> d2)) & 1;
            heads ^= (hh << d1) | (hh << d2);
            digits ^= (dd << d1) | (dd << d2);
            return (heads << 9) | digits;
        }
        
    }
    
    @SuppressWarnings("serial")
    public static class Key implements Serializable {
        int[] values;
        int hashCode;

        Key(int[] values) {
            this.values = values;
            this.hashCode = Arrays.hashCode(values);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Key other = (Key) obj;
            return hashCode == other.hashCode && Arrays.equals(values, other.values);
        }

        @Override
        public String toString() {
            return bundlesStr(Bundle.unpackAll(values));
        }
    }

    @SuppressWarnings("serial")
    private static class SolutionLengthLookup implements Serializable {
        
        private SolutionLengthLookup[] next; // bundleIndex -> nextStateIndex
        private byte[] nextSolutionLength; // bundleIndex -> nextSolutionLength
        private int maxKnownNextSolutionLength; // next, or next-next, or ...
        private int maxNextShape;
        private int maxNextBundles;
        
        public SolutionLengthLookup(int maxNextShape) {
            this.maxNextShape = maxNextShape;
            this.maxNextBundles = Bundle.maxBundleIndexOfShape[maxNextShape]+1;
        }
        
        public SolutionLengthLookup updateNextLookup(Bundle bundle) {
            int bundleIndex = bundle.index();
            int maxNextShape = bundle.shape();
            if (next == null) {
                next = new SolutionLengthLookup[maxNextBundles];
            }
            if (next[bundleIndex] == null) {
                next[bundleIndex] = new SolutionLengthLookup(maxNextShape);
            }
            return next[bundleIndex];
        }

        public void updateNextSolutionLength(Bundle bundle, int len) {
            int bundleIndex = bundle.index();
            if (this.nextSolutionLength == null) {
                this.nextSolutionLength = new byte[maxNextBundles];
            }
            if (this.nextSolutionLength[bundleIndex] == 0) {
                this.nextSolutionLength[bundleIndex] = (byte) len;
            } else {
                if (this.nextSolutionLength[bundleIndex] != len) throw new RuntimeException();
            }
        }

        public void updateMaxKnownNextSolutionLength(int maxLen) {
            if (this.maxKnownNextSolutionLength < maxLen) {
                this.maxKnownNextSolutionLength = maxLen;
            }
        }

        public SolutionLengthLookup getNextLookup(Bundle bundle) {
            int bundleIndex = bundle.index();
            if (next == null) {
                return null;
            }
            return next[bundleIndex];
        }
        
        public int getNextSolutionLength(Bundle bundle) {
            int bundleIndex = bundle.index();
            if (nextSolutionLength == null) {
                return 0;
            }
            return nextSolutionLength[bundleIndex];
        }

        public int getMaxKnownNextSolutionLength() {
            return maxKnownNextSolutionLength;
        }

        public void mergeTo(SolutionLengthLookup other) {
            if (this.maxNextShape != other.maxNextShape) throw new RuntimeException();
            if (this.maxNextBundles != other.maxNextBundles) throw new RuntimeException();
            if (other.maxKnownNextSolutionLength < this.maxKnownNextSolutionLength) {
                other.maxKnownNextSolutionLength = this.maxKnownNextSolutionLength;
            }
            if (this.nextSolutionLength != null) {
                if (other.nextSolutionLength == null) {
                    other.nextSolutionLength = new byte[other.maxNextBundles];
                }
                mergeSolutionLengthFromTo(this.nextSolutionLength, other.nextSolutionLength);
            }
            if (this.next != null) {
                if (other.next == null) {
                    other.next = new SolutionLengthLookup[other.maxNextBundles];
                }
                for (int i = 0; i < next.length; i++) {
                    if (this.next[i] != null) {
                        if (other.next[i] == null) {
                            other.next[i] = new SolutionLengthLookup(this.next[i].maxNextShape);
                        }
                        this.next[i].mergeTo(other.next[i]);
                    }
                }
            }
        }
        
    }

    @SuppressWarnings("serial")
    private static class Precalc implements Serializable {
        
        private final byte[] maxKnownSolutionLengthByShape1 = new byte[1<<12]; // shape1 -> maxKnownSolutionLength
        private final byte[] maxKnownSolutionLengthByShape12 = new byte[1<<20]; // shape1+shape2 -> maxKnownSolutionLength
        private final byte[] solutionLength; // stateIndex -> solutionLength
        private final byte[] maxKnownNextSolutionLength; // stateIndex -> maxKnownNextSolutionLength
        private final SolutionLengthLookup[] lookup; // stateIndex -> solutionLengthLookup
        private final int numStates;
        
        public Precalc(int numStates) {
            this.solutionLength = new byte[numStates];
            this.maxKnownNextSolutionLength = new byte[numStates];
            this.lookup = new SolutionLengthLookup[numStates];
            this.numStates = numStates;
        }
        
        public void updateMaxKnownSolutionLengthByShape1(int swapLevel, int shape1, int len) {
            int key = shapeKey1(swapLevel, shape1);
            if (this.maxKnownSolutionLengthByShape1[key] < len) {
                this.maxKnownSolutionLengthByShape1[key] = (byte) len;
            }
        }

        public void updateMaxKnownSolutionLengthByShape12(int swapLevel, int shape1, int shape2, int len) {
            int key = shapeKey12(swapLevel, shape1, shape2);
            if (this.maxKnownSolutionLengthByShape12[key] < len) {
                this.maxKnownSolutionLengthByShape12[key] = (byte) len;
            }
        }
        
        public void updateMaxKnownNextSolutionLength(int state, int maxNextLen) {
            if (this.maxKnownNextSolutionLength[state] < maxNextLen) {
                this.maxKnownNextSolutionLength[state] = (byte) maxNextLen;
            }
        }
        
        public void updateSolutionLength(int state, int len) {
            if (this.solutionLength[state] == 0) {
                this.solutionLength[state] = (byte) len;
            } else {
                if (this.solutionLength[state] != len) throw new RuntimeException();
            }
        }

        public SolutionLengthLookup updateLookup(int state, int maxNextShape) {
            if (this.lookup[state] == null) {
                this.lookup[state] = new SolutionLengthLookup(maxNextShape);
            }
            return this.lookup[state];
        }

        public int getMaxKnownSolutionLengthByShape1(int swapLevel, int shape1) {
            int key = shapeKey1(swapLevel, shape1);
            return this.maxKnownSolutionLengthByShape1[key];
        }

        public int getMaxKnownSolutionLengthByShape12(int swapLevel, int shape1, int shape2) {
            int key = shapeKey12(swapLevel, shape1, shape2);
            return this.maxKnownSolutionLengthByShape12[key];
        }

        public int getMaxKnownNextSolutionLength(int state) {
            return maxKnownNextSolutionLength[state];
        }

        public int getSolutionLength(int state) {
            return solutionLength[state];
        }

        public SolutionLengthLookup getLookup(int state) {
            return lookup[state];
        }

        private int shapeKey1(int swapLevel, int shape1) {
            return (shape1 << 4) | swapLevel;
        }
        
        private int shapeKey12(int swapLevel, int shape1, int shape2) {
            return (shape2 << 12) | (shape1 << 4) | swapLevel;
        }
        
        public void mergeTo(Precalc other) {
            if (this.numStates != other.numStates) throw new RuntimeException();
            mergeMaxKnownLengthFromTo(this.maxKnownSolutionLengthByShape1, other.maxKnownSolutionLengthByShape1);
            mergeMaxKnownLengthFromTo(this.maxKnownSolutionLengthByShape12, other.maxKnownSolutionLengthByShape12);
            mergeSolutionLengthFromTo(this.solutionLength, other.solutionLength);
            mergeMaxKnownLengthFromTo(this.maxKnownNextSolutionLength, other.maxKnownNextSolutionLength);
            for (int state = 0; state < numStates; state++) {
                if (this.lookup[state] != null) {
                    if (other.lookup[state] == null) {
                        other.lookup[state] = new SolutionLengthLookup(this.lookup[state].maxNextShape);
                    }
                    this.lookup[state].mergeTo(other.lookup[state]);
                }
            }
        }

    }
    
    @SuppressWarnings("serial")
    private static class SwapStore implements Serializable {

        private int numStates;
        
        private Key[] keys = new Key[0]; // stateIndex -> key
        private String[] shapes = new String[0]; // stateIndex -> shape
        private long[][] next = new long[0][0]; // stateIndex -> bundleIndex -> [swapCode, nextStateIndex]

        public int addNewState(String shape, Key key) {
            int state = numStates++;
            ensureCapacity(numStates);
            shapes[state] = shape;
            keys[state] = key;
            return state;
        }

        private void ensureCapacity(int requiredCapacity) {
            int capacity = keys.length;
            if (capacity < requiredCapacity) {
                while (capacity < requiredCapacity) {
                    capacity += 1000000;
                }
                shapes = Arrays.copyOf(shapes, capacity);
                keys = Arrays.copyOf(keys, capacity);
                next = Arrays.copyOf(next, capacity);
            }
        }

        public void setNextStateSwap(int prevState, int maxNextBundles, Bundle bundle, int[] swap, int state) {
            int bundleIndex = bundle.index();
            if (next[prevState] == null) {
                next[prevState] = new long[maxNextBundles];
            }
            next[prevState][bundleIndex] = encodeState(swap, state);
        }

        public int getNextState(int prevState, Bundle bundle, int[] swapResult) {
            int bundleIndex = bundle.index();
            if (next[prevState] == null) {
                return -1;
            }
            return decodeState(next[prevState][bundleIndex], swapResult);
        }
        
        private long encodeState(int[] swap, int state) {
            int swapCode = encodeSwap(swap);
            return ((long)swapCode << 32) | state;
        }

        private int decodeState(long stateCode, int[] swapResult) {
            int swapCode = (int) ((stateCode >> 32) & MASK_32);
            int state = (int) (stateCode & MASK_32);
            if (swapResult != null) {
                decodeSwap(swapCode, swapResult);
            }
            return state;
        }

    }
    
    // Encode/decode a permutation of digits 1..9 (represented as array of values 0..8) as one 32-bit integer.
    //
    // swapCode: val0:4bits, val1:4bits, val2:4bits, val3:4bits, val4:4bits, val5:4bits, val6:4bits, val7:4bits
    //   value8 is calculated as 0+1+2+...+8-val0-...-val7
    //
    // Example: 
    //   swap: [7, 5, 3, 8, 0, 1, 2, 4, 6]
    //   mapping: 0 -> 7, 1 -> 5, 2 -> 3, 3 -> 8, 4 -> 0, 5 -> 1, 6 -> 2, 7 -> 4, 8 -> 6
    //   swapCode: 7, 5, 3, 8, 0, 1, 2, 4
    //
    //@SuppressWarnings("unused")
    public static void decodeSwap(int swapCode, int[] result) {
        int remainder = 36;
        for (int d = 7; d >= 0; d--) {
            int value = swapCode & MASK_4;
            result[d] = value;
            remainder -= value;
            swapCode >>>= 4;
        }
        result[8] = remainder;
        if (!isValidSwap(result)) {
            throw new IllegalArgumentException("swapCode = "+swapCode+", result = "+Arrays.toString(result));
        }
    }

    public static int encodeSwap(int[] swap) {
        if (!isValidSwap(swap)) {
            throw new IllegalArgumentException("swap = "+swap);
        }
        int result = 0;
        for (int d = 0; d < 8; d++) {
            int value = swap[d] & MASK_4;
            result = (result << 4) | value;
        }
        return result;
    }

    public static boolean isValidSwap(int[] swap) {
        if (swap == null || swap.length != 9) {
            return false;
        }
        int digits = MASK_9;
        for (int d = 0; d < 9; d++) {
            int value = swap[d];
            int mask = 1 << value;
            if (value < 0 || value > 8 || (digits & mask) == 0) {
                return false;
            }
            digits &= ~mask;
        }
        return true;
    }
    
    public static void mergeMaxKnownLengthFromTo(byte[] from, byte[] to) {
        byte[] array1 = from;
        byte[] array2 = to;
        int len = array1.length;
        for (int i = 0; i < len; i++) {
            if (array2[i] < array1[i]) {
                array2[i] = array1[i];
            }
        }
    }
    
    public static void mergeSolutionLengthFromTo(byte[] from, byte[] to) {
        byte[] array1 = from;
        byte[] array2 = to;
        int len = array1.length;
        for (int i = 0; i < len; i++) {
            byte ansLen = array1[i];
            if (ansLen != 0) {
                if (array2[i] == 0) {
                    array2[i] = (byte) ansLen;
                } else {
                    if (array2[i] != ansLen) throw new RuntimeException();
                }
            }
        }
    }
    
    public static class Checkpoint implements Runnable {
        
        private final File file;
        private final Thread thread;
        private final Map<Key, Integer> solutions = Collections.synchronizedMap(new MemoryEfficientHashMap<>());
        private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        private volatile boolean shutdown;

        public Checkpoint(File file) {
            this.file = file;
            loadSolutions();
            this.thread = new Thread(this);
            this.thread.setDaemon(true);
            this.thread.start();
        }

//        private void loadSolutions() {
//            KeyBuilder keyBuilder = new KeyBuilder();
//            loadSolutionsFromFile(file, (bundles, ans) -> {
//                Key key = keyBuilder.makeKey(bundles);
//                solutions.put(key, ans.length());
//                String shape = shape(bundles);
//                solutionsByShape.computeIfAbsent(shape, s -> new ArrayList<>()).add(key);
//            });
//        }
        
        private void loadSolutions() {
            File fileGz = new File(file.getPath() + ".gz");
            File sourceFile = fileGz.exists() ? fileGz : file;
            if (!sourceFile.exists()) return;
            System.out.println("Loading checkpoint ...");
            Map<Bundle[], Integer> answers = new ConcurrentHashMap<>();
            generateKeysInParallel((consumer) -> {
                loadSolutionsFromFile(sourceFile, (bundles, ans) -> {
                    answers.put(bundles, ans.length());
                    consumer.accept(bundles);
                });
            }, (bundles, key) -> {
                Integer ansLen = answers.get(bundles);
                solutions.put(key, ansLen);
            });
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
                queue.put(bundlesStr(problem) + " " + ans);
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

    }

    public static class Problem {

        public final Bundle[] bundles;
        public final Key key;
        public volatile String answer;
        public volatile long solveDuration;

        public Problem(Bundle[] bundles) {
            this(bundles, null);
        }
        
        public Problem(Bundle[] bundles, Key key) {
            this.bundles = bundles;
            this.key = key;
        }

        public void printResult(String suffix) {
            System.out.println(formatLog(String.format("%s => %s [%d], %d ms%s", 
                    bundlesStr(bundles), answer, answer != null ? answer.length() : 0, solveDuration, suffix)));
        }
        
    }

    public static class ThreadAllocator implements Supplier<Integer> {

        private int numThreads;
        private int numWorkers;

        public ThreadAllocator(int numThreads, int numWorkers) {
            this.numThreads = numThreads;
            this.numWorkers = numWorkers;
        }

        public synchronized void removeWorker() {
            numWorkers--;
        }
        
        @Override
        public synchronized Integer get() {
            int workers = Math.max(1, numWorkers);
            return (numThreads + workers - 1) / workers;
        }
        
    }
    
    public static void solveInParallel(String alg, Consumer<Consumer<Problem>> input, Consumer<Problem> output) {
        int numSolvers = (NUM_SOLVERS > 0) ? NUM_SOLVERS : NUM_WORKER_THREADS;
        ThreadAllocator threadAllocator = new ThreadAllocator(NUM_WORKER_THREADS, numSolvers);
        
        ParallelPipeline<Problem, Problem> pipeline = new ParallelPipeline<>(100);
        pipeline.addInputWorker(input);
        pipeline.addBatchProcessors(numSolvers, () -> {
            Solver solver = createSolver(alg, threadAllocator);
            return (problem) -> {
                if (solver != null && problem.answer == null) {
                    solver.solve(problem);
                }
                return problem;
            };
        }, () -> {
            threadAllocator.removeWorker();
            //System.out.println("threadAllocator: threads=" + threadAllocator.numThreads 
            //        + ", workers=" + threadAllocator.numWorkers + ", alloc=" + threadAllocator.get());
        });
        pipeline.consumeOutput((problem) -> output.accept(problem));
    }
    
    public static class ShapeInfo {
        
        public final int numHeads1, numHeads2, numHeads3, numHeads4, numHeads5, numHeads6;
        public final int numDigits1, numDigits2, numDigits3, numDigits4, numDigits5, numDigits6;
        
        public final boolean isMajor1, isMajor2, isMajor3, isMajor4, isMajor5, isMajor6;
        public final boolean isSmaller1, isSmaller2, isSmaller3, isSmaller4, isSmaller5, isSmaller6;
        public final boolean isPieces;
        
        public final int maxSize;
        public final int numHeads;
        public final int numBundles;
        
        public final int numMajorHeads;
        public final int numMajorBundles;
        public final int numSmallerHeads;
        public final int numSmallerBundles;
        
        public final int maxHeads;
        public final int maxMajorHeads;
        public final int maxSmallerHeads;
        
        public ShapeInfo(String shape) {
            int[] shapes = parseShape(shape);
            int shape1 = (shapes.length > 0) ? shapes[0] : 0;
            int shape2 = (shapes.length > 1) ? shapes[1] : 0;
            int shape3 = (shapes.length > 2) ? shapes[2] : 0;
            int shape4 = (shapes.length > 3) ? shapes[3] : 0;
            int shape5 = (shapes.length > 4) ? shapes[4] : 0;
            int shape6 = (shapes.length > 5) ? shapes[5] : 0;
            this.numHeads1 = shape1 & MASK_4;
            this.numHeads2 = shape2 & MASK_4;
            this.numHeads3 = shape3 & MASK_4;
            this.numHeads4 = shape4 & MASK_4;
            this.numHeads5 = shape5 & MASK_4;
            this.numHeads6 = shape6 & MASK_4;
            this.numDigits1 = shape1 >> 4; 
            this.numDigits2 = shape2 >> 4; 
            this.numDigits3 = shape3 >> 4; 
            this.numDigits4 = shape4 >> 4;
            this.numDigits5 = shape5 >> 4;
            this.numDigits6 = shape6 >> 4;
            this.maxSize = numDigits1;
            this.numHeads = numHeads1 + numHeads2 + numHeads3 + numHeads4 + numHeads5 + numHeads6;
            this.numBundles = (numDigits1 > 0 ? 1 : 0) 
                    + (numDigits2 > 0 ? 1 : 0) 
                    + (numDigits3 > 0 ? 1 : 0) 
                    + (numDigits4 > 0 ? 1 : 0)
                    + (numDigits5 > 0 ? 1 : 0)
                    + (numDigits6 > 0 ? 1 : 0);
            this.isMajor1 = numDigits1 == maxSize;
            this.isMajor2 = numDigits2 == maxSize;
            this.isMajor3 = numDigits3 == maxSize;
            this.isMajor4 = numDigits4 == maxSize;
            this.isMajor5 = numDigits5 == maxSize;
            this.isMajor6 = numDigits6 == maxSize;
            this.isSmaller1 = numDigits1 > 0 && numDigits1 < maxSize;
            this.isSmaller2 = numDigits2 > 0 && numDigits2 < maxSize;
            this.isSmaller3 = numDigits3 > 0 && numDigits3 < maxSize;
            this.isSmaller4 = numDigits4 > 0 && numDigits4 < maxSize;
            this.isSmaller5 = numDigits5 > 0 && numDigits5 < maxSize;
            this.isSmaller6 = numDigits6 > 0 && numDigits6 < maxSize;
            this.isPieces = numHeads1 <= 1 && numHeads2 <= 1 && numHeads3 <= 1 && numHeads4 <= 1 && numHeads5 <= 1 && numHeads6 <= 1;
            this.numMajorHeads = (isMajor1 ? numHeads1 : 0) 
                    + (isMajor2 ? numHeads2 : 0) 
                    + (isMajor3 ? numHeads3 : 0) 
                    + (isMajor4 ? numHeads4 : 0)
                    + (isMajor5 ? numHeads5 : 0)
                    + (isMajor6 ? numHeads6 : 0);
            this.numMajorBundles = (isMajor1 ? 1 : 0) 
                    + (isMajor2 ? 1 : 0) 
                    + (isMajor3 ? 1 : 0) 
                    + (isMajor4 ? 1 : 0)
                    + (isMajor5 ? 1 : 0)
                    + (isMajor6 ? 1 : 0);
            this.numSmallerHeads = (isSmaller1 ? numHeads1 : 0) 
                    + (isSmaller2 ? numHeads2 : 0) 
                    + (isSmaller3 ? numHeads3 : 0) 
                    + (isSmaller4 ? numHeads4 : 0)
                    + (isSmaller5 ? numHeads5 : 0)
                    + (isSmaller6 ? numHeads6 : 0);
            this.numSmallerBundles = (isSmaller1 ? 1 : 0) 
                    + (isSmaller2 ? 1 : 0) 
                    + (isSmaller3 ? 1 : 0) 
                    + (isSmaller4 ? 1 : 0)
                    + (isSmaller5 ? 1 : 0)
                    + (isSmaller6 ? 1 : 0);
            this.maxHeads = Math.max(Math.max(
                            Math.max(numHeads1, numHeads2),
                            Math.max(numHeads3, numHeads4)), 
                            Math.max(numHeads5, numHeads6));
            this.maxMajorHeads = Math.max(Math.max(
                    Math.max((isMajor1 ? numHeads1 : 0), (isMajor2 ? numHeads2 : 0)),
                    Math.max((isMajor3 ? numHeads3 : 0), (isMajor4 ? numHeads4 : 0))), 
                    Math.max((isMajor5 ? numHeads5 : 0), (isMajor6 ? numHeads6 : 0)));
            this.maxSmallerHeads = Math.max(Math.max(
                    Math.max((isSmaller1 ? numHeads1 : 0), (isSmaller2 ? numHeads2 : 0)),
                    Math.max((isSmaller3 ? numHeads3 : 0), (isSmaller4 ? numHeads4 : 0))), 
                    Math.max((isSmaller5 ? numHeads5 : 0), (isSmaller6 ? numHeads6 : 0)));
        }
        
        public static int[] parseShape(String shape) {
            String[] split = shape.split(",");
            int[] result = new int[split.length];
            for (int i = 0; i < split.length; i++) {
                String part = split[i];
                String[] sizes = part.split("/");
                int numHeads = Integer.parseInt(sizes[0]);
                int numDigits = Integer.parseInt(sizes[1]);
                result[i] = shape(numHeads, numDigits);
            }
            return result;
        }

        public static int[] shapeHeadCounts(String shape) {
            int[] shapes = parseShape(shape);
            int[] headCounts = new int[10];
            for (int sh : shapes) {
                int numHeads = sh & MASK_4;
                int numDigits = sh >> 4;
                headCounts[numDigits] += numHeads;
            }
            return headCounts;
        }

        public static int compareShapesByHeadCounts(String shape1, String shape2) {
            int[] c1 = shapeHeadCounts(shape1);
            int[] c2 = shapeHeadCounts(shape2);
            for (int i = 9; i >= 0; i--) {
                if (c1[i] != c2[i]) {
                    return (c1[i] < c2[i]) ? -1 : 1;
                }
            }
            return shape1.compareTo(shape2); // just compare lexicographically, if same counts
        }

        public static int compareShapesByTotalHeadCounts(String shape1, String shape2) {
            int[] c1 = shapeHeadCounts(shape1);
            int[] c2 = shapeHeadCounts(shape2);
            int s1 = Arrays.stream(c1).sum();
            int s2 = Arrays.stream(c2).sum();
            return Integer.compare(s1,  s2);
        }

    }
    
    
    public static int MAX_N = 9;
    
    public static int MIN_PIECE_CHECK_SIZE = MAX_N-2;

    public static int MAX_SWAP_LEVEL = 3; // max value returned by swapLevel()
    //public static int[] MAX_SWAP_LEVEL_SHAPE = new int[] {-1, shape(1, 9), shape(1, 8), shape(1, 7), shape(4, 4), shape(4, 4), shape(3, 3)};
    public static int[] MAX_SWAP_LEVEL_SHAPE = new int[] {-1, shape(1, 9), shape(5, 5), shape(4, 4), shape(4, 4)};
    //public static int[] MAX_SWAP_LEVEL_SHAPE = new int[] {-1, shape(1, 8), shape(4, 4), shape(3, 3), shape(2, 2)};
    
    public static int NUM_WORKER_THREADS = Runtime.getRuntime().availableProcessors();
    public static int NUM_SOLVERS = -1;
    
    public static String PRECALC_ALG = "dfs";
    public static String SOLVE_ALG = "dfs-batch";
    
    public static int DFS_BATCH_SIZE = 1000;
    public static long DFS_MAX_CACHE = 200_000_000L;
    public static long DFS_MAX_SAVE_CACHE = 200_000_000L;
    public static int DFS_BATCH_SAVE_MINUTES = Integer.MAX_VALUE;

    public static int DFS_DISK_BLOCK_SIZE = 100_000_000; // N prefixes in, N*5 prefixes out
    public static int DFS_DISK_BATCH_SIZE = 1_000_000; // N states in, N*5 states out (up to 1kb each)
    public static int DFS_DISK_SEEN_SIZE = 10_000_000; // keys (up to 1kb each)

    public static int BFS_DISK_MAX_EXPECTED_BUCKET_SIZE = 40 * 1024 * 1024;
    public static int BFS_DISK_EXPECTED_GROWTH_FACTOR = 4;

    public static String MAX_PRECALC_SHAPE = null;
    
    public static String CHECKPOINT_SHAPES = null;
    
    public static boolean PRINT_ALLOC = false;
    public static boolean SAVE_PRECALC = true;
    
    public static int DIST_PC = 1000;
    public static int DIST = 1000;
    public static int DIST_HBA0 = 1000;
    public static int DIST_HBA1 = 1000;
    public static int DIST_HBA2 = 1000;
    public static int DIST_HBA3 = 1000;
    public static int DIST_HBA4 = 1000;
    
    public static final String PROBLEMS_DIR = "problems";
    public static final String SOLUTIONS_DIR = "solutions";
    public static final String PRECALC_DIR = "precalc";
    public static final String PRECALC_FILE = "precalc.bin.gz";
    public static final String PRECALC_SIZE_FILE = "precalc-%d.bin.gz";
    public static final String SWAP_STORE_FILE = "swap-store.bin.gz";
    public static final String LOOKUP_FILE = "lookup.txt";
    public static final String COUNTS_FILE = "counts.bin.gz";
    
    public static final long MASK_32 = (1L<<32)-1;
    public static final int MASK_18 = (1<<18)-1;
    public static final int MASK_15 = (1<<15)-1;
    public static final int MASK_9 = (1<<9)-1;
    public static final int MASK_8 = (1<<8)-1;
    public static final int MASK_4 = (1<<4)-1;

    public static SwapStore swapStore = new SwapStore();
    public static Precalc precalc = new Precalc(0);
    
    public static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static boolean DEBUG = false;
    public static boolean DEBUG_NEXT_MOVES = false;
    
    public static boolean PRINT_LEVELS = true;
    public static boolean PRINT_SHAPE_STATS = false;
    
    public static int PRINT_STATES_STEP = -1;

    private static long totalPrecalcCalcTime;

    public static List<String> enumShapes() {
        List<String> shapes = new ArrayList<>();

        for (int numDigits1 = 1; numDigits1 <= MAX_N; numDigits1++) {
            for (int numHeads1 = 1; numHeads1 <= numDigits1; numHeads1++) {
                shapes.add(String.format("%d/%d", numHeads1, numDigits1));
                for (int numDigits2 = 1; numDigits2 <= numDigits1; numDigits2++) {
                    if (numDigits2 < numDigits1-2) continue;
                    int maxHeads2 = numDigits2 < numDigits1 ? numDigits2 : numHeads1;
                    for (int numHeads2 = 1; numHeads2 <= maxHeads2; numHeads2++) {
                        shapes.add(String.format("%d/%d,%d/%d", numHeads1, numDigits1, 
                                numHeads2, numDigits2));
                        for (int numDigits3 = 1; numDigits3 <= numDigits2; numDigits3++) {
                            if (numDigits3 < numDigits1-2) continue;
                            int maxHeads3 = numDigits3 < numDigits2 ? numDigits3 : numHeads2;
                            for (int numHeads3 = 1; numHeads3 <= maxHeads3; numHeads3++) {
                                shapes.add(String.format("%d/%d,%d/%d,%d/%d", numHeads1, numDigits1, 
                                        numHeads2, numDigits2, numHeads3, numDigits3));
                                for (int numDigits4 = 1; numDigits4 <= numDigits3; numDigits4++) {
                                    if (numDigits4 < numDigits1-2) continue;
                                    int maxHeads4 = numDigits4 < numDigits3 ? numDigits4 : numHeads3;
                                    for (int numHeads4 = 1; numHeads4 <= maxHeads4; numHeads4++) {
                                        shapes.add(String.format("%d/%d,%d/%d,%d/%d,%d/%d", numHeads1, numDigits1, 
                                                numHeads2, numDigits2, numHeads3, numDigits3, numHeads4, numDigits4));
                                        if (numDigits1 > 6) continue;//XXX skip unused, to speed up enumeration
                                        for (int numDigits5 = 1; numDigits5 <= numDigits4; numDigits5++) {
                                            if (numDigits5 < numDigits1-2) continue;
                                            int maxHeads5 = numDigits5 < numDigits4 ? numDigits5 : numHeads4;
                                            for (int numHeads5 = 1; numHeads5 <= maxHeads5; numHeads5++) {
                                                shapes.add(String.format("%d/%d,%d/%d,%d/%d,%d/%d,%d/%d", numHeads1, numDigits1, 
                                                        numHeads2, numDigits2, numHeads3, numDigits3, 
                                                        numHeads4, numDigits4, numHeads5, numDigits5));
                                                if (numDigits1 > 4) continue;//XXX skip unused, to speed up enumeration
                                                for (int numDigits6 = 1; numDigits6 <= numDigits5; numDigits6++) {
                                                    if (numDigits6 < numDigits1-2) continue;
                                                    int maxHeads6 = numDigits6 < numDigits5 ? numDigits6 : numHeads5;
                                                    for (int numHeads6 = 1; numHeads6 <= maxHeads6; numHeads6++) {
                                                        shapes.add(String.format("%d/%d,%d/%d,%d/%d,%d/%d,%d/%d,%d/%d", 
                                                                numHeads1, numDigits1, numHeads2, numDigits2, 
                                                                numHeads3, numDigits3, numHeads4, numDigits4, 
                                                                numHeads5, numDigits5, numHeads6, numDigits6));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        shapes.sort(ShapeInfo::compareShapesByHeadCounts);

        return shapes;
    }

    // may skip k1 and k2 dups; bundles 3-4-5-6 are sorted.
    public static void iterateBundlesOfShape(String shape, boolean skipK1Dups, boolean skipK2Dups, Consumer<Bundle[]> callback) {
        int[] shapes = ShapeInfo.parseShape(shape);
        
        int shape1 = (shapes.length > 0) ? shapes[0] : 0;
        int shape2 = (shapes.length > 1) ? shapes[1] : 0;
        int shape3 = (shapes.length > 2) ? shapes[2] : 0;
        int shape4 = (shapes.length > 3) ? shapes[3] : 0;
        int shape5 = (shapes.length > 4) ? shapes[4] : 0;
        int shape6 = (shapes.length > 5) ? shapes[5] : 0;

        int numDigits1 = shape1 >> 4;
        int numDigits2 = shape2 >> 4;  
        int numDigits3 = shape3 >> 4;
        int numDigits4 = shape4 >> 4;
        int numDigits5 = shape5 >> 4;
        int numDigits6 = shape6 >> 4;

        int[] groupSize = new int[9];
        int[] groupStartIndex = new int[9];
        
        boolean[] seenK2 = new boolean[1<<17];
        
        Bundle[] bundlesOfShape1 = Bundle.bundlesOfShape[shape1];

        if (skipK1Dups) {
            int numHeads1 = shape1 & MASK_4;
            int heads1 = (1<<numHeads1)-1;
            int digits1 = (1<<numDigits1)-1;
            Bundle bundle1 = Bundle.unpack((heads1 << 9) | digits1);
            
            bundlesOfShape1 = new Bundle[] {bundle1};
        }
        
        for (Bundle bundle1 : bundlesOfShape1) {
            int digits1 = bundle1.digits();
            if ((digits1 & ~((1<<MAX_N)-1)) != 0) continue;

            if (numDigits2 == 0) {
                callback.accept(new Bundle[] {bundle1});
                continue;
            }

            for (Bundle bundle2 : Bundle.bundlesOfShape[shape2]) {
                int digits2 = bundle2.digits();
                if ((digits2 & ~((1<<MAX_N)-1)) != 0) continue;
                
                if ((digits2 == digits1)) continue; 
                
                if (isDup(bundle1, bundle2)) continue;
                
                if (numDigits3 == 0) {
                    callback.accept(new Bundle [] {bundle1, bundle2});
                    continue;
                }

                if (skipK2Dups) {
                    int k2 = K2(bundle1, bundle2, null, groupSize, groupStartIndex);
                    
                    if (seenK2[k2]) continue;
                    
                    seenK2[k2] = true;
                }
                
                for (Bundle bundle3 : Bundle.bundlesOfShape[shape3]) {
                    int digits3 = bundle3.digits();
                    if ((digits3 & ~((1<<MAX_N)-1)) != 0) continue;
                    
                    if ((digits3 == digits1 || digits3 == digits2)) continue; 
                    
                    if (isDup(bundle1, bundle3)) continue;
                    if (isDup(bundle2, bundle3)) continue;
                    
                    if (numDigits4 == 0) {
                        callback.accept(new Bundle[] {bundle1, bundle2, bundle3});
                        continue;
                    }
                    
                    for (Bundle bundle4 : Bundle.bundlesOfShape[shape4]) {
                        int digits4 = bundle4.digits();
                        if ((digits4 & ~((1<<MAX_N)-1)) != 0) continue;
                        
                        if (bundle4.toSortable() >= bundle3.toSortable()) continue;
                        
                        if ((digits4 == digits1 || digits4 == digits2 || digits4 == digits3)) continue;
                        
                        if (isDup(bundle1, bundle4)) continue;
                        if (isDup(bundle2, bundle4)) continue;
                        if (isDup(bundle3, bundle4)) continue;

                        if (numDigits5 == 0) {
                            callback.accept(new Bundle[] {bundle1, bundle2, bundle3, bundle4});
                            continue;
                        }

                        for (Bundle bundle5 : Bundle.bundlesOfShape[shape5]) {
                            int digits5 = bundle5.digits();
                            if ((digits5 & ~((1<<MAX_N)-1)) != 0) continue;
                            
                            if (bundle5.toSortable() >= bundle4.toSortable()) continue;
                            
                            if ((digits5 == digits1 || digits5 == digits2 || digits5 == digits3 || digits5 == digits4)) continue;
                            
                            if (isDup(bundle1, bundle5)) continue;
                            if (isDup(bundle2, bundle5)) continue;
                            if (isDup(bundle3, bundle5)) continue;
                            if (isDup(bundle4, bundle5)) continue;

                            if (numDigits6 == 0) {
                                callback.accept(new Bundle[] {bundle1, bundle2, bundle3, bundle4, bundle5});
                                continue;
                            }

                            for (Bundle bundle6 : Bundle.bundlesOfShape[shape6]) {
                                int digits6 = bundle6.digits();
                                if ((digits6 & ~((1<<MAX_N)-1)) != 0) continue;
                                
                                if (bundle6.toSortable() >= bundle5.toSortable()) continue;
                                
                                if ((digits6 == digits1 || digits6 == digits2 || digits6 == digits3 || digits6 == digits4 || digits6 == digits5)) continue;
                                
                                if (isDup(bundle1, bundle6)) continue;
                                if (isDup(bundle2, bundle6)) continue;
                                if (isDup(bundle3, bundle6)) continue;
                                if (isDup(bundle4, bundle6)) continue;
                                if (isDup(bundle5, bundle6)) continue;

                                callback.accept(new Bundle[] {bundle1, bundle2, bundle3, bundle4, bundle5, bundle6});
                            }
                        }
                    }
                }
            }
        }
    }

    // is 2 a dup of 1, either whole or partially
    public static boolean isDup(Bundle bundle1, Bundle bundle2) {
        int heads1 = bundle1.heads();
        int heads2 = bundle2.heads(); 
        int digits1 = bundle1.digits();
        int digits2 = bundle2.digits();
        if ((digits2 & ~digits1) == 0) {
            if ((heads1 & ~digits2) != 0) return true;
            if ((heads1 & heads2) != 0) return true;
        }
        return false;
    }

    public static void iterateUniqueBundlesOfShape(String shape, Consumer<Bundle[]> callback) {
        Set<Key> seen = new MemoryEfficientHashSet<>();
        generateKeysInParallel((consumer) -> {
            iterateBundlesOfShape(shape, true, true, consumer);
        }, (bundles, key) -> {
            if (!seen.contains(key)) {
                callback.accept(bundles);
                seen.add(key);
            }
        });
    }
    
    public static List<Bundle[]> generateUniqueBundlesOfShape(String shape) {
        List<Bundle[]> result = new ArrayList<>();
        iterateUniqueBundlesOfShape(shape, result::add);
        return result;
    }
    
    public static void generateKeysInParallel(Collection<Bundle[]> input, BiConsumer<Bundle[], Key> resultCallback) {
        generateKeysInParallel((consumer) -> {
            for (Bundle[] bundles : input) {
                consumer.accept(bundles);
            }
        }, resultCallback);
    }

    public static void generateKeysInParallel(Consumer<Consumer<Bundle[]>> inputCallback, 
            BiConsumer<Bundle[], Key> resultCallback) {
        ParallelBatchPipeline<Bundle[], Key> pipeline = new ParallelBatchPipeline<>(1000, 100);
        pipeline.addInputWorker(inputCallback);
        pipeline.addBatchProcessors(NUM_WORKER_THREADS, () -> {
            KeyBuilder keyBuilder = new KeyBuilder();
            return (bundles) -> {
                return keyBuilder.makeKey(bundles);
            };
        });
        pipeline.consumeOutput(resultCallback);
    }
    

    private static List<String> restrictPrecalcShapes(List<String> shapes) {
        return shapes.stream()
                .filter(shape -> {
                    ShapeInfo si = new ShapeInfo(shape);
                    int numBundles = si.numBundles;
                    int maxSize = si.maxSize;
                    int maxHeads = si.maxHeads; 
                    
                    int h1 = si.numHeads1, h2 = si.numHeads2, h3 = si.numHeads3, h4 = si.numHeads4, h5 = si.numHeads5, h6 = si.numHeads6;
                    int d1 = si.numDigits1, d2 = si.numDigits2, d3 = si.numDigits3, d4 = si.numDigits4, d5 = si.numDigits5, d6 = si.numDigits6;

                    int d = 0;
                    if (numBundles == 2) d = d1-d2;
                    if (numBundles == 3) d = d1-d3;
                    if (numBundles == 4) d = d1-d4;
                    if (numBundles == 5) d = d1-d5;
                    if (numBundles == 6) d = d1-d6;
    
                    if (MAX_N <= 7) {
                        if (maxSize == MAX_N) {
                            return numBundles == 1 && (h1 == 1);
                        } else if (maxSize == MAX_N-1) {
                            return numBundles == 1
                                    || numBundles == 2 && (h1 == 1);
                        } else if (maxSize == MAX_N-2) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3 && (h1 == 1);
                        } else if (maxSize == MAX_N-3) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3;
                        } else if (maxSize == MAX_N-4) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3
                                    || numBundles == 4;
                        } else {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3
                                    || numBundles == 4;
                                    //|| numBundles == 5;
                        }
                    } else if (MAX_N <= 8) {
                        //if (maxSize > 5) return false;
                        if (maxSize == MAX_N) {
                            return numBundles == 1 && (maxHeads == 1);
                        } else if (maxSize == MAX_N-1) {
                            return numBundles == 1
                                    || numBundles == 2 && (maxHeads == 1)
                                    || numBundles == 3 && (maxHeads == 1);
                        } else if (maxSize == MAX_N-2) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3 && (maxHeads == 1);
                        } else if (maxSize == MAX_N-3) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3;
                        } else if (maxSize == MAX_N-4) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3
                                    || numBundles == 4;
                        } else {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3
                                    || numBundles == 4
                                    || numBundles == 5;
                        }
                    } else {
                        //if (maxSize > 4) return false;
                        if (maxSize == MAX_N) {
                            return numBundles == 1 && (maxHeads == 1);
                        } else if (maxSize == MAX_N-1) {
                            return numBundles == 1
                                    || numBundles == 2 && (maxHeads == 1 && d <= 1);
                        } else if (maxSize == MAX_N-2) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3 && (maxHeads == 1 && d <= 1);
                        } else if (maxSize == MAX_N-3) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3;
                        } else if (maxSize == MAX_N-4) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3
                                    || numBundles == 4;
                        } else if (maxSize == MAX_N-5) {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3
                                    || numBundles == 4
                                    || numBundles == 5;
                        } else {
                            return numBundles == 1
                                    || numBundles == 2
                                    || numBundles == 3
                                    || numBundles == 4
                                    || numBundles == 5;
                                    //|| numBundles == 6;
                        }
                    }
                })
                .collect(Collectors.toList());
    }

    private static File shapeProblemsFile(String shape) {
        return new File(String.format("%s/%sx%s/%s.txt.gz", PROBLEMS_DIR, shape.charAt(2), shape.split(",").length, shape.replace('/', '@')));
    }

    private static File shapeSolutionsFile(String shape) {
        return new File(String.format("%s/%sx%s/%s.txt", SOLUTIONS_DIR, shape.charAt(2), shape.split(",").length, shape.replace('/', '@')));
    }

    private static File shapePrecalcFile(String shape, int swapLevel) {
        return new File(String.format("%s/%sx%s/%s-%s.bin.gz", PRECALC_DIR, shape.charAt(2), shape.split(",").length, shape.replace('/', '@'), swapLevel));
    }
    
    public static List<Bundle[]> uniqueBundlesOfShape(String shape) {
        File shapeFile = shapeProblemsFile(shape);
        if (!shapeFile.exists()) {
            System.out.println("Generating " + shape);
            System.out.println("Writing " + shapeFile.getName());
            printWriteToFileGZ(shapeFile, (writer) -> {
                iterateUniqueBundlesOfShape(shape, (bundles) -> {
                    writer.println(bundlesStr(bundles));
                });
            });
        }
        
        //System.out.println("Reading " + shapeFileName);
        List<Bundle[]> uniqueBundles = new ArrayList<>();
        readLinesFromFile(shapeFile, (line) -> {
            uniqueBundles.add(parseBundles(line));
        });
        return uniqueBundles;
    }
    
    public static void precalc(long[] durations) {
        File precalcFile = new File(PRECALC_FILE);
        if (precalcFile.exists()) {
            loadPrecalc(precalcFile);
            return;
        }

        List<String> allShapes = loadShapes();

        File swapStoreFile = new File(SWAP_STORE_FILE);
        if (swapStoreFile.exists()) {
            System.out.println("Loading swap store...");
            swapStore = deserializeObjectFromFileGZ(swapStoreFile);
        } else {
            System.out.println("Calculating swap store...");
            swapStore = calculateSwapStore();
            serializeObjectToFileGZ(swapStoreFile, swapStore);
        }
        
        System.out.println("Swap store size: " + toMB(memoryUsage(swapStore)));
        
        Map<String, List<Integer>> statesByShape = IntStream.range(0, swapStore.numStates).boxed()
                .collect(Collectors.groupingBy(
                    state -> swapStore.shapes[state]));
        
        System.out.println("Calculating totals ...");
        
        Map<String, Long> bundleCountByShape = loadShapeCounts(allShapes);
        
        Map<Integer, List<String>> shapesBySize = allShapes.stream()
                .collect(Collectors.groupingBy(
                        shape -> new ShapeInfo(shape).maxSize));
        
        Map<Integer, Long> bundleCountBySize = shapesBySize.entrySet().stream()
                .collect(Collectors.toMap(
                        Entry::getKey, 
                        entry -> entry.getValue().stream()
                            .mapToLong(shape -> bundleCountByShape.getOrDefault(shape, 0L))
                            .sum()));
        
        AtomicLong totalPrecalc = new AtomicLong(printTotalPrecalc(allShapes, bundleCountByShape));
        AtomicLong currPrecalc = new AtomicLong(1);
        
        precalc = new Precalc(swapStore.numStates);
        
        Map<String, String> lookup = new MemoryEfficientHashMap<>();
        File lookupFile = new File(LOOKUP_FILE);
        if (lookupFile.exists()) {
            System.out.println("Loading lookup ...");
            loadSolutionsFromFile(lookupFile, (bundles, ans) -> {
                lookup.put(bundlesStr(bundles), ans);
            });
        }

        File checkpointFile = null;
        String checkpointShape = null;
        if (CHECKPOINT_SHAPES != null) {
            for (String shape : CHECKPOINT_SHAPES.split(";")) {
                File file = checkpointFile(shape);
                if (file.exists()) {
                    // remember the last existing file from the list
                    checkpointFile = file;
                    checkpointShape = shape;
                }
            }
        }
        
        int shapeSize = MAX_N;
        if (checkpointFile != null) {
            loadPrecalc(checkpointFile);
            shapeSize = new ShapeInfo(checkpointShape).maxSize;
        } else {
            for (; shapeSize > 1; shapeSize--) {
                File precalcPrevSizeFile = new File(String.format(PRECALC_SIZE_FILE, shapeSize-1));
                if (precalcPrevSizeFile.exists()) {
                    loadPrecalc(precalcPrevSizeFile);
                    break;
                }
            }
        }
        
        for (int size = 1; size < shapeSize; size++) {
            Long count = bundleCountBySize.get(size);
            if (count != null) {
                currPrecalc.addAndGet(count);
            }
        }
        
        for (; shapeSize <= MAX_N; shapeSize++) {
            List<String> shapes = new ArrayList<>(shapesBySize.getOrDefault(shapeSize, Collections.emptyList()));
            removeCompletedShapes(shapes, checkpointShape, bundleCountByShape, totalPrecalc, currPrecalc);
            if (shapes.isEmpty()) continue;
            
            long beginTime = System.currentTimeMillis();
            
            precalcShapeSize(shapeSize, shapes, lookup, currPrecalc, totalPrecalc, bundleCountByShape, statesByShape);
            
            long endTime = System.currentTimeMillis();

            System.out.println("Size precalc time " + shapeSize + ": " + (endTime-beginTime) + " ms");
            
            durations[shapeSize] = endTime - beginTime;

            // if (SAVE_PRECALC) {
            //File precalcSizeFile = new File(String.format(PRECALC_SIZE_FILE, shapeSize));
            //savePrecalc(precalcSizeFile);
            // }
        }

        if (PRINT_ALLOC) System.out.println("Precalc:  memory = " + toMB(memoryUsage(precalc)));
        
        // if (SAVE_PRECALC) {
        //savePrecalc(precalcFile);
        // }
    }
    
    private static SwapStore calculateSwapStore() {
        List<Integer> allShapes = new ArrayList<>();
        for (int numDigits = 1; numDigits <= MAX_N; numDigits++) {
            for (int numHeads = 1; numHeads <= numDigits; numHeads++) {
                int shape = shape(numHeads, numDigits);
                allShapes.add(shape);
            }
        }
        
        SwapStore swapStore = new SwapStore();
        swapStore.addNewState("", new Key(new int[0]));
        
        Map<Key, Integer> uniqueStates = new HashMap<>();
        
        int[] swap = new int[9];
        KeyBuilder keyBuilder = new KeyBuilder();
        
        List<Integer> levelItems = new ArrayList<>();
        levelItems.add(0);
        
        for (int level = 1; level <= MAX_SWAP_LEVEL; level++) {
            List<Integer> nextLevelItems = new ArrayList<>();
            
            int item = 0;
            int numLevelStates = 0;
            
            int maxLevelShape = MAX_SWAP_LEVEL_SHAPE[level];
            
            for (int prevState : levelItems) {
                item++;
                
                String prevShape = swapStore.shapes[prevState];
                Bundle[] prevBundles = Bundle.unpackAll(swapStore.keys[prevState].values);

                int minPrevShape = shape(9, 9);
                int maxPrevShape = 0;
                for (Bundle bundle : prevBundles) {
                    int shape = bundle.shape();
                    minPrevShape = Math.min(shape, minPrevShape);
                    maxPrevShape = Math.max(shape, maxPrevShape);
                }
                
                if (maxPrevShape > maxLevelShape) continue;
                
                int maxNextShape = Math.min(minPrevShape, maxLevelShape);
                int maxNextBundles = Bundle.maxBundleIndexOfShape[maxNextShape]+1;
                
                System.out.println(bundlesStr(prevBundles) + " (" + item + "/" + levelItems.size() + ")");
                
                for (int shape : allShapes) {
                    if (shape > maxNextShape) continue;
                    for (Bundle bundle : Bundle.bundlesOfShape[shape]) {
                        Bundle[] bundles = appendBundles(prevBundles, bundle);
                        Key key = keyBuilder.makeKey(bundles, swap);
                        Bundle[] keyBundles = Bundle.unpackAll(key.values);
                        
                        verifySwap(bundles, swap, keyBundles);
                        
                        Integer state = uniqueStates.get(key);
                        if (state == null) {
                            String bundlesShape = appendShape(prevShape, shape).intern();
                            state = swapStore.addNewState(bundlesShape, key);
                            uniqueStates.put(key, state);
                            
                            if (level < MAX_SWAP_LEVEL) {
                                nextLevelItems.add(state);
                            }
                            
                            numLevelStates++;
                        }
                        
                        swapStore.setNextStateSwap(prevState, maxNextBundles, bundle, swap, state);
                    }
                }
            }
            
            System.out.println("Level " + level + " states: " + numLevelStates);
            
            levelItems = nextLevelItems;
        }
        
        return swapStore;
    }

    private static void verifySwap(Bundle[] bundles, int[] swap, Bundle[] keyBundles) {
        Bundle[] swappedBundles = new Bundle[bundles.length];
        for (int i = 0; i < bundles.length; i++) {
            swappedBundles[i] = bundles[i].swapBundleDigits(swap);
        }
        keyBundles = Arrays.copyOf(keyBundles, keyBundles.length);
        Arrays.sort(swappedBundles, Comparator.comparing(Bundle::toSortable));
        Arrays.sort(keyBundles, Comparator.comparing(Bundle::toSortable));
        if (!Arrays.equals(swappedBundles, keyBundles)) {
            throw new RuntimeException();
        }
    }

    private static Bundle[] appendBundles(Bundle[] bundles0, Bundle... bundles1) {
        int len0 = bundles0.length;
        int len1 = bundles1.length;
        Bundle[] newBundles = Arrays.copyOf(bundles0, len0 + len1);
        for (int i = 0; i < len1; i++) {
            newBundles[len0 + i] = bundles1[i];
        }
        return newBundles;
    }
    
    private static String appendShape(String prevShape, int shape) {
        return (prevShape == null || prevShape.isEmpty()) ? shapeStr(shape) : prevShape + "," + shapeStr(shape);
    }

    private static void removeCompletedShapes(List<String> shapes, String checkpointShape,
            Map<String, Long> bundleCountByShape, AtomicLong totalPrecalc, AtomicLong currPrecalc) {
        if (checkpointShape != null) {
            Iterator<String> iter = shapes.iterator();
            while (iter.hasNext()) {
                String shape = iter.next();
                if (ShapeInfo.compareShapesByHeadCounts(shape, checkpointShape) <= 0) {
                    currPrecalc.addAndGet(bundleCountByShape.getOrDefault(shape, 0L));
                    System.out.println(String.format("Skip shape %s (%d / %d)", shape, currPrecalc.get(), totalPrecalc.get()));
                    iter.remove();
                    continue;
                }
                break;
            }
        }

        if (MAX_PRECALC_SHAPE != null) {
            Iterator<String> iter = shapes.iterator();
            while (iter.hasNext()) {
                String shape = iter.next();
                if (ShapeInfo.compareShapesByHeadCounts(shape, MAX_PRECALC_SHAPE) > 0) {
                    currPrecalc.addAndGet(bundleCountByShape.getOrDefault(shape, 0L));
                    System.out.println(String.format("Skip shape %s (%d / %d)", shape, currPrecalc.get(), totalPrecalc.get()));
                    iter.remove();
                }
            }
        }
    }

    private static List<String> loadShapes() {
        List<String> allShapes = enumShapes();

        allShapes = restrictPrecalcShapes(allShapes);
        
        return allShapes;
    }

    private static Map<String, Long> loadShapeCounts(List<String> allShapes) {
        HashMap<String, Long> counts;
        
        File countsFile = new File(COUNTS_FILE);
        if (countsFile.exists()) {
            counts = deserializeObjectFromFileGZ(countsFile);
        } else {
            counts = new HashMap<>();
        }

        List<String> missing = allShapes.stream()
                .filter(shape -> !counts.containsKey(shape))
                .collect(Collectors.toList());
        
        if (!missing.isEmpty()) {
            for (String shape : missing) {
                long count = uniqueBundlesOfShape(shape).size();
                counts.put(shape, count);
            }
            
            serializeObjectToFileGZ(countsFile, counts);
        }
        
        return counts;
    }

    private static long printTotalPrecalc(List<String> shapes, Map<String, Long> shapeBundleCounts) {
        long totalPrecalc = 0;
        long[] precalcBySize = new long[10];
        
        for (String shape : shapes) {
            Long numBundles = shapeBundleCounts.get(shape);
            
            int shapeSize = new ShapeInfo(shape).maxSize;
            precalcBySize[shapeSize] += numBundles;
            
            totalPrecalc += numBundles;
        }
        
        System.out.println("Total precalc: " + totalPrecalc + " " + Arrays.toString(precalcBySize));
        
        return totalPrecalc;
    }

    private static void precalcShapeSize(int shapeSize, List<String> shapes, Map<String, String> lookup,
            AtomicLong currPrecalc, AtomicLong totalPrecalc, Map<String, Long> bundleCountByShape, 
            Map<String, List<Integer>> statesByShape) {
        for (String shape : shapes) {
            Precalc shapePrecalc;
            
            int swapLevel = swapLevel(shape);

            File shapePrecalcFile = shapePrecalcFile(shape, swapLevel);
            if (shapePrecalcFile.exists()) {
                System.out.println(String.format("Loading shape precalc %s (swapLevel %s)", shape, swapLevel));
                shapePrecalc = deserializeObjectFromFileGZ(shapePrecalcFile);
                currPrecalc.addAndGet(bundleCountByShape.getOrDefault(shape, 0L));
            } else {
                shapePrecalc = new Precalc(swapStore.numStates);

                precalcShape(shape, swapLevel, lookup, shapePrecalc, currPrecalc, totalPrecalc, statesByShape);
                
                if (!shapePrecalcFile.exists() && SAVE_PRECALC && !"none".equals(PRECALC_ALG) && !"skip".equals(PRECALC_ALG)) {
                    System.out.println(String.format("Saving precalc %s", shapePrecalcFile));
                    serializeObjectToFileGZ(shapePrecalcFile, shapePrecalc);
                }
            }

            shapePrecalc.mergeTo(precalc);
                
            if (PRINT_ALLOC) {
                System.out.println("Shape alloc: "
                        + "memory = " + toMB(memoryUsage(shapePrecalc)) 
                        + ", total = " + toMB(memoryUsage(precalc)) 
                        + ", shape = " + shape);
            }

            if (CHECKPOINT_SHAPES != null && Arrays.asList(CHECKPOINT_SHAPES.split(";")).contains(shape)) {
                File checkpointFile = checkpointFile(shape);
                if (!checkpointFile.exists()) {
                    savePrecalc(checkpointFile);
                }
            }
        }
    }

    private static void precalcShape(String shape, int swapLevel, Map<String, String> lookup, Precalc shapePrecalc, 
            AtomicLong currPrecalc, AtomicLong totalPrecalc, Map<String, List<Integer>> statesByShape) {
        System.out.println(String.format("Precalc shape %s (%d / %d)", shape, currPrecalc.get(), totalPrecalc.get()));
        long begin = System.currentTimeMillis();
        
        List<Bundle[]> uniqueBundles = uniqueBundlesOfShape(shape);
        
        File solutionsFile = shapeSolutionsFile(shape);
        
        //XXX solutionsFile = new File("solutions.txt");
        
        Checkpoint checkpoint = new Checkpoint(solutionsFile);

        long time1 = System.currentTimeMillis();
        
        solveShape(shape, checkpoint, lookup, uniqueBundles, currPrecalc, totalPrecalc);
        
        long time2 = System.currentTimeMillis();
        
        if (!checkpoint.solutions.isEmpty()) {
            applyShapeSolutions(shape, swapLevel, checkpoint, uniqueBundles, shapePrecalc, statesByShape);
        }
        
        long time3 = System.currentTimeMillis();

        checkpoint.shutdown();
        
        compressSolutions(solutionsFile);
        
        long end = System.currentTimeMillis();
        
        totalPrecalcCalcTime += time2-time1;
        
        System.out.println("Shape precalc time: shape = " + shape
                + ", calc = " + (time2-time1) + " ms"
                + ", apply = " + (time3-time2) + " ms"
                + ", compress = " + (end-time3) + " ms"
                + ", total = " + (end-begin) + " ms");
    }

    private static void compressSolutions(File solutionsFile) {
        if (!solutionsFile.exists()) return;
        File gzipFile = new File(solutionsFile.getPath() + ".gz");
        if (gzipFile.exists()) return;
        System.out.println(String.format("Compressing %s ...", solutionsFile));
        File tmpFile = new File(gzipFile.getPath() + ".tmp");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile), IO_BUFFER_SIZE);
                MiGzOutputStream gzOut = new MiGzOutputStream(out);
                FileInputStream in = new FileInputStream(solutionsFile)) {
            byte[] buf = new byte[65536];
            int len;
            while ((len = in.read(buf)) != -1) {
                gzOut.write(buf, 0, len);
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        if (!solutionsFile.delete()) {
            throw new RuntimeException();
        }
        if (!tmpFile.renameTo(gzipFile)) {
            throw new RuntimeException();
        }
    }

    private static void solveShape(String shape, Checkpoint checkpoint, Map<String, String> lookup,
            List<Bundle[]> uniqueBundles, AtomicLong currPrecalc, AtomicLong totalPrecalc) {
        AtomicLong shapePrecalc = new AtomicLong(uniqueBundles.size());
        
        String alg = PRECALC_ALG;
        
        solveInParallel(alg, (consumer) -> {
            generateKeysInParallel(uniqueBundles, (bundles, key) -> {
                if (!checkpoint.containsKey(key) && !alg.equals("none")) {
                    Problem problem = new Problem(bundles, key);
                    String ans = lookup.remove(bundlesStr(bundles));
                    if (ans != null) {
                        problem.answer = ans;
                    }
                    consumer.accept(problem);
                } else {
                    shapePrecalc.decrementAndGet();
                    currPrecalc.incrementAndGet();
                }
            });
        }, (problem) -> {
            if (problem.answer != null) {
                problem.printResult(String.format(" (%d, %d / %d)", shapePrecalc.get(), currPrecalc.get(), totalPrecalc.get()));
                checkpoint.put(problem.bundles, problem.key, problem.answer);
            }
            shapePrecalc.decrementAndGet();
            currPrecalc.incrementAndGet();
        });
    }

    private static void applyShapeSolutions(String shape, int swapLevel, Checkpoint checkpoint, List<Bundle[]> uniqueBundles, 
            Precalc shapePrecalc, Map<String, List<Integer>> statesByShape) {
        System.out.println("Applying solutions ...");

        applySwapStoreSolutions(shape, swapLevel, checkpoint, uniqueBundles, shapePrecalc);
        
        applyLookupSolutions(shape, swapLevel, checkpoint, shapePrecalc, statesByShape);
    }

    private static void applySwapStoreSolutions(String shape0, int swapLevel, Checkpoint checkpoint, 
            List<Bundle[]> uniqueBundles, Precalc shapePrecalc) {
        generateKeysInParallel(uniqueBundles, (bundles, key) -> {
            Integer ansLen = checkpoint.get(key);
            if (ansLen == null) return; // skipped
            addSolution(bundles, ansLen, shapePrecalc, swapLevel);
        });
    }
    
    private static void addSolution(Bundle[] bundles, int ansLen, Precalc shapePrecalc, int maxSwapLevel) {
        int numBundles = bundles.length;
        Bundle[] sortedBundles = new Bundle[numBundles];
        Bundle[] permBundles = new Bundle[numBundles];
        
        prepareBundles(bundles, sortedBundles);

        for (int swapLevel = 0; swapLevel <= maxSwapLevel; swapLevel++) {
            if (numBundles > 0) shapePrecalc.updateMaxKnownSolutionLengthByShape1(swapLevel, bundles[0].shape(), ansLen);
            if (numBundles > 1) shapePrecalc.updateMaxKnownSolutionLengthByShape12(swapLevel, bundles[0].shape(), bundles[1].shape(), ansLen);
        }
        
        int[] groupSizes = calculateBundleGroupSizes(sortedBundles, numBundles);
        
        for (int[] permutation : Permutations.permutations(numBundles, groupSizes)) {
            for (int i = 0; i < numBundles; i++) {
                int index = permutation[i];
                permBundles[i] = sortedBundles[index];
            }
            
            addPermSolution(permBundles, ansLen, shapePrecalc);
        }
    }

    private static int[] calculateBundleGroupSizes(Bundle[] sortedBundles, int numBundles) {
        int[] groupSizes = new int[numBundles];
        int numGroups = 0;
        for (int i = 0; i < numBundles; i++) {
            groupSizes[numGroups]++;
            if (i == numBundles-1 || sortedBundles[i].shape() != sortedBundles[i+1].shape()) {
                numGroups++;
            }
        }
        return groupSizes;
    }

    private static void addPermSolution(Bundle[] bundles, int ansLen, Precalc shapePrecalc) {
        int[] swap = identitySwap();
        int[] nextSwap = new int[9];

        int state = 0;
        for (Bundle bundle : bundles) {
            Bundle bundleSW = bundle.swapBundleDigits(swap);

            shapePrecalc.updateMaxKnownNextSolutionLength(state, ansLen);
            
            state = swapStore.getNextState(state, bundleSW, nextSwap);
            if (state < 0) return; // next state is not defined
            
            appendSwap(swap, nextSwap, swap);
        }
        shapePrecalc.updateSolutionLength(state, ansLen);
    }

    private static int swapLevel(String shape) {
        ShapeInfo si = new ShapeInfo(shape);
        int numBundles = si.numBundles;
        int maxSize = si.maxSize;
        
        int h1 = si.numHeads1, h2 = si.numHeads2, h3 = si.numHeads3, h4 = si.numHeads4, h5 = si.numHeads5, h6 = si.numHeads6;
        int d1 = si.numDigits1, d2 = si.numDigits2, d3 = si.numDigits3, d4 = si.numDigits4, d5 = si.numDigits5, d6 = si.numDigits6;

        int[] hh = new int[] {0, h1, h2, h3, h4, h5, h6};
        //int[] dd = new int[] {0, d1, d2, d3, d4, d5, d6};
        boolean isPieces = Arrays.stream(hh).allMatch(h -> h <= 1);
        if (isPieces && maxSize >= MIN_PIECE_CHECK_SIZE) {
            return 1;
        }

        if (numBundles <= 3) {
            return 1;
        } else if (numBundles == 4) {
            return 2;
        } else if (numBundles == 5) {
            return 3;
        } else if (numBundles == 6) {
            return 3;
        } else {
            throw new RuntimeException();
        }
    }

    private static void applyLookupSolutions(String shape0, int swapLevel, Checkpoint checkpoint, Precalc shapePrecalc, 
            Map<String, List<Integer>> statesByShape) {
        int[] shapes = ShapeInfo.parseShape(shape0);
        
        if (swapLevel >= shapes.length) return;
            
        String swapShape = Arrays.stream(shape0.split(","))
                .limit(swapLevel)
                .collect(Collectors.joining(","));
        
        int[] lookupShapes = Arrays.copyOfRange(shapes, swapLevel, shapes.length);
        
        List<Integer> swapStates = statesByShape.get(swapShape);
        if (swapStates == null) return;
        
        for (int state : swapStates) {
            Bundle[] stateBundles = Bundle.unpackAll(swapStore.keys[state].values);
            
            int maxNextShape = (swapLevel > 0) ? shapes[swapLevel-1] : shape(9, 9);
            SolutionLengthLookup stateLookup = shapePrecalc.updateLookup(state, maxNextShape);

            applyStateLookupSolutions(shapes, stateBundles, stateLookup, lookupShapes, checkpoint, shapePrecalc);
        }
    }

    private static void applyStateLookupSolutions(int[] shapes, Bundle[] stateBundles, SolutionLengthLookup stateLookup, 
            int[] lookupShapes, Checkpoint checkpoint, Precalc shapePrecalc) {
        generateKeysInParallel((consumer) -> {
            if (lookupShapes.length == 0) {
                consumer.accept(stateBundles);
            }
            iterateLookupShapeBundles(lookupShapes, (lookupBundles) -> {
                Bundle[] bundles = Arrays.copyOf(stateBundles, stateBundles.length + lookupBundles.length);
                System.arraycopy(lookupBundles, 0, bundles, stateBundles.length, lookupBundles.length);
                consumer.accept(bundles);
            });
        }, (bundles, key) -> {
            Integer ansLen = checkpoint.get(key);
            if (ansLen == null) return;
            
            int swapLevel = stateBundles.length;
            int numBundles = shapes.length;
            
            int shape1 = (numBundles > 0) ? shapes[0] : 0;
            int shape2 = (numBundles > 1) ? shapes[1] : 0;
            
            if (numBundles > 0) shapePrecalc.updateMaxKnownSolutionLengthByShape1(swapLevel, shape1, ansLen);
            if (numBundles > 1) shapePrecalc.updateMaxKnownSolutionLengthByShape12(swapLevel, shape1, shape2, ansLen);
            
            SolutionLengthLookup lookup = stateLookup;
            
            for (int i = 0; i < lookupShapes.length; i++) {
                Bundle bundle = bundles[stateBundles.length+i];

                lookup.updateMaxKnownNextSolutionLength(ansLen);
                if (i < lookupShapes.length-1) {
                    lookup = lookup.updateNextLookup(bundle);
                } else {
                    lookup.updateNextSolutionLength(bundle, ansLen);
                }
            }
        });
    }

    private static void iterateLookupShapeBundles(int[] shapes, Consumer<Bundle[]> callback) {
        int numBundles = shapes.length;
        
        if (numBundles > 4) throw new IllegalArgumentException();
        
        int shape1 = (numBundles > 0) ? shapes[0] : 0;
        int shape2 = (numBundles > 1) ? shapes[1] : 0;
        int shape3 = (numBundles > 2) ? shapes[2] : 0;
        int shape4 = (numBundles > 3) ? shapes[3] : 0;
        
        for (Bundle bundle1 : Bundle.bundlesOfShape[shape1]) {
            if (numBundles == 1) {
                callback.accept(new Bundle[] {bundle1});
                continue;
            }
            
            for (Bundle bundle2 : Bundle.bundlesOfShape[shape2]) {
                if (bundle2.toSortable() > bundle1.toSortable()) continue;
                
                if (numBundles == 2) {
                    callback.accept(new Bundle[] {bundle1, bundle2});
                    continue;
                }
                
                for (Bundle bundle3 : Bundle.bundlesOfShape[shape3]) {
                    if (bundle3.toSortable() > bundle2.toSortable()) continue;
                    
                    if (numBundles == 3) {
                        callback.accept(new Bundle[] {bundle1, bundle2, bundle3});
                        continue;
                    }
                    
                    for (Bundle bundle4 : Bundle.bundlesOfShape[shape4]) {
                        if (bundle4.toSortable() > bundle3.toSortable()) continue;
                        
                        if (numBundles == 4) {
                            callback.accept(new Bundle[] {bundle1, bundle2, bundle3, bundle4});
                            continue;
                        }
                    }
                }
            }
        }
    }

    private static int[] identitySwap() {
        return new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    private static void appendSwap(int[] swap1, int[] swap2, int[] result) {
        // result can be the same as swap1, not not the same as swap2
        if (result == swap2) throw new IllegalArgumentException();
        for (int d = 0; d < 9; d++) {
            result[d] = swap2[swap1[d]];
        }
    }

    private static void loadPrecalc(File file) {
        System.out.println("Loading " + file.getName() + " ...");
        deserializeFromFileGZ(file, stream -> {
            try {
                swapStore = (SwapStore) stream.readObject();
                precalc = (Precalc) stream.readObject();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void savePrecalc(File file) {
        System.out.println("Writing " + file.getName() + " ...");
        serializeToFileGZ(file, stream -> {
            try {
                stream.writeObject(swapStore);
                stream.writeObject(precalc);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static String toMB(long value) {
        return (value/1024/1024) + " MB";
    }
    
    public static List<String> toMB(List<Long> list) {
        return list.stream()
                .map(x -> toMB(x))
                .collect(Collectors.toList());
    }
    
    public static Map<String, String> toMB(Map<String, Long> map) {
        return map.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, 
                entry -> toMB(entry.getValue())));
    }
    
    public static Map<String, List<String>> toMBList(Map<String, List<Long>> map) {
        return map.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, 
                entry -> toMB(entry.getValue())));
    }

    
    // groupSize divides the digits [1, 2, 3, 4, 5, 6, 7, 8, 9] into 9 groups. 
    // Each groupSize[i] value describes how many digits are in the i-th group.
    //
    // The result will be the mask consisting of 9 zero bits separated with one bits according 
    // to the groups described by groupSize array.
    // 
    // The optional swap array will map the digits of the bundles into consecutive digits 
    // in the corresponding groups.
    // 
    // For example:
    //   digits: [1, 2, 3, 4, 5, 6, 7, 8, 9] 
    //   groupSize: [3, 0, 1, 2, 0, 0, 2, 1, 0] 
    //   groups: [{1, 2, 3}, {}, {4}, {5, 6}, {}, {}, {7, 8}, {9}, {}].
    //   groupStartIndex: [0, 3, 3, 4, 6, 6, 6, 8, 9]
    //   result: 00011010011100101
    //
    // The following should be true: groupStartIndex[i] + groupSize[i] = groupStartIndex[i+1]
    // The following should be true: sum(groupSize) = 9
    // 
    // The result will always have 8 ones and 9 zero bits, so 17 bits total.
    //
    public static int K2(Bundle bundle1, Bundle bundle2, int[] swap, int[] groupSize, int[] groupStartIndex) {
        int h1 = bundle1.heads();
        int h2 = bundle2.heads();
        int d1 = bundle1.digits();
        int d2 = bundle2.digits();
        int t1 = d1 & ~h1;
        int t2 = d2 & ~h2;

        groupSize[0] = bitCount(h1 & h2);
        groupSize[1] = bitCount(h1 & t2);
        groupSize[2] = bitCount(h1 & ~d2);

        groupSize[3] = bitCount(t1 & h2);
        groupSize[4] = bitCount(t1 & t2);
        groupSize[5] = bitCount(t1 & ~d2);

        groupSize[6] = bitCount(~d1 & h2);
        groupSize[7] = bitCount(~d1 & t2);
        groupSize[8] = bitCount(~d1 & ~d2 & MASK_9);

        // Effectively, swap bundle1 and bundle2 if needed, to always return minimum value 
        // between K2(bundle1, bundle2) and K2(bundle2, bundle1).
        // 
        //       h2 t2 ~d2
        //    h1  0  1  2
        //    t1  3  4  5
        //   ~d1  6  7  8
        //
        if (((groupSize[1] << 16) | (groupSize[2] << 8) | groupSize[5]) 
                < ((groupSize[3] << 16) | (groupSize[6] << 8) | groupSize[7])) {
            int t;
            t = d1; d1 = d2; d2 = t;
            t = h1; h1 = h2; h2 = t;
            t = groupSize[1]; groupSize[1] = groupSize[3]; groupSize[3] = t; 
            t = groupSize[2]; groupSize[2] = groupSize[6]; groupSize[6] = t; 
            t = groupSize[5]; groupSize[5] = groupSize[7]; groupSize[7] = t; 
        }
        
        int result = 0;
        for (int i = 1; i < 9; i++) {
            int c = groupSize[i];
            // append bit '1' followed by groupSize[i] of '0' bits
            result = (result << (c+1)) | (1 << c); 
        }

        if (swap != null) {
            int lastIndex = 0;
            for (int i = 0; i < 9; i++) {
                groupStartIndex[i] = lastIndex;
                lastIndex += groupSize[i];
            }
            
            for (int d = 0; d < 9; d++) {
                int mask = 1 << d;
                int groupIndex;
                if ((d1 & mask) != 0) {
                    if ((h1 & mask) != 0) {
                        if ((d2 & mask) != 0) {
                            groupIndex = (h2 & mask) != 0 ? 0 : 1; 
                        } else {
                            groupIndex = 2;
                        }
                    } else {
                        if ((d2 & mask) != 0) {
                            groupIndex = (h2 & mask) != 0 ? 3 : 4;
                        } else {
                            groupIndex = 5;
                        }
                    }
                } else {
                    if ((d2 & mask) != 0) {
                        groupIndex = (h2 & mask) != 0 ? 6 : 7;
                    } else {
                        groupIndex = 8;
                    }
                }
                swap[d] = groupStartIndex[groupIndex]++;
            }
        }
        
        return result; // 17 bit
    }
    

    public static class PartialsLookup {
        
        private final Bundle[] sortedPieces = new Bundle[9*256];
        private final int[] sortBuffer = new int[9*256];
        private final Bundle[] swappedBundles = new Bundle[1024];
        private final int[] sortedBundleIndexes = new int[1024];
        private final int[] swap1 = new int[9];
        private final int[] swap2 = new int[9];
        private final int[] swap3 = new int[9];
        private final int[] swap4 = new int[9];
        private final int[] swap5 = new int[9];
        private final int[] nextSwap = new int[9];
    
        static long enterHA;
        static long enterPP;
        static long passHA;
        static long timeHA;
        static long matchHA0;
        static long matchHA1;
        static long matchHA2;
        static long matchHA3;
        static long matchHA4;
        static long matchPHA0;
        static long matchPHA1;
        static long matchPHA2;
        static long matchPHA3;
        static long matchPHA4;
        static long timeHA0;
        static long timeHA1;
        static long timeHA2;
        static long timeHA3;
        static long timeHA4;
        static long timePP;
        static long timePHA0;
        static long timePHA1;
        static long timePHA2;
        static long timePHA3;
        static long timePHA4;
        
        private static String matchTime(Long x, Long y) {
            return String.format("%d (%d ms)", x, y/1000000L);
        }
        
        public static void printHAStats() {
            System.out.println();
            System.out.println("HA enter: " + matchTime(enterHA, timeHA));
            System.out.println("HA pass: " + passHA);
            System.out.println("HA0: " + matchTime(matchHA0, timeHA0));
            System.out.println("HA1: " + matchTime(matchHA1, timeHA1));
            System.out.println("HA2: " + matchTime(matchHA2, timeHA2));
            System.out.println("HA3: " + matchTime(matchHA3, timeHA3));
            System.out.println("HA4: " + matchTime(matchHA4, timeHA4));
            System.out.println("PHA0: " + matchTime(matchPHA0, timePHA0));
            System.out.println("PHA1: " + matchTime(matchPHA1, timePHA1));
            System.out.println("PHA2: " + matchTime(matchPHA2, timePHA2));
            System.out.println("PHA3: " + matchTime(matchPHA3, timePHA3));
            System.out.println("PHA4: " + matchTime(matchPHA4, timePHA4));
            System.out.println("PP: " + matchTime(enterPP, timePP));
        }
        
        public static void resetHAStats() {
            enterHA = 0;
            enterPP = 0;
            passHA = 0;
            timeHA = 0;
            matchHA0 = 0;
            matchHA1 = 0;
            matchHA2 = 0;
            matchHA3 = 0;
            matchHA4 = 0;
            matchPHA0 = 0;
            matchPHA1 = 0;
            matchPHA2 = 0;
            matchPHA3 = 0;
            matchPHA4 = 0;
            timeHA0 = 0;
            timeHA1 = 0;
            timeHA2 = 0;
            timeHA3 = 0;
            timeHA4 = 0;
            timePP = 0;
            timePHA0 = 0;
            timePHA1 = 0;
            timePHA2 = 0;
            timePHA3 = 0;
            timePHA4 = 0;
        }
        
        public int canHaveAnswer(Bundle[] sortedBundles, int len, int nextMoves, int bestAnsLen) {
            enterHA++;
            long beginHA = System.nanoTime();
            try {
            long begin;
            long duration;
            
            if (len > 0 && bestAnsLen == 0) return 0;
            
            begin = System.nanoTime();
            nextMoves = canHaveAnswerLookup(sortedBundles, len, bestAnsLen, nextMoves, true, 1, swapStore, precalc, true);
            duration = System.nanoTime() - begin;
            timeHA1 += duration;
            if (nextMoves == 0) matchHA1++;
            if (nextMoves == 0) return 0;
            
            begin = System.nanoTime();
            nextMoves = canHaveAnswerLookup(sortedBundles, len, bestAnsLen, nextMoves, false, 2, swapStore, precalc, true);
            duration = System.nanoTime() - begin;
            timeHA2 += duration;
            if (nextMoves == 0) matchHA2++;
            if (nextMoves == 0) return 0;
            
            begin = System.nanoTime();
            nextMoves = canHaveAnswerLookup(sortedBundles, len, bestAnsLen, nextMoves, false, 3, swapStore, precalc, true);
            duration = System.nanoTime() - begin;
            timeHA3 += duration;
            if (nextMoves == 0) matchHA3++;
            if (nextMoves == 0) return 0;
            
            begin = System.nanoTime();
            nextMoves = canHaveAnswerLookup(sortedBundles, len, bestAnsLen, nextMoves, false, 4, swapStore, precalc, true);
            duration = System.nanoTime() - begin;
            timeHA4 += duration;
            if (nextMoves == 0) matchHA4++;
            if (nextMoves == 0) return 0;
            
            enterPP++;
            begin = System.nanoTime();
            int numPieces = preparePieces(sortedBundles, len, sortedPieces, sortBuffer);
            duration = System.nanoTime() - begin;
            timePP += duration;
    
            begin = System.nanoTime();
            nextMoves = canHaveAnswerLookup(sortedPieces, numPieces, bestAnsLen, nextMoves, true, 1, swapStore, precalc, false);
            duration = System.nanoTime() - begin;
            timePHA1 += duration;
            if (nextMoves == 0) matchPHA1++;
            if (nextMoves == 0) return 0;
    
            begin = System.nanoTime();
            nextMoves = canHaveAnswerLookup(sortedPieces, numPieces, bestAnsLen, nextMoves, false, 2, swapStore, precalc, false);
            duration = System.nanoTime() - begin;
            timePHA2 += duration;
            if (nextMoves == 0) matchPHA2++;
            if (nextMoves == 0) return 0;
            
            passHA++;
            return nextMoves;
            } finally {
                timeHA += System.nanoTime() - beginHA;
            }
        }
        
        private int canHaveAnswerLookup(Bundle[] sortedBundles, int len, int bestAnsLen, int nextMoves, 
                boolean checkSwapLen, int swapLevel, SwapStore swapStore, Precalc precalc, boolean isBundles) {
            if (len <= 0) return nextMoves;
            
            int maxDigits = sortedBundles[0].numDigits();
        
            int dist = isBundles ? DIST : DIST_PC;
            if (swapLevel == 1) dist = Math.min(dist, DIST_HBA1);
            else if (swapLevel == 2) dist = Math.min(dist, DIST_HBA2);
            else if (swapLevel == 3) dist = Math.min(dist, DIST_HBA3);
            else if (swapLevel == 4) dist = Math.min(dist, DIST_HBA4);

            int state0 = 0;
            if (precalc.getMaxKnownNextSolutionLength(state0) < bestAnsLen) return nextMoves;
            
            if (swapLevel == 0) {
                return checkLookup(sortedBundles, len, bestAnsLen, nextMoves, 
                        new Bundle[0], 
                        state0, null, 0, dist, maxDigits, precalc);
            }
            
            for (int i = 0; i < len && i < dist; i++) {
                Bundle bundle1 = sortedBundles[i];
                int shape1 = bundle1.shape();
                int numDigits1 = bundle1.numDigits();

                if (numDigits1 < maxDigits) break;

                if (precalc.getMaxKnownSolutionLengthByShape1(swapLevel, shape1) < bestAnsLen) break;
                
                int state1 = swapStore.getNextState(state0, bundle1, swap1);
                if (state1 < 0) continue;
                
                if (checkSwapLen) {
                    int length1 = precalc.getSolutionLength(state1);
                    if (length1 >= bestAnsLen) {
                        Bundle[] bundles = new Bundle[] {bundle1};
                        if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length1, bundles);
                        nextMoves &= heads(bundles);
                        if (length1 > bestAnsLen || nextMoves == 0) {
                            if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length1, bundles);
                            return 0;
                        }
                    }
                }

                if (swapLevel == 1) {
                    nextMoves = checkLookup(sortedBundles, len, bestAnsLen, nextMoves, 
                            new Bundle[] {bundle1}, 
                            state1, swap1, i+1, dist, maxDigits, precalc);
                    if (nextMoves == 0) return 0;
                    continue;
                }
                
                if (precalc.getMaxKnownNextSolutionLength(state1) < bestAnsLen) continue;
                
                for (int j = i+1; j < len && j < dist; j++) {
                    Bundle bundle2 = sortedBundles[j];
                    int shape2 = bundle2.shape();
                    int numDigits2 = bundle2.numDigits();

                    if (bundle2.toSortable() >= bundle1.toSortable()) throw new RuntimeException();
                    if (numDigits2 < numDigits1-2) break;
                    if (numDigits2 > numDigits1) throw new RuntimeException();
                    
                    if (precalc.getMaxKnownSolutionLengthByShape12(swapLevel, shape1, shape2) < bestAnsLen) break;
                    
                    Bundle bundle22 = bundle2.swapBundleDigits(swap1);

                    int state2 = swapStore.getNextState(state1, bundle22, nextSwap);
                    if (state2 < 0) continue;

                    if (checkSwapLen) {
                        int length2 = precalc.getSolutionLength(state2);
                        if (length2 >= bestAnsLen) {
                            Bundle[] bundles = new Bundle[] {bundle1, bundle2};
                            if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length2, bundles);
                            nextMoves &= heads(bundles);
                            if (length2 > bestAnsLen || nextMoves == 0) {
                                if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length2, bundles);
                                return 0;
                            }
                        }
                    }

                    appendSwap(swap1, nextSwap, swap2);
                    
                    if (swapLevel == 2) {
                        nextMoves = checkLookup(sortedBundles, len, bestAnsLen, nextMoves, 
                                new Bundle[] {bundle1, bundle2}, 
                                state2, swap2, j+1, dist, maxDigits, precalc);
                        if (nextMoves == 0) return 0;
                        continue;
                    }
                    
                    if (precalc.getMaxKnownNextSolutionLength(state2) < bestAnsLen) continue;
                    
                    for (int k = j+1; k < len && k < dist; k++) {
                        Bundle bundle3 = sortedBundles[k];
                        int numDigits3 = bundle3.numDigits();

                        if (bundle3.toSortable() >= bundle2.toSortable()) throw new RuntimeException();
                        if (numDigits3 < numDigits1-2) break;
                        if (numDigits3 > numDigits2) throw new RuntimeException();
                        
                        Bundle bundle33 = bundle3.swapBundleDigits(swap2);

                        int state3 = swapStore.getNextState(state2, bundle33, nextSwap);
                        if (state3 < 0) continue;

                        if (checkSwapLen) {
                            int length3 = precalc.getSolutionLength(state3);
                            if (length3 >= bestAnsLen) {
                                Bundle[] bundles = new Bundle[] {bundle1, bundle2, bundle3};
                                if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length3, bundles);
                                nextMoves &= heads(bundles);
                                if (length3 > bestAnsLen || nextMoves == 0) {
                                    if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length3, bundles);
                                    return 0;
                                }
                            }
                        }

                        appendSwap(swap2, nextSwap, swap3);

                        if (swapLevel == 3) {
                            nextMoves = checkLookup(sortedBundles, len, bestAnsLen, nextMoves, 
                                    new Bundle[] {bundle1, bundle2, bundle3}, 
                                    state3, swap3, k+1, dist, maxDigits, precalc);
                            if (nextMoves == 0) return 0;
                            continue;
                        }
                        
                        if (precalc.getMaxKnownNextSolutionLength(state3) < bestAnsLen) continue;
                        
                        for (int l = k+1; l < len && l < dist; l++) {
                            Bundle bundle4 = sortedBundles[l];
                            int numDigits4 = bundle4.numDigits();

                            if (bundle4.toSortable() >= bundle3.toSortable()) throw new RuntimeException();
                            if (numDigits4 < numDigits1-2) break;
                            if (numDigits4 > numDigits3) throw new RuntimeException();
                            
                            Bundle bundle44 = bundle4.swapBundleDigits(swap3);

                            int state4 = swapStore.getNextState(state3, bundle44, nextSwap);
                            if (state4 < 0) continue;

                            if (checkSwapLen) {
                                int length4 = precalc.getSolutionLength(state4);
                                if (length4 >= bestAnsLen) {
                                    Bundle[] bundles = new Bundle[] {bundle1, bundle2, bundle3, bundle4};
                                    if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length4, bundles);
                                    nextMoves &= heads(bundles);
                                    if (length4 > bestAnsLen || nextMoves == 0) {
                                        if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length4, bundles);
                                        return 0;
                                    }
                                }
                            }
                            
                            appendSwap(swap3, nextSwap, swap4);

                            if (swapLevel == 4) {
                                nextMoves = checkLookup(sortedBundles, len, bestAnsLen, nextMoves, 
                                        new Bundle[] {bundle1, bundle2, bundle3, bundle4}, 
                                        state4, swap4, l+1, dist, maxDigits, precalc);
                                if (nextMoves == 0) return 0;
                                continue;
                            }
                            
                            if (precalc.getMaxKnownNextSolutionLength(state4) < bestAnsLen) continue;
                            
                            for (int m = l+1; m < len && m < dist; m++) {
                                Bundle bundle5 = sortedBundles[m];
                                int numDigits5 = bundle5.numDigits();

                                if (bundle5.toSortable() >= bundle4.toSortable()) throw new RuntimeException();
                                if (numDigits5 < numDigits1-2) break;
                                if (numDigits5 > numDigits4) throw new RuntimeException();
                                
                                Bundle bundle55 = bundle5.swapBundleDigits(swap4);

                                int state5 = swapStore.getNextState(state4, bundle55, nextSwap);
                                if (state5 < 0) continue;

                                if (checkSwapLen) {
                                    int length5 = precalc.getSolutionLength(state5);
                                    if (length5 >= bestAnsLen) {
                                        Bundle[] bundles = new Bundle[] {bundle1, bundle2, bundle3, bundle4, bundle5};
                                        if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length5, bundles);
                                        nextMoves &= heads(bundles);
                                        if (length5 > bestAnsLen || nextMoves == 0) {
                                            if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length5, bundles);
                                            return 0;
                                        }
                                    }
                                }

                                appendSwap(swap4, nextSwap, swap5);

                                if (swapLevel == 5) {
                                    nextMoves = checkLookup(sortedBundles, len, bestAnsLen, nextMoves, 
                                            new Bundle[] {bundle1, bundle2, bundle3, bundle4, bundle5}, 
                                            state5, swap5, m+1, dist, maxDigits, precalc);
                                    if (nextMoves == 0) return 0;
                                    continue;
                                }
                                
                                if (precalc.getMaxKnownNextSolutionLength(state5) < bestAnsLen) continue;
                                
                                for (int n = m+1; n < len && n < dist; n++) {
                                    Bundle bundle6 = sortedBundles[n];
                                    int numDigits6 = bundle6.numDigits();

                                    if (bundle6.toSortable() >= bundle5.toSortable()) throw new RuntimeException();
                                    if (numDigits6 < numDigits1-2) break;
                                    if (numDigits6 > numDigits5) throw new RuntimeException();
                                    
                                    Bundle bundle66 = bundle6.swapBundleDigits(swap5);

                                    int state6 = swapStore.getNextState(state5, bundle66, nextSwap);
                                    if (state6 < 0) continue;

                                    if (checkSwapLen) {
                                        int length6 = precalc.getSolutionLength(state6);
                                        if (length6 >= bestAnsLen) {
                                            Bundle[] bundles = new Bundle[] {bundle1, bundle2, bundle3, bundle4, bundle5, bundle6};
                                            if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length6, bundles);
                                            nextMoves &= heads(bundles);
                                            if (length6 > bestAnsLen || nextMoves == 0) {
                                                if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length6, bundles);
                                                return 0;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return nextMoves;
        }
        
        private int checkLookup(Bundle[] sortedBundles, int len, int bestAnsLen, int nextMoves, 
                Bundle[] prevBundles, int state, int[] swap, int startIndex, int dist, int maxDigits, 
                Precalc precalc) {
            int swapLevel = prevBundles.length;
            
            SolutionLengthLookup lookup0 = precalc.getLookup(state);
            if (lookup0 == null) return nextMoves;
            
            if (lookup0.getMaxKnownNextSolutionLength() < bestAnsLen) return nextMoves;
            
            int numIndexes = 0;

            for (int i = startIndex; i < len && i < dist; i++) {
                Bundle bundle1 = sortedBundles[i];
                int numDigits1 = bundle1.numDigits();

                if (numDigits1 < maxDigits-2) break;
                
                Bundle bundle11 = (swap != null) ? bundle1.swapBundleDigits(swap) : bundle1;

                swappedBundles[i] = bundle11;
                
                // sort indexes by [shape11, bundle11] in descending order (given that shape11==shape1),
                // so that iterBundlesOfShape could skip and iterate less,
                // because we're ever going to check canHBA-3-4-5 bundles only in that order
                appendSortedBundleIndex(sortedBundleIndexes, numIndexes++, swappedBundles, i);
            }

            for (int index1 = 0; index1 < numIndexes; index1++) {
                int i = sortedBundleIndexes[index1];
                Bundle bundle1 = sortedBundles[i];
                Bundle bundle11 = swappedBundles[i];

                if (swapLevel == 0 && precalc.getMaxKnownSolutionLengthByShape1(swapLevel, bundle1.shape()) < bestAnsLen) break;
                if (swapLevel == 1 && precalc.getMaxKnownSolutionLengthByShape12(swapLevel, prevBundles[0].shape(), bundle1.shape()) < bestAnsLen) break;
                
                int length1 = lookup0.getNextSolutionLength(bundle11);
                if (length1 >= bestAnsLen) {
                    Bundle[] bundles = appendBundles(prevBundles, bundle1);
                    if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length1, bundles);
                    nextMoves &= heads(bundles);
                    if (length1 > bestAnsLen || nextMoves == 0) {
                        if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length1, bundles);
                        return 0;
                    }
                }

                SolutionLengthLookup lookup1 = lookup0.getNextLookup(bundle11);
                if (lookup1 == null) continue;
                
                if (lookup1.getMaxKnownNextSolutionLength() < bestAnsLen) continue;

                for (int index2 = index1+1; index2 < numIndexes; index2++) {
                    int j = sortedBundleIndexes[index2];
                    Bundle bundle2 = sortedBundles[j];
                    Bundle bundle22 = swappedBundles[j];
                    
                    if (swapLevel == 0 && precalc.getMaxKnownSolutionLengthByShape12(swapLevel, bundle1.shape(), bundle2.shape()) < bestAnsLen) break;
                    
                    int length2 = lookup1.getNextSolutionLength(bundle22);
                    if (length2 >= bestAnsLen) {
                        Bundle[] bundles = appendBundles(prevBundles, bundle1, bundle2);
                        if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length2, bundles);
                        nextMoves &= heads(bundles);
                        if (length2 > bestAnsLen || nextMoves == 0) {
                            if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length2, bundles);
                            return 0;
                        }
                    }
    
                    SolutionLengthLookup lookup2 = lookup1.getNextLookup(bundle22);
                    if (lookup2 == null) continue;
                    
                    if (lookup2.getMaxKnownNextSolutionLength() < bestAnsLen) continue;
    
                    for (int index3 = index2+1; index3 < numIndexes; index3++) {
                        int k = sortedBundleIndexes[index3];
                        Bundle bundle3 = sortedBundles[k];
                        Bundle bundle33 = swappedBundles[k];
    
                        int length3 = lookup2.getNextSolutionLength(bundle33);
                        if (length3 >= bestAnsLen) {
                            Bundle[] bundles = appendBundles(prevBundles, bundle1, bundle2, bundle3);
                            if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length3, bundles);
                            nextMoves &= heads(bundles);
                            if (length3 > bestAnsLen || nextMoves == 0) {
                                if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length3, bundles);
                                return 0;
                            }
                        }
                        
                        SolutionLengthLookup lookup3 = lookup2.getNextLookup(bundle33);
                        if (lookup3 == null) continue;
                        
                        if (lookup3.getMaxKnownNextSolutionLength() < bestAnsLen) continue;
    
                        for (int index4 = index3+1; index4 < numIndexes; index4++) {
                            int l = sortedBundleIndexes[index4];
                            Bundle bundle4 = sortedBundles[l];
                            Bundle bundle44 = swappedBundles[l];
    
                            int length4 = lookup3.getNextSolutionLength(bundle44);
                            if (length4 >= bestAnsLen) {
                                Bundle[] bundles = appendBundles(prevBundles, bundle1, bundle2, bundle3, bundle4);
                                if (DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length4, bundles);
                                nextMoves &= heads(bundles);
                                if (length4 > bestAnsLen || nextMoves == 0) {
                                    if (DEBUG) debugHbaResult(nextMoves, bestAnsLen, length4, bundles);
                                    return 0;
                                }
                            }
                        }
                    }
                }
            }
            return nextMoves;
        }
        
        private void appendSortedBundleIndex(int[] sortedBundleIndexes, int len, Bundle[] bundles, int bundleIdx) {
            Bundle bundle = bundles[bundleIdx];
            int insertPos = len;
            while (insertPos > 0) {
                int prevIndex = sortedBundleIndexes[insertPos-1];
                Bundle prevBundle = bundles[prevIndex];
                if (prevBundle.toSortable() >= bundle.toSortable()) {
                    break;
                }
                sortedBundleIndexes[insertPos] = prevIndex;
                insertPos--;
            }
            sortedBundleIndexes[insertPos] = bundleIdx;
        }

        private int preparePieces(Bundle[] sortedBundles, int len, Bundle[] sortedPieces, int[] sortBuffer) {
            int numDigits0 = sortedBundles[0].numDigits();
            if (numDigits0 < MIN_PIECE_CHECK_SIZE) return 0;//XXX
            
            int numPieces = 0;
    
            for (int i = 0; i < len; i++) {
                Bundle bundle = sortedBundles[i];
                int heads = bundle.heads();
                int digits = bundle.digits();
                int numDigits = bundle.numDigits();
    
                if (numDigits < numDigits0-1) break;
                
                for (int d = 0; d < MAX_N; d++) {
                    int mask = 1 << d;
                    if ((heads & mask) != 0) {
                        int piece = (mask << 9) | digits;
                        int pieceShape = shape(1, numDigits);
                        int sortablePiece = (pieceShape << (9+9)) | piece;
                        
                        sortBuffer[numPieces++] = -sortablePiece; // reverse sort order
                    }
                }
            }

            Arrays.sort(sortBuffer, 0, numPieces);
            
            for (int i = 0; i < numPieces; i++) {
                sortedPieces[i] = Bundle.unpack((-sortBuffer[i]) & MASK_18);
            }
            
            return numPieces;
        }
    
    }
        
    public static int heads(Bundle[] bundles) {
        return heads(bundles, bundles.length);
    }

    public static int digits(Bundle[] bundles) {
        return digits(bundles, bundles.length);
    }

    public static int heads(Bundle[] bundles, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            result |= bundles[i].heads();
            if (result == MASK_9) break;
        }
        return result;
    }
    
    public static int digits(Bundle[] bundles, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            result |= bundles[i].digits();
            if (result == MASK_9) break;
        }
        return result;
    }
    
    public static void makeBundleSwap1234(Bundle bundle, int[] swap) {
        int heads = bundle.heads();
        int digits = bundle.digits();
        int numHeads = bundle.numHeads();
        int numDigits = bundle.numDigits();
        int nextHeadDigit = 0;
        int nextTailDigit = numHeads;
        int nextMissingDigit = numDigits;
        for (int d = 0; d < 9; d++) {
            int mask = 1 << d;
            if ((heads & mask) != 0) {
                swap[d] = nextHeadDigit++;
            } else if ((digits & mask) != 0) {
                swap[d] = nextTailDigit++;
            } else {
                swap[d] = nextMissingDigit++;
            }
        }
    }
    
    public static String maskDigitsToString(int mask) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (((mask >> i) & 1) != 0) {
                builder.append((char)('1' + i));
            }
        }
        return builder.toString();
    }

    public static final String bundlesStr(Bundle... bundles) {
        return Arrays.stream(bundles)
               .map(Bundle::toString)
               .collect(Collectors.joining(" "));
    }
    
    public static final Bundle[] parseBundles(String str) {
        return Arrays.stream(str.split(" "))
                .map(Bundle::parse)
                .toArray(Bundle[]::new);
    }

    public static String formatLog(String message) {
        String date = LOG_DATE_FORMAT.format(new Date());
        long usedHeapMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        return date + " [" + usedHeapMB +" MB] - " + message;
    }

    public static int shape(int numHeads, int numDigits) {
        return (numDigits << 4) | numHeads;
    }

    public static String shape(Bundle[] bundles) {
        return Arrays.stream(bundles)
                .map(bundle -> shapeStr(bundle.shape()))
                .collect(Collectors.joining(","));
    }
    
    public static String shapeStr(int shape) {
        int numHeads = shape & MASK_4;
        int numDigits = shape >> 4;
        return numHeads + "/" + numDigits;
    }
    
    private static final int IO_BUFFER_SIZE = 65536;//2*1024*1024;

    public static void deserializeFromFileGZ(File file, Consumer<ObjectInputStream> callback) {
        if (!file.exists()) return;
        try (ObjectInputStream stream = new ObjectInputStream(new MiGzInputStream(
                new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE)))) {
            callback.accept(stream);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static void serializeToFileGZ(File file, Consumer<ObjectOutputStream> callback) {
        file.getAbsoluteFile().getParentFile().mkdirs();
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try (ObjectOutputStream stream = new ObjectOutputStream(new MiGzOutputStream(
                new BufferedOutputStream(new FileOutputStream(tmpFile), IO_BUFFER_SIZE)))) {
            callback.accept(stream);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        if (file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("Could not delete file: " + file.getAbsolutePath());
            }
        }
        if (!tmpFile.renameTo(file)) {
            throw new RuntimeException("Could not rename file: " + tmpFile.getAbsolutePath() 
                    + " -> " + file.getAbsolutePath());
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T deserializeObjectFromFileGZ(File file) {
        Object[] result = new Object[1];
        deserializeFromFileGZ(file, stream -> {
            try {
                result[0] = stream.readObject();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });
        return (T) result[0];
    }
    
    public static void serializeObjectToFileGZ(File file, Serializable object) {
        serializeToFileGZ(file, stream -> {
            try {
                stream.writeObject(object);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void printWriteToFileGZ(File file, Consumer<PrintWriter> consumer) {
        file.getAbsoluteFile().getParentFile().mkdirs();
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile), IO_BUFFER_SIZE);
                MiGzOutputStream gzOut = new MiGzOutputStream(out);
                PrintWriter pw = new PrintWriter(gzOut)) {
            consumer.accept(pw);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        if (file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException();
            }
        }
        if (!tmpFile.renameTo(file)) {
            throw new RuntimeException();
        }
    }

    public static void readLinesFromFile(File file, Consumer<String> callback) {
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(fileReader(file))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                callback.accept(line);
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("resource")
    private static Reader fileReader(File file) throws FileNotFoundException {
        if (file.getName().endsWith(".gz")) {
            return new InputStreamReader(new MiGzInputStream(
                    new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE)));
        }
        return new FileReader(file);
    }
    
    public static void loadSolutionsFromFile(File file, BiConsumer<Bundle[], String> callback) {
        readLinesFromFile(file, (line) -> {
            int index = line.lastIndexOf(' ');
            String answer = line.substring(index+1);
            Bundle[] bundles = parseBundles(line.substring(0, index));
            callback.accept(bundles, answer);
        });
    }

    public static File checkpointFile(String shape) {
        return new File(String.format("precalc-%s.bin.gz", shape.replace('/', '@')));
    }
    
    public static void init(String[] args) throws Exception {
        System.out.println();
        for (int index = 0; index < args.length; index++) {
            String arg = args[index];
            if ("-t".equals(arg)) {
                NUM_WORKER_THREADS = Integer.parseInt(args[++index]);
                System.out.println("NUM_WORKER_THREADS: " + NUM_WORKER_THREADS);
            } else if ("-s".equals(arg)) {
                NUM_SOLVERS = Integer.parseInt(args[++index]);
                System.out.println("NUM_SOLVERS: " + NUM_SOLVERS);
            } else if ("-N".equals(arg)) {
                MAX_N = Integer.parseInt(args[++index]);
                System.out.println("MAX_N: " + MAX_N);
            } else if ("-max-swap-level".equals(arg)) {
                MAX_SWAP_LEVEL = Integer.parseInt(args[++index]);
                System.out.println("MAX_SWAP_LEVEL: " + MAX_SWAP_LEVEL);
            } else if ("-min-piece-check-size".equals(arg)) {
                MIN_PIECE_CHECK_SIZE = Integer.parseInt(args[++index]);
                System.out.println("MIN_PIECE_CHECK_SIZE: " + MIN_PIECE_CHECK_SIZE);
            } else if ("-solve-alg".equals(arg)) {
                SOLVE_ALG = args[++index];
                System.out.println("SOLVE_ALG: " + SOLVE_ALG);
            } else if ("-precalc-alg".equals(arg)) {
                PRECALC_ALG = args[++index];
                System.out.println("PRECALC_ALG: " + PRECALC_ALG);
            } else if ("-dfs-max-cache".equals(arg)) {
                DFS_MAX_CACHE = Long.parseLong(args[++index]);
                System.out.println("DFS_MAX_CACHE: " + DFS_MAX_CACHE);
            } else if ("-dfs-max-save-cache".equals(arg)) {
                DFS_MAX_SAVE_CACHE = Long.parseLong(args[++index]);
                System.out.println("DFS_MAX_SAVE_CACHE: " + DFS_MAX_SAVE_CACHE);
            } else if ("-dfs-batch-save-minutes".equals(arg)) {
                DFS_BATCH_SAVE_MINUTES = Integer.parseInt(args[++index]);
                System.out.println("DFS_BATCH_SAVE_MINUTES: " + DFS_BATCH_SAVE_MINUTES);
            } else if ("-dfs-disk-block-size".equals(arg)) {
                DFS_DISK_BLOCK_SIZE = Integer.parseInt(args[++index]);
                System.out.println("DFS_DISK_BLOCK_SIZE: " + DFS_DISK_BLOCK_SIZE);
            } else if ("-dfs-disk-batch-size".equals(arg)) {
                DFS_DISK_BATCH_SIZE = Integer.parseInt(args[++index]);
                System.out.println("DFS_DISK_BATCH_SIZE: " + DFS_DISK_BATCH_SIZE);
            } else if ("-dfs-disk-seen-size".equals(arg)) {
                DFS_DISK_SEEN_SIZE = Integer.parseInt(args[++index]);
                System.out.println("DFS_DISK_SEEN_SIZE: " + DFS_DISK_SEEN_SIZE);
            } else if ("-dfs-batch-size".equals(arg)) {
                DFS_BATCH_SIZE = Integer.parseInt(args[++index]);
                System.out.println("DFS_BATCH_SIZE: " + DFS_BATCH_SIZE);
            } else if ("-checkpoint-shapes".equals(arg)) {
                CHECKPOINT_SHAPES = args[++index];
                System.out.println("CHECKPOINT_SHAPES: " + CHECKPOINT_SHAPES);
            } else if ("-max-precalc-shape".equals(arg)) {
                MAX_PRECALC_SHAPE = args[++index];
                System.out.println("MAX_PRECALC_SHAPE: " + MAX_PRECALC_SHAPE);
            } else if ("-dist-pc".equals(arg)) {
                DIST_PC = Integer.parseInt(args[++index]);
                System.out.println("DIST_PC: " + DIST_PC);
            } else if ("-dist".equals(arg)) {
                DIST = Integer.parseInt(args[++index]);
                System.out.println("DIST: " + DIST);
            } else if ("-dist-hba0".equals(arg)) {
                DIST_HBA0 = Integer.parseInt(args[++index]);
                System.out.println("DIST_HBA0: " + DIST_HBA0);
            } else if ("-dist-hba1".equals(arg)) {
                DIST_HBA1 = Integer.parseInt(args[++index]);
                System.out.println("DIST_HBA1: " + DIST_HBA1);
            } else if ("-dist-hba2".equals(arg)) {
                DIST_HBA2 = Integer.parseInt(args[++index]);
                System.out.println("DIST_HBA2: " + DIST_HBA2);
            } else if ("-dist-hba3".equals(arg)) {
                DIST_HBA3 = Integer.parseInt(args[++index]);
                System.out.println("DIST_HBA3: " + DIST_HBA3);
            } else if ("-dist-hba4".equals(arg)) {
                DIST_HBA4 = Integer.parseInt(args[++index]);
                System.out.println("DIST_HBA4: " + DIST_HBA4);
            } else if ("-alloc".equals(arg)) {
                PRINT_ALLOC = true;
                System.out.println("PRINT_ALLOC: " + PRINT_ALLOC);
            } else if ("-save-precalc".equals(arg)) {
                SAVE_PRECALC = Boolean.valueOf(args[++index]);
                System.out.println("SAVE_PRECALC: " + SAVE_PRECALC);
            } else if ("-debug".equals(arg)) {
                DEBUG = true;
                System.out.println("DEBUG: " + DEBUG);
            } else if ("-debug-next-moves".equals(arg)) {
                DEBUG_NEXT_MOVES = true;
                System.out.println("DEBUG_NEXT_MOVES: " + DEBUG_NEXT_MOVES);
            } else if ("-print-levels".equals(arg)) {
                PRINT_LEVELS = true;
                System.out.println("PRINT_LEVELS: " + PRINT_LEVELS);
            } else if ("-print-shape-stats".equals(arg)) {
                PRINT_SHAPE_STATS = true;
                System.out.println("PRINT_SHAPE_STATS: " + PRINT_SHAPE_STATS);
            } else if ("-print-states-step".equals(arg)) {
                PRINT_STATES_STEP = Integer.parseInt(args[++index]);
                System.out.println("PRINT_STATES_STEP: " + PRINT_STATES_STEP);
            } else if ("-solve-file".equals(arg)) {
                precalc(new long[10]);
                System.out.println("precalc done");
                Solver solver = createSolver(SOLVE_ALG, new ThreadAllocator(NUM_WORKER_THREADS, 1));
                long begin = System.currentTimeMillis();
                Files.lines(Paths.get(args[++index])).forEach(line -> {
                    Problem problem = new Problem(parseBundles(line.trim()));
                    solver.solve(problem);
                    problem.printResult("");
                });
                long end = System.currentTimeMillis();
                System.out.println("solve done in " + timeStr(end-begin));
                System.exit(0);
            } else if ("-solve-file-mt".equals(arg)) {
                precalc(new long[10]);
                System.out.println("precalc done");
                long begin = System.currentTimeMillis();
                Path inputFile = Paths.get(args[++index]);
                solveInParallel(SOLVE_ALG, (consumer) -> {
                    try {
                        Files.lines(inputFile).forEach(line -> {
                            Problem problem = new Problem(parseBundles(line.trim()));
                            consumer.accept(problem);
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.exit(-1);
                    }
                }, (problem) -> {
                    problem.printResult("");
                });
                long end = System.currentTimeMillis();
                System.out.println("solve done in " + timeStr(end-begin));
                System.exit(0);
            } else if ("-solve".equals(arg)) {
                precalc(new long[10]);
                System.out.println("precalc done");
                String bundlesStr = args[++index];
                System.out.println("solve: " + bundlesStr);
                Problem problem = new Problem(parseBundles(bundlesStr));
                Solver solver = createSolver(SOLVE_ALG, new ThreadAllocator(NUM_WORKER_THREADS, 1));
                solver.solve(problem);
                problem.printResult("");
                PartialsLookup.printHAStats();
                System.exit(0);
            } else if ("-solve-level".equals(arg)) {
                precalc(new long[10]);
                System.out.println("precalc done");
                String bundlesStr = args[++index];
                int level = Integer.parseInt(args[++index]);
                System.out.println("solve: " + bundlesStr);
                System.out.println("level: " + level);
                Problem problem = new Problem(parseBundles(bundlesStr));
                Solver solver = createSolver(SOLVE_ALG, new ThreadAllocator(NUM_WORKER_THREADS, 1));
                long begin = System.currentTimeMillis();
                String ans = solver.solve(problem.bundles, level);
                long end = System.currentTimeMillis();
                problem.answer = ans;
                problem.solveDuration = end-begin;
                problem.printResult("");
                PartialsLookup.printHAStats();
                System.exit(0);
            } else if ("-check-hba".equals(arg)) {
                DEBUG = true;
                DEBUG_NEXT_MOVES = true;
                precalc(new long[10]);
                System.out.println("precalc done");
                String bundlesStr = args[++index];
                int bestAnsLen = Integer.parseInt(args[++index]);
                System.out.println("bundles: " + bundlesStr);
                System.out.println("bestAnsLen: " + bestAnsLen);
                PartialsLookup partialsLookup = new PartialsLookup();
                Bundle[] bundles = parseBundles(bundlesStr);
                Bundle[] sortedBundles = new Bundle[bundles.length];
                int len = bundles.length;
                int nextMoves = prepareBundles(bundles, sortedBundles);
                nextMoves = partialsLookup.canHaveAnswer(sortedBundles, len, nextMoves, bestAnsLen);
                System.out.println(nextMoves == 0 ? "Reject" : "Accept");
                System.exit(0);
            } else if ("-print-shapes-memory-usage".equals(arg)) {
                Map<String, Long> usageByKey = new LinkedHashMap<>();
                Files.walk(Paths.get(PRECALC_DIR))
                    .sorted()
                    .forEach(path -> {
                        File file = path.toFile();
                        String fileName = file.getName();
                        if (!fileName.endsWith(".bin.gz")) return;
                        String shapeLevel = fileName.split("[.]")[0];
                        String[] split = shapeLevel.split("-");
                        String shape = split[0].replace('@', '/');
                        String level = split[1];
                        String[] parts = shape.split(",");
                        String key = String.join(",", Arrays.asList(parts).subList(0, parts.length-1)) + ",*" + "-" + level;
                        System.out.println(String.format("Loading shape precalc %s (swapLevel %s)", shape, level));
                        Precalc shapePrecalc = deserializeObjectFromFileGZ(file);
                        long shapeUsage = memoryUsage(shapePrecalc);
                        usageByKey.compute(key, (k, v) -> {
                            return Math.max(shapeUsage, v != null ? v : 0);
                        });
                    });
                usageByKey.forEach((key, usage) -> {
                    System.out.println(key + "\t" + (usage / 1024 / 1024));
                });
                System.exit(0);
            } else {
                throw new RuntimeException("Unknown argument: " + arg);
            }
        }
    }


    public static void main(String[] args) throws Exception {
        init(args);
        long begin = System.currentTimeMillis();
        long[] durations = new long[10];
        precalc(durations);
        long precalcEnd = System.currentTimeMillis();
        String[] expectedAnswers = new String[] {
                null, 
                "1", 
                "121", 
                "1213121", 
                "123412314213", 
                "1234512341523142351",
                "1234516234152361425312643512", 
                "123451672341526371425361274351263471253", 
                "1234156782315426738152643718265341278635124376812453", 
                "123456781923451678234915627348152963471825364912783546123976845123"};
        Solver solver = createSolver(SOLVE_ALG, new ThreadAllocator(NUM_WORKER_THREADS, 1));
        for (int i = 1; i <= MAX_N && solver != null; i++) {
            int mask = (1<<i)-1;
            Bundle bundle = Bundle.unpack((mask << 9) | mask);
            Problem problem = new Problem(new Bundle[] {bundle});
            solver.solve(problem);
            problem.printResult("");
            String solution = problem.answer;
            if (!solution.equals(expectedAnswers[i])) {
                RuntimeException error = new RuntimeException(solution + " != " + expectedAnswers[i]);
                error.printStackTrace(System.out);
                throw error;
            }
        }
        long solveEnd = System.currentTimeMillis();
        System.out.println(formatLog("precalc " + timeStr(precalcEnd-begin)) + " " + Arrays.toString(durations));
        System.out.println(formatLog("precalc calc " + timeStr(totalPrecalcCalcTime)));//XXX
        System.out.println(formatLog("solve " + timeStr(solveEnd-precalcEnd)));
        System.out.println(formatLog("total " + timeStr(solveEnd-begin)));
    }

    public static String timeStr(long millis) {
        StringBuilder builder = new StringBuilder();
        String suffix = "dhms";
        long[] divider = new long[] {TimeUnit.DAYS.toMillis(1), TimeUnit.HOURS.toMillis(1), 
                TimeUnit.MINUTES.toMillis(1), TimeUnit.SECONDS.toMillis(1)};
        for (int i = 0; i < suffix.length(); i++) {
            if (builder.length() > 0 || millis >= divider[i] || i+1 == suffix.length()) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                long value = millis / divider[i];
                builder.append(value);
                builder.append(suffix.charAt(i));
                millis -= value * divider[i];
            }
        }
        return builder.toString();
    }

    public static long memoryUsage(Object obj) {
        return -1;//GraphLayout.parseInstance(obj).totalSize();
    }

    public static Solver createSolver(String algName, ThreadAllocator threadAllocator) {
        switch (algName) {
        case "none": return null;
        case "skip": return new SkipSolver();
        case "bfs": return new BFSSolver();
        case "bfs-batch": return new BFSBatchSolver(threadAllocator);
        case "bfs-disk": return new BFSDiskSolver();
        case "dfs": return new DFSSolver();
        case "dfs-loop": return new DFSLoopSolver();
        case "dfs-batch": return new DFSBatchSolver(threadAllocator);
        case "dfs-disk": return new DFSDiskSolver();
        default: throw new RuntimeException("Unknown algorithm: " + algName);
        }
    }

    public static abstract class Solver {

        public abstract String solve(Bundle[] bundles0, int bestAnsLen);

        public void solve(Problem problem) {
            boolean exit = false;
//            if ("1234/1234 15/1256 27/2678 8/378".equals(P069.toString(problem.bundles))) {
//                exit = true;
//            }
            long begin = System.currentTimeMillis();
            String answer = null;
            for (int len = 1; len <= 100; len++) {
                answer = solve(problem.bundles, len);
                if (answer != null) {
                    break;
                }
            }
            long end = System.currentTimeMillis();
            if (answer == null) {
                throw new IllegalStateException();
            }
            problem.answer = answer;
            problem.solveDuration = end - begin;
            if (exit) {
                System.exit(0);
            }
        }
        
    }
    
    public static class SkipSolver extends Solver {
        
        public String solve(Bundle[] bundles0, int bestAnsLen) {
            return null;
        }

        public void solve(Problem problem) {
            // do nothing
        }
        
    }
    
    public static class BFSSolver extends Solver {
    
        public static class State {
            String path;
            Bundle[] bundles;
            int allowMoves;

            State(String path, Bundle[] bundles, int allowMoves) {
                this.path = path;
                this.bundles = bundles;
                this.allowMoves = allowMoves;
            }
        }

        private final KeyBuilder keyBuilder = new KeyBuilder();
        private final MoveBuilder moveBuilder = new MoveBuilder();
        private final PartialsLookup partialsLookup = new PartialsLookup();
        private final Bundle[] sortedBundles = new Bundle[256];
  
        @Override
        public String solve(Bundle[] bundles0, int bestAnsLen) {
            long begin = System.currentTimeMillis();
            long prev = begin;
            List<State> states = Arrays.asList(new State("", bundles0, -1));
            for (int level = 0; level < bestAnsLen; level++) {
                if (PRINT_LEVELS) {
                    long now = System.currentTimeMillis();
                    printLevel(bundles0, bestAnsLen, level, begin, prev, now, states);
                    prev = now;
                }
                if (PRINT_SHAPE_STATS) {
                    printShapeStats(states);
                }
                List<State> next = new ArrayList<>();
                Set<Key> seen = new MemoryEfficientHashSet<>();
                long stateNum = 0;
                for (State state : states) {
                    String path = state.path;
                    Bundle[] bundles = state.bundles;
                    int allowMoves = state.allowMoves;
    
                    stateNum++;
                    if (PRINT_STATES_STEP > 0) {
                        if (stateNum % PRINT_STATES_STEP == 0) {
                            printState(bundles0, bestAnsLen, level, state, stateNum);
                        }
                    }
                    
                    int moves = heads(bundles);
                    
                    moves &= allowMoves;
                    for (int m = 0; m < 9; m++) {
                        if (((moves >> m) & 1) == 0) continue;
    
                        Bundle[] nextBundles = moveBuilder.makeMove(bundles, m);
                        String nextPath = path + (1+m);
    
                        if (nextBundles.length == 0) {
                            return nextPath;
                        }
    
                        int len = nextBundles.length;
                        int nextMoves = prepareBundles(nextBundles, sortedBundles);
                        
                        Key nextKey = keyBuilder.makeKey(sortedBundles, len);
                        if (seen.contains(nextKey)) {
                            continue;
                        }
                        seen.add(nextKey);
                        
                        if (DEBUG) System.out.println(nextPath + ": " + bundlesStr(Arrays.copyOf(sortedBundles, len)));
                        
                        nextMoves = partialsLookup.canHaveAnswer(sortedBundles, len, nextMoves, bestAnsLen - nextPath.length());
                        if (nextMoves == 0) {
                            if (DEBUG) System.out.println("Reject: " + nextPath);
                            continue;
                        }
                        if (DEBUG) System.out.println("Accept: " + nextPath);
    
                        State nextState = new State(nextPath, nextBundles, nextMoves);
                        next.add(nextState);
                    }
                }
                states = next;
                if (states.isEmpty()) {
                    return null;
                }
            }
            return null;
        }

        private void printLevel(Bundle[] bundles0, int bestAnsLen, int level, long begin, long prev, long now, List<State> states) {
            String problem = bundlesStr(bundles0);
            State state = states.get(states.size()/2);
            String someStr = bundlesStr(sortSzDesc(state.bundles));
            System.out.println(formatLog(problem + " " + bestAnsLen + " " + level+" "+states.size()
                    + " [" + (now-begin) + " ms, +" + (now-prev) + " ms]" 
                    + (PRINT_STATES_STEP <= 0 ? "; " + someStr : "")));
        }
        
        private void printShapeStats(List<State> states) {
            Map<String, List<State>> byShape = states.stream()
                    .collect(Collectors.groupingBy(
                            state -> shape(Arrays.stream(sortSzDesc(state.bundles)).limit(5).toArray(Bundle[]::new))));
            byShape.entrySet().stream()
                .sorted(Comparator.<Entry<String, List<State>>, Integer>comparing(entry -> entry.getValue().size()).reversed())
                .limit(10)
                .forEach(entry -> System.out.println(entry.getKey()+": "+entry.getValue().size()));
        }

        private void printState(Bundle[] bundles0, int bestAnsLen, int level, State state, long stateNum) {
            String problem = bundlesStr(bundles0);
            System.out.println(formatLog(problem + " " + bestAnsLen + " " + level
                    +" " + stateNum + " " + bundlesStr(sortSzDesc(state.bundles))));
            
        }
        
    }

    public static class BFSBatchSolver extends Solver {
        
        public static class State {
            String prefix;
            Bundle[] bundles;
            Bundle[] sortedBundles;
            Key key;
            int moves;

            State(String prefix, Bundle[] bundles, int moves) {
                this.prefix = prefix;
                this.bundles = bundles;
                this.sortedBundles = new Bundle[bundles.length];
                this.moves = moves;
            }
        }

        private final ThreadAllocator threadAllocator;
        private final MoveBuilder moveBuilder = new MoveBuilder();
  
        public BFSBatchSolver(ThreadAllocator threadAllocator) {
            this.threadAllocator = threadAllocator;
        }

        @Override
        public String solve(Bundle[] bundles0, int bestAnsLen) {
            long beginTime = System.currentTimeMillis();
            long prevTime = beginTime;
            int moves0 = heads(bundles0);
            State state0 = new State("", bundles0, moves0);
            ArrayList<State> states = new ArrayList<>();
            states.add(state0);
            for (int level = 0; level < bestAnsLen; level++) {
                if (PRINT_LEVELS) {
                    long now = System.currentTimeMillis();
                    printLevel(bundles0, bestAnsLen, level, beginTime, prevTime, now, states);
                    prevTime = now;
                }
                if (PRINT_SHAPE_STATS) {
                    printShapeStats(states);
                }

                AtomicLong stateNum = new AtomicLong();
                int level_ = level;
                
                Integer numThreads = threadAllocator.get();
                processInParallel(states, numThreads, () -> {
                    KeyBuilder keyBuilder = new KeyBuilder();
                    Set<Key> seenLocal = new MemoryEfficientHashSet<>();
                    return (state) -> {
                        Bundle[] bundles = state.bundles;
                        Bundle[] sortedBundles = state.sortedBundles;
                        
                        if (PRINT_STATES_STEP > 0) {
                            long num = stateNum.incrementAndGet();
                            if (num % PRINT_STATES_STEP == 0) {
                                printState(bundles0, bestAnsLen, level_, "makeKey", num, state);
                            }
                        }
                        
                        int moves = prepareBundles(bundles, sortedBundles);

                        state.key = keyBuilder.makeKey(sortedBundles, bundles.length);
                        state.moves = moves;

                        if (!seenLocal.add(state.key)) {
                            state.prefix = null;
                            state.bundles = null;
                            state.sortedBundles = null;
                            state.moves = 0;
                            state.key = null;
                        }
                    };
                });

                Set<Key> seen = new MemoryEfficientHashSet<>();
                for (State state : states) {
                    if (state.bundles == null) continue;
                    
                    if (!seen.add(state.key)) {
                        state.prefix = null;
                        state.bundles = null;
                        state.sortedBundles = null;
                        state.moves = 0;
                    }
                    state.key = null;
                }
                seen.clear();

                stateNum.set(0L);
                
                processInParallel(states, numThreads, () -> {
                    PartialsLookup partialsLookup = new PartialsLookup();
                    return (state) -> {
                        String prefix = state.prefix;
                        Bundle[] bundles = state.bundles;
                        Bundle[] sortedBundles = state.sortedBundles;
                        int moves = state.moves;
                        
                        if (PRINT_STATES_STEP > 0) {
                            long num = stateNum.incrementAndGet();
                            if (num % PRINT_STATES_STEP == 0) {
                                printState(bundles0, bestAnsLen, level_, "canHaveAnswer", num, null);
                            }
                        }

                        if (bundles == null) return;
                        
                        moves = partialsLookup.canHaveAnswer(sortedBundles, bundles.length, moves, bestAnsLen - prefix.length());
                        
                        state.moves = moves;
                        
                        state.sortedBundles = null;
                    };
                });
                
                ArrayList<State> nextStates = new ArrayList<>();
                stateNum.set(0L);
                
                int numStates = states.size();
                for (int index = 0; index < numStates; index++) {
                    State state = states.get(index);
                    states.set(index, null);
                    
                    String prefix = state.prefix;
                    Bundle[] bundles = state.bundles;
                    int moves = state.moves;

                    if (bundles == null) continue;
                    
                    for (int m = 0; m < 9; m++) {
                        if (((moves >> m) & 1) == 0) continue;
                        
                        Bundle[] nextBundles = moveBuilder.makeMove(bundles, m);
                        String nextPrefix = prefix + (1+m);

                        if (nextBundles.length == 0) {
                            return nextPrefix;
                        }
                        
                        if (PRINT_STATES_STEP > 0) {
                            long num = stateNum.incrementAndGet();
                            if (num % PRINT_STATES_STEP == 0) {
                                printState(bundles0, bestAnsLen, level_, "makeMove", num, null);
                            }
                        }

                        State nextState = new State(nextPrefix, nextBundles, -1);
                        nextStates.add(nextState);
                    }
                }

                states.clear();
                states.addAll(nextStates);
                
                if (states.isEmpty()) {
                    return null;
                }
            }
            return null;
        }

        private void printLevel(Bundle[] bundles0, int bestAnsLen, int level, long begin, long prev, long now, List<State> states) {
            String problem = bundlesStr(bundles0);
            State state = states.get(states.size()/2);
            String someStr = bundlesStr(sortSzDesc(state.bundles));
            System.out.println(formatLog(problem + " " + bestAnsLen + " " + level+" "+states.size()
                    + " [" + (now-begin) + " ms, +" + (now-prev) + " ms]" 
                    + (PRINT_STATES_STEP <= 0 ? "; " + someStr : "")));
        }

        private void printShapeStats(ArrayList<State> states) {
            Map<String, List<State>> byShape = states.stream()
                    .collect(Collectors.groupingBy(
                            state -> shape(Arrays.stream(sortSzDesc(state.bundles)).limit(5).toArray(Bundle[]::new))));
            byShape.entrySet().stream()
                .sorted(Comparator.<Entry<String, List<State>>, Integer>comparing(entry -> entry.getValue().size()).reversed())
                .limit(10)
                .forEach(entry -> System.out.println(entry.getKey()+": "+entry.getValue().size()));
        }

        private void printState(Bundle[] bundles0, int bestAnsLen, int level, String prefix, long stateNum, State state) {
            String problem = bundlesStr(bundles0);
            System.out.println(formatLog(problem + " " + bestAnsLen + " " + level
                    + " " + prefix
                    + " " + stateNum 
                    + " " + (state != null ? bundlesStr(sortSzDesc(state.bundles)) : "")));
            
        }
        
    }
    
    public static class BFSDiskSolver extends Solver {
        
        private static final int COMPRESSION_LEVEL = 1;
        
        private static final String SKIP_FILE = "skip.txt";
    
        private String skipProblem;
        private Integer skipLength;
        private Integer skipLevel;

        private void loadSkipFile() {
            if (!new File(SKIP_FILE).exists()) return;
            try (BufferedReader reader = new BufferedReader(new FileReader(SKIP_FILE))) {
                skipProblem = reader.readLine();
                skipLength = Integer.parseInt(reader.readLine());
                skipLevel = Integer.parseInt(reader.readLine());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void saveSkipFile(Bundle[] problem, int bestAnsLen, int level) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(SKIP_FILE))) {
                pw.println(bundlesStr(problem));
                pw.println(bestAnsLen);
                pw.println(level);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void solve(Problem problem) {
            loadSkipFile();
            if (skipProblem != null) {
                if (!skipProblem.equals(bundlesStr(problem.bundles))) return;
                System.out.println("Skip to problem: " + bundlesStr(problem.bundles));
                skipProblem = null;
            }
            
            long begin = System.currentTimeMillis();
            String answer = null;
            for (int len = 1; len <= 100; len++) {
                if (skipLength != null) {
                    if (len < skipLength) continue;
                    System.out.println("Skip to bestAnsLen: " + len);
                    skipLength = null;
                } 
                
                answer = solve(problem.bundles, len);
                if (answer != null) {
                    break;
                }
            }
            long end = System.currentTimeMillis();
            if (answer == null) {
                throw new IllegalStateException();
            }
            problem.answer = answer;
            problem.solveDuration = end - begin;
        }
    
       @Override
        public String solve(Bundle[] bundles0, int bestAnsLen) {
            String problem = bundlesStr(bundles0);
            String problemDir = bundlesStr(bundles0).replace(' ', '_').replace('/', '@');
            String baseDir = String.format("%s/%02d", problemDir, bestAnsLen);
            
            if (skipLevel == null) {
                String levelDir0 = String.format("%s/prefixes-%02d-%02d", baseDir, bestAnsLen, 0);
                String bucketFile0 = String.format("%s/%03d.gz", levelDir0, 0);
                
                writeBucket(bucketFile0, pw -> pw.println(""));
            }
            
            for (int level = 0; level < bestAnsLen; level++) {
                if (skipLevel != null) {
                    if (level < skipLevel) continue;
                    System.out.println("Skip to level: " + level);
                    skipLevel = null;
                }
                saveSkipFile(bundles0, bestAnsLen, level);
                
                String levelDir = String.format("%s/prefixes-%02d-%02d", baseDir, bestAnsLen, level);
                String nextLevelDir = String.format("%s/prefixes-%02d-%02d", baseDir, bestAnsLen, level+1);
                String nextLevelUnsortedDir = String.format("%s/prefixes-%02d-%02d-unsorted", baseDir, bestAnsLen, level+1);

                if (!new File(levelDir).exists()) {
                    break;
                }
                
                int numNextLevelBuckets = estimateNumberOfNextLevelBuckets(levelDir);
                
                int level_ = level;
                
                Consumer<String> logger = (str) -> {
                    System.out.println(formatLog(problem + " " + bestAnsLen + " " + level_ + " " + str));
                };

                AtomicReference<String> answer = new AtomicReference<>();
                
                if (!new File(nextLevelUnsortedDir).exists()) {
                    writeUnsorted(levelDir, nextLevelUnsortedDir, numNextLevelBuckets, bundles0, bestAnsLen, answer, logger);
                }
                
                if (answer.get() != null) {
                    return answer.get();
                }

                writeNextLevel(nextLevelUnsortedDir, numNextLevelBuckets, nextLevelDir, bundles0, logger);
            }
            return null;
       }

       private void writeUnsorted(String levelDir, String unsortedDir, int numBuckets, Bundle[] bundles0, 
               int bestAnsLen, AtomicReference<String> answer, Consumer<String> logger) {
           AtomicLong numInStates = new AtomicLong();
           AtomicLong numOutStates = new AtomicLong();

           BlockingQueue<String> resultQueue = new LinkedBlockingQueue<String>();

           writeBuckets(unsortedDir, numBuckets, writer -> {
               processInParallel(NUM_WORKER_THREADS, String.class, (consumer) -> {
                   readSortedBuckets(levelDir, prefix -> {
                       if (!resultQueue.isEmpty()) return;
                       consumer.accept(prefix);
                   });
               }, () -> {
                   final Bundle[] sortedBundles = new Bundle[256];
                   final KeyBuilder keyBuilder = new KeyBuilder();
                   final MoveBuilder moveBuilder = new MoveBuilder();
                   final PartialsLookup partialsLookup = new PartialsLookup();
                   return (prefix) -> {
                       long numIn = numInStates.incrementAndGet();

                       Bundle[] bundles = moveBuilder.makeMoves(bundles0, prefix);

                       int moves = prepareBundles(bundles, sortedBundles);

                       moves = partialsLookup.canHaveAnswer(sortedBundles, bundles.length, moves, bestAnsLen - prefix.length());
                       if (moves == 0) return;

                       for (int m = 0; m < 9; m++) {
                           if (((moves >> m) & 1) == 0) continue;

                           Bundle[] nextBundles = moveBuilder.makeMove(bundles, m);
                           String nextPrefix = prefix + (1+m);

                           if (nextBundles.length == 0) {
                               resultQueue.add(nextPrefix);
                               return;
                           }

                           int hash = keyBuilder.makeHash(nextBundles);

                           writer.accept(nextPrefix, hash);

                           long numOut = numOutStates.incrementAndGet();
                           if (numOut % 1000000 == 0) {
                               logger.accept(numIn+" -> "+numOut + " " + bundlesStr(sortSzDesc(nextBundles)));
                           }
                       }
                   };
               });
           });                

           logger.accept("states = " + numOutStates.get());
           
           if (!resultQueue.isEmpty()) {
               List<String> results = new ArrayList<>(resultQueue);
               Collections.sort(results);

               answer.set(results.get(0));
           }
       }

       private void writeNextLevel(String unsortedDir, int numBuckets, String nextLevelDir, Bundle[] bundles0, 
               Consumer<String> logger) {
           AtomicLong numInStates = new AtomicLong();
           AtomicLong numOutStates = new AtomicLong();
           
           processInParallel(NUM_WORKER_THREADS, Integer.class, (consumer) -> {
               for (int bucket = 0; bucket < numBuckets; bucket++) {
                   consumer.accept(bucket);
               }
           }, () -> {
               return (bucket) -> {
                   String bucketFile = String.format("%s/%03d.gz", unsortedDir, bucket);
                   String sortedBucketFile = String.format("%s/%03d.gz", nextLevelDir, bucket);
                   String sortedBucketTemp = sortedBucketFile + ".tmp";
                   
                   if (!new File(bucketFile).exists()) return;
                   if (new File(sortedBucketFile).exists()) return;
                   
                   List<String> list = new ArrayList<>();
                   Set<Key> seen = new MemoryEfficientHashSet<>();
                   
                   readBucket(bucketFile, prefix -> list.add(prefix)); 

                   Collections.sort(list);

                   final KeyBuilder keyBuilder = new KeyBuilder();
                   final MoveBuilder moveBuilder = new MoveBuilder();
                   
                   writeBucket(sortedBucketTemp, pw -> {
                       for (String prefix : list) {
                           long numIn = numInStates.incrementAndGet();
                           
                           Bundle[] bundles = moveBuilder.makeMoves(bundles0, prefix);
                           
                           Key key = keyBuilder.makeKey(bundles);
                           if (seen.add(key)) {
                               pw.println(prefix);
                               long numOut = numOutStates.incrementAndGet();
                               if (numOut % 1000000 == 0) {
                                   logger.accept(numIn+" -> "+numOut + " " + bundlesStr(sortSzDesc(bundles)));
                               }
                           }
                       }
                   });

                   renameFile(sortedBucketTemp, sortedBucketFile);
               };
           });
           
           logger.accept("unique_states = " + numOutStates.get());
           
           deleteFiles(unsortedDir);
       }

       private static void deleteFiles(String dir) {
            File dirFile = new File(dir);
            File[] files = dirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        throw new RuntimeException();
                    }
                }
            }
            if (!dirFile.delete()) {
                throw new RuntimeException();
            }
        }

        private static void renameFile(String src, String dest) {
            if (!new File(src).renameTo(new File(dest))) {
                throw new RuntimeException();
            }
        }

        private static void writeBucket(String fileName, Consumer<PrintWriter> consumer) {
            printWriteToFileGZ(new File(fileName), consumer);
        }

        private static void readBucket(String fileName, Consumer<String> consumer) {
            readLinesFromFileGZ(new File(fileName), consumer);
        }
        
        private static void printWriteToFileGZ(File file, Consumer<PrintWriter> consumer) {
            file.getAbsoluteFile().getParentFile().mkdirs();
            File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
            try (PrintWriter pw = new PrintWriter(new MiGzOutputStream(
                    new BufferedOutputStream(new FileOutputStream(tmpFile), IO_BUFFER_SIZE)) {{ 
                        setCompressionLevel(COMPRESSION_LEVEL); }})) {
                consumer.accept(pw);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
            if (file.exists()) {
                if (!file.delete()) {
                    throw new RuntimeException();
                }
            }
            if (!tmpFile.renameTo(file)) {
                throw new RuntimeException();
            }
        }

        private static void readLinesFromFileGZ(File file, Consumer<String> callback) {
            if (!file.exists()) return;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                    new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE))))) {
                String line = null;
                while ((line = reader.readLine()) != null) {
                    callback.accept(line);
                }
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        private int estimateNumberOfNextLevelBuckets(String levelDir) {
            File[] bucketFiles = new File(levelDir).listFiles();
            long totalSize = 0;
            for (File file : bucketFiles) {
                totalSize += file.length();
            }
            double numBuckets = (double) totalSize * BFS_DISK_EXPECTED_GROWTH_FACTOR / BFS_DISK_MAX_EXPECTED_BUCKET_SIZE;
            int result = (int) Math.pow(2, Math.ceil(Math.log(numBuckets) / Math.log(2)));
            return Math.max(result, 8); // at least 8 buckets, to enable parallelism
        }

        private static void writeBuckets(String dir, int numBuckets, Consumer<BiConsumer<String, Integer>> consumer) {
            if ((numBuckets & (numBuckets-1)) != 0) {
                throw new RuntimeException("numBuckets must be a power of two");
            }
            Object[] locks = new Object[numBuckets];
            for (int bucket = 0; bucket < numBuckets; bucket++) {
                locks[bucket] = new Object();
            }
            PrintWriter[] writers = new PrintWriter[numBuckets];
            new File(dir).mkdirs();
            consumer.accept((prefix, hash) -> {
                int bucket = (hash ^ (hash >> 16)) & (numBuckets-1);
                synchronized (locks[bucket]) {
                    if (writers[bucket] == null) {
                        String bucketFile = String.format("%s/%03d.gz", dir, bucket);
                        try {
                            writers[bucket] = new PrintWriter(new GZIPOutputStream(new BufferedOutputStream(
                                    new FileOutputStream(bucketFile))) {{ def.setLevel(COMPRESSION_LEVEL); }});
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    writers[bucket].println(prefix);
                }
            });
            for (PrintWriter writer : writers) {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            }            
        }

        private static void readSortedBuckets(String dir, Consumer<String> consumer) {
            try {
                File[] bucketFiles = new File(dir).listFiles();
                int numBuckets = bucketFiles.length;
                BufferedReader[] readers = new BufferedReader[numBuckets];
                String[] values = new String[numBuckets];
                PriorityQueue<Integer> queue = new PriorityQueue<Integer>((i, j) -> {
                    String value1 = values[i];
                    String value2 = values[j];
                    if (value1 == null) return 1;
                    if (value2 == null) return -1;
                    return value1.compareTo(value2);
                });
                for (int bucket = 0; bucket < numBuckets; bucket++) {
                    File bucketFile = bucketFiles[bucket];
                    @SuppressWarnings("resource")
                    BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                            new FileInputStream(bucketFile))));
                    readers[bucket] = reader;
                    values[bucket] = reader.readLine();
                    queue.add(bucket);
                }
                while (true) {
                    Integer bucket = queue.poll();
                    String prefix = values[bucket];
                    if (prefix == null) break;
                    consumer.accept(prefix);
                    if (readers[bucket] != null) {
                        values[bucket] = readers[bucket].readLine();
                    }
                    queue.add(bucket);
                }
                for (BufferedReader reader : readers) {
                    if (reader != null) {
                        reader.close();
                    }
                }            
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
    }
    
    public static class DFSLoopSolver extends Solver {

        private static class LevelState {
            Bundle[] bundles;
            int moves;
            int m;
            Set<Key> seen = new MemoryEfficientHashSet<>();
        }
        
        private final KeyBuilder keyBuilder = new KeyBuilder();
        private final MoveBuilder moveBuilder = new MoveBuilder();
        private final PartialsLookup partialsLookup = new PartialsLookup();
        private final Bundle[] sortedBundles = new Bundle[256];

        @Override
        public String solve(Bundle[] bundles0, int bestAnsLen) {
            LevelState[] states = new LevelState[bestAnsLen+1];
            for (int i = 0; i <= bestAnsLen; i++) {
                states[i] = new LevelState();
            }
            
            int level = 0;
            
            long movesDone = 0;
            long newMovesDone = 0;
            
            long begin = System.currentTimeMillis();
            long prev = begin;
                    
            int moves = heads(bundles0);
            
            states[0].bundles = bundles0;
            states[0].moves = moves;
            states[0].m = -1;
            
nextLevel:            
            while (level >= 0) {
                LevelState s = states[level];

                // pick next move
                while (true) {
                    s.m++;
                    if (s.m >= 9) {
                        level--;
                        continue nextLevel;
                    }
                    if (((s.moves >> s.m) & 1) != 0) {
                        break;
                    }
                }

                // make move
                Bundle[] nextBundles = moveBuilder.makeMove(s.bundles, s.m);

                movesDone++;
                newMovesDone++;
                
                // maybe cleanup some cache, to avoid OOM; print statistics
                if (newMovesDone >= 1000000) {
                    long[] numIntsCached = new long[bestAnsLen+1];
                    long totalCached = 0;
                    for (int i = 0; i < states.length; i++) {
                        long num = 0;
                        for (Key k : states[i].seen) {
                            num += k.values.length;
                        }
                        numIntsCached[i] = num;
                        totalCached += num;
                    }
                    long now = System.currentTimeMillis();
                    StringBuilder path = new StringBuilder();
                    for (int i = 0; i <= level; i++) {
                        path.append(states[i].m + 1);
                    }
                    System.out.println(formatLog(bundlesStr(bundles0) + " " + bestAnsLen + " " + level+", "+movesDone
                            + " moves, " + totalCached + " cache, " + path.toString()
                            + " [" + (now-begin) + " ms, +" + (now-prev) + " ms]"));
                    prev = now;
                    for (int i = bestAnsLen; i >= 0; i--) {
                        if (totalCached < DFS_MAX_CACHE) break;
                        states[i].seen.clear();
                        totalCached -= numIntsCached[i];
                    }
                    newMovesDone = 0;
                } 
                
                // check result
                if (nextBundles.length == 0) {
                    StringBuilder path = new StringBuilder();
                    for (int i = 0; i <= level; i++) {
                        path.append(states[i].m + 1);
                    }
                    return path.toString();
                }

                int len = nextBundles.length;
                int nextMoves = prepareBundles(nextBundles, sortedBundles);
                
                // check duplicates
                Key nextKey = keyBuilder.makeKey(sortedBundles, len);
                Set<Key> seen = states[level+1].seen;
                if (seen.contains(nextKey)) {
                    continue;
                }
                seen.add(nextKey);

                // check canHaveAnswer
                nextMoves = partialsLookup.canHaveAnswer(sortedBundles, len, nextMoves, bestAnsLen - (level+1));
                if (nextMoves == 0) {
                    continue;
                }
                
                // try next level
                states[level+1].bundles = nextBundles;
                states[level+1].moves = nextMoves;
                states[level+1].m = -1;
                
                level++;
            }
            
            return null;
        }

    }
    
    public static class DFSSolver extends Solver {

        private final KeyBuilder keyBuilder = new KeyBuilder();
        private final MoveBuilder moveBuilder = new MoveBuilder();
        private final PartialsLookup partialsLookup = new PartialsLookup();
        private final Bundle[] sortedBundles = new Bundle[256];

        @Override
        public String solve(Bundle[] bundles0, int bestAnsLen) {
            ArrayList<Set<Key>> seenList = new ArrayList<>();
            while (seenList.size() <= bestAnsLen) {
                seenList.add(new MemoryEfficientHashSet<>());
            }
            return solveDFS(bundles0, "", -1, bestAnsLen, seenList);
        }
        
        public String solveDFS(Bundle[] bundles, String path, int allowMoves, int bestAnsLen,
                ArrayList<Set<Key>> seenList) {
            int moves = heads(bundles);
            
            moves &= allowMoves;
            
            for (int m = 0; m < 9; m++) {
                if (((moves >> m) & 1) == 0) continue;
                
                Bundle[] nextBundles = moveBuilder.makeMove(bundles, m);
                String nextPath = path + (1+m);
                
                if (nextBundles.length == 0) {
                    return nextPath;
                }
                
                int len = nextBundles.length;
                int nextMoves = prepareBundles(nextBundles, sortedBundles);
                
                Key nextKey = keyBuilder.makeKey(sortedBundles, len);
                Set<Key> seen = seenList.get(nextPath.length());
                if (seen.contains(nextKey)) {
                    continue;
                }
                seen.add(nextKey);
                
                if (DEBUG) System.out.println(nextPath + ": " + bundlesStr(Arrays.copyOf(sortedBundles, len)));
                
                nextMoves = partialsLookup.canHaveAnswer(sortedBundles, len, nextMoves, bestAnsLen - nextPath.length());
                if (nextMoves == 0) {
                    if (DEBUG) System.out.println("Reject: " + nextPath);
                    continue;
                }
                if (DEBUG) System.out.println("Accept: " + nextPath);
                
                String ans = solveDFS(nextBundles, nextPath, nextMoves, bestAnsLen, seenList);
                if (ans != null) {
                    return ans;
                }
            }
            return null;
        }
        
    }   
    
    public static class DFSBatchSolver extends Solver {

        public static class State {
            String prefix;
            Bundle[] bundles;
            Bundle[] sortedBundles;
            Key key;
            int moves;
            
            State(String prefix, Bundle[] bundles, int moves) {
                this.prefix = prefix;
                this.bundles = bundles;
                this.sortedBundles = new Bundle[bundles.length];
                this.moves = moves;
            }
        }

        @SuppressWarnings("serial")
        public static class SaveState implements Serializable {
            int bestAnsLen;
            ArrayList<Set<Key>> doneList;
            
            SaveState(int bestAnsLen, ArrayList<Set<Key>> doneList) {
                this.bestAnsLen = bestAnsLen;
                this.doneList = doneList;
            }
        }
        
        public static class RunState {
            
            private static final String SAVE_FILE = "dfs-batch-done-%s.bin.gz";
            private static final String SAVE_TRIGGER_FILE = "save";
            
            Bundle[] bundles0;
            int bestAnsLen;
            long begin;
            long prev;
            long movesDone;
            long movesSinceCleanup;
            long prevSave;
            File saveFile;
            ArrayList<Set<Key>> doneList = new ArrayList<>();
            ArrayList<AtomicLong> doneListNumInts = new ArrayList<>();

            RunState(Bundle[] bundles0, int bestAnsLen) {
                this.bundles0 = bundles0;
                String problem = bundlesStr(bundles0);
                String problemStr = problem.replace('/', '@').replace(' ', '_');
                this.saveFile = new File(String.format(SAVE_FILE, problemStr));
                while (this.doneList.size() <= bestAnsLen) {
                    this.doneList.add(new MemoryEfficientHashSet<>());
                    this.doneListNumInts.add(new AtomicLong());
                }
                this.bestAnsLen = bestAnsLen;
                this.begin = System.currentTimeMillis();
                this.prev = this.begin;
                this.prevSave = System.currentTimeMillis();
            }

            public static RunState loadState(Bundle[] bundles0) {
                RunState runState = new RunState(bundles0, 1);
                if (!runState.saveFile.exists()) {
                    return null;
                }
                System.out.println("Loading state...");
                SaveState saveState = deserializeObjectFromFileGZ(runState.saveFile);
                runState.doneList = saveState.doneList;
                runState.bestAnsLen = saveState.bestAnsLen;
                runState.recalcNumInts();
                return runState;
            }

            public void onMove(int level, String prefix) {
                movesDone++;
                movesSinceCleanup++;
                
                if (movesSinceCleanup >= 1000000) {
                    File triggerFile = new File(SAVE_TRIGGER_FILE);
                    long now = System.currentTimeMillis();
                    if (now > prevSave + TimeUnit.MINUTES.toMillis(DFS_BATCH_SAVE_MINUTES) || triggerFile.exists()) {
                        System.out.println("Saving state...");
                        long maxSaveInts = readMaxSaveInts(triggerFile, DFS_MAX_SAVE_CACHE);
                        SaveState saveSate = selectSaveState(maxSaveInts);
                        serializeObjectToFileGZ(saveFile, saveSate);
                        triggerFile.delete();
                        prevSave = now;
                    }
                    
                    long totalCached = cleanupCache(DFS_MAX_CACHE);
                    printLevel(level, prefix, totalCached);
                    movesSinceCleanup = 0;
                }
            }

            private long readMaxSaveInts(File triggerFile, long defaultValue) {
                if (triggerFile.exists()) {
                    try {
                        String value = new String(Files.readAllBytes(triggerFile.toPath())).trim();
                        if (!value.isEmpty()) {
                            return Long.parseLong(value);
                        }
                    } catch (Exception e) {
                        System.out.println(e.getClass() + ": " + e.getMessage());
                    }
                }
                return defaultValue;
            }

            private SaveState selectSaveState(long maxCached) {
                long totalCached = 0;
                long[] numIntsCached = new long[bestAnsLen+1];
                for (int i = 0; i < doneList.size(); i++) {
                    long num = doneListNumInts.get(i).get();
                    numIntsCached[i] = num;
                    totalCached += num;
                }
                ArrayList<Set<Key>> saveList = new ArrayList<Set<Key>>(doneList.size());
                while (saveList.size() < doneList.size()) {
                    saveList.add(new MemoryEfficientHashSet<>());
                }
                for (int i = bestAnsLen; i >= 0; i--) {
                    if (totalCached > maxCached) {
                        totalCached -= numIntsCached[i];
                    } else {
                        saveList.set(i, doneList.get(i));
                    }
                }
                return new SaveState(bestAnsLen, saveList);
            }

            private long cleanupCache(long maxCached) {
                long totalCached = 0;
                long[] numIntsCached = new long[bestAnsLen+1];
                for (int i = 0; i < doneList.size(); i++) {
                    long num = doneListNumInts.get(i).get();
                    numIntsCached[i] = num;
                    totalCached += num;
                }
                for (int i = bestAnsLen; i >= 0; i--) {
                    if (totalCached < maxCached) break;
                    doneList.get(i).clear();
                    doneListNumInts.get(i).set(0);
                    totalCached -= numIntsCached[i];
                }
                return totalCached;
            }
            
            private void recalcNumInts() {
                for (int i = 0; i < doneList.size(); i++) {
                    Set<Key> done = doneList.get(i);
                    AtomicLong doneNumInts = doneListNumInts.get(i);
                    doneNumInts.set(done.stream().mapToLong(k -> k.values.length).sum());
                }
            }

            private void printLevel(int level, String prefix, long totalCached) {
                List<Integer> doneSizes = doneList.stream().map(Set::size).collect(Collectors.toList());
                long now = System.currentTimeMillis();
                System.out.println(formatLog(bundlesStr(bundles0) + " " + bestAnsLen + " " + level
                        +", "+movesDone+ " moves, " + totalCached + " cache, " + prefix
                        + " [" + (now-begin) + " ms, +" + (now-prev) + " ms]"));
                System.err.println("  Cached: " + doneSizes);
                prev = now;
            }
        }
        
        private final ThreadAllocator threadAllocator;
        private final MoveBuilder moveBuilder = new MoveBuilder();

        public DFSBatchSolver(ThreadAllocator threadAllocator) {
            this.threadAllocator = threadAllocator;
        }

        public void solve(Problem problem) {
            long begin = System.currentTimeMillis();
            Bundle[] bundles0 = problem.bundles;
            int moves0 = heads(bundles0);
            String answer = null;
            int bestAnsLen = 1;
            RunState runState = RunState.loadState(bundles0);
            if (runState != null) {
                bestAnsLen = runState.bestAnsLen;
            }
            for (; bestAnsLen <= 100; bestAnsLen++) {
                State state0 = new State("", bundles0, moves0);
                ArrayList<State> states = new ArrayList<>();
                states.add(state0);
                if (runState == null) {
                    runState = new RunState(bundles0, bestAnsLen);
                }
                answer = solveDFS_Batch_(states, runState);
                if (answer != null) {
                    break;
                }
                runState = null;
            }
            long end = System.currentTimeMillis();
            if (answer == null) {
                throw new IllegalStateException();
            }
            problem.answer = answer;
            problem.solveDuration = end - begin;
        }

        // to be used only for experiments, does not support save/restore
        @Override
        public String solve(Bundle[] bundles0, int bestAnsLen) {
            int moves0 = heads(bundles0);
            State state0 = new State("", bundles0, moves0);
            ArrayList<State> states = new ArrayList<>();
            states.add(state0);
            RunState runState = new RunState(bundles0, bestAnsLen);
            return solveDFS_Batch_(states, runState);
        }
        
        public String solveDFS_Batch_(ArrayList<State> states, RunState runState) {
            int level = states.get(0).prefix.length();
            int bestAnsLen = runState.bestAnsLen;
            
            int numThreads = threadAllocator.get();
            numThreads += numThreads/2; // workaround for suboptimal parallelization
            
            processInParallel(states, numThreads, () -> {
                KeyBuilder keyBuilder = new KeyBuilder();
                Set<Key> seen = new MemoryEfficientHashSet<>();
                return (state) -> {
                    Bundle[] bundles = state.bundles;
                    Bundle[] sortedBundles = state.sortedBundles;
                    
                    int moves = prepareBundles(bundles, sortedBundles);

                    state.key = keyBuilder.makeKey(sortedBundles, bundles.length);
                    state.moves = moves;

                    // pre-filter locally
                    if (!seen.add(state.key)) {
                        state.prefix = null;
                        state.bundles = null;
                        state.sortedBundles = null;
                        state.moves = 0;
                        state.key = null;
                    }
                };
            });

            Set<Key> seen = new MemoryEfficientHashSet<>();
            Set<Key> done = runState.doneList.get(level);
            AtomicLong doneNumInts = runState.doneListNumInts.get(level);
            
            for (State state : states) {
                if (state.key == null) continue;
                if (!seen.add(state.key) || done.contains(state.key)) {
                    state.prefix = null;
                    state.bundles = null;
                    state.sortedBundles = null;
                    state.moves = 0;
                }
                state.key = null;
            }

            processInParallel(states, numThreads, () -> {
                PartialsLookup partialsLookup = new PartialsLookup();
                return (state) -> {                
                    String prefix = state.prefix;
                    Bundle[] bundles = state.bundles;
                    Bundle[] sortedBundles = state.sortedBundles;
                    int moves = state.moves;

                    if (bundles == null) return;

                    moves = partialsLookup.canHaveAnswer(sortedBundles, bundles.length, moves, bestAnsLen - prefix.length());

                    state.moves = moves;

                    state.sortedBundles = null;
                };
            });

            ArrayList<State> nextStates = new ArrayList<>();
            
            for (State state : states) {
                String prefix = state.prefix;
                Bundle[] bundles = state.bundles;
                int moves = state.moves;

                if (bundles == null) continue;
                
                for (int m = 0; m < 9; m++) {
                    if (((moves >> m) & 1) == 0) continue;
                    
                    Bundle[] nextBundles = moveBuilder.makeMove(bundles, m);
                    String nextPrefix = prefix + (1+m);

                    runState.onMove(level, prefix);
                    
                    if (nextBundles.length == 0) {
                        return nextPrefix;
                    }
                    
                    State nextState = new State(nextPrefix, nextBundles, -1);
                    nextStates.add(nextState);
                }

                int batchSize = done.size() / 10;
                batchSize = Math.max(DFS_BATCH_SIZE, Math.min(batchSize, DFS_BATCH_SIZE * 100));
                
                if (nextStates.size() > batchSize) {
                    String ans = solveDFS_Batch_(nextStates, runState);
                    if (ans != null) {
                        return ans;
                    }
                    nextStates.clear();
                }
            }

            if (nextStates.size() > 0) {
                String ans = solveDFS_Batch_(nextStates, runState);
                if (ans != null) {
                    return ans;
                }
            }

            done.addAll(seen);
            doneNumInts.addAndGet(seen.stream().mapToLong(k -> k.values.length).sum());
            
            return null;
        }

    }
    
    public static class DFSDiskSolver extends Solver {

        public static class State {
            String prefix;
            Key key;
            List<State> nextStates;

            State(String prefix, Key key) {
                this.prefix = prefix;
                this.key = key;
            }
        }

        private static final File problemDir(Bundle[] bundles) {
            return new File(bundlesStr(bundles).replace(' ', '_').replace('/', '@'));
        }
        
        private static final File bestAnsLenDir(Bundle[] bundles, int bestAnsLen) {
            return new File(problemDir(bundles), String.format("%02d", bestAnsLen));
        }
        
        private static final File prefixesFile(File bestAnsLenDir, String minPrefix) {
            return new File(bestAnsLenDir, String.format("%s-prefixes.txt.gz", minPrefix));
        }
        
        @Override
        public void solve(Problem problem) {
            long begin = System.currentTimeMillis();
            String answer = null;
            for (int bestAnsLen = findBestAnsLenDir(problem); bestAnsLen <= 99; bestAnsLen++) {
                answer = solve(problem.bundles, bestAnsLen);
                if (answer != null) {
                    break;
                }
            }
            long end = System.currentTimeMillis();
            if (answer == null) {
                throw new IllegalStateException();
            }
            problem.answer = answer;
            problem.solveDuration = end - begin;
        }

        private int findBestAnsLenDir(Problem problem) {
            for (int len = 1; len <= 99; len++) {
                File dir = bestAnsLenDir(problem.bundles, len);
                if (dir.exists()) return len;
            }
            return 1;
        }
    
        @Override
        public String solve(Bundle[] bundles0, int bestAnsLen) {
            long begin = System.currentTimeMillis();
            
            File bestAnsLenDir = bestAnsLenDir(bundles0, bestAnsLen);

            Comparator<String> dfsComparator = (a, b) -> {
                int minLen = Math.min(a.length(), b.length());
                String s1 = a.substring(0, minLen);
                String s2 = b.substring(0, minLen);
                int cmp = s1.compareTo(s2);
                if (cmp == 0) {
                    cmp = Integer.compare(a.length(), b.length());
                }
                return cmp;
            };
            
            if (minPrefixesFile(bestAnsLenDir, dfsComparator) == null) {
                File prefixesFile = prefixesFile(bestAnsLenDir, "");
                printWriteToFileGZ(prefixesFile, pw -> {
                    pw.println();
                });
            }
            
            String answer = null;
            File prefixesFile = null;
            
            while (answer == null && (prefixesFile = minPrefixesFile(bestAnsLenDir, dfsComparator)) != null) {
                answer = processPrefixes(bundles0, bestAnsLen, prefixesFile);
            }
            
            if (answer == null) {
                if (!bestAnsLenDir.delete()) throw new RuntimeException();
            }
            
            long end = System.currentTimeMillis();
            
            System.out.println(formatLog(bundlesStr(bundles0) + " " + bestAnsLen + ": level = " + timeStr(end-begin)));
            
            return answer;
        }
        
        private File minPrefixesFile(File dir, Comparator<String> prefixComparator) {
            String[] names = dir.list();
            if (names == null) {
                return null;
            }

            return Arrays.stream(names)
                .filter(name -> name.endsWith("-prefixes.txt.gz"))
                .min(Comparator.comparing(DFSDiskSolver::prefixesFilePrefix, prefixComparator))
                .map(name -> new File(dir, name))
                .orElse(null);
        }

        private static String prefixesFilePrefix(String fileName) {
            return fileName.substring(0, fileName.indexOf('-'));
        }
        
        private static void trackTime(String timerName, Map<String, Long> timerResults, Runnable callback) {
            long begin = System.currentTimeMillis();
            callback.run();
            long end = System.currentTimeMillis();
            long duration = end - begin;
            long totalTime = timerResults.getOrDefault(timerName, 0L) + duration;
            timerResults.put(timerName, totalTime);
        }
        
        private String processPrefixes(Bundle[] bundles0, int bestAnsLen, File prefixesFile) {
            AtomicReference<String> answer = new AtomicReference<>();
            
            Map<String, Long> timingResults = new LinkedHashMap<>();
            List<String> nextBlocksSummary = new ArrayList<>();

            File bestAnsLenDir = bestAnsLenDir(bundles0, bestAnsLen);
            
            String minPrefix = prefixesFilePrefix(prefixesFile.getName());
            int level = minPrefix.length();
            
            Consumer<String> logger = (str) -> {
                System.out.println(formatLog(bundlesStr(bundles0) + " " + bestAnsLen + " " + level + " " + minPrefix + ": " + str));
            };
            
            long beginBlock = System.currentTimeMillis();

            AtomicLong numInStates = new AtomicLong();
            AtomicLong numOutStates = new AtomicLong();
            AtomicLong numOutPrefixes = new AtomicLong();
            
            Set<Key> seenStates = new MemoryEfficientHashSet<>();
            
            writePrefixesInBlocks(DFS_DISK_BLOCK_SIZE, bestAnsLenDir, nextBlocksSummary, (prefixWriter) -> {
                readStatesInBatches(DFS_DISK_BATCH_SIZE, prefixesFile, (states) -> {
                    if (answer.get() != null) return;
                    
                    logger.accept("process");
                    
                    trackTime("processStates", timingResults, () -> {
                        processStates(states, bundles0, bestAnsLen, numInStates, numOutStates, answer, logger);
                    });
                    
                    if (answer.get() != null) return;
                    
                    logger.accept("unique");
                    
                    trackTime("uniqueNextStates", timingResults, () -> {
                        collectUniqueNextStates(states, seenStates, numOutPrefixes, prefixWriter, logger);
                    });
                });
            });
            
            
            String ans = answer.get();
            if (ans != null) {
                return ans;
            }
            
            logger.accept("states = " + numOutStates.get());
            logger.accept("next_prefixes = " + numOutPrefixes.get());

            for (String message : nextBlocksSummary) {
                logger.accept(message);
            }
            
            if (!prefixesFile.delete()) {
                throw new RuntimeException();
            }
            
            long endBlock = System.currentTimeMillis();
            
            logger.accept("block = " + timeStr(endBlock - beginBlock));
            logger.accept(timingResults.entrySet().stream()
                    .map(entry -> entry.getKey() + " = " + timeStr(entry.getValue()))
                    .collect(Collectors.joining(", ")));

            return null;
        }

        private void readStatesInBatches(int batchSize, File prefixesFile, Consumer<ArrayList<State>> callback) {
            ArrayList<State> batch = new ArrayList<>();
            
            readLinesFromFile(prefixesFile, prefix -> {
                batch.add(new State(prefix, null));
                if (batch.size() == batchSize) {
                    callback.accept(batch);
                    batch.clear();
                }
            });
            
            if (!batch.isEmpty()) {
                callback.accept(batch);
            }
        }

        private void processStates(ArrayList<State> states, Bundle[] bundles0, int bestAnsLen, AtomicLong numInStates,
                AtomicLong numOutStates, AtomicReference<String> answer, Consumer<String> logger) {
            BlockingQueue<String> resultQueue = new LinkedBlockingQueue<String>();

            processInParallel(states, NUM_WORKER_THREADS, () -> {
                final Bundle[] sortedBundles = new Bundle[256];
                final KeyBuilder keyBuilder = new KeyBuilder();
                final MoveBuilder moveBuilder = new MoveBuilder();
                final PartialsLookup partialsLookup = new PartialsLookup();
                return (state) -> {
                    long numIn = numInStates.incrementAndGet();
                    
                    String prefix = state.prefix;
                    
                    Bundle[] bundles = moveBuilder.makeMoves(bundles0, prefix);
                    
                    int moves = prepareBundles(bundles, sortedBundles);
                    
                    moves = partialsLookup.canHaveAnswer(sortedBundles, bundles.length, moves, bestAnsLen - prefix.length());
                    
                    if (moves == 0) {
                        state.nextStates = Collections.emptyList();
                        return;
                    }
                        
                    List<State> nextStates = new ArrayList<>();
                    
                    for (int m = 0; m < 9; m++) {
                        if (((moves >> m) & 1) == 0) continue;
                        
                        Bundle[] nextBundles = moveBuilder.makeMove(bundles, m);
                        String nextPrefix = prefix + (1+m);
                        
                        if (nextBundles.length == 0) {
                            resultQueue.add(nextPrefix);
                            continue;
                        }
                        
                        Key nextKey = keyBuilder.makeKey(nextBundles);
                        
                        nextStates.add(new State(nextPrefix, nextKey));
                        
                        long numOut = numOutStates.incrementAndGet();
                        if (numOut % 1000000 == 0) {
                            logger.accept(numIn + " -> " + numOut + " " + bundlesStr(sortSzDesc(nextBundles)));
                        }
                    }
                    
                     state.nextStates = nextStates;
                };
            });
            
            if (!resultQueue.isEmpty()) {
                List<String> results = new ArrayList<>(resultQueue);
                Collections.sort(results);
                answer.set(results.get(0));
            }
        }

        private void collectUniqueNextStates(ArrayList<State> states, Set<Key> seenStates, 
                AtomicLong numOutPrefixes, Consumer<String> prefixWriter, Consumer<String> logger) {
            for (State state : states) {
                for (State nextState : state.nextStates) {
                    String prefix = nextState.prefix;
                    Key key = nextState.key;

                    if (seenStates.add(key)) {
                        prefixWriter.accept(prefix);

                        numOutPrefixes.incrementAndGet();

                        if (seenStates.size() == DFS_DISK_SEEN_SIZE) {
                            trimSeenStates(seenStates, logger);
                        }
                    }
                }
            }
        }

        private void trimSeenStates(Set<Key> seenStates, Consumer<String> logger) {
            logger.accept("trim");
            
            Iterator<Key> iterator = seenStates.iterator();
            int count = 0;
            while (iterator.hasNext()) {
                iterator.next();
                count++;
                if (count == 3) {
                    iterator.remove();
                    count = 0;
                }
            }
        }

        private void writePrefixesInBlocks(int blockSize, File bestAnsLenDir,
                List<String> nextBlocksSummary, Consumer<Consumer<String>> writerCallback) {

            class PrefixesWriter implements Consumer<String> {

                private static final int MIGZ_BUFFER_SIZE = 65536;
                
                private final List<File> blockFiles = new ArrayList<>();
                private final List<File> tmpFiles = new ArrayList<>();
                
                private PrintWriter blockWriter;
                private String minPrefix;
                private String maxPrefix;
                private int numPrefixesWritten;
                
                @Override
                public void accept(String prefix) {
                    try {
                        if (blockWriter == null) {
                            blockWriter = createBlockWriter(prefix);
                        }
                        
                        blockWriter.println(prefix);
                        numPrefixesWritten++;
                        
                        if (minPrefix == null) {
                            minPrefix = prefix;
                        }
                        maxPrefix = prefix;
                        
                        if (numPrefixesWritten == blockSize) {
                            closeBlockWriter();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                public PrintWriter createBlockWriter(String minPrefix) throws IOException {
                    bestAnsLenDir.getAbsoluteFile().mkdirs();
                    
                    File prefixesFile = prefixesFile(bestAnsLenDir, minPrefix);
                    File tmpFile = new File(bestAnsLenDir, prefixesFile.getName() + ".tmp");
                    tmpFiles.add(tmpFile);
                    blockFiles.add(prefixesFile);
                    
                    BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile), IO_BUFFER_SIZE);
                    MiGzOutputStream gzOut = new MiGzOutputStream(out, NUM_WORKER_THREADS, MIGZ_BUFFER_SIZE);
                    return new PrintWriter(gzOut);
                }

                public void closeBlockWriter() {
                    if (blockWriter != null) {
                        blockWriter.close();
                        blockWriter = null;
                        
                        nextBlocksSummary.add("save next prefixes " + numPrefixesWritten + " (min: " + minPrefix
                                + ", max: " + maxPrefix + ")");

                        numPrefixesWritten = 0;
                        minPrefix = null;
                        maxPrefix = null;
                    }
                }

                public void commitTmpFiles() {
                    for (int index = 0; index < blockFiles.size(); index++) {
                        File blockFile = blockFiles.get(index);
                        File tmpFile = tmpFiles.get(index);
                        
                        if (blockFile.exists() && !blockFile.delete()) {
                            throw new RuntimeException();
                        }
                        if (!tmpFile.renameTo(blockFile)) {
                            throw new RuntimeException();
                        }
                    }
                }
                
            }
            
            PrefixesWriter writer = new PrefixesWriter();
            
            writerCallback.accept(writer);
            
            writer.closeBlockWriter();

            writer.commitTmpFiles();
        }

    }
    
    private static final ThreadLocal<int[]> prepareBundlesSortBuffer = ThreadLocal.withInitial(() -> new int[1024]);

    public static int prepareBundles(Bundle[] bundles, Bundle[] sortedBundles) {
        int len = bundles.length;
        int[] buffer = prepareBundlesSortBuffer.get();

        for (int i = 0; i < len; i++) {
            buffer[i] = -bundles[i].toSortable(); // sort in descending order
        }

        Arrays.sort(buffer, 0, len);

        for (int i = 0; i < len; i++) {
            sortedBundles[i] = Bundle.unpack((-buffer[i]) & MASK_18);
        }
        return heads(bundles);
    }

    public static Bundle[] sortSzDesc(Bundle[] bundles) {
        return Arrays.stream(bundles)
                .map(Bundle::toSortable)
                .sorted(Collections.reverseOrder())
                .map(bb -> Bundle.unpack(bb & MASK_18))
                .toArray(Bundle[]::new);
    }
        
    public static class MoveBuilder {
        
        private final Bundle[] newBundles = new Bundle[1024];
        
        // cacheMoveResult[index] is the result of moves cacheMove[0..index] applied to cacheBundles0
        private Bundle[] cacheBundles0;
        private final char[] cacheMove = new char[100];
        private final Bundle[][] cacheMoveResult = new Bundle[100][];
    
        public Bundle[] makeMoves(Bundle[] bundles, String moves) {
            int index = 0;
            Bundle[] bundles0 = bundles;
            
            if (cacheBundles0 != null && Arrays.equals(cacheBundles0, bundles0)) {
                while (index < moves.length() && cacheMove[index] == moves.charAt(index)) {
                    bundles = cacheMoveResult[index];
                    index++;
                }
            } else {
                cacheBundles0 = bundles0;
            }
            
            while (index < moves.length()) {
                int move = moves.charAt(index)-'1';
                bundles = makeMove(bundles, move);
                
                cacheMove[index] = moves.charAt(index);
                cacheMoveResult[index] = bundles;
                
                index++;
            }
            
            cacheMove[index] = 0;
            cacheMoveResult[index] = null;
            
            return bundles;
        }
        
        public Bundle[] makeMovesNoCache(Bundle[] bundles, String moves) {
            for (int index = 0; index < moves.length(); index++) {
                int move = moves.charAt(index)-'1';
                bundles = makeMove(bundles, move);
            }
            return bundles;
        }
        
        public Bundle[] makeMove(Bundle[] bundles, int move) {
            int r = 0, len = bundles.length, moveMask = 1 << move;
            for (int i = 0; i < len; i++) {
                Bundle bundle = bundles[i];
                int remainingHeads = bundle.heads() & ~moveMask;
                if (remainingHeads != 0) {
                    newBundles[r++] = Bundle.unpack((remainingHeads << 9) | bundle.digits());
                }
            }
            next:
            for (int i = 0; i < len; i++) {
                Bundle bundle = bundles[i];
                int heads = bundle.heads();
                int digits = bundle.digits();
                if ((heads & moveMask) != 0) {
                    digits = digits & ~moveMask;
                    heads = digits;
                    if (heads == 0) continue;
    
                    // try to add new bundle: 1 x 12/1234 -> 2/1234 [234/234]<- this
                    // the new bundle will have to be de-duplicated: 
                    // the de-duplicated bundle may either be contained in a tail: 2/1234 34/34 -> 2/1234
                    // or it may be part of both heads and digits: 2/1234 234/234 -> 2/1234 34/234
                    for (int j = 0; j < r; j++) {
                        Bundle b = newBundles[j];
                        int h = b.heads();
                        int d = b.digits();
                        if ((digits & ~d) != 0) continue; // de-duplicated bundle must not have other digits
        
                        // case 1: all de-duplicated digits are contained in a tail
                        if ((h & ~digits) != 0) continue next; // 2/1234 34/34 -> 2/1234
        
                        // case 2: shared heads
                        heads = heads & ~h; // 2/1234 234/234 -> 2/1234 34/234
                        if (heads == 0) continue next;
                    }
    
                    newBundles[r++] = Bundle.unpack((heads << 9) | digits);
                }
            }
            return Arrays.copyOf(newBundles, r);
        }
    
    }

    
    public static class KeyBuilder {
            
        private final Bundle[] sortedBundles = new Bundle[256];
        private final long[] hash = new long[9];
        private final int[] groupSizes = new int[9];
        private final int[] key1 = new int[256];
        private final int[] key2 = new int[256];
        private final int[] swap = new int[9];
                
        public Key makeKey(Bundle[] bundles) {
            prepareBundles(bundles, sortedBundles);
            return makeKey(sortedBundles, bundles.length, null);
        }

        public Key makeKey(Bundle[] bundles, int[] swapResult) {
            prepareBundles(bundles, sortedBundles);
            return makeKey(sortedBundles, bundles.length, swapResult);
        }
        
        public int makeHash(Bundle[] bundles) {
            prepareBundles(bundles, sortedBundles);
            
            calcStructHash(sortedBundles, bundles.length, hash);
            
            long result = 0;
            for (long value : hash) {
                result ^= value;
            }
            return (int) (result ^ (result >> 32));
        }
        
        public Key makeKey(Bundle[] sortedBundles, int len) {
            return makeKey(sortedBundles, len, null);
        }
        
        private Key makeKey(Bundle[] sortedBundles, int len, int[] swapResult) {
            int[] digits = new int[9];
            for (int d = 0; d < 9; d++) {
                digits[d] = d;
            }
            
            int digitsMask = digits(sortedBundles, len);
            int numDigits = bitCount(digitsMask);

            calcStructHash(sortedBundles, len, hash);
            for (int d = 0; d < 9; d++) {
                hash[d] = (hash[d] >>> 1) | (((digitsMask >> d) & 1L) << 63);
            }
            
            calculateDigitGroups(digits, hash, groupSizes);
            
            return findMinKeyUsingPermutations(sortedBundles, len, digits, groupSizes, numDigits, swapResult);
        }
        
        // sort digits by their hash. calculate consecutive equivalence groups of sorted digits.
        // digits within each group have the same hash and produce equivalent bundles (having same shortest 
        // solution length), when applied as permutation of bundle digits.
        // missing digits are sorted to go at the end, not participating in groups and their permutations.
        private void calculateDigitGroups(int[] digits, long[] hash, int[] groupSizes) {
            //Arrays.sort(digits, Comparator.comparing(d -> hash[d]));
            for (int i = 1; i < 9; i++) {
                int t = digits[i];
                int j = i-1;
                while (j >= 0 && hash[digits[j]] > hash[t]) {
                    digits[j+1] = digits[j];
                    j--;
                }
                digits[j+1] = t;
            }

            int numGroups = 0;
            Arrays.fill(groupSizes, 0);
            for (int d = 0; d < 9; d++) {
                groupSizes[numGroups]++;
                if (d == 8 || (hash[digits[d]] >> 4) != (hash[digits[d+1]] >> 4)) {
                    numGroups++;
                }
            }
        }

        private Key findMinKeyUsingPermutations(Bundle[] sortedBundles, int len, int[] digits, int[] groupSizes, 
                int numDigits, int[] swapResult) {
            int[] minKey = key1;
            minKey[0] = Integer.MAX_VALUE;
            
            for (int[] permutation : Permutations.permutations(numDigits, groupSizes)) {
                for (int d = 0; d < 9; d++) {
                    int index = (d < numDigits) ? permutation[d] : d;
                    swap[digits[index]] = d;
                }
                
                int[] newKey = (minKey == key2) ? key1 : key2;
                
                for (int i = 0; i < len; i++) {
                    newKey[i] = sortedBundles[i].swapBundleDigits(swap).pack();
                }
                Arrays.sort(newKey, 0, len);

                if (compareKeys(newKey, minKey, len) < 0) {
                    minKey = newKey;
                    if (swapResult != null) {
                        for (int i = 0; i < 9; i++) {
                            swapResult[i] = swap[i];
                        }
                    }
                }
            }
    
            return new Key(Arrays.copyOf(minKey, len));
        }

        public int compareKeys(int[] key1, int[] key2, int len) {
            for (int i = 0; i < len; i++) {
                int a = key1[i], b = key2[i];
                if (a != b) {
                    return a < b ? -1 : 1;
                }
            }
            return 0;
        }
    
        private final int[] numHeads = new int[9];
        private final int[] numDigits = new int[9];
        private final int[] headsMask = new int[9];
        private final int[] digitsMask = new int[9];
    
        public void calcStructHash(Bundle[] sortedBundles, int len, long[] result) {
            Arrays.fill(result, 0L);
            for (int i = 0; i < len; i++) {
                Bundle bundle = sortedBundles[i];
                int heads = bundle.heads();
                int digits = bundle.digits();
    
                for (int d = 0; d < 9; d++) {
                    if (((digits >> d) & 1) != 0) {
                        numDigits[d]++;
                        digitsMask[d] |= digits;
    
                        if (((heads >> d) & 1) != 0) {
                            numHeads[d]++;
                            headsMask[d] |= heads;
                        }
                    }
                }
    
                if (i+1 == len || sortedBundles[i+1].shape() != sortedBundles[i].shape()) {
                    for (int d = 0; d < 9; d++) {
                        long value = result[d];
                        value = 31 * value + numHeads[d];
                        value = 31 * value + numDigits[d];
                        value = 31 * value + bitCount(headsMask[d]);
                        value = 31 * value + bitCount(digitsMask[d]);
                        result[d] = value;
                    }
                    Arrays.fill(numHeads, 0);
                    Arrays.fill(numDigits, 0);
                    Arrays.fill(headsMask, 0);
                    Arrays.fill(digitsMask, 0);
                }
            }
        }
    
    }

    private static class ParallelBatchPipeline<I, O> implements Consumer<I> {

        private static class Batch<I, O> {
            public final List<I> inputs;
            public final List<O> outputs;
            
            public Batch(int size) {
                inputs = new ArrayList<>(size);
                outputs = new ArrayList<>(size);
            }
        }
    
        private final int batchSize;
        private final BlockingQueue<Batch<I, O>> inputQueue;
        private final AtomicLong batchCounter = new AtomicLong();
        private final ExecutorService threadPool = THREAD_POOL;
        private final BlockingQueue<Batch<I, O>> outputQueue;

        private final AtomicReference<Runnable> inputWorker = new AtomicReference<>();
        private final AtomicInteger nextProcessorIndex = new AtomicInteger(1);
        
        private Batch<I, O> nextBatch;
        
        private volatile boolean inputDone;
        
        public ParallelBatchPipeline(int batchSize, int queueSize) {
            this.nextBatch = new Batch<>(batchSize);
            this.batchSize = batchSize;
            this.inputQueue = new ArrayBlockingQueue<>(queueSize);
            this.outputQueue = new ArrayBlockingQueue<>(queueSize);
        }
        
        @Override
        public synchronized void accept(I input) {
            if (inputDone) throw new IllegalStateException();
            nextBatch.inputs.add(input);
            if (nextBatch.inputs.size() == batchSize) {
                flushInput(nextBatch, inputQueue, batchCounter);
                nextBatch = new Batch<>(batchSize);
            }
        }
        
        public synchronized void inputDone() {
            flushInput(nextBatch, inputQueue, batchCounter);
            inputDone = true;
        }
        
        private void flushInput(Batch<I, O> batch, BlockingQueue<Batch<I, O>> inputQueue, AtomicLong batchCounter) {
            try {
                inputQueue.put(batch);
                batchCounter.incrementAndGet();
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(-1);
            }
        }

        public void addInputWorker(Consumer<Consumer<I>> callback) {
            Runnable worker = () -> {
                Thread.currentThread().setName("InputWorker");
                try {
                    callback.accept(ParallelBatchPipeline.this);
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(-1);
                } finally {
                    inputDone();
                }
            };
            if (!inputWorker.compareAndSet(null, worker)) {
                throw new IllegalStateException();
            }
            threadPool.submit(worker);
        }

        public void addBatchProcessors(int numProcessors, Supplier<Function<I, O>> processorFactory) {
            for (int i = 1; i <= numProcessors; i++) {
                int index = nextProcessorIndex.getAndIncrement();
                Runnable processorTask = createProcessor(index, processorFactory);
                this.threadPool.submit(processorTask);
            }
        }

        private Runnable createProcessor(int processorIndex, Supplier<Function<I, O>> processorFactory) {
            String processorName = "Processor-" + processorIndex;
            Function<I, O> processor = processorFactory.get();
            return () -> {
                Thread.currentThread().setName(processorName);
                try {
                    while (!(inputDone && inputQueue.isEmpty())) {
                        Batch<I, O> batch = inputQueue.poll();
                        if (batch != null) {
                            for (I input : batch.inputs) {
                                O output = processor.apply(input);
                                batch.outputs.add(output);
                            }
                            outputQueue.put(batch);
                        } else {
                            Thread.yield();
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(-1);
                }
            };
        }

        public void consumeOutput(BiConsumer<I, O> callback) {
            try {
                while (!(inputDone && batchCounter.get() == 0)) {
                    Batch<I, O> batch = outputQueue.poll();
                    if (batch != null) {
                        int size = batch.inputs.size();
                        for (int i = 0; i < size; i++) {
                            I input = batch.inputs.get(i);
                            O output = batch.outputs.get(i);
                            callback.accept(input, output);
                        }
                        batchCounter.decrementAndGet();
                    } else {
                        Thread.yield();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(-1);
            }
        }
        
    }
    
    private static class ParallelPipeline<I, O> implements Consumer<I> {

        private final BlockingQueue<I> inputQueue;
        private final AtomicLong batchCounter = new AtomicLong();
        private final ExecutorService threadPool = THREAD_POOL;
        private final BlockingQueue<O> outputQueue;

        private final AtomicReference<Runnable> inputWorker = new AtomicReference<>();
        private final AtomicInteger nextProcessorIndex = new AtomicInteger(1);
        
        private volatile boolean inputDone;
        
        public ParallelPipeline(int queueSize) {
            this.inputQueue = new ArrayBlockingQueue<>(queueSize);
            this.outputQueue = new ArrayBlockingQueue<>(queueSize);
        }
        
        @Override
        public synchronized void accept(I input) {
            if (inputDone) throw new IllegalStateException();
            try {
                inputQueue.put(input);
                batchCounter.incrementAndGet();
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(-1);
            }
        }
        
        public synchronized void inputDone() {
            inputDone = true;
        }
        
        public void addInputWorker(Consumer<Consumer<I>> callback) {
            Runnable worker = () -> {
                Thread.currentThread().setName("InputWorker");
                try {
                    callback.accept(ParallelPipeline.this);
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(-1);
                } finally {
                    inputDone();
                }
            };
            if (!inputWorker.compareAndSet(null, worker)) {
                throw new IllegalStateException();
            }
            threadPool.submit(worker);
        }

        public void addBatchProcessors(int numProcessors, Supplier<Function<I, O>> processorFactory) {
            addBatchProcessors(numProcessors, processorFactory, null);
        }
        
        public void addBatchProcessors(int numProcessors, Supplier<Function<I, O>> processorFactory, Runnable onDone) {
            for (int i = 1; i <= numProcessors; i++) {
                int index = nextProcessorIndex.getAndIncrement();
                Runnable processorTask = createProcessor(index, processorFactory, onDone);
                this.threadPool.submit(processorTask);
            }
        }

        private Runnable createProcessor(int processorIndex, Supplier<Function<I, O>> processorFactory, Runnable onDone) {
            String processorName = "Processor-" + processorIndex;
            Function<I, O> processor = processorFactory.get();
            return () -> {
                Thread.currentThread().setName(processorName);
                try {
                    while (!(inputDone && inputQueue.isEmpty())) {
                        I input = inputQueue.poll();
                        if (input != null) {
                            O output = processor.apply(input);
                            outputQueue.put(output);
                        } else {
                            Thread.yield();
                        }
                    }
                    if (onDone != null) {
                        onDone.run();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                    System.exit(-1);
                }
            };
        }

        public void consumeOutput(Consumer<O> callback) {
            try {
                while (!(inputDone && batchCounter.get() == 0)) {
                    O output = outputQueue.poll();
                    if (output != null) {
                        callback.accept(output);
                        batchCounter.decrementAndGet();
                    } else {
                        Thread.yield();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(-1);
            }
        }
        
    }

    private static ExecutorService THREAD_POOL = Executors.newCachedThreadPool((runnable) -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });
    
    public static <T> void processInParallel(ArrayList<T> array, int numThreads, Supplier<Consumer<T>> workerFactory) {
        int numItems = array.size();
        if (numThreads == 1) {
            Consumer<T> worker = workerFactory.get();
            for (int index = 0; index < numItems; index++) {
                worker.accept(array.get(index));
            }
        } else {
            ExecutorService threadPool = THREAD_POOL;
            AtomicInteger nextIndex = new AtomicInteger();
            CountDownLatch latch = new CountDownLatch(numThreads);
            for (int i = 0; i < numThreads; i++) {
                threadPool.submit(() -> {
                    try {
                        Consumer<T> worker = workerFactory.get();
                        int index;
                        while ((index = nextIndex.getAndIncrement()) < numItems) {
                            worker.accept(array.get(index));
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                        System.exit(-1);
                    }
                    latch.countDown();
                });
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
        }
    }

    public static <T> void processInParallel(int numThreads, Class<T> inputType, Consumer<Consumer<T>> input, 
            Supplier<Consumer<T>> processorFactory) {
        ParallelPipeline<T, T> pipeline = new ParallelPipeline<>(10000);
        pipeline.addInputWorker(input);
        pipeline.addBatchProcessors(numThreads, () -> {
            Consumer<T> processor = processorFactory.get();
            return (value) -> {
                processor.accept(value);
                return value;
            };
        });
        pipeline.consumeOutput((value) -> {});
    }
    
    private static void debugHbaNextMoves(int nextMoves, int bestAnsLen, int length, Bundle... bundles) {
        if (DEBUG_NEXT_MOVES) {
            int heads = heads(bundles);
            System.out.println("canHaveAnswer: " 
                    + bundlesStr(bundles) 
                    + ", bestAnsLen = " + bestAnsLen + ", length = " + length 
                    + ", nextMoves = " + nextMoves + " [" + maskDigitsToString(nextMoves) + "]" 
                    + ", heads = " + heads + " [" + maskDigitsToString(heads) + "]"
                    + " => nextMoves = " + (nextMoves & heads) + " [" + maskDigitsToString(nextMoves & heads) + "]");
        }
    }

    private static void debugHbaResult(int nextMoves, int bestAnsLen, int length, Bundle... bundles) {
        System.out.println("canHaveAnswer: " 
                + bundlesStr(bundles) 
                + ", bestAnsLen = " + bestAnsLen + ", length = " + length 
                + ", nextMoves = " + nextMoves + " [" + maskDigitsToString(nextMoves) + "]");
    }
    
}

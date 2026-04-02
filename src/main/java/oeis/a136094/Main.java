/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094;

import static oeis.a136094.Bundle.parseBundles;

import oeis.a136094.partials.Partials;
import oeis.a136094.partials.PartialsInit;
import oeis.a136094.solver.Solver;
import oeis.a136094.util.LogUtils;

public class Main {

    public static int MAX_N = 7;
    
    public static int MIN_PIECE_CHECK_SIZE = -1;
    
    public static int NUM_WORKER_THREADS = Runtime.getRuntime().availableProcessors();
    
    public static String PRECALC_ALG = "dfs";
    public static String SOLVE_ALG = "dfs-batch";
    
    public static String SOLVE_PROBLEM = null;
    
    public static int DFS_BATCH_SIZE = 10000;
    public static long DFS_BATCH_MAX_CACHE = 200_000_000L;
    public static int DFS_SWARM_MAX_GROUPS = 6;
    public static boolean DFS_SWARM_BATCH_MODE = false;

    public static int DFS_DISK_BLOCK_SIZE = 50_000_000; // N prefixes in, N*5 prefixes out
    public static int DFS_DISK_BATCH_SIZE = 10_000_000; // N nodes in, N*5 nodes out (up to 1kb each)
    public static int DFS_DISK_SEEN_SIZE = 50_000_000; // keys (up to 1kb each)
    
    public static boolean PRINT_PROGRESS = true;
    
    public static boolean APPLY_PARTIALS = true;
    public static boolean SAVE_FILES = false;
    
    public static String MAX_PRECALC_SHAPE = null;
    public static String CHECKPOINT_SHAPES = null;
    
    public static int MAX_LOOP_123 = 1000;
    public static int MAX_LOOP_45 = 1000;

    public static boolean DEBUG = false;
    public static boolean DEBUG_NEXT_MOVES = false;
    
    public static int minPieceCheckSize() {
        if (MIN_PIECE_CHECK_SIZE >= 0) {
            return MIN_PIECE_CHECK_SIZE;
        }
        return MAX_N - 2;
    }

    public static boolean shouldPrecalcShape(String shape) {
        ShapeInfo si = new ShapeInfo(shape);
        int numBundles = si.numBundles;
        int maxSize = si.maxSize;
        int maxHeads = si.maxHeads;
        
        int d1 = si.numDigits1, d2 = si.numDigits2, d3 = si.numDigits3;
        
        if (MAX_N <= 7) {
            if (maxSize == MAX_N) {
                return numBundles == 1 && (maxHeads == 1);
            } else if (maxSize == MAX_N-1) {
                return numBundles == 1
                        || numBundles == 2 && (maxHeads == 1 && d2 >= d1-1)
                        || numBundles == 3 && (maxHeads == 1 && d3 >= d1-1);
            } else if (maxSize == MAX_N-2) {
                return numBundles == 1
                        || numBundles == 2
                        || numBundles == 3 && (maxHeads == 1 && d3 >= d1-1);
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
            }
        } else if (MAX_N <= 8) {
            if (maxSize == MAX_N) {
                return numBundles == 1 && (maxHeads == 1);
            } else if (maxSize == MAX_N-1) {
                return numBundles == 1
                        || numBundles == 2 && (maxHeads == 1 && d2 >= d1-1)
                        || numBundles == 3 && (maxHeads == 1 && d3 >= d1-1);
            } else if (maxSize == MAX_N-2) {
                return numBundles == 1
                        || numBundles == 2
                        || numBundles == 3 && (maxHeads == 1 && d3 >= d1-1);
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
            if (maxSize == MAX_N) {
                return numBundles == 1 && (maxHeads == 1);
            } else if (maxSize == MAX_N-1) {
                return numBundles == 1
                        || numBundles == 2 && (maxHeads == 1 && d2 >= d1-1)
                        || numBundles == 3 && (maxHeads == 1 && d3 >= d1-1);
            } else if (maxSize == MAX_N-2) {
                return numBundles == 1
                        || numBundles == 2
                        || numBundles == 3 && (maxHeads == 1 && d3 >= d1-1);
            } else if (maxSize == MAX_N-3) {
                return numBundles == 1
                        || numBundles == 2
                        || numBundles == 3;
            } else if (maxSize == MAX_N-4) {
                return numBundles == 1
                        || numBundles == 2
                        || numBundles == 3
                        || numBundles == 4 && si.numMajorHeads <= 4; 
            } else if (maxSize == MAX_N-5) {
                return numBundles == 1
                        || numBundles == 2
                        || numBundles == 3
                        || numBundles == 4
                        || numBundles == 5 && si.numMajorHeads <= 6; 
            } else {
                return numBundles == 1
                        || numBundles == 2
                        || numBundles == 3
                        || numBundles == 4
                        || numBundles == 5;
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        processArgs(args);
        
        long precalcBegin = System.currentTimeMillis();
        Partials partials = PartialsInit.precalc();
        long precalcEnd = System.currentTimeMillis();

        if (SOLVE_PROBLEM != null) {
            System.out.println("precalc done");
            System.out.println("solve: " + SOLVE_PROBLEM);
            solveCustomProblem(SOLVE_PROBLEM, partials);
        } else {
            long solveBegin = System.currentTimeMillis();
            solveDefaultProblems(partials);
            long solveEnd = System.currentTimeMillis();
            
            System.out.println(LogUtils.formatLog("precalc %s", LogUtils.timeStr(precalcEnd - precalcBegin)));
            System.out.println(LogUtils.formatLog("precalc calc %s", LogUtils.timeStr(PartialsInit.totalPrecalcCalcTime)));
            System.out.println(LogUtils.formatLog("solve %s", LogUtils.timeStr(solveEnd - solveBegin)));
            System.out.println(LogUtils.formatLog("total %s", LogUtils.timeStr(solveEnd - precalcBegin)));
        }
    }

    private static void processArgs(String[] args) throws Exception {
        System.out.println();   
        for (int index = 0; index < args.length; index++) {
            String[] split = args[index].split("=", 2);
            String key = split[0];
            String value = (split.length > 1) ? split[1] : null;
            
            if ("--threads".equals(key)) {
                NUM_WORKER_THREADS = Integer.parseInt(value);
                System.out.println("NUM_WORKER_THREADS: " + NUM_WORKER_THREADS);
            } else if ("--n".equals(key)) {
                MAX_N = Integer.parseInt(value);
                System.out.println("MAX_N: " + MAX_N);
            } else if ("--min-piece-check-size".equals(key)) {
                MIN_PIECE_CHECK_SIZE = Integer.parseInt(value);
                System.out.println("MIN_PIECE_CHECK_SIZE: " + MIN_PIECE_CHECK_SIZE);
            } else if ("--solve-alg".equals(key)) {
                SOLVE_ALG = value;
                System.out.println("SOLVE_ALG: " + SOLVE_ALG);
            } else if ("--precalc-alg".equals(key)) {
                PRECALC_ALG = value;
                System.out.println("PRECALC_ALG: " + PRECALC_ALG);
            } else if ("--dfs-batch-max-cache".equals(key)) {
                DFS_BATCH_MAX_CACHE = Long.parseLong(value);
                System.out.println("DFS_BATCH_MAX_CACHE: " + DFS_BATCH_MAX_CACHE);
            } else if ("--dfs-batch-size".equals(key)) {
                DFS_BATCH_SIZE = Integer.parseInt(value);
                System.out.println("DFS_BATCH_SIZE: " + DFS_BATCH_SIZE);
            } else if ("--dfs-disk-block-size".equals(key)) {
                DFS_DISK_BLOCK_SIZE = Integer.parseInt(value);
                System.out.println("DFS_DISK_BLOCK_SIZE: " + DFS_DISK_BLOCK_SIZE);
            } else if ("--dfs-disk-batch-size".equals(key)) {
                DFS_DISK_BATCH_SIZE = Integer.parseInt(value);
                System.out.println("DFS_DISK_BATCH_SIZE: " + DFS_DISK_BATCH_SIZE);
            } else if ("--dfs-disk-seen-size".equals(key)) {
                DFS_DISK_SEEN_SIZE = Integer.parseInt(value);
                System.out.println("DFS_DISK_SEEN_SIZE: " + DFS_DISK_SEEN_SIZE);
            } else if ("--dfs-swarm-max-groups".equals(key)) {
                DFS_SWARM_MAX_GROUPS = Integer.parseInt(value);
                System.out.println("DFS_SWARM_MAX_GROUPS: " + DFS_SWARM_MAX_GROUPS);
            } else if ("--dfs-swarm-batch-mode".equals(key)) {
                DFS_SWARM_BATCH_MODE = parseBoolean(value, true);
                System.out.println("DFS_SWARM_BATCH_MODE: " + DFS_SWARM_BATCH_MODE);
            } else if ("--print-progress".equals(key)) {
                PRINT_PROGRESS = parseBoolean(value, true);
                System.out.println("PRINT_PROGRESS: " + PRINT_PROGRESS);
            } else if ("--checkpoint-shapes".equals(key)) {
                CHECKPOINT_SHAPES = value;
                System.out.println("CHECKPOINT_SHAPES: " + CHECKPOINT_SHAPES);
            } else if ("--max-precalc-shape".equals(key)) {
                MAX_PRECALC_SHAPE = value;
                System.out.println("MAX_PRECALC_SHAPE: " + MAX_PRECALC_SHAPE);
            } else if ("--max-loop-123".equals(key)) {
                MAX_LOOP_123 = Integer.parseInt(value);
                System.out.println("MAX_LOOP_123: " + MAX_LOOP_123);
            } else if ("--max-loop-45".equals(key)) {
                MAX_LOOP_45 = Integer.parseInt(value);
                System.out.println("MAX_LOOP_45: " + MAX_LOOP_45);
            } else if ("--apply-partials".equals(key)) {
                APPLY_PARTIALS = parseBoolean(value, true);
                System.out.println("APPLY_PARTIALS: " + APPLY_PARTIALS);
            } else if ("--save-files".equals(key)) {
                SAVE_FILES = parseBoolean(value, true);
                System.out.println("SAVE_FILES: " + SAVE_FILES);
            } else if ("--debug".equals(key)) {
                DEBUG = parseBoolean(value, true);
                System.out.println("DEBUG: " + DEBUG);
            } else if ("--debug-next-moves".equals(key)) {
                DEBUG_NEXT_MOVES = parseBoolean(value, true);
                System.out.println("DEBUG_NEXT_MOVES: " + DEBUG_NEXT_MOVES);
            } else if ("--solve".equals(key)) {
                SOLVE_PROBLEM = args[++index];
                System.out.println("SOLVE_PROBLEM: " + SOLVE_PROBLEM);
            } else {
                throw new RuntimeException("Unknown argument: " + key);
            }
        }
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        return (value != null) ? "true".equals(value) : defaultValue;
    }

    private static void solveCustomProblem(String bundles, Partials partials) {
        Problem problem = new Problem(parseBundles(bundles));
        Solver solver = Solver.createSolver(SOLVE_ALG, partials);
        solver.solve(problem);
        problem.printResult("");
    }

    private static void solveDefaultProblems(Partials partials) {
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
        
        Problem[] problems = new Problem[10];
        for (int i = 1; i <= 9; i++) {
            int digits = (1 << i) - 1;
            Bundle bundle = Bundle.of(digits, digits); // all permutations of N digits
            problems[i] = new Problem(new Bundle[] {bundle});
        }
        
        Solver solver = Solver.createSolver(SOLVE_ALG, partials);
        
        if (solver == null) return;
        
        for (int i = 1; i <= MAX_N; i++) {
            Problem problem = problems[i];
            String expectedAnswer = expectedAnswers[i];
            
            solver.solve(problem);
            problem.printResult("");
            
            String answer = problem.getAnswer();
            if (!answer.equals(expectedAnswer)) {
                RuntimeException error = new RuntimeException(answer + " != " + expectedAnswer);
                error.printStackTrace(System.out);
                throw error;
            }
        }
    }

}

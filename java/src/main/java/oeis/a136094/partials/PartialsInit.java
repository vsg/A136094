/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.partials;

import static oeis.a136094.key.KeyUtils.K2;
import static oeis.a136094.util.FileUtils.compressFile;
import static oeis.a136094.util.FileUtils.deserializeObjectFromFileGZ;
import static oeis.a136094.util.FileUtils.printWriteToFileGZ;
import static oeis.a136094.util.FileUtils.readLinesFromFile;
import static oeis.a136094.util.FileUtils.serializeObjectToFileGZ;
import static oeis.a136094.util.ParallelUtils.processInBatches;
import static oeis.a136094.util.ParallelUtils.processInParallel;
import static oeis.a136094.util.Utils.MASK_4;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import oeis.a136094.Bundle;
import oeis.a136094.Checkpoint;
import oeis.a136094.Main;
import oeis.a136094.Problem;
import oeis.a136094.ShapeInfo;
import oeis.a136094.key.Key;
import oeis.a136094.key.KeyBuilder;
import oeis.a136094.solver.DFSSwarmSolver;
import oeis.a136094.solver.Solver;
import oeis.a136094.util.MemoryEfficientHashSet;

public class PartialsInit {

    private static final String PROBLEMS_DIR = "problems";
    private static final String SOLUTIONS_DIR = "solutions";
    private static final String COUNTS_FILE = "counts.txt.gz";
    
    public static long totalPrecalcCalcTime;
    
    public static Partials precalc() {
        Partials partials = new Partials();
        
        List<String> shapes = enumShapes().stream()
                .filter(Main::shouldPrecalcShape)
                .sorted(ShapeInfo::compareShapesByHeadCounts)
                .toList();
        
        List<String> checkpoints = Optional.ofNullable(Main.CHECKPOINT_SHAPES)
                .map(s -> List.of(s.split(";")))
                .orElse(List.of());
        
        System.out.println("Calculating totals ...");
        
        Map<String, Long> problemCountByShape = generateProblems(shapes);
        
        AtomicLong totalPrecalc = new AtomicLong(calcTotalPrecalc(shapes, problemCountByShape));
        AtomicLong currPrecalc = new AtomicLong(1);
        
        String checkpointShape = findLastExistingCheckpoint(checkpoints);
        if (checkpointShape != null) {
            File checkpointFile = checkpointFile(checkpointShape);
            partials = loadPrecalc(checkpointFile);
        }

        if (Main.MAX_PRECALC_SHAPE != null) {
            shapes = shapes.stream()
                    .filter(shape -> ShapeInfo.compareShapesByHeadCounts(shape, Main.MAX_PRECALC_SHAPE) <= 0)
                    .toList();
        }
        
        for (String shape : shapes) {
            if (isPrecalcDone(shape, checkpointShape)) {
                currPrecalc.addAndGet(problemCountByShape.getOrDefault(shape, 0L));
                continue;
            }
            
            precalcShape(shape, partials, currPrecalc, totalPrecalc);
            
            if (checkpoints.contains(shape)) {
                File saveFile = checkpointFile(shape);
                if (!saveFile.exists()) {
                    savePrecalc(saveFile, partials);
                }
            }
        }
        
        return partials;
    }

    private static String findLastExistingCheckpoint(List<String> checkpoints) {
        return checkpoints.stream()
                .filter(shape -> checkpointFile(shape).exists())
                .reduce((first, second) -> second)
                .orElse(null);
    }
    
    private static boolean isPrecalcDone(String shape, String checkpointShape) {
        return checkpointShape != null && ShapeInfo.compareShapesByHeadCounts(shape, checkpointShape) <= 0;
    }
    
    private static Partials loadPrecalc(File file) {
        System.out.println("Loading " + file.getName() + " ...");
        return deserializeObjectFromFileGZ(file);
    }

    private static void savePrecalc(File file, Partials partials) {
        System.out.println("Writing " + file.getName() + " ...");
        serializeObjectToFileGZ(file, partials);
    }

    private static List<String> enumShapes() {
        List<String> shapes = new ArrayList<>();
        for (int numDigits1 = 1; numDigits1 <= Main.MAX_N; numDigits1++) {
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
        return shapes;
    }

    private static Map<String, Long> generateProblems(List<String> shapes) {
        Map<String, Long> counts = Collections.synchronizedMap(new LinkedHashMap<>());
        
        File countsFile = new File(COUNTS_FILE);
        if (countsFile.exists()) {
            loadShapeCounts(countsFile, counts);
        }

        List<String> missing = shapes.stream()
                .filter(shape -> !counts.containsKey(shape))
                .toList();

        if (!missing.isEmpty()) {
            missing.forEach(shape -> {
                long count = generateProblemsOfShape(shape);
                counts.put(shape, count);
            });
            saveShapeCounts(countsFile, counts);
        }
        
        return counts;
    }

    public static long generateProblemsOfShape(String shape) {
        File problemsFile = shapeProblemsFile(shape);
        if (problemsFile.exists()) {
            return loadProblemsOfShape(shape).size();
        }
        System.out.println("Generating " + shape);
        System.out.println("Writing " + problemsFile.getName());
        AtomicLong count = new AtomicLong();
        printWriteToFileGZ(problemsFile, (writer) -> {
            iterateUniqueBundlesOfShape(shape, (bundles) -> {
                count.incrementAndGet();
                writer.println(Bundle.bundlesToString(bundles));
            });
        });
        return count.get();
    }
    
    public static void iterateUniqueBundlesOfShape(String shape, Consumer<Bundle[]> callback) {
        Set<Key> seen = new MemoryEfficientHashSet<>();
        KeyBuilder.generateKeysInParallel((consumer) -> {
            iterateBundlesOfShape(shape, true, true, consumer);
        }, (bundles, key) -> {
            if (seen.add(key)) {
                callback.accept(bundles);
            }
        });
    }
    
    private static void saveShapeCounts(File countsFile, Map<String, Long> counts) {
        printWriteToFileGZ(countsFile, writer -> {
            counts.forEach((shape, count) -> {
                writer.println(shape + " " + count);
            });
        });
    }
    
    private static void loadShapeCounts(File countsFile, Map<String, Long> counts) {
        readLinesFromFile(countsFile, line -> {
            String[] split = line.trim().split(" ");
            String shape = split[0];
            Long count = Long.valueOf(split[1]);
            counts.put(shape, count);
        });
    }

    // may skip k1 and k2 dups; bundles 3-4-5 are sorted.
    public static void iterateBundlesOfShape(String shape, boolean skipK1Dups, boolean skipK2Dups, Consumer<Bundle[]> callback) {
        int[] shapes = ShapeInfo.parseShape(shape);
        
        int shape1 = (shapes.length > 0) ? shapes[0] : 0;
        int shape2 = (shapes.length > 1) ? shapes[1] : 0;
        int shape3 = (shapes.length > 2) ? shapes[2] : 0;
        int shape4 = (shapes.length > 3) ? shapes[3] : 0;
        int shape5 = (shapes.length > 4) ? shapes[4] : 0;

        int numDigits1 = shape1 >> 4;
        int numDigits2 = shape2 >> 4;  
        int numDigits3 = shape3 >> 4;
        int numDigits4 = shape4 >> 4;
        int numDigits5 = shape5 >> 4;

        int validDigits = (1 << Main.MAX_N) - 1;
        
        int[] groupSize = new int[9];
        int[] groupStartIndex = new int[9];
        
        boolean[] seenK2 = new boolean[1<<17];
        
        Bundle[] bundlesOfShape1 = Bundle.BUNDLES_OF_SHAPE[shape1];

        if (skipK1Dups) {
            int numHeads1 = shape1 & MASK_4;
            int heads1 = (1<<numHeads1)-1;
            int digits1 = (1<<numDigits1)-1;
            Bundle bundle1 = Bundle.unpack((heads1 << 9) | digits1);
            
            bundlesOfShape1 = new Bundle[] {bundle1};
        }
        
        for (Bundle bundle1 : bundlesOfShape1) {
            int digits1 = bundle1.digits();
            if ((digits1 & ~validDigits) != 0) continue;

            if (numDigits2 == 0) {
                callback.accept(new Bundle[] {bundle1});
                continue;
            }

            for (Bundle bundle2 : Bundle.BUNDLES_OF_SHAPE[shape2]) {
                int digits2 = bundle2.digits();
                if ((digits2 & ~validDigits) != 0) continue;
                
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
                
                for (Bundle bundle3 : Bundle.BUNDLES_OF_SHAPE[shape3]) {
                    int digits3 = bundle3.digits();
                    if ((digits3 & ~validDigits) != 0) continue;
                    
                    if ((digits3 == digits1 || digits3 == digits2)) continue; 
                    
                    if (isDup(bundle1, bundle3)) continue;
                    if (isDup(bundle2, bundle3)) continue;
                    
                    if (numDigits4 == 0) {
                        callback.accept(new Bundle[] {bundle1, bundle2, bundle3});
                        continue;
                    }
                    
                    for (Bundle bundle4 : Bundle.BUNDLES_OF_SHAPE[shape4]) {
                        int digits4 = bundle4.digits();
                        if ((digits4 & ~validDigits) != 0) continue;
                        
                        if (bundle4.toSortKey() >= bundle3.toSortKey()) continue;
                        
                        if ((digits4 == digits1 || digits4 == digits2 || digits4 == digits3)) continue;
                        
                        if (isDup(bundle1, bundle4)) continue;
                        if (isDup(bundle2, bundle4)) continue;
                        if (isDup(bundle3, bundle4)) continue;

                        if (numDigits5 == 0) {
                            callback.accept(new Bundle[] {bundle1, bundle2, bundle3, bundle4});
                            continue;
                        }

                        for (Bundle bundle5 : Bundle.BUNDLES_OF_SHAPE[shape5]) {
                            int digits5 = bundle5.digits();
                            if ((digits5 & ~validDigits) != 0) continue;
                            
                            if (bundle5.toSortKey() >= bundle4.toSortKey()) continue;
                            
                            if ((digits5 == digits1 || digits5 == digits2 || digits5 == digits3 || digits5 == digits4)) continue;
                            
                            if (isDup(bundle1, bundle5)) continue;
                            if (isDup(bundle2, bundle5)) continue;
                            if (isDup(bundle3, bundle5)) continue;
                            if (isDup(bundle4, bundle5)) continue;

                            callback.accept(new Bundle[] {bundle1, bundle2, bundle3, bundle4, bundle5});
                        }
                    }
                }
            }
        }
    }

    // is bundle2 a duplicate of bundle1, either whole or partially
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

    private static File shapeProblemsFile(String shape) {
        return new File(String.format("%s/%sx%s/%s.txt.gz", PROBLEMS_DIR, shape.charAt(2), shape.split(",").length, shape.replace('/', '@')));
    }

    private static File shapeSolutionsFile(String shape) {
        return new File(String.format("%s/%sx%s/%s.txt", SOLUTIONS_DIR, shape.charAt(2), shape.split(",").length, shape.replace('/', '@')));
    }

    public static File checkpointFile(String shape) {
        return new File(String.format("precalc-%s.bin.gz", shape.replace('/', '@')));
    }
    
    public static List<Bundle[]> loadProblemsOfShape(String shape) {
        File problemsFile = shapeProblemsFile(shape);
        if (!problemsFile.exists()) {
            throw new RuntimeException("Problems not found for shape " + shape);
        }
        //System.out.println("Reading " + shapeFileName);
        List<Bundle[]> problems = new ArrayList<>();
        readLinesFromFile(problemsFile, (line) -> {
            problems.add(Bundle.parseBundles(line));
        });
        return problems;
    }

    private static long calcTotalPrecalc(List<String> shapes, Map<String, Long> shapeBundleCounts) {
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

    private static void precalcShape(String shape, Partials partials, AtomicLong currPrecalc, AtomicLong totalPrecalc) {
        System.out.println(String.format("Precalc shape %s (%d / %d)", shape, currPrecalc.get(), totalPrecalc.get()));
        long begin = System.currentTimeMillis();
        
        List<Bundle[]> uniqueBundles = loadProblemsOfShape(shape);
        
        File solutionsFile = shapeSolutionsFile(shape);
        
        Checkpoint checkpoint = new Checkpoint(solutionsFile);

        long time1 = System.currentTimeMillis();
        
        solveShape(shape, checkpoint, uniqueBundles, partials, currPrecalc, totalPrecalc);
        
        long time2 = System.currentTimeMillis();
        
        if (!checkpoint.isEmpty() && !Main.NO_APPLY) {
            applyShapeSolutions(shape, checkpoint, partials);
        }
        
        long time3 = System.currentTimeMillis();

        checkpoint.shutdown();
        
        compressFile(solutionsFile);
        
        long end = System.currentTimeMillis();
        
        totalPrecalcCalcTime += time2-time1;
        
        System.out.println("Shape precalc time: shape = " + shape
                + ", calc = " + (time2-time1) + " ms"
                + ", apply = " + (time3-time2) + " ms"
                + ", compress = " + (end-time3) + " ms"
                + ", total = " + (end-begin) + " ms");
    }

    private static void solveShape(String shape, Checkpoint checkpoint, List<Bundle[]> uniqueBundles, 
            Partials partials, AtomicLong currPrecalc, AtomicLong totalPrecalc) {
        AtomicLong shapePrecalc = new AtomicLong(uniqueBundles.size());
        
        String alg = Main.PRECALC_ALG;
        
        Consumer<Consumer<Problem>> inputGenerator = (consumer) -> {
            KeyBuilder.generateKeysInParallel(uniqueBundles, (bundles, key) -> {
                if (!checkpoint.containsKey(key) && !alg.equals("none")) {
                    consumer.accept(new Problem(bundles, key));
                } else {
                    shapePrecalc.decrementAndGet();
                    currPrecalc.incrementAndGet();
                }
            });
        };
        Supplier<Solver> solverFactory = () -> {
            return Solver.createSolver(alg, partials);
        };
        processInBatches(inputGenerator, 100000, (batch) -> {
            solveInParallel(batch, alg, solverFactory, (problem) -> {
                if (problem.hasAnswer()) {
                    checkpoint.put(problem.getBundles(), problem.getKey(), problem.getAnswer());
                    problem.printResult(String.format(" (%d, %d / %d)", shapePrecalc.get(), currPrecalc.get(), totalPrecalc.get()));
                }
                shapePrecalc.decrementAndGet();
                currPrecalc.incrementAndGet();
            });
        });
    }

    private static void solveInParallel(List<Problem> batch, String alg, Supplier<Solver> solverFactory, Consumer<Problem> resultCallback) {
        if (alg.equals("dfs-swarm")) {
            DFSSwarmSolver solver = (DFSSwarmSolver) solverFactory.get();
            solver.solveProblems(batch, resultCallback);
        } else {
            int numSolvers = Solver.isMultiThreadedAlg(alg) ? 1 : Main.NUM_WORKER_THREADS;
            processInParallel(batch, numSolvers, () -> {
                Solver solver = solverFactory.get();
                return (problem) -> {
                    if (solver != null && !problem.hasAnswer()) {
                        solver.solve(problem);
                    }
                };
            }, null, resultCallback);
        }
    }

    private static void applyShapeSolutions(String shape, Checkpoint checkpoint, Partials partials) {
        System.out.println("Applying solutions ...");

        int[] shapes = ShapeInfo.parseShape(shape);
        
        if (shapes.length <= 3) {
            applyShapeSolutions123(shape, checkpoint, partials);
        } else {
            applyShapeSolutions45(shape, checkpoint, partials);
        }
    }

    private static void applyShapeSolutions123(String shape, Checkpoint checkpoint, Partials partials) {
        int[] swap1 = new int[9];
        
        KeyBuilder.generateKeysInParallel((consumer) -> {
            iterateBundlesOfShape(shape, true, false, consumer);
        }, (bundles, key) -> {
            Integer ansLen = checkpoint.get(key);
            if (ansLen == null) return; // skipped
            
            Bundle bundle1 = (bundles.length >= 1) ? bundles[0] : null;
            Bundle bundle2 = (bundles.length >= 2) ? bundles[1] : null;
            Bundle bundle3 = (bundles.length >= 3) ? bundles[2] : null;

            int shape1 = bundle1.shape();

            bundle1.makeBundleSwap1234(swap1);
            
            Bundle bundle22 = (bundle2 != null) ? bundle2.swapBundleDigits(swap1) : null;
            Bundle bundle33 = (bundle3 != null) ? bundle3.swapBundleDigits(swap1) : null;

            applySolution123(partials, bundles.length, shape1, bundle22, bundle33, ansLen);

            if (bundles.length > 2 && bundle22.shape() == bundle33.shape()) {
                applySolution123(partials, bundles.length, shape1, bundle33, bundle22, ansLen);
            }
        });
    }

    private static void applySolution123(Partials partials, int numBundles, 
            int shape1, Bundle bundle22, Bundle bundle33, int ansLen) {
        if (numBundles == 1) {
            partials.updateSolutionLength1(shape1, ansLen);
        } else if (numBundles == 2) {
            partials.updateSolutionLength2(shape1, bundle22, ansLen);
        } else {
            partials.updateSolutionLength3(shape1, bundle22, bundle33, ansLen);
        }
        
        if (numBundles > 1) partials.updateMaxKnownNextSolutionLength123By1(shape1, ansLen);
        if (numBundles > 2) partials.updateMaxKnownNextSolutionLength123By2(shape1, bundle22, ansLen);
    }

    private static void applyShapeSolutions45(String shape, Checkpoint checkpoint, Partials partials) {
        int[] k2swap = new int[9];
        int[] groupSize = new int[9];
        int[] groupStartIndex = new int[9];

        KeyBuilder.generateKeysInParallel((consumer) -> {
            iterateBundlesOfShape(shape, true, true, consumer);
        }, (bundles, key) -> {
            Integer ansLen = checkpoint.get(key);
            if (ansLen == null) return; // skipped
            
            Bundle bundle1 = (bundles.length >= 1) ? bundles[0] : null;
            Bundle bundle2 = (bundles.length >= 2) ? bundles[1] : null;
            Bundle bundle3 = (bundles.length >= 3) ? bundles[2] : null;
            Bundle bundle4 = (bundles.length >= 4) ? bundles[3] : null;
            Bundle bundle5 = (bundles.length >= 5) ? bundles[4] : null;
            
            int shape1 = bundle1.shape();

            int k2 = K2(bundle1, bundle2, k2swap, groupSize, groupStartIndex);
            
            Bundle bundle33 = (bundle3 != null) ? bundle3.swapBundleDigits(k2swap) : null;
            Bundle bundle44 = (bundle4 != null) ? bundle4.swapBundleDigits(k2swap) : null;
            Bundle bundle55 = (bundle5 != null) ? bundle5.swapBundleDigits(k2swap) : null;
            
            applySolution45(partials, bundles.length, shape1, k2, bundle33, bundle44, bundle55, ansLen);

            if (bundle33.shape() == bundle44.shape()) {
                applySolution45(partials, bundles.length, shape1, k2, bundle44, bundle33, bundle55, ansLen);
                if (bundles.length > 4 && bundle44.shape() == bundle55.shape()) {
                    applySolution45(partials, bundles.length, shape1, k2, bundle33, bundle55, bundle44, ansLen);
                    applySolution45(partials, bundles.length, shape1, k2, bundle55, bundle33, bundle44, ansLen);
                    applySolution45(partials, bundles.length, shape1, k2, bundle44, bundle55, bundle33, ansLen);
                    applySolution45(partials, bundles.length, shape1, k2, bundle55, bundle44, bundle33, ansLen);
                }
            } else {
                if (bundles.length > 4 && bundle44.shape() == bundle55.shape()) {
                    applySolution45(partials, bundles.length, shape1, k2, bundle33, bundle55, bundle44, ansLen);
                }
            }
        });
    }
    
    
    private static void applySolution45(Partials partials, int numBundles, 
            int shape1, int k2, Bundle bundle33, Bundle bundle44, Bundle bundle55, int ansLen) {
        if (numBundles == 4) {
            partials.updateSolutionLength4(k2, bundle33, bundle44, ansLen);
        } else {
            partials.updateSolutionLength5(k2, bundle33, bundle44, bundle55, ansLen);
        }
        
        if (numBundles > 1) partials.updateMaxKnownNextSolutionLength45By1(shape1, ansLen);
        if (numBundles > 2) partials.updateMaxKnownNextSolutionLength45By2(k2, ansLen);
        if (numBundles > 3) partials.updateMaxKnownNextSolutionLength45By3(k2, bundle33, ansLen);
        if (numBundles > 4) partials.updateMaxKnownNextSolutionLength45By4(k2, bundle33, bundle44, ansLen);
    }

}

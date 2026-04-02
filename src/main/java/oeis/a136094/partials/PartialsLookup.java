/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.partials;

import static oeis.a136094.Bundle.heads;
import static oeis.a136094.key.KeyUtils.K2;
import static oeis.a136094.util.BitUtils.digitsMaskToString;

import java.util.Arrays;

import oeis.a136094.Bundle;
import oeis.a136094.Main;

public class PartialsLookup {
    
    private final Partials partials;
    private final Bundle[] swappedBundles = new Bundle[1024];
    private final Bundle[] originalBundles = new Bundle[1024];

    public PartialsLookup(Partials partials) {
        this.partials = partials;
    }
    
    public int canHaveAnswer(String path, Bundle[] sortedBundles, int bestAnsLen) {
        if (Main.DEBUG) System.out.println(path + ": " + Bundle.bundlesToString(sortedBundles));
        
        int moves = heads(sortedBundles);
        
        moves = canHaveAnswer(sortedBundles, moves, bestAnsLen - path.length());
        if (moves == 0) {
            if (Main.DEBUG) System.out.println("Reject: " + path);
            return 0;
        }
        if (Main.DEBUG) System.out.println("Accept: " + path);
        
        return moves;
    }
    
    private int canHaveAnswer(Bundle[] sortedBundles, int nextMoves, int bestAnsLen) {
        if (sortedBundles.length == 0 || bestAnsLen == 0) return 0;
        
        nextMoves = canHaveAnswerLookup123(sortedBundles, bestAnsLen, nextMoves);
        if (nextMoves == 0) return 0;
        
        nextMoves = canHaveAnswerLookup45(sortedBundles, bestAnsLen, nextMoves);
        if (nextMoves == 0) return 0;
        
        Bundle[] sortedPieces = splitIntoPieces(sortedBundles);
        if (sortedPieces == null) return nextMoves;

        nextMoves = canHaveAnswerLookup123(sortedPieces, bestAnsLen, nextMoves);
        if (nextMoves == 0) return 0;
        
        nextMoves = canHaveAnswerLookup45(sortedPieces, bestAnsLen, nextMoves);
        if (nextMoves == 0) return 0;
        
        return nextMoves;
    }
    
    private int canHaveAnswerLookup123(Bundle[] sortedBundles, int bestAnsLen, int nextMoves) {
        int len = sortedBundles.length;
        if (len <= 0) return nextMoves;
        
        int maxDigits = sortedBundles[0].numDigits();
    
        len = Math.min(len, Main.MAX_LOOP_123);

        for (int index1 = 0; index1 < len; index1++) {
            Bundle bundle1 = sortedBundles[index1];
            int shape1 = bundle1.shape();
            int numDigits1 = bundle1.numDigits();

            if (numDigits1 < maxDigits) break;
            
            nextMoves = checkSolutionLength1(shape1, bestAnsLen, nextMoves, bundle1);
            if (nextMoves == 0) return 0;

            if (partials.getMaxKnownNextSolutionLength123By1(shape1) < bestAnsLen) continue;

            int[] swap1 = bundle1.makeBundleSwap1234();
            
            int numSwapped = 0;

            for (int j = index1+1; j < len; j++) {
                Bundle bundle2 = sortedBundles[j];
                int numDigits2 = bundle2.numDigits();

                if (numDigits2 < maxDigits-2) break;
                
                Bundle bundle22 = bundle2.swapBundleDigits(swap1);

                int index = numSwapped++;
                originalBundles[index] = bundle2;
                swappedBundles[index] = bundle22;
            }

            for (int index2 = 0; index2 < numSwapped; index2++) {
                Bundle bundle2 = originalBundles[index2];
                Bundle bundle22 = swappedBundles[index2];
                
                nextMoves = checkSolutionLength2(shape1, bundle22, bestAnsLen, nextMoves, bundle1, bundle2);
                if (nextMoves == 0) return 0;

                if (partials.getMaxKnownNextSolutionLength123By2(shape1, bundle22) < bestAnsLen) continue;

                for (int index3 = index2+1; index3 < numSwapped; index3++) {
                    Bundle bundle3 = originalBundles[index3];
                    Bundle bundle33 = swappedBundles[index3];
                    
                    nextMoves = checkSolutionLength3(shape1, bundle22, bundle33, bestAnsLen, nextMoves, bundle1, bundle2, bundle3);
                    if (nextMoves == 0) return 0;
                }
            }
        }
        return nextMoves;
    }

    private int canHaveAnswerLookup45(Bundle[] sortedBundles, int bestAnsLen, int nextMoves) {
        int len = sortedBundles.length;
        if (len <= 0) return nextMoves;
        
        int[] k2swap = new int[9];
        int[] groupSize = new int[9];
        int[] groupStartIndex = new int[9];
        
        int maxDigits = sortedBundles[0].numDigits();
    
        len = Math.min(len, Main.MAX_LOOP_45);

        for (int index1 = 0; index1 < len; index1++) {
            Bundle bundle1 = sortedBundles[index1];
            int shape1 = bundle1.shape();
            int numDigits1 = bundle1.numDigits();

            if (numDigits1 < maxDigits) break;
            
            if (partials.getMaxKnownNextSolutionLength45By1(shape1) < bestAnsLen) continue;
            
            for (int index2 = index1+1; index2 < len; index2++) {
                Bundle bundle2 = sortedBundles[index2];
                int numDigits2 = bundle2.numDigits();

                if (bundle2.toSortKey() >= bundle1.toSortKey()) throw new RuntimeException();
                if (numDigits2 < numDigits1-2) break;
                if (numDigits2 > numDigits1) throw new RuntimeException();
                
                int k2 = K2(bundle1, bundle2, k2swap, groupSize, groupStartIndex);
                
                if (partials.getMaxKnownNextSolutionLength45By2(k2) < bestAnsLen) continue;
                
                int numSwapped = 0;

                for (int k = index2+1; k < len; k++) {
                    Bundle bundle3 = sortedBundles[k];
                    int numDigits3 = bundle3.numDigits();

                    if (numDigits3 < maxDigits-2) break;
                    
                    Bundle bundle33 = bundle3.swapBundleDigits(k2swap);

                    int index = numSwapped++;
                    originalBundles[index] = bundle3;
                    swappedBundles[index] = bundle33;
                }

                for (int index3 = 0; index3 < numSwapped; index3++) {
                    Bundle bundle3 = originalBundles[index3];
                    Bundle bundle33 = swappedBundles[index3];

                    if (partials.getMaxKnownNextSolutionLength45By3(k2, bundle33) < bestAnsLen) continue;

                    for (int index4 = index3+1; index4 < numSwapped; index4++) {
                        Bundle bundle4 = originalBundles[index4];
                        Bundle bundle44 = swappedBundles[index4];
                        
                        nextMoves = checkSolutionLength4(k2, bundle33, bundle44, bestAnsLen, nextMoves, 
                                bundle1, bundle2, bundle3, bundle4);
                        if (nextMoves == 0) return 0;
        
                        if (partials.getMaxKnownNextSolutionLength45By4(k2, bundle33, bundle44) < bestAnsLen) continue;
                        
                        for (int index5 = index4+1; index5 < numSwapped; index5++) {
                            Bundle bundle5 = originalBundles[index5];
                            Bundle bundle55 = swappedBundles[index5];
        
                            nextMoves = checkSolutionLength5(k2, bundle33, bundle44, bundle55, bestAnsLen, nextMoves, 
                                    bundle1, bundle2, bundle3, bundle4, bundle5);
                            if (nextMoves == 0) return 0;
                        }
                    }
                }
            }
        }
        return nextMoves;
    }

    private int checkSolutionLength1(int shape1, int bestAnsLen, int nextMoves, Bundle... bundles) {
        int length1 = partials.getSolutionLength1(shape1);
        if (length1 >= bestAnsLen) {
            if (Main.DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length1, bundles);
            nextMoves &= heads(bundles);
            if (length1 > bestAnsLen || nextMoves == 0) {
                if (Main.DEBUG) debugHbaResult(nextMoves, bestAnsLen, length1, bundles);
                return 0;
            }
        }
        return nextMoves;
    }
    
    private int checkSolutionLength2(int shape1, Bundle bundle22, int bestAnsLen, int nextMoves, Bundle... bundles) {
        int length2 = partials.getSolutionLength2(shape1, bundle22);
        if (length2 >= bestAnsLen) {
            if (Main.DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length2, bundles);
            nextMoves &= heads(bundles);
            if (length2 > bestAnsLen || nextMoves == 0) {
                if (Main.DEBUG) debugHbaResult(nextMoves, bestAnsLen, length2, bundles);
                return 0;
            }
        }
        return nextMoves;
    }

    private int checkSolutionLength3(int shape1, Bundle bundle22, Bundle bundle33, int bestAnsLen, int nextMoves, Bundle... bundles) {
        int length3 = partials.getSolutionLength3(shape1, bundle22, bundle33);
        if (length3 >= bestAnsLen) {
            if (Main.DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length3, bundles);
            nextMoves &= heads(bundles);
            if (length3 > bestAnsLen || nextMoves == 0) {
                if (Main.DEBUG) debugHbaResult(nextMoves, bestAnsLen, length3, bundles);
                return 0;
            }
        }
        return nextMoves;
    }

    private int checkSolutionLength4(int k2, Bundle bundle33, Bundle bundle44, int bestAnsLen, int nextMoves, Bundle... bundles) {
        int length4 = partials.getSolutionLength4(k2, bundle33, bundle44);
        if (length4 >= bestAnsLen) {
            if (Main.DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length4, bundles);
            nextMoves &= heads(bundles);
            if (length4 > bestAnsLen || nextMoves == 0) {
                if (Main.DEBUG) debugHbaResult(nextMoves, bestAnsLen, length4, bundles);
                return 0;
            }
        }
        return nextMoves;
    }

    private int checkSolutionLength5(int k2, Bundle bundle33, Bundle bundle44, Bundle bundle55, int bestAnsLen, int nextMoves, Bundle... bundles) {
        int length5 = partials.getSolutionLength5(k2, bundle33, bundle44, bundle55);
        if (length5 >= bestAnsLen) {
            if (Main.DEBUG) debugHbaNextMoves(nextMoves, bestAnsLen, length5, bundles);
            nextMoves &= heads(bundles);
            if (length5 > bestAnsLen || nextMoves == 0) {
                if (Main.DEBUG) debugHbaResult(nextMoves, bestAnsLen, length5, bundles);
                return 0;
            }
        }
        return nextMoves;
    }

    private Bundle[] splitIntoPieces(Bundle[] sortedBundles) {
        int numDigits0 = sortedBundles[0].numDigits();
        if (numDigits0 < Main.minPieceCheckSize()) return null;
        
        Bundle[] pieces = Arrays.stream(sortedBundles)
                .takeWhile(b -> b.numDigits() >= numDigits0 - 1)
                .mapMulti((bundle, consumer) -> Arrays.stream(bundle.pieces()).forEach(consumer))
                .toArray(Bundle[]::new);
        
        return Bundle.sortBundles(pieces);
    }

    private static void debugHbaNextMoves(int nextMoves, int bestAnsLen, int length, Bundle... bundles) {
        if (Main.DEBUG_NEXT_MOVES) {
            int heads = heads(bundles);
            System.out.println("canHaveAnswer: " 
                    + Bundle.bundlesToString(bundles) 
                    + ", bestAnsLen = " + bestAnsLen + ", length = " + length 
                    + ", nextMoves = " + nextMoves + " [" + digitsMaskToString(nextMoves) + "]" 
                    + ", heads = " + heads + " [" + digitsMaskToString(heads) + "]"
                    + " => nextMoves = " + (nextMoves & heads) + " [" + digitsMaskToString(nextMoves & heads) + "]");
        }
    }

    private static void debugHbaResult(int nextMoves, int bestAnsLen, int length, Bundle... bundles) {
        System.out.println("canHaveAnswer: " 
                + Bundle.bundlesToString(bundles) 
                + ", bestAnsLen = " + bestAnsLen + ", length = " + length 
                + ", nextMoves = " + nextMoves + " [" + digitsMaskToString(nextMoves) + "]");
    }
    
}
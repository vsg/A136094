/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094;

import static oeis.a136094.Bundle.shape;
import static oeis.a136094.util.Utils.MASK_4;

public class ShapeInfo {
    
    public final int shape1, shape2, shape3, shape4, shape5;
    public final int numHeads1, numHeads2, numHeads3, numHeads4, numHeads5;
    public final int numDigits1, numDigits2, numDigits3, numDigits4, numDigits5;
    
    public final int maxSize;
    public final int numHeads;
    public final int numBundles;
    
    public final int maxHeads;
    public final int numMajorHeads;
    
    public ShapeInfo(String shape) {
        int[] shapes = parseShape(shape);
        this.shape1 = (shapes.length > 0) ? shapes[0] : 0;
        this.shape2 = (shapes.length > 1) ? shapes[1] : 0;
        this.shape3 = (shapes.length > 2) ? shapes[2] : 0;
        this.shape4 = (shapes.length > 3) ? shapes[3] : 0;
        this.shape5 = (shapes.length > 4) ? shapes[4] : 0;
        this.numHeads1 = shape1 & MASK_4;
        this.numHeads2 = shape2 & MASK_4;
        this.numHeads3 = shape3 & MASK_4;
        this.numHeads4 = shape4 & MASK_4;
        this.numHeads5 = shape5 & MASK_4;
        this.numDigits1 = shape1 >> 4; 
        this.numDigits2 = shape2 >> 4; 
        this.numDigits3 = shape3 >> 4; 
        this.numDigits4 = shape4 >> 4;
        this.numDigits5 = shape5 >> 4;
        this.maxSize = numDigits1;
        this.numHeads = numHeads1 + numHeads2 + numHeads3 + numHeads4 + numHeads5;
        this.numBundles = (numDigits1 > 0 ? 1 : 0) 
                + (numDigits2 > 0 ? 1 : 0) 
                + (numDigits3 > 0 ? 1 : 0) 
                + (numDigits4 > 0 ? 1 : 0)
                + (numDigits5 > 0 ? 1 : 0);
        this.maxHeads = Math.max(Math.max(
                        Math.max(numHeads1, numHeads2),
                        Math.max(numHeads3, numHeads4)), 
                        numHeads5);
        this.numMajorHeads = (numDigits1 == maxSize ? numHeads1 : 0) 
                + (numDigits2 == maxSize ? numHeads2 : 0) 
                + (numDigits3 == maxSize ? numHeads3 : 0) 
                + (numDigits4 == maxSize ? numHeads4 : 0)
                + (numDigits5 == maxSize ? numHeads5 : 0);
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

    public static int compareShapesByHeadCounts(String shape1, String shape2) {
        int[] c1 = shapeHeadCounts(shape1);
        int[] c2 = shapeHeadCounts(shape2);
        for (int i = 9; i >= 0; i--) {
            if (c1[i] != c2[i]) {
                return (c1[i] < c2[i]) ? -1 : 1;
            }
        }
        return shape1.compareTo(shape2); // if all counts are same, compare lexicographically
    }

    private static int[] shapeHeadCounts(String shape) {
        int[] shapes = parseShape(shape);
        int[] headCounts = new int[10];
        for (int sh : shapes) {
            int numHeads = sh & MASK_4;
            int numDigits = sh >> 4;
            headCounts[numDigits] += numHeads;
        }
        return headCounts;
    }

}
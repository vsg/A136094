/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.partials;

import java.io.Serializable;

import oeis.a136094.Bundle;

@SuppressWarnings("serial")
public class Partials implements Serializable {
    
    private static final int MAX_ALLOC_SHAPE = Bundle.shape(9, 9);
    
    private final byte[] solutionLength1 = new byte[256]; // shape1 -> solutionLength
    private final byte[][] solutionLength2 = new byte[256][]; // shape1 -> bundleIndex22 -> solutionLength
    private final byte[][][] solutionLength3 = new byte[256][][]; // shape1 -> bundleIndex22 -> bundleIndex33 -> solutionLength
    private final byte[][][] solutionLength4 = new byte[1<<17][][]; // k2 -> bundleIndex33 -> bundleIndex44 -> solutionLength
    private final byte[][][][] solutionLength5 = new byte[1<<17][][][]; // k2 -> bundleIndex33 -> bundleIndex44 -> bundleIndex55 -> solutionLength
    
    private final byte[] maxKnownNextSolutionLength123By1 = new byte[256]; // shape1 -> maxKnownNextSolutionLength
    private final byte[][] maxKnownNextSolutionLength123By2 = new byte[256][]; // shape1 -> bundleIndex22 -> maxKnownNextSolutionLength
    
    private final byte[] maxKnownNextSolutionLength45By1 = new byte[256]; // shape1 -> maxKnownNextSolutionLength
    private final byte[] maxKnownNextSolutionLength45By2 = new byte[1<<17]; // k2 -> maxKnownNextSolutionLength
    private final byte[][] maxKnownNextSolutionLength45By3 = new byte[1<<17][]; // k2 -> bundleIndex33 -> maxKnownNextSolutionLength
    private final byte[][][] maxKnownNextSolutionLength45By4 = new byte[1<<17][][]; // k2 -> bundleIndex33 -> bundleIndex44 -> maxKnownNextSolutionLength
    
    private byte[] allocateLookup1(int maxShape) {
        int size = Bundle.MAX_BUNDLE_INDEX_OF_SHAPE[maxShape]+1;
        return new byte[size];
    }
    
    private byte[][] allocateLookup2(int maxShape) {
        int size = Bundle.MAX_BUNDLE_INDEX_OF_SHAPE[maxShape]+1;
        return new byte[size][];
    }
    
    private byte[][][] allocateLookup3(int maxShape) {
        int size = Bundle.MAX_BUNDLE_INDEX_OF_SHAPE[maxShape]+1;
        return new byte[size][][];
    }
    
    public void updateSolutionLength1(int shape1, int len) {
        if (solutionLength1[shape1] == 0) {
            solutionLength1[shape1] = (byte) len;
        } else {
            if (solutionLength1[shape1] != len) throw new RuntimeException();
        }
    }

    public void updateSolutionLength2(int shape1, Bundle bundle22, int len) {
        int bundleIndex22 = bundle22.index();
        if (solutionLength2[shape1] == null) {
            solutionLength2[shape1] = allocateLookup1(shape1);
        }
        if (solutionLength2[shape1][bundleIndex22] == 0) {
            solutionLength2[shape1][bundleIndex22] = (byte) len;
        } else {
            if (solutionLength2[shape1][bundleIndex22] != len) throw new RuntimeException();
        }
    }
    
    public void updateSolutionLength3(int shape1, Bundle bundle22, Bundle bundle33, int len) {
        int bundleIndex22 = bundle22.index();
        int bundleIndex33 = bundle33.index();
        if (solutionLength3[shape1] == null) {
            solutionLength3[shape1] = allocateLookup2(shape1);
        }
        if (solutionLength3[shape1][bundleIndex22] == null) {
            solutionLength3[shape1][bundleIndex22] = allocateLookup1(bundle22.shape());
        }
        if (solutionLength3[shape1][bundleIndex22][bundleIndex33] == 0) {
            solutionLength3[shape1][bundleIndex22][bundleIndex33] = (byte) len;
        } else {
            if (solutionLength3[shape1][bundleIndex22][bundleIndex33] != len) throw new RuntimeException();
        }
    }
    
    public void updateSolutionLength4(int k2, Bundle bundle33, Bundle bundle44, int len) {
        int bundleIndex33 = bundle33.index();
        int bundleIndex44 = bundle44.index();
        if (solutionLength4[k2] == null) {
            solutionLength4[k2] = allocateLookup2(MAX_ALLOC_SHAPE);
        }
        if (solutionLength4[k2][bundleIndex33] == null) {
            solutionLength4[k2][bundleIndex33] = allocateLookup1(bundle33.shape());
        }
        if (solutionLength4[k2][bundleIndex33][bundleIndex44] == 0) {
            solutionLength4[k2][bundleIndex33][bundleIndex44] = (byte) len;
        } else {
            if (solutionLength4[k2][bundleIndex33][bundleIndex44] != len) throw new RuntimeException();
        }
    }
    
    public void updateSolutionLength5(int k2, Bundle bundle33, Bundle bundle44, Bundle bundle55, int len) {
        int bundleIndex33 = bundle33.index();
        int bundleIndex44 = bundle44.index();
        int bundleIndex55 = bundle55.index();
        if (solutionLength5[k2] == null) {
            solutionLength5[k2] = allocateLookup3(MAX_ALLOC_SHAPE);
        }
        if (solutionLength5[k2][bundleIndex33] == null) {
            solutionLength5[k2][bundleIndex33] = allocateLookup2(bundle33.shape());
        }
        if (solutionLength5[k2][bundleIndex33][bundleIndex44] == null) {
            solutionLength5[k2][bundleIndex33][bundleIndex44] = allocateLookup1(bundle44.shape());
        }
        if (solutionLength5[k2][bundleIndex33][bundleIndex44][bundleIndex55] == 0) {
            solutionLength5[k2][bundleIndex33][bundleIndex44][bundleIndex55] = (byte) len;
        } else {
            if (solutionLength5[k2][bundleIndex33][bundleIndex44][bundleIndex55] != len) throw new RuntimeException();
        }
    }

    public void updateMaxKnownNextSolutionLength123By1(int shape1, int len) {
        if (maxKnownNextSolutionLength123By1[shape1] < len) {
            maxKnownNextSolutionLength123By1[shape1] = (byte) len;
        }
    }
    
    public void updateMaxKnownNextSolutionLength123By2(int shape1, Bundle bundle22, int len) {
        int bundleIndex22 = bundle22.index();
        if (maxKnownNextSolutionLength123By2[shape1] == null) {
            maxKnownNextSolutionLength123By2[shape1] = allocateLookup1(shape1);
        }
        if (maxKnownNextSolutionLength123By2[shape1][bundleIndex22] < len) {
            maxKnownNextSolutionLength123By2[shape1][bundleIndex22] = (byte) len;
        }
    }

    public void updateMaxKnownNextSolutionLength45By1(int shape1, int len) {
        if (maxKnownNextSolutionLength45By1[shape1] < len) {
            maxKnownNextSolutionLength45By1[shape1] = (byte) len;
        }
    }

    public void updateMaxKnownNextSolutionLength45By2(int k2, int len) {
        if (maxKnownNextSolutionLength45By2[k2] < len) {
            maxKnownNextSolutionLength45By2[k2] = (byte) len;
        }
    }

    public void updateMaxKnownNextSolutionLength45By3(int k2, Bundle bundle33, int len) {
        int bundleIndex33 = bundle33.index();
        if (maxKnownNextSolutionLength45By3[k2] == null) {
            maxKnownNextSolutionLength45By3[k2] = allocateLookup1(MAX_ALLOC_SHAPE);
        }
        if (maxKnownNextSolutionLength45By3[k2][bundleIndex33] < len) {
            maxKnownNextSolutionLength45By3[k2][bundleIndex33] = (byte) len;
        }
    }

    public void updateMaxKnownNextSolutionLength45By4(int k2, Bundle bundle33, Bundle bundle44, int len) {
        int bundleIndex33 = bundle33.index();
        int bundleIndex44 = bundle44.index();
        if (maxKnownNextSolutionLength45By4[k2] == null) {
            maxKnownNextSolutionLength45By4[k2] = allocateLookup2(MAX_ALLOC_SHAPE);
        }
        if (maxKnownNextSolutionLength45By4[k2][bundleIndex33] == null) {
            maxKnownNextSolutionLength45By4[k2][bundleIndex33] = allocateLookup1(bundle33.shape());
        }
        if (maxKnownNextSolutionLength45By4[k2][bundleIndex33][bundleIndex44] < len) {
            maxKnownNextSolutionLength45By4[k2][bundleIndex33][bundleIndex44] = (byte) len;
        }
    }

    public int getSolutionLength1(int shape1) {
        return solutionLength1[shape1];
    }

    public int getSolutionLength2(int shape1, Bundle bundle22) {
        int bundleIndex22 = bundle22.index();
        if (solutionLength2[shape1] == null) return 0;
        return solutionLength2[shape1][bundleIndex22];
    }

    public int getSolutionLength3(int shape1, Bundle bundle22, Bundle bundle33) {
        int bundleIndex22 = bundle22.index();
        int bundleIndex33 = bundle33.index();
        if (solutionLength3[shape1] == null
                || solutionLength3[shape1][bundleIndex22] == null) return 0;
        byte[][] a = solutionLength3[shape1];
        byte[] b = a[bundleIndex22];
        byte c = b[bundleIndex33];
        return c;
    }

    public int getSolutionLength4(int k2, Bundle bundle33, Bundle bundle44) {
        int bundleIndex33 = bundle33.index();
        int bundleIndex44 = bundle44.index();
        if (solutionLength4[k2] == null
                || solutionLength4[k2][bundleIndex33] == null) return 0;
        return solutionLength4[k2][bundleIndex33][bundleIndex44];
    }

    public int getSolutionLength5(int k2, Bundle bundle33, Bundle bundle44, Bundle bundle55) {
        int bundleIndex33 = bundle33.index();
        int bundleIndex44 = bundle44.index();
        int bundleIndex55 = bundle55.index();
        if (solutionLength5[k2] == null
                || solutionLength5[k2][bundleIndex33] == null
                || solutionLength5[k2][bundleIndex33][bundleIndex44] == null) return 0;
        return solutionLength5[k2][bundleIndex33][bundleIndex44][bundleIndex55];
    }

    public int getMaxKnownNextSolutionLength123By1(int shape1) {
        return maxKnownNextSolutionLength123By1[shape1];
    }
    
    public int getMaxKnownNextSolutionLength123By2(int shape1, Bundle bundle22) {
        int bundleIndex22 = bundle22.index();
        if (maxKnownNextSolutionLength123By2[shape1] == null) return 0;
        return maxKnownNextSolutionLength123By2[shape1][bundleIndex22];
    }

    public int getMaxKnownNextSolutionLength45By1(int shape1) {
        return maxKnownNextSolutionLength45By1[shape1];
    }

    public int getMaxKnownNextSolutionLength45By2(int k2) {
        return maxKnownNextSolutionLength45By2[k2];
    }

    public int getMaxKnownNextSolutionLength45By3(int k2, Bundle bundle33) {
        int bundleIndex33 = bundle33.index();
        if (maxKnownNextSolutionLength45By3[k2] == null) return 0;
        return maxKnownNextSolutionLength45By3[k2][bundleIndex33];
    }

    public int getMaxKnownNextSolutionLength45By4(int k2, Bundle bundle33, Bundle bundle44) {
        int bundleIndex33 = bundle33.index();
        int bundleIndex44 = bundle44.index();
        if (maxKnownNextSolutionLength45By4[k2] == null
                || maxKnownNextSolutionLength45By4[k2][bundleIndex33] == null) return 0;
        return maxKnownNextSolutionLength45By4[k2][bundleIndex33][bundleIndex44];
    }

}
/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.util;

import java.io.*;

/**
 * Copies stdin to stdout and to a file, like Unix tee.
 */
public class Tee {

    public static void main(String[] args) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(args[0])) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = System.in.read(buf)) != -1) {
                System.out.write(buf, 0, n);
                System.out.flush();
                fos.write(buf, 0, n);
            }
        }
    }
    
}
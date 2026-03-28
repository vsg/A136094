/*
 * Copyright (c) 2026 Vitaliy Garnashevich
 * SPDX-License-Identifier: Apache-2.0
 */
package oeis.a136094.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Serializable;
import java.util.function.Consumer;

import com.linkedin.migz.MiGzInputStream;
import com.linkedin.migz.MiGzOutputStream;

import oeis.a136094.Main;

public class FileUtils {

    private static final int IO_BUFFER_SIZE = 65536;
    private static final int MIGZ_BLOCK_SIZE = 65536;

    @SuppressWarnings("unchecked")
    public static <T> T deserializeObjectFromFileGZ(File file) {
        if (!file.exists()) throw new IllegalArgumentException();
        try (InputStream fileIn = new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE);
                MiGzInputStream migzIn = new MiGzInputStream(fileIn);
                ObjectInputStream in = new ObjectInputStream(migzIn)) {
            return (T) in.readObject();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void serializeObjectToFileGZ(File file, Serializable object) {
        file.getAbsoluteFile().getParentFile().mkdirs();
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try (OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(tmpFile), IO_BUFFER_SIZE);
                MiGzOutputStream migzOut = new MiGzOutputStream(fileOut, Main.NUM_WORKER_THREADS, MIGZ_BLOCK_SIZE);
                ObjectOutputStream out = new ObjectOutputStream(migzOut)) {
            out.writeObject(object);
        } catch(Exception e) {
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

    public static void printWriteToFileGZ(File file, Consumer<PrintWriter> consumer) {
        file.getAbsoluteFile().getParentFile().mkdirs();
        File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile), IO_BUFFER_SIZE);
                MiGzOutputStream gzOut = new MiGzOutputStream(out, Main.NUM_WORKER_THREADS, MIGZ_BLOCK_SIZE);
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

    private static Reader fileReader(File file) throws FileNotFoundException {
        if (file.getName().endsWith(".gz")) {
            return new InputStreamReader(new MiGzInputStream(
                    new BufferedInputStream(new FileInputStream(file), IO_BUFFER_SIZE)));
        }
        return new FileReader(file);
    }

    public static void compressFile(File file) {
        if (!file.exists()) return;
        File gzipFile = new File(file.getPath() + ".gz");
        if (gzipFile.exists()) return;
        System.out.println(String.format("Compressing %s ...", file));
        File tmpFile = new File(gzipFile.getPath() + ".tmp");
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile), IO_BUFFER_SIZE);
                MiGzOutputStream gzOut = new MiGzOutputStream(out, Main.NUM_WORKER_THREADS, MIGZ_BLOCK_SIZE);
                FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[65536];
            int len;
            while ((len = in.read(buf)) != -1) {
                gzOut.write(buf, 0, len);
            }
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        if (!file.delete()) {
            throw new RuntimeException();
        }
        if (!tmpFile.renameTo(gzipFile)) {
            throw new RuntimeException();
        }
    }

}

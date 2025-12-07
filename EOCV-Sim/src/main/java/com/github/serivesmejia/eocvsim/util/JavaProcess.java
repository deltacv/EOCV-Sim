/*
 * Copyright (c) 2024 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A utility class for executing a Java process to run a main class within this project.
 */
public final class JavaProcess {

    public interface ProcessIOReceiver {
        void receiveLine(String line, boolean isError, int pid);
    }

    public static class SLF4JIOReceiver implements ProcessIOReceiver {
        private final Logger logger;

        public SLF4JIOReceiver(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void receiveLine(String line, boolean isError, int pid) {
            if (isError) logger.error(line);
            else logger.info(line);
        }
    }

    // ------------------------------------------------------------------------

    private JavaProcess() {
    }

    private static int count;
    private static ConcurrentLinkedQueue<Process> subprocesses = new ConcurrentLinkedQueue<>();

    public static boolean killSubprocessesOnExit = true;
    private static final Logger logger = LoggerFactory.getLogger(JavaProcess.class);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!killSubprocessesOnExit) return;

            for (Process p : subprocesses) {
                if (p.isAlive()) {
                    logger.info("SHUTDOWN - Killing subprocess with PID {}", p.pid());
                    p.destroyForcibly();
                }
            }
        }, "JavaProcess-ShutdownHook"));
    }

    // ========================================================================
    // ASYNC VERSION (MAIN VERSION)
    // ========================================================================
    public static CompletableFuture<Integer> execClasspathAsync(
            Class<?> klass,
            ProcessIOReceiver receiver,
            String classpath,
            List<String> jvmArgs,
            List<String> args
    ) throws IOException {

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String className = klass.getName();

        List<String> command = new LinkedList<>();
        command.add(javaBin);
        if (jvmArgs != null) command.addAll(jvmArgs);
        command.add("-cp");
        command.add(classpath);
        command.add(className);
        if (args != null) command.addAll(args);

        int processIndex = count++;
        logger.info("Executing Java process #{}: {}", processIndex, className);
        logger.debug("Java Binary: {}", javaBin);
        logger.debug("JVM Args: {}", jvmArgs);
        logger.debug("Program Args: {}", args);
        logger.debug("Classpath: {}", classpath);

        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        long pid = process.pid();

        subprocesses.add(process);

        // No receiver → inherit IO → return exit future
        if (receiver == null) {
            pb.inheritIO();
            logger.info("Process #{} running with inherited IO, PID {}", processIndex, pid);
            return process.onExit().thenApply(Process::exitValue);
        }

        logger.info("Process #{} started with PID {}, routing IO to {}",
                processIndex, pid, receiver.getClass().getSimpleName());

        // ONE thread for reading stdout + stderr
        Thread ioThread = new Thread(() -> handleIO(process, receiver), "JavaProcess-IO-" + pid);
        ioThread.start();

        // Exit code future
        CompletableFuture<Integer> exitFuture = new CompletableFuture<>();

        // When process exits, wait for IO thread to finish draining streams
        process.onExit().thenAccept(p -> {
            try {
                ioThread.join(); // no exitWatcher needed
                exitFuture.complete(p.exitValue());
            } catch (InterruptedException e) {
                exitFuture.completeExceptionally(e);
            }
        });

        return exitFuture;
    }

    // ========================================================================
    // IO HANDLER
    // ========================================================================
    private static void handleIO(Process process, ProcessIOReceiver receiver) {
        try (
                BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()))
        ) {
            boolean running = true;

            while (running) {
                running = process.isAlive();

                // Read stdout
                while (out.ready()) {
                    String line = out.readLine();
                    if (line == null) break;
                    receiver.receiveLine(line, false, (int) process.pid());
                }

                // Read stderr
                while (err.ready()) {
                    String line = err.readLine();
                    if (line == null) break;
                    receiver.receiveLine(line, true, (int) process.pid());
                }

                // Exit only when fully drained
                if (!running && (!out.ready() && !err.ready())) break;

                Thread.sleep(5);
            }

        } catch (Exception ignored) {
        }
    }

    // ========================================================================
    // SYNC VERSION (OPTIONAL, STILL SUPPORTED)
    // ========================================================================
    public static int execClasspath(
            Class<?> klass,
            ProcessIOReceiver receiver,
            String classpath,
            List<String> jvmArgs,
            List<String> args
    ) throws InterruptedException, IOException {

        return execClasspathAsync(klass, receiver, classpath, jvmArgs, args)
                .join(); // wait synchronously
    }

    public static int exec(Class<?> klass, List<String> jvmArgs, List<String> args)
            throws InterruptedException, IOException {
        return execClasspath(klass, null, System.getProperty("java.class.path"), jvmArgs, args);
    }

    public static int exec(Class<?> klass, ProcessIOReceiver ioReceiver, List<String> jvmArgs, List<String> args)
            throws InterruptedException, IOException {
        return execClasspath(klass, ioReceiver, System.getProperty("java.class.path"), jvmArgs, args);
    }
}

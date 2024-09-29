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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * A utility class for executing a Java process to run a main class within this project.
 */
public final class JavaProcess {

    public interface ProcessIOReceiver {
        void receive(InputStream in, InputStream err);
    }

    private JavaProcess() {}

    /**
     * Executes a Java process with the given class and arguments.
     * @param klass the class to execute
     * @param ioReceiver the receiver for the process' input and error streams (will use inheritIO if null)
     * @param classpath the classpath to use
     *                  (use System.getProperty("java.class.path") for the default classpath)
     * @param jvmArgs the JVM arguments to pass to the process
     * @param args the arguments to pass to the class
     * @return the exit value of the process
     * @throws InterruptedException if the process is interrupted
     * @throws IOException if an I/O error occurs
     */
    public static int execClasspath(Class klass, ProcessIOReceiver ioReceiver, String classpath, List<String> jvmArgs, List<String> args) throws InterruptedException, IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String className = klass.getName();

        List<String> command = new LinkedList<>();
        command.add(javaBin);
        if (jvmArgs != null) {
            command.addAll(jvmArgs);
        }
        command.add("-cp");
        command.add(classpath);
        command.add(className);
        if (args != null) {
            command.addAll(args);
        }

        ProcessBuilder builder = new ProcessBuilder(command);

        if (ioReceiver != null) {
            Process process = builder.start();
            ioReceiver.receive(process.getInputStream(), process.getErrorStream());
            killOnExit(process);

            process.waitFor();
            return process.exitValue();
        } else {
            builder.inheritIO();
            Process process = builder.start();
            killOnExit(process);

            process.waitFor();
            return process.exitValue();
        }
    }

    private static void killOnExit(Process process) {
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));
    }

    /**
     * Executes a Java process with the given class and arguments.
     * @param klass the class to execute
     * @param jvmArgs the JVM arguments to pass to the process
     * @param args the arguments to pass to the class
     * @return the exit value of the process
     * @throws InterruptedException if the process is interrupted
     * @throws IOException if an I/O error occurs
     */
    public static int exec(Class klass, List<String> jvmArgs, List<String> args) throws InterruptedException, IOException {
        return execClasspath(klass, null, System.getProperty("java.class.path"), jvmArgs, args);
    }

}
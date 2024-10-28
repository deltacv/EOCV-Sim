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

package io.github.deltacv.eocvsim.sandbox.restrictions

val dynamicLoadingPackageWhitelist = setOf(
    "java.lang",
    "java.lang.RuntimeException",
    "java.util",
    "java.awt",
    "javax.swing",
    "java.nio",
    "java.io.IOException",
    "java.io.File",
    "java.io.PrintStream",

    "kotlin",

    "com.github.serivesmejia.eocvsim",
    "io.github.deltacv.eocvsim",
    "org.firstinspires.ftc",
    "com.qualcomm",
    "org.opencv",
    "org.openftc",
    "android",

    "com.moandjiezana.toml",
    "net.lingala.zip4j",
    "com.google.gson",
    "com.google.jimfs",
    "org.slf4j",
    "com.apache.logging",
    "com.formdev.flatlaf"
)

val dynamicLoadingExactMatchBlacklist = setOf(
    "java.lang.Runtime"
)

val dynamicLoadingPackageBlacklist  = setOf(
    // System and Runtime Classes
    "java.lang.ProcessBuilder",
    "java.lang.reflect",

    // File and I/O Operations
    "java.io.RandomAccessFile",
    "java.nio.file.Paths",
    "java.nio.file.Files",
    "java.nio.file.FileSystems",

    // Thread and Process Management
    "java.lang.Process",

    // EOCV-Sim dangerous utils
    "com.github.serivesmejia.eocvsim.util.SysUtil",
    "com.github.serivesmejia.eocvsim.util.io",
    "com.github.serivesmejia.eocvsim.util.ClasspathScan",
    "com.github.serivesmejia.eocvsim.util.JavaProcess",
    "com.github.serivesmejia.eocvsim.util.ReflectUtil",
    "com.github.serivesmejia.eocvsim.util.FileExtKt",
    "com.github.serivesmejia.eocvsim.util.compiler",
    "com.github.serivesmejia.eocvsim.config",
)

val dynamicLoadingMethodBlacklist = setOf(
    "java.lang.System#load",
    "java.lang.System#loadLibrary",
    "java.lang.System#setSecurityManager",
    "java.lang.System#getSecurityManager",
    "java.lang.System#setProperty",
    "java.lang.System#clearProperty",
    "java.lang.System#setenv",

    "java.lang.Class#newInstance",
    "java.lang.Class#forName",

    "java.io.File#delete",
    "java.io.File#createNewFile",
    "java.io.File#mkdirs",
    "java.io.File#renameTo",
    "java.io.File#setExecutable",
    "java.io.File#setReadable",
    "java.io.File#setWritable",
    "java.io.File#setLastModified",
    "java.io.File#deleteOnExit",
    "java.io.File#setReadOnly",
    "java.io.File#setWritable",
    "java.io.File#setReadable",
    "java.io.File#setExecutable",
)
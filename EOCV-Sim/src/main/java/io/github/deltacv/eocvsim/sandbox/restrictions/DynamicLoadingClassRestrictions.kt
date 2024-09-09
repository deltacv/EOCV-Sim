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

val dynamicLoadingPackageBlacklist  = setOf(
    // System and Runtime Classes
    "java.lang.Runtime",
    "java.lang.ProcessBuilder",
    "java.lang.reflect",

    // File and I/O Operations
    "java.io.RandomAccessFile",
    "java.nio.file.Paths",
    "java.nio.file.Files",
    "java.nio.file.FileSystems",

    // Security and Encryption
    "javax.crypto.Cipher",
    "javax.crypto.KeyGenerator",
    "javax.crypto.SecretKey",

    // Security Management
    "java.security.AccessController",
    "java.security.KeyStore",
    "java.security.PrivilegedAction",

    // Thread and Process Management
    "java.lang.Process",

    // Dynamic Code Execution
    "javax.script.ScriptEngineManager",
    "javax.script.ScriptEngine",
    "sun.misc",

    // EOCV-Sim dangerous utils
    "com.github.serivesmejia.eocvsim.util.SysUtil",
    "com.github.serivesmejia.eocvsim.util.io",
    "com.github.serivesmejia.eocvsim.util.ClasspathScan",
    "com.github.serivesmejia.eocvsim.util.JavaProcess",
    "com.github.serivesmejia.eocvsim.util.ReflectUtil",
    "com.github.serivesmejia.eocvsim.util.FileExtKt",
    "com.github.serivesmejia.eocvsim.util.compiler",
    "com.github.serivesmejia.eocvsim.config",

    "io.github.deltacv.eocvsim.plugin.sandbox.nio.JimfsWatcher",
    "io.github.deltacv.eocvsim.plugin.sandbox.nio.ZipToJimfs"
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
    "java.io.File#renameTo"
)
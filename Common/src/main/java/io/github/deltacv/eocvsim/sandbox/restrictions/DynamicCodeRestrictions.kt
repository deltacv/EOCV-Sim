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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND.
 */

package io.github.deltacv.eocvsim.sandbox.restrictions

/**
 * === Dynamic Code Sandbox Restrictions ===
 *
 * These lists define the static sandbox used when loading plugin and dynamic code.
 * The sandbox is enforced by PluginClassLoader and MethodCallByteCodeChecker.
 *
 * IMPORTANT:
 * This is NOT a runtime security sandbox. All checks are static and name-based,
 * with limited bytecode inspection for direct method calls.
 *
 * ---------------------------------------------------------------------------
 * 1) dynamicCodePackageWhitelist
 * ---------------------------------------------------------------------------
 * Controls which classes a plugin is ALLOWED TO ATTEMPT to load.
 *
 * - If a class name does NOT match ANY entry in this list, loading is denied
 *   (unless the plugin has super access).
 *
 * NOTE:
 * This is not a strict package whitelist; any class whose name contains a
 * whitelisted string is considered allowed to proceed to further checks.
 *
 * AUTHORITY:
 * - Lowest authority list.
 * - Can be overridden by ALL blacklist mechanisms below.
 *
 * ---------------------------------------------------------------------------
 * 2) dynamicCodePackageBlacklist
 * ---------------------------------------------------------------------------
 * Denies access to entire namespaces considered dangerous.
 *
 * - If a class name contains a blacklisted package name, loading is denied.
 * - HOWEVER: if the class matched the whitelist, this blacklist is SKIPPED.
 *
 * NOTE:
 * This means whitelist entries OVERRIDE this blacklist.
 *
 * AUTHORITY:
 * - Higher authority than the whitelist
 * - Lower authority than exact-match and always-blacklisted entries
 *
 * ---------------------------------------------------------------------------
 * 3) dynamicCodeExactMatchBlacklist
 * ---------------------------------------------------------------------------
 * Denies access to specific high-risk classes regardless of whitelist status.
 *
 * - Only exact class name matches are denied.
 * - This list is checked even if the class is whitelisted.
 *
 * USE CASE:
 * - Blocking Runtime, Unsafe, MethodHandles, Robot, etc.
 *
 * AUTHORITY:
 * - Higher authority than whitelist and package blacklist
 * - Lower authority than always-blacklisted packages
 *
 * ---------------------------------------------------------------------------
 * 4) dynamicCodePackageAlwaysBlacklist
 * ---------------------------------------------------------------------------
 * Absolute deny list for entire namespaces.
 *
 * - These packages are ALWAYS denied.
 * - Cannot be overridden by whitelist or any other list.
 *
 * USE CASE:
 * - Internal host implementation packages
 * - Embedded or sensitive subsystems
 *
 * AUTHORITY:
 * - Highest authority for class-level access control
 *
 * ---------------------------------------------------------------------------
 * 5) dynamicCodeMethodBlacklist
 * ---------------------------------------------------------------------------
 * Denies specific method invocations at the bytecode level.
 *
 * - Enforced by MethodCallByteCodeChecker.
 * - Only applies to classes loaded from plugin JARs.
 * - Prevents direct invocation of forbidden methods.
 *
 * IMPORTANT LIMITATIONS:
 * - Does NOT apply to:
 *   - JDK / parent classloader classes
 *   - Dependency classpath JARs
 *   - Indirect calls (reflection, MethodHandles, invokedynamic)
 *
 * AUTHORITY:
 * - Independent of class/package checks
 * - Final line of defense for direct method calls
 *
 * ---------------------------------------------------------------------------
 * SUMMARY OF AUTHORITY (lowest → highest)
 * ---------------------------------------------------------------------------
 * 1. dynamicCodePackageWhitelist
 * 2. dynamicCodePackageBlacklist
 * 3. dynamicCodeExactMatchBlacklist
 * 4. dynamicCodePackageAlwaysBlacklist
 *
 * dynamicCodeMethodBlacklist is enforced separately at bytecode load time.
 *
 * ---------------------------------------------------------------------------
 * SECURITY NOTE:
 * ---------------------------------------------------------------------------
 * This sandbox is designed to prevent accidental or low-effort misuse.
 * It does NOT provide strong isolation against malicious or hostile code.
 */

val dynamicCodePackageWhitelist = setOf(
    // Core language & collections
    "java.lang",
    "java.util",

    // UI (restricted by blacklist below)
    "java.awt",
    "javax.swing",

    // Limited IO / NIO (restricted by blacklist below)
    "java.nio",
    "java.io.IOException",
    "java.io.PrintStream",

    // Kotlin stdlib (restricted by blacklist below)
    "kotlin",

    // FTC / OpenCV ecosystem
    "org.firstinspires.ftc",
    "com.qualcomm",
    "org.opencv",
    "org.openftc",
    "android",

    // API
    "io.github.deltacv.eocvsim.plugin",
    "io.github.deltacv.eocvsim.sandbox",

    // Third-party libs explicitly allowed
    "com.moandjiezana.toml",
    "net.lingala.zip4j",
    "com.google.gson",
    "com.google.jimfs",
    "org.slf4j",
    "com.apache.logging",
    "com.formdev.flatlaf"
)

val dynamicCodeExactMatchBlacklist = setOf(
    // JVM / runtime control
    "java.lang.Runtime",
    "java.lang.System",
    "java.lang.ProcessHandle",

    // Unsafe & internals
    "sun.misc.Unsafe",
    "jdk.internal.misc.Unsafe",

    // MethodHandles / invokedynamic
    "java.lang.invoke.MethodHandles",
    "java.lang.invoke.MethodHandle",
    "java.lang.invoke.CallSite",

    // Desktop / OS interaction
    "java.awt.Robot",
    "java.awt.Desktop",
    "java.awt.Toolkit",

    // Native bridges
    "com.sun.jna"
)

val dynamicCodePackageBlacklist = setOf(
    // Reflection & invocation
    "java.lang.reflect",
    "sun.reflect",
    "sun.invoke",

    // JVM internals
    "jdk.internal",
    "jdk.internal.vm",
    "jdk.internal.loader",

    // Process execution
    "java.lang.Process",
    "java.lang.ProcessBuilder",

    // Threading / async / DoS
    "java.lang.Thread",
    "java.lang.ThreadGroup",
    "java.util.concurrent",
    "java.util.Timer",
    "java.util.TimerTask",

    // Networking
    "java.net",
    "javax.net",
    "sun.net",

    // File & filesystem
    "java.io.RandomAccessFile",
    "java.io.FileInputStream",
    "java.io.FileOutputStream",
    "java.io.FileReader",
    "java.io.FileWriter",
    "java.io.BufferedReader",
    "java.io.BufferedWriter",
    "java.io.InputStreamReader",
    "java.io.OutputStreamWriter",

    // NIO filesystem & memory
    "java.nio.file",
    "java.nio.channels.FileChannel",
    "java.nio.MappedByteBuffer",

    // Serialization / deserialization
    "java.io.ObjectInputStream",
    "java.io.ObjectOutputStream",
    "java.io.ObjectStreamClass",
    "java.io.Externalizable",
    "java.io.Serializable",

    // Classloading & modules
    "java.lang.ClassLoader",
    "java.lang.Module",
    "java.lang.Package",

    // Zip / JAR inspection
    "java.util.jar",
    "java.util.zip",

    // AWT escape surfaces
    "java.awt.event",
    "java.awt.dnd",
    "java.awt.datatransfer",
    "java.awt.peer",

    // Native wrappers
    "sun.nio",
    "sun.awt",
    "sun.font",

    // Kotlin escape hatches
    "kotlin.reflect",
    "kotlin.system",
    "kotlin.io.path"
)

val dynamicCodePackageAlwaysBlacklist = setOf(
    // Embedded PaperVision (avoid duplicate / unsafe classloading)
    "io.github.deltacv.papervision",

    // Logging system isolation
    "org.apache.logging.log4j"
)

val dynamicCodeMethodBlacklist = setOf(
    // JVM / native control
    "java.lang.System#load",
    "java.lang.System#loadLibrary",
    "java.lang.System#exit",
    "java.lang.System#setSecurityManager",
    "java.lang.System#getSecurityManager",
    "java.lang.System#setProperty",
    "java.lang.System#clearProperty",
    "java.lang.Runtime#addShutdownHook",

    // Reflection & class tricks
    "java.lang.Class#newInstance",
    "java.lang.Class#forName",
    "java.lang.Class#getDeclaredFields",
    "java.lang.Class#getDeclaredMethods",
    "java.lang.Class#getDeclaredConstructors",
    "java.lang.Class#getClassLoader",
    "java.lang.Class#getModule",
    "java.lang.Class#getProtectionDomain",

    // Thread control
    "java.lang.Thread#start",
    "java.lang.Thread#stop",
    "java.lang.Thread#sleep",
    "java.lang.Thread#setDaemon",

    // File operations
    "java.io.File#delete",
    "java.io.File#createNewFile",
    "java.io.File#mkdirs",
    "java.io.File#renameTo",
    "java.io.File#deleteOnExit",
    "java.io.File#setExecutable",
    "java.io.File#setReadable",
    "java.io.File#setWritable",
    "java.io.File#setReadOnly",
    "java.io.File#setLastModified",
    "java.io.File#list",
    "java.io.File#listFiles",
    "java.io.File#listRoots",
    "java.io.File#canRead",
    "java.io.File#canWrite",
    "java.io.File#canExecute",
    "java.io.File#length",
    "java.io.File#getTotalSpace",
    "java.io.File#getFreeSpace",
    "java.io.File#getUsableSpace",
    "java.io.File#getAbsolutePath",
    "java.io.File#getCanonicalPath",
    "java.io.File#getParentFile",
    "java.io.File#exists",
    "java.io.File#isDirectory",
    "java.io.File#isFile",
    "java.io.File#isHidden",
    "java.io.File#lastModified"
)

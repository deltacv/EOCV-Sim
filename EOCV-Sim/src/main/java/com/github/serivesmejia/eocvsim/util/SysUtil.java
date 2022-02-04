/*
 * Copyright (c) 2021 Sebastian Erives
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

import com.github.serivesmejia.eocvsim.util.io.EOCVSimFolder;
import org.opencv.core.Core;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SysUtil {

    static Logger logger = LoggerFactory.getLogger(SysUtil.class);

    public static OperatingSystem OS = SysUtil.getOS();
    public static int MB = 1024 * 1024;
    public static String GH_NATIVE_LIBS_URL = "https://github.com/serivesmejia/OpenCVNativeLibs/raw/master/";

    public static OperatingSystem getOS() {
        String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (osName.contains("nux")) {
            return OperatingSystem.LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return OperatingSystem.MACOS;
        }

        return OperatingSystem.UNKNOWN;
    }

    public static void copyStream(File inFile, OutputStream out) throws IOException {
        try (InputStream in = new FileInputStream(inFile)) {
            copyStream(in, out);
        }
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        int cbBuffer = Math.min(4096, in.available());
        byte[] buffer = new byte[cbBuffer];

        while(true) {
            int cbRead = in.read(buffer);
            if(cbRead <= 0) break;

            out.write(buffer, 0, cbRead);
        }
    }

    public static CopyFileIsData copyFileIs(InputStream is, File toPath, boolean replaceIfExisting) throws IOException {

        boolean alreadyExists = true;

        if (toPath.exists()) {
            if (replaceIfExisting) {
                Files.copy(is, toPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                alreadyExists = false;
            }
        } else {
            Files.copy(is, toPath.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        is.close();

        CopyFileIsData data = new CopyFileIsData();
        data.alreadyExists = alreadyExists;
        data.file = toPath;

        return data;

    }

    public static CopyFileIsData copyFileIsTemp(InputStream is, String fileName, boolean replaceIfExisting) throws IOException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        File tempFile = new File(tmpDir + File.separator + fileName);

        return copyFileIs(is, tempFile, replaceIfExisting);
    }

    public static long getMemoryUsageMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / MB;
    }

    public static String loadIsStr(InputStream is, Charset charset) throws UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(is, String.valueOf(charset)))
                .lines().collect(Collectors.joining("\n"));
    }

    public static String loadFileStr(File f) {
        String content = "";

        try {
            content = new String(Files.readAllBytes(f.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }

    public static void replaceStrInFile(File f, String target, String replacement) {
        String fileContents = loadFileStr(f);
        saveFileStr(f, fileContents.replace(target, replacement));
    }

    public static boolean saveFileStr(File f, String contents) {
        try {
            FileWriter fw = new FileWriter(f);
            fw.append(contents);
            fw.close();
            return true;
        } catch (IOException e) {
            logger.error("Exception while trying to save file " + f.getAbsolutePath(), e);
            return false;
        }
    }

    public static void download(String url, String fileName) throws Exception {
        try (InputStream in = URI.create(url).toURL().openStream()) {
            Files.copy(in, Paths.get(fileName));
        }
    }

    public static File getAppData() {
        return new File(System.getProperty("user.home") + File.separator);
    }

    public static File getEOCVSimFolder() {
        return EOCVSimFolder.INSTANCE;
    }

    public static Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    public static List<File> filesUnder(File parent, Predicate<File> predicate) {
        ArrayList<File> result = new ArrayList<>();

        if(parent.isDirectory()) {
            for(File child : parent.listFiles()) {
                result.addAll(filesUnder(child, predicate));
            }
        } else if(parent.exists() && (predicate != null && predicate.test(parent))) {
            result.add(parent.getAbsoluteFile());
        }

        return result;
    }

    public static List<File> filesUnder(File parent, String extension) {
        return filesUnder(parent, (f) -> f.getName().endsWith(extension));
    }

    public static List<File> filesUnder(File parent) {
        return filesUnder(parent, (f) -> true);
    }

    public static List<File> filesIn(File parent, Predicate<File> predicate) {
        ArrayList<File> result = new ArrayList<>();

        if(!parent.exists()) return result;

        if(parent.isDirectory()) {
            for(File f : parent.listFiles()) {
                if(predicate != null && predicate.test(f))
                    result.add(f);
            }
        } else {
            if(predicate != null && predicate.test(parent))
                result.add(parent);
        }

        return result;
    }

    public static List<File> filesIn(File parent, String extension) {
        return filesIn(parent, (f) -> f.getName().endsWith(extension));
    }

    public static void deleteFilesUnder(File parent, Predicate<File> predicate) {
        for(File file : parent.listFiles()) {
            if(file.isDirectory())
                deleteFilesUnder(file, predicate);

            if(predicate != null) {
                if(predicate.test(file)) file.delete();
            } else {
                file.delete();
            }
        }
    }

    public static void deleteFilesUnder(File parent) {
        deleteFilesUnder(parent, null);
    }

    public static boolean migrateFile(File oldFile, File newFile) {
        if(newFile.exists() || !oldFile.exists()) return false;

        logger.info("Migrating old file " + oldFile.getAbsolutePath() + "  to " + newFile.getAbsolutePath());

        try {
            Files.move(oldFile.toPath(), newFile.toPath());
        } catch (IOException e) {
            logger.warn("Failed to migrate old file " + oldFile.getAbsolutePath());
            return false;
        }

        return true;
    }

    public static File getRelativePath(File root, File child) {
        File result = new File("");

        while(!root.equals(child)) {
            File parent = child.getParentFile();
            result = new File(new File(child.getName()), result.getPath());

            if(parent == null) break;

            child = parent;
        }

        return result;
    }

    public static List<File> getClasspathFiles() {
        String[] classpaths = System.getProperty("java.class.path").split(File.pathSeparator);
        ArrayList<File> files = new ArrayList<>();

        for(String path : classpaths) {
            files.add(new File(path));
        }

        return files;
    }

    public static CommandResult runShellCommand(String command) {
        CommandResult result = new CommandResult();

        ProcessBuilder processBuilder = new ProcessBuilder();
        if (OS == OperatingSystem.WINDOWS) {
            processBuilder.command("cmd.exe", "/c", command);
        } else {
            processBuilder.command("sh", "-c", command);
        }

        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = "";
            StringBuilder message = new StringBuilder();

            while((line = reader.readLine()) != null) {
                message.append(line);
            }

            result.exitCode = process.waitFor();

            result.output = message.toString();
        } catch (IOException | InterruptedException e) {
            result.output = StrUtil.fromException(e);
            result.exitCode = -1;
        }

        return result;
    }

    public enum OperatingSystem {
        WINDOWS,
        LINUX,
        MACOS,
        UNKNOWN
    }

    public static class CopyFileIsData {
        public File file = null;
        public boolean alreadyExists = false;
    }

    public static class CommandResult {
        public String output = "";
        public int exitCode = 0;
    }

}

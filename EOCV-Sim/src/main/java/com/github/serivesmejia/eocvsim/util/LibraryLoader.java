/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.util;

import java.io.IOException;

import org.opencv.core.Core;
import org.wpilib.math.jni.WPIMathJNI;
import org.wpilib.net.WPINetJNI;
import org.wpilib.util.WPIUtilJNI;
import org.wpilib.vision.apriltag.jni.AprilTagJNI;
import org.wpilib.vision.camera.CameraServerJNI;
import org.wpilib.vision.camera.OpenCvLoader;

public class LibraryLoader {
    public record Result(boolean success, Throwable error) {}

    public static Result loadLibraries() {
        WPIUtilJNI.Helper.setExtractOnStaticLoad(false);
        AprilTagJNI.Helper.setExtractOnStaticLoad(false);
        WPIMathJNI.Helper.setExtractOnStaticLoad(false);
        WPINetJNI.Helper.setExtractOnStaticLoad(false);
        CameraServerJNI.Helper.setExtractOnStaticLoad(false);
        OpenCvLoader.Helper.setExtractOnStaticLoad(false);

        try {
            CombinedRuntimeLoader.loadLibraries(LibraryLoader.class, "wpiutiljni");
            WPIUtilJNI.checkMsvcRuntime();

            CombinedRuntimeLoader.loadLibraries(LibraryLoader.class,
                    "apriltagjni", "wpimathjni", "wpinetjni", "cscorejni");

            CombinedRuntimeLoader.loadLibraries(LibraryLoader.class, Core.NATIVE_LIBRARY_NAME);
        } catch(Exception | UnsatisfiedLinkError e) {
            return new Result(false, e);
        }

        return new Result(true, null);
    }
}
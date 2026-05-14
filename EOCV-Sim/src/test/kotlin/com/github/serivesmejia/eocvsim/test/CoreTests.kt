/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

@file:Suppress("UNUSED")

package com.github.serivesmejia.eocvsim.test

import com.github.serivesmejia.eocvsim.util.LibraryLoader
import io.kotest.core.spec.style.StringSpec
import org.opencv.core.Mat
import org.wpilib.vision.apriltag.jni.AprilTagJNI

class LibrariesTest : StringSpec({
    "Loading Libraries" {
        LibraryLoader.loadLibraries()
    }

    "Creating a Mat" {
        Mat()
    }

    "Create AprilTag Detector" {
        val handle = AprilTagJNI.createDetector()
        assert(handle != 0.toLong())
    }
})
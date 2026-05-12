/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

@file:Suppress("UNUSED")

package com.github.serivesmejia.eocvsim.test

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.util.LibraryLoader
import io.kotest.core.spec.style.StringSpec
import org.opencv.core.Mat
import org.openftc.apriltag.AprilTagDetectorJNI

class OpenCvTest : StringSpec({
    "Loading native library" {
        LibraryLoader.loadLibraries()
    }

    "Creating a Mat" {
        Mat()
    }
})

class AprilTagsTest : StringSpec({
    "Create AprilTag detector" {
        val detector = AprilTagDetectorJNI.createApriltagDetector(
                AprilTagDetectorJNI.TagFamily.TAG_36h11.string,
                0f, 3
        )

        println("Created detector $detector")
    }
})

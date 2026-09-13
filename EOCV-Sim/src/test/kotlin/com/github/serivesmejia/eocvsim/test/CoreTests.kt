/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

@file:Suppress("UNUSED")

package com.github.serivesmejia.eocvsim.test

import com.github.serivesmejia.eocvsim.util.LibraryLoader
import io.kotest.core.spec.style.DescribeSpec
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

class TestingImplementationPlan : DescribeSpec({
    describe("unit testing implementation plan") {
        it("covers pure version and compatibility logic in ParsedVersion").config(enabled = false) {
            // TODO: Move to Common/src/test/kotlin/org/deltacv/common/util/ParsedVersionSpec.kt
            // Verify major/minor/patch parsing, patch defaults, compares across pre-release semantics,
            // and rejection of invalid string formats.
        }

        it("covers plugin authority cache and TOML serialization safety").config(enabled = false) {
            // TODO: Move to Common/src/test/kotlin/org/deltacv/eocvsim/plugin/security/AuthorityFetcherSpec.kt
            // Verify: single authority table is not duplicated, timestamp is updated once, invalid TOML is reset,
            // cache entries expire after TTL, and duplicate authority names do not re-define the same table.
        }

        it("covers plugin repository artifact parsing and dependency checks").config(enabled = false) {
            // TODO: Move to EOCV-Sim/src/test/kotlin/org/deltacv/eocvsim/plugin/repository/PluginRepositorySpec.kt
            // Verify: parseArtifact handles malformed input, findArtifactRootUrl builds the expected Maven layout,
            // checkForUpdates compares current vs latest correctly, and invalid repositories return null.
        }

        it("covers file watch filtering and recursive directory registration").config(enabled = false) {
            // TODO: Move to EOCV-Sim/src/test/kotlin/com/github/serivesmejia/eocvsim/util/io/FileWatcherSpec.kt
            // Verify: extension matching is case-insensitive, recursive watch registration works for nested folders,
            // and change callbacks only fire for expected file types.
        }

        it("covers the plugin signing and verification pipeline").config(enabled = false) {
            // TODO: Move to Common/src/test/kotlin/org/deltacv/eocvsim/plugin/security/PluginSigningSpec.kt
            // Verify: parsePem strips PEM wrappers, public key parsing succeeds, signature verification accepts
            // matching keys and rejects tampered class data.
        }
    }
})
/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.openftc.apriltag;

import org.wpilib.vision.apriltag.AprilTagDetection;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Internal cache for AprilTag detection arrays to simulate pointer-based returns
 */
class AprilTagDetectionCache
{
    private static final Map<Long, AprilTagDetection[]> cache = new HashMap<>();
    private static final AtomicLong pointerCounter = new AtomicLong(1);

    /**
     * Cache a detection array and return a fake pointer ID
     */
    static long cacheDetections(AprilTagDetection[] detections) {
        long fakePtr = pointerCounter.getAndIncrement();
        cache.put(fakePtr, detections);
        return fakePtr;
    }

    /**
     * Retrieve cached detections by fake pointer ID
     */
    static AprilTagDetection[] getDetections(long fakePtr) {
        return cache.get(fakePtr);
    }

    /**
     * Remove cached detections by fake pointer ID
     */
    static void freeDetections(long fakePtr) {
        cache.remove(fakePtr);
    }
}


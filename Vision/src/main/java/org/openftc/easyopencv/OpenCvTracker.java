/*
 * Copyright (c) 2019 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.openftc.easyopencv;

import org.opencv.core.Mat;

public abstract class OpenCvTracker {
    private final Mat mat = new Mat();

    public abstract Mat processFrame(Mat input);

    protected final Mat processFrameInternal(Mat input) {
        input.copyTo(mat);
        return processFrame(mat);
    }
}

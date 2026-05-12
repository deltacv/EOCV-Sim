/*
 * Copyright (c) 2020 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.openftc.easyopencv;

import org.opencv.core.Mat;

public abstract class TimestampedOpenCvPipeline extends OpenCvPipeline
{
    private long timestamp;

    @Override
    public final Mat processFrame(Mat input)
    {
        return processFrame(input, timestamp);
    }

    public abstract Mat processFrame(Mat input, long captureTimeNanos);

    protected void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }
}

package io.github.deltacv.vision.util;

import org.opencv.core.Mat;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.TimestampedOpenCvPipeline;

public abstract class ViewportPipeline extends TimestampedOpenCvPipeline implements OpenCvCamera {

    protected final FrameQueue queue;

    private final Mat emptyMat;

    protected ViewportPipeline(int maxQueueItems) {
        queue = new FrameQueue(maxQueueItems + 3);
        emptyMat = queue.takeMat();
    }

    protected ViewportPipeline() {
        this(10);
    }

    @Override
    public Mat processFrame(Mat input, long captureTimeNanos) {
        Mat output = queue.poll();
        if(output == null) {
            output = emptyMat;
        }

        return output;
    }

    public FrameQueue getOutputQueue() {
        return queue;
    }

}
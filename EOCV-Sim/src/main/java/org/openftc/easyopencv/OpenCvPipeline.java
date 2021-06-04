package org.openftc.easyopencv;

import org.opencv.core.Mat;

public abstract class OpenCvPipeline {

    public abstract Mat processFrame(Mat input);

    public void onViewportTapped() { }

    public void init(Mat mat) { }

}
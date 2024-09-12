package io.github.deltacv.eocvsim.pipeline;

import io.github.deltacv.eocvsim.stream.ImageStreamer;
import org.opencv.core.Mat;
import org.openftc.easyopencv.OpenCvPipeline;

public abstract class StreamableOpenCvPipeline extends OpenCvPipeline {

    protected ImageStreamer streamer = null;

    public void streamFrame(int id, Mat image, Integer cvtCode) {
        streamer.sendFrame(id, image, cvtCode);
    }

}

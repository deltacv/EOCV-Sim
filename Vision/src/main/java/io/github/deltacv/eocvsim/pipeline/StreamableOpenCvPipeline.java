package io.github.deltacv.eocvsim.pipeline;

import io.github.deltacv.eocvsim.stream.ImageStreamer;
import org.opencv.core.Mat;
import org.openftc.easyopencv.OpenCvPipeline;

/**
 * A pipeline that can stream frames to an {@link ImageStreamer}.
 * This class adds a streamFrame method that allows the pipeline to send frames to the streamer.
 */
public abstract class StreamableOpenCvPipeline extends OpenCvPipeline {

    private Object streamerLock = new Object();
    private ImageStreamer streamer = null;

    public void streamFrame(int id, Mat image, Integer cvtCode) {
        synchronized (streamerLock) {
            if (streamer != null) {
                streamer.sendFrame(id, image, cvtCode);
            }
        }
    }

    public void setStreamer(ImageStreamer streamer) {
        synchronized (streamerLock) {
            this.streamer = streamer;
        }
    }

    public ImageStreamer getStreamer() {
        synchronized (streamerLock) {
            return streamer;
        }
    }

}
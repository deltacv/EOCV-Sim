package io.github.deltacv.eocvsim.pipeline;

import io.github.deltacv.eocvsim.stream.ImageStreamer;
import org.opencv.core.Mat;
import org.openftc.easyopencv.OpenCvPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pipeline that can stream frames to an {@link ImageStreamer}.
 * This class adds a streamFrame method that allows the pipeline to send frames to the streamer.
 */
public abstract class StreamableOpenCvPipeline extends OpenCvPipeline {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Object streamerLock = new Object();
    private ImageStreamer streamer = null;

    private boolean hasLoggedStreamerAbsence = false;

    public void streamFrame(int id, Mat image, Integer cvtCode) {
        synchronized (streamerLock) {
            if (streamer != null) {
                streamer.sendFrame(id, image, cvtCode);
            } else {
                if (!hasLoggedStreamerAbsence) {
                    logger.warn("No ImageStreamer set for this StreamableOpenCvPipeline. Frames will not be streamed.");
                    hasLoggedStreamerAbsence = true;
                }
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
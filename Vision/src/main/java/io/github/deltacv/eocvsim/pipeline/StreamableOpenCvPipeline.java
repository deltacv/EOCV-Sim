/*
 * Copyright (c) 2026 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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
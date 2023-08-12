package io.github.deltacv.vision.external.source;

import org.opencv.core.Mat;

public interface VisionSourced {
    void onNewFrame(Mat frame, long timestamp);
}

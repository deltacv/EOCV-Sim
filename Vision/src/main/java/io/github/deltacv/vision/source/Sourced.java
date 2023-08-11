package io.github.deltacv.vision.source;

import org.opencv.core.Mat;

public interface Sourced {
    void onNewFrame(Mat frame, long timestamp);
}

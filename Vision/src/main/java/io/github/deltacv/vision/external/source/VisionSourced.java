package io.github.deltacv.vision.external.source;

import org.opencv.core.Mat;

public interface VisionSourced {

    default void onFrameStart() {}

    void stop();

    void onNewFrame(Mat frame, long timestamp);

}

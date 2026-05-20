/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.vision.external.source;

import org.opencv.core.Mat;

public interface FrameReceiver {
    default void onFrameStart() {}
    void consume(Mat frame, long timestamp);

    void stop();
}


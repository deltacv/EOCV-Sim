/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.vision.external.source;

import org.opencv.core.Size;

public interface VisionSource {

    int init();

    CameraControlMap getControlMap();

    boolean start(Size requestedSize);

    boolean attach(FrameReceiver sourced);
    boolean remove(FrameReceiver sourced);

    boolean stop();

    boolean close();

}


/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.vision.external.source;

import org.opencv.core.Size;

public interface VisionSource {

    int check();

    CameraControlMap getControlMap();

    boolean start(Size size);

    boolean attach(FrameReceiver sourced);
    boolean remove(FrameReceiver sourced);

    boolean stop();

    boolean close();

}


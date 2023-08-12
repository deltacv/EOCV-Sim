package io.github.deltacv.vision.external.source;

import org.opencv.core.Size;

public interface VisionSource {

    int init();

    boolean start(Size requestedSize);

    boolean attach(VisionSourced sourced);
    boolean remove(VisionSourced sourced);

    boolean stop();

    boolean close();

}

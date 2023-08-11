package io.github.deltacv.vision.source;

import org.opencv.core.Size;

public interface Source {

    int init();

    boolean start(Size requestedSize);

    boolean attach(Sourced sourced);
    boolean remove(Sourced sourced);

    boolean stop();

    boolean close();

}

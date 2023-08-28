package io.github.deltacv.common.image;

import org.opencv.core.Mat;

public interface MatPoster {

    default void post(Mat m) {
        post(m, null);
    }

    void post(Mat m, Object context);

}

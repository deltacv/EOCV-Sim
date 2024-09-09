package io.github.deltacv.common.image;

import org.opencv.core.Mat;

/**
 * A class that allows for posting Mats to a certain place.
 */
public interface MatPoster {

    /**
     * Post a Mat to a certain place.
     * @param m The Mat to post
     */
    default void post(Mat m) {
        post(m, null);
    }

    /**
     * Post a Mat to a certain place with a context object.
     * @param m The Mat to post
     * @param context The context object
     */
    void post(Mat m, Object context);

}

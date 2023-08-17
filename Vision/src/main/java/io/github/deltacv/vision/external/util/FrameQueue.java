package io.github.deltacv.vision.external.util;

import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue;
import org.opencv.core.Mat;
import org.openftc.easyopencv.MatRecycler;

import java.util.concurrent.ArrayBlockingQueue;

public class FrameQueue {

    private final EvictingBlockingQueue<Mat> viewportQueue;
    private final MatRecycler matRecycler;

    public FrameQueue(int maxQueueItems) {
        viewportQueue = new EvictingBlockingQueue<>(new ArrayBlockingQueue<>(maxQueueItems));
        matRecycler = new MatRecycler(maxQueueItems + 2);

        viewportQueue.setEvictAction(this::evict);
    }

    public Mat takeMatAndPost() {
        Mat mat = matRecycler.takeMatOrNull();
        viewportQueue.add(mat);

        return mat;
    }


    public Mat takeMat() {
        return matRecycler.takeMatOrNull();
    }

    public Mat poll() {
        Mat mat = viewportQueue.poll();

        if(mat != null) {
            evict(mat);
        }

        return mat;
    }

    private void evict(Mat mat) {
        if(mat instanceof MatRecycler.RecyclableMat) {
            matRecycler.returnMat((MatRecycler.RecyclableMat) mat);
        }
    }

}

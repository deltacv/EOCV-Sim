/*
 * Copyright (c) 2023 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

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

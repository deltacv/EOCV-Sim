/*
 * Copyright (c) 2021 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.gui.util;

import com.github.serivesmejia.eocvsim.util.fps.FpsCounter;
import org.firstinspires.ftc.robotcore.internal.collections.EvictingBlockingQueue;
import org.opencv.core.Mat;
import org.openftc.easyopencv.MatRecycler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;

public class MatPoster {

    private final ArrayList<Postable> postables = new ArrayList<>();

    private final EvictingBlockingQueue<Mat> postQueue;
    private final MatRecycler matRecycler;

    private final String name;

    private final Thread posterThread;

    public final FpsCounter fpsCounter = new FpsCounter();

    private final Object lock = new Object();

    private volatile boolean paused = false;

    private volatile boolean hasPosterThreadStarted = false;

    Logger logger;

    public static MatPoster createWithoutRecycler(String name, int maxQueueItems) {
        return new MatPoster(name, maxQueueItems, null);
    }

    public MatPoster(String name, int maxQueueItems) {
        this(name, new MatRecycler(maxQueueItems + 2));
    }

    public MatPoster(String name, MatRecycler recycler) {
        this(name, recycler.getSize(), recycler);
    }

    public MatPoster(String name, int maxQueueItems, MatRecycler recycler) {
        postQueue = new EvictingBlockingQueue<>(new ArrayBlockingQueue<>(maxQueueItems));
        matRecycler = recycler;
        posterThread = new Thread(new PosterRunnable(), "MatPoster-" + name + "-Thread");

        this.name = name;

        logger = LoggerFactory.getLogger("MatPoster-" + name);

        postQueue.setEvictAction(this::evict); //release mat and return it to recycler if it's dropped by the EvictingBlockingQueue
    }

    public void post(Mat m) {
        if (m == null || m.empty()) {
            logger.warn("Tried to post empty or null mat, skipped this frame.");
            return;
        }

        if (matRecycler != null) {
            if(matRecycler.getAvailableMatsAmount() < 1) {
                //evict one if we don't have any available mats in the recycler
                evict(postQueue.poll());
            }

            MatRecycler.RecyclableMat recycledMat = matRecycler.takeMat();
            m.copyTo(recycledMat);

            postQueue.offer(recycledMat);
        } else {
            postQueue.offer(m);
        }
    }

    public void synchronizedPost(Mat m) {
        synchronize(() -> post(m));
    }

    public Mat pull() throws InterruptedException {
        synchronized(lock) {
            return postQueue.take();
        }
    }

    public void clearQueue() {
        if(postQueue.size() == 0) return;

        synchronized(lock) {
            postQueue.clear();
        }
    }

    public void synchronize(Runnable runn) {
        synchronized(lock) {
            runn.run();
        }
    }

    public void addPostable(Postable postable) {
        //start mat posting thread if it hasn't been started yet
        if (!posterThread.isAlive() && !hasPosterThreadStarted) {
            posterThread.start();
        }

        postables.add(postable);
    }

    public void stop() {
        logger.info("Destroying...");

        posterThread.interrupt();

        for (Mat m : postQueue) {
            if (m != null) {
                if(m instanceof MatRecycler.RecyclableMat) {
                    ((MatRecycler.RecyclableMat)m).returnMat();
                }
            }
        }

        matRecycler.releaseAll();
    }

    private void evict(Mat m) {
        if (m instanceof MatRecycler.RecyclableMat) {
            ((MatRecycler.RecyclableMat) m).returnMat();
        }
        m.release();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean getPaused() {
        synchronized(lock) {
            return paused;
        }
    }

    public String getName() {
        return name;
    }

    public interface Postable {
        void post(Mat m);
    }

    private class PosterRunnable implements Runnable {

        private Mat postableMat = new Mat();

        @Override
        public void run() {
            hasPosterThreadStarted = true;

            while (!Thread.interrupted()) {

                while(paused && !Thread.currentThread().isInterrupted()) {
                    Thread.yield();
                }

                if (postQueue.size() == 0 || postables.size() == 0) continue; //skip if we have no queued frames

                synchronized(lock) {
                    fpsCounter.update();

                    try {
                        Mat takenMat = postQueue.take();

                        for (Postable postable : postables) {
                            takenMat.copyTo(postableMat);
                            postable.post(postableMat);
                        }

                        takenMat.release();

                        if (takenMat instanceof MatRecycler.RecyclableMat) {
                            ((MatRecycler.RecyclableMat) takenMat).returnMat();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    } catch (Exception ex) { }
                }

            }

            logger.warn("Thread interrupted (" + Integer.toHexString(hashCode()) + ")");
        }
    }

}
/*
 * Copyright (c) 2023 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.deltacv.vision.external.source;

import org.deltacv.vision.external.util.ThrowableHandler;
import org.deltacv.vision.external.util.Timestamped;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public abstract class VisionSourceBase implements VisionSource {

    private final Object lock = new Object();

    ArrayList<FrameReceiver> frameReceivers = new ArrayList<>();

    VisionSourceBaseHelperThread helperThread;

    public VisionSourceBase(ThrowableHandler throwableHandler) {
        helperThread = new VisionSourceBaseHelperThread(this, throwableHandler);
    }

    public VisionSourceBase() {
        this(null);
    }

    @Override
    public final boolean start(Size size) {
        boolean result = startSource(size);

        helperThread.start();

        return result;
    }

    public abstract boolean startSource(Size size);

    @Override
    public boolean attach(FrameReceiver sourced) {
        synchronized (lock) {
            return frameReceivers.add(sourced);
        }
    }

    @Override
    public boolean remove(FrameReceiver sourced) {
        synchronized (lock) {
            return frameReceivers.remove(sourced);
        }
    }

    @Override
    public final boolean stop() {
        if(!helperThread.isAlive() || helperThread.isInterrupted()) return false;

        helperThread.interrupt();

        return stopSource();
    }

    public abstract boolean stopSource();

    public abstract Timestamped<Mat> pullFrame();

    private Timestamped<Mat> pullFrameInternal() {
        for(FrameReceiver sourced : frameReceivers) {
            synchronized (sourced) {
                sourced.onFrameStart();
            }
        }

        return pullFrame();
    }

    private static class VisionSourceBaseHelperThread extends Thread {

        VisionSourceBase sourceBase;
        ThrowableHandler throwableHandler;

        boolean shouldStop = false;

        Logger logger;

        public VisionSourceBaseHelperThread(VisionSourceBase sourcedBase, ThrowableHandler throwableHandler) {
            super("Thread-SourceBaseHelper-" + sourcedBase.getClass().getSimpleName());
            logger = LoggerFactory.getLogger(getName());

            this.sourceBase = sourcedBase;
            this.throwableHandler = throwableHandler;
        }

        @Override
        public void run() {
            logger.info("starting");

            while (!isInterrupted() && !shouldStop) {
                Timestamped<Mat> frame = null;

                try {
                    frame = sourceBase.pullFrameInternal();
                } catch(Throwable e) {
                    logger.error("VisionSource threw an exception", e);

                    if(throwableHandler != null) {
                        throwableHandler.handle(e);
                    }
                }

                synchronized (sourceBase.lock) {
                    for (FrameReceiver frameReceiver : sourceBase.frameReceivers) {
                        try {
                            frameReceiver.consume(frame.getValue(), frame.getTimestamp());
                        } catch(Throwable e) {
                            if(e instanceof InterruptedException) {
                                logger.warn("FrameReceiver interrupted", e);
                                Thread.currentThread().interrupt();
                                continue;
                            }

                            logger.error("FrameReceiver threw an exception", e);

                            if(throwableHandler != null) {
                                throwableHandler.handle(e);
                            }
                        }
                    }
                }
            }

            synchronized (sourceBase.lock) {
                for (FrameReceiver frameReceiver : sourceBase.frameReceivers) {
                    frameReceiver.stop();
                }
            }

            logger.info("stop");
        }
    }

}


package io.github.deltacv.vision.external.source;

import io.github.deltacv.vision.external.util.Timestamped;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class VisionSourceBase implements VisionSource {

    private final Object lock = new Object();

    ArrayList<VisionSourced> sourceds = new ArrayList<>();

    SourceBaseHelperThread helperThread = new SourceBaseHelperThread(this);

    @Override
    public final boolean start(Size size) {
        boolean result = startSource(size);

        helperThread.start();

        return result;
    }

    public abstract boolean startSource(Size size);

    @Override
    public boolean attach(VisionSourced sourced) {
        synchronized (lock) {
            return sourceds.add(sourced);
        }
    }

    @Override
    public boolean remove(VisionSourced sourced) {
        synchronized (lock) {
            return sourceds.remove(sourced);
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
        for(VisionSourced sourced : sourceds) {
            synchronized (sourced) {
                sourced.onFrameStart();
            }
        }

        return pullFrame();
    }

    private static class SourceBaseHelperThread extends Thread {

        VisionSourceBase sourceBase;
        boolean shouldStop = false;

        Logger logger;

        public SourceBaseHelperThread(VisionSourceBase sourcedBase) {
            super("Thread-SourceBaseHelper-" + sourcedBase.getClass().getSimpleName());
            logger = LoggerFactory.getLogger(getName());

            this.sourceBase = sourcedBase;
        }

        @Override
        public void run() {
            VisionSourced[] sourceds = new VisionSourced[0];

            logger.info("starting");

            while (!isInterrupted() && !shouldStop) {
                Timestamped<Mat> frame = sourceBase.pullFrameInternal();

                synchronized (sourceBase.lock) {
                    sourceds = sourceBase.sourceds.toArray(new VisionSourced[0]);
                }

                for (VisionSourced sourced : sourceds) {
                    sourced.onNewFrame(frame.getValue(), frame.getTimestamp());
                }
            }

            for(VisionSourced sourced : sourceds) {
                sourced.stop();
            }

            logger.info("stop");
        }
    }

}

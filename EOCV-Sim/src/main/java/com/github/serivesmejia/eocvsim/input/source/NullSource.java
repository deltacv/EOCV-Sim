package com.github.serivesmejia.eocvsim.input.source;

import com.github.serivesmejia.eocvsim.input.InputSource;
import org.opencv.core.Mat;

import javax.swing.filechooser.FileFilter;

public class NullSource extends InputSource {
    @Override
    public boolean init() {
        return true;
    }

    @Override
    public void reset() {

    }

    @Override
    public void close() {

    }

    Mat emptyMat = new Mat();

    @Override
    public Mat update() {
        return emptyMat;
    }

    @Override
    public void onPause() {

    }

    @Override
    public void onResume() {

    }

    @Override
    protected InputSource internalCloneSource() {
        return new NullSource();
    }

    @Override
    public FileFilter getFileFilters() {
        return null;
    }

    @Override
    public long getCaptureTimeNanos() {
        return System.nanoTime();
    }
}

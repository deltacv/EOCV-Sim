/*
 * Copyright (c) 2019 Sebastian Erives
 * Licensed under the MIT License.
 */

package org.openftc.easyopencv;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * A utility class for managing the re-use of Mats
 * so as to re-use already allocated memory instead
 * of constantly allocating new Mats and then freeing
 * them after use.
 */
public class MatRecycler {
    private final RecyclableMat[] mats;
    private final ArrayBlockingQueue<RecyclableMat> availableMats;

    static Logger logger = LoggerFactory.getLogger(MatRecycler.class);

    public MatRecycler(int num, int rows, int cols, int type) {
        mats = new RecyclableMat[num];
        availableMats = new ArrayBlockingQueue<>(num);

        for (int i = 0; i < mats.length; i++) {
            mats[i] = new RecyclableMat(i, rows, cols, type);
            availableMats.add(mats[i]);
        }
    }

    public MatRecycler(int num) {
        this(num, 0, 0, CvType.CV_8UC3);
    }

    public synchronized RecyclableMat takeMatOrNull() {
        if (availableMats.isEmpty()) {
            return null;
        }

        try {
            return takeMatOrInterrupt();
        } catch (InterruptedException e) {
            return null;
        }
    }

    public synchronized RecyclableMat takeMatOrInterrupt() throws InterruptedException {
        if(availableMats.size() == 0) {
            throw new RuntimeException("All mats have been checked out!");
        }

        RecyclableMat mat;

        mat = availableMats.take();
        mat.checkedOut = true;

        return mat;
    }

    public synchronized void returnMat(RecyclableMat mat) {
        if (mat != mats[mat.idx]) {
            throw new IllegalArgumentException("This mat does not belong to this recycler!");
        }

        if (mat.checkedOut) {
            mat.checkedOut = false;
            availableMats.add(mat);
        } else {
            throw new IllegalArgumentException("This mat has already been returned!");
        }
    }

    public void releaseAll() {
        for (Mat mat : mats) {
            mat.release();
        }
    }

    public int getSize() {
        return mats.length;
    }

    public int getAvailableMatsAmount() { return availableMats.size(); }

    public final class RecyclableMat extends Mat {

        private int idx = -1;
        private volatile boolean checkedOut = false;

        private RecyclableMat(int idx) {
            this.idx = idx;
        }

        private RecyclableMat(int idx, int rows, int cols, int type) {
            super(rows, cols, type);
            this.idx = idx;
        }

        private Object context;

        public void setContext(Object context)
        {
            this.context = context;
        }

        public Object getContext()
        {
            return context;
        }

        public void returnMat() {
            synchronized(MatRecycler.this) {
                try {
                    MatRecycler.this.returnMat(this);
                } catch (IllegalArgumentException ex) {
                    logger.warn("Tried to return a Mat which was already returned", ex);
                }
            }
        }

        public boolean isCheckedOut() { return checkedOut; }

        @Override
        public void copyTo(Mat mat) {
            super.copyTo(mat);
            if(mat instanceof RecyclableMat) {
                ((RecyclableMat) mat).setContext(getContext());
            }
        }
    }
}


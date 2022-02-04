/*
 * Copyright (c) 2019 OpenFTC Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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

    public synchronized RecyclableMat takeMat() {
        if (availableMats.size() == 0) {
            throw new RuntimeException("All mats have been checked out!");
        }

        RecyclableMat mat = null;
        try {
            mat = availableMats.take();
            mat.checkedOut = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

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

    }
}

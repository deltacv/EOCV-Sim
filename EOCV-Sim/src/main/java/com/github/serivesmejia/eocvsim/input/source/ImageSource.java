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

package com.github.serivesmejia.eocvsim.input.source;

import com.github.serivesmejia.eocvsim.input.InputSource;
import com.github.serivesmejia.eocvsim.util.FileFilters;
import com.google.gson.annotations.Expose;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.MatRecycler;

import javax.swing.filechooser.FileFilter;

public class ImageSource extends InputSource {

    @Expose
    private final String imgPath;
    @Expose
    private volatile Size size;

    private volatile transient MatRecycler.RecyclableMat img;
    private volatile transient MatRecycler.RecyclableMat lastCloneTo;

    private volatile transient boolean initialized = false;

    private volatile transient MatRecycler matRecycler = new MatRecycler(2);

    public ImageSource(String imgPath) {
        this(imgPath, null);
    }

    public ImageSource(String imgPath, Size size) {
        this.imgPath = imgPath;
        this.size = size;
    }

    @Override
    public boolean init() {

        if (initialized) return false;
        initialized = true;

        if (matRecycler == null) matRecycler = new MatRecycler(2);

        readImage();

        return img != null && !img.empty();

    }

    @Override
    public void onPause() {
        //if(img != null) img.release();
    }

    @Override
    public void onResume() {
    }

    @Override
    public void reset() {

        if (!initialized) return;

        if (lastCloneTo != null) {
            lastCloneTo.returnMat();
            lastCloneTo = null;
        }

        if (img != null) {
            img.returnMat();
            img = null;
        }

        matRecycler.releaseAll();

        initialized = false;

    }

    public void close() {

        if (img != null) {
            matRecycler.returnMat(img);
            img = null;
        }

        if (lastCloneTo != null) {
            lastCloneTo.returnMat();
            lastCloneTo = null;
        }

        matRecycler.releaseAll();

    }

    public void readImage() {

        Mat readMat = Imgcodecs.imread(this.imgPath);

        if (img == null) img = matRecycler.takeMat();

        if (readMat.empty()) {
            return;
        }

        readMat.copyTo(img);
        readMat.release();

        if (this.size != null) {
            Imgproc.resize(img, img, this.size, 0.0, 0.0, Imgproc.INTER_AREA);
        } else {
            this.size = img.size();
        }

        Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2RGB);

    }

    @Override
    public Mat update() {

        if (isPaused) return lastCloneTo;
        if (lastCloneTo == null) lastCloneTo = matRecycler.takeMat();

        if (img == null) return null;

        img.copyTo(lastCloneTo);

        return lastCloneTo;

    }

    @Override
    protected InputSource internalCloneSource() {
        return new ImageSource(imgPath, size);
    }

    @Override
    public FileFilter getFileFilters() {
        return FileFilters.imagesFilter;
    }

    @Override
    public String toString() {
        if (size == null) size = new Size();
        return "ImageSource(\"" + imgPath + "\", " + (size != null ? size.toString() : "null") + ")";
    }

}
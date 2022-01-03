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

package com.github.serivesmejia.eocvsim.gui.component;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.util.MatPoster;
import com.github.serivesmejia.eocvsim.util.image.DynamicBufferedImageRecycler;
import com.github.serivesmejia.eocvsim.util.CvUtil;
import com.qualcomm.robotcore.util.Range;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Viewport extends JPanel {

    public final ImageX image = new ImageX();
    public final MatPoster matPoster;

    private Mat lastVisualizedMat = null;
    private Mat lastVisualizedScaledMat = null;

    private final DynamicBufferedImageRecycler buffImgGiver = new DynamicBufferedImageRecycler();

    private volatile BufferedImage lastBuffImage;
    private volatile Dimension lastDimension;

    private double scale;

    private final EOCVSim eocvSim;

    Logger logger = LoggerFactory.getLogger(getClass());

    public Viewport(EOCVSim eocvSim, int maxQueueItems) {
        super(new GridBagLayout());

        this.eocvSim = eocvSim;
        setViewportScale(eocvSim.configManager.getConfig().zoom);

        add(image, new GridBagConstraints());

        matPoster = new MatPoster("Viewport", maxQueueItems);
        attachToPoster(matPoster);
    }

    public void postMatAsync(Mat mat) {
        matPoster.post(mat);
    }

    public synchronized void postMat(Mat mat) {
        if(lastVisualizedMat == null) lastVisualizedMat = new Mat(); //create latest mat if we have null reference
        if(lastVisualizedScaledMat == null) lastVisualizedScaledMat = new Mat(); //create last scaled mat if null reference

        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);

        mat.copyTo(lastVisualizedMat); //copy given mat to viewport latest one

        double wScale = (double) frame.getWidth() / mat.width();
        double hScale = (double) frame.getHeight() / mat.height();

        double calcScale = (wScale / hScale) * 1.5;
        double finalScale = Math.max(0.1, Math.min(3, scale * calcScale));

        Size size = new Size(mat.width() * finalScale, mat.height() * finalScale);
        Imgproc.resize(mat, lastVisualizedScaledMat, size, 0.0, 0.0, Imgproc.INTER_AREA); //resize mat to lastVisualizedScaledMat

        Dimension newDimension = new Dimension(lastVisualizedScaledMat.width(), lastVisualizedScaledMat.height());

        if(lastBuffImage != null) buffImgGiver.returnBufferedImage(lastBuffImage);

        lastBuffImage = buffImgGiver.giveBufferedImage(newDimension, 2);
        lastDimension = newDimension;

        CvUtil.matToBufferedImage(lastVisualizedScaledMat, lastBuffImage);

        image.setImage(lastBuffImage); //set buff image to ImageX component

        eocvSim.configManager.getConfig().zoom = scale; //store latest scale if store setting turned on
    }

    public void attachToPoster(MatPoster poster) {
        poster.addPostable((m) -> {
            try {
                Imgproc.cvtColor(m, m, Imgproc.COLOR_RGB2BGR);
                postMat(m);
            } catch(Exception ex) {
                logger.error("Couldn't visualize last mat", ex);
            }
        });
    }

    public void flush() {
        buffImgGiver.flushAll();
    }

    public void stop() {
        matPoster.stop();
        flush();
    }

    public synchronized void setViewportScale(double scale) {
        scale = Range.clip(scale, 0.1, 3);

        boolean scaleChanged = this.scale != scale;
        this.scale = scale;

        if(lastVisualizedMat != null && scaleChanged)
            postMat(lastVisualizedMat);

    }

    public synchronized Mat getLastVisualizedMat() {
        return lastVisualizedMat;
    }

    public synchronized double getViewportScale() {
        return scale;
    }

}

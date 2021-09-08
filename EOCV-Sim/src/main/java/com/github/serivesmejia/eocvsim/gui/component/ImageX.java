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

import com.github.serivesmejia.eocvsim.gui.util.GuiUtil;
import com.github.serivesmejia.eocvsim.util.CvUtil;
import org.opencv.core.Mat;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageX extends JLabel {

    volatile ImageIcon icon;

    public ImageX() {
        super();
    }

    public ImageX(ImageIcon img) {
        this();
        setImage(img);
    }

    public ImageX(BufferedImage img) {
        this();
        setImage(img);
    }

    public void setImage(ImageIcon img) {
        if (icon != null)
            icon.getImage().flush(); //flush old image :p

        icon = img;

        setIcon(icon); //set to the new image
    }

    public synchronized void setImage(BufferedImage img) {
        Graphics2D g2d = (Graphics2D) getGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        setImage(new ImageIcon(img)); //set to the new image
    }

    public synchronized void setImageMat(Mat m) {
        setImage(CvUtil.matToBufferedImage(m));
    }

    public synchronized BufferedImage getImage() {
        return (BufferedImage)icon.getImage();
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        setImage(GuiUtil.scaleImage(icon, width, height)); //set to the new image
    }

    @Override
    public void setSize(Dimension dimension) {
        super.setSize(dimension);
        setImage(GuiUtil.scaleImage(icon, (int)dimension.getWidth(), (int)dimension.getHeight())); //set to the new image
    }

}

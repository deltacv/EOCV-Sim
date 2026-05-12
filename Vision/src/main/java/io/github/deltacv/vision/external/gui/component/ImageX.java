/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package io.github.deltacv.vision.external.gui.component;

import io.github.deltacv.vision.external.gui.util.ImgUtil;
import io.github.deltacv.vision.external.util.CvUtil;
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
        setImage(ImgUtil.scaleImage(icon, width, height)); //set to the new image
    }

    @Override
    public void setSize(Dimension dimension) {
        super.setSize(dimension);
        setImage(ImgUtil.scaleImage(icon, (int)dimension.getWidth(), (int)dimension.getHeight())); //set to the new image
    }

}


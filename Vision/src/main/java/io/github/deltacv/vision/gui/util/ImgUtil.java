package io.github.deltacv.vision.gui.util;

import javax.swing.*;
import java.awt.*;

public class ImgUtil {

    private ImgUtil() { }

    public static ImageIcon scaleImage(ImageIcon icon, int w, int h) {

        int nw = icon.getIconWidth();
        int nh = icon.getIconHeight();

        if (icon.getIconWidth() > w) {
            nw = w;
            nh = (nw * icon.getIconHeight()) / icon.getIconWidth();
        }

        if (nh > h) {
            nh = h;
            nw = (icon.getIconWidth() * nh) / icon.getIconHeight();
        }

        return new ImageIcon(icon.getImage().getScaledInstance(nw, nh, Image.SCALE_SMOOTH));
    }

}

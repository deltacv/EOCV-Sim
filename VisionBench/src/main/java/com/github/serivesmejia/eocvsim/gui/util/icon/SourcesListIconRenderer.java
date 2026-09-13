/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.util.icon;

import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary;
import com.github.serivesmejia.eocvsim.gui.Icons;
import com.github.serivesmejia.eocvsim.input.InputSourceManager;

import javax.swing.*;
import java.awt.*;

public class SourcesListIconRenderer extends DefaultListCellRenderer {

    public static final int ICO_W = 15;
    public static final int ICO_H = 15;

    public InputSourceManager sourceManager = null;

    public SourcesListIconRenderer(InputSourceManager sourceManager) {
        this.sourceManager = sourceManager;
    }

    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {

        // Get the renderer component from parent class
        JLabel label = (JLabel) super.getListCellRendererComponent(list,
                value, index, isSelected, cellHasFocus);

        switch (sourceManager.getSourceType((String) value)) {
            case IMAGE:
                label.setIcon(EOCVSimIconLibrary.INSTANCE.getIcoImg().resized(15, 15));
                break;
            case CAMERA:
                label.setIcon(EOCVSimIconLibrary.INSTANCE.getIcoCam().resized(15, 15));
                break;
            case VIDEO:
                label.setIcon(EOCVSimIconLibrary.INSTANCE.getIcoVid().resized(15, 15));
                break;
            case HTTP:
                label.setIcon(EOCVSimIconLibrary.INSTANCE.getIcoStream().resized(15, 15));
                break;
        }

        return label;

    }

}


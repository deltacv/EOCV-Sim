/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.theme;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.intellijthemes.*;

import javax.swing.*;

public enum Theme {

    Default(() -> {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    }),
    System(() -> {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }),
    Light(FlatLightLaf::setup),
    Dark(FlatDarkLaf::setup),
    Darcula(FlatDarculaLaf::setup),
    Light_Intellij(FlatIntelliJLaf::setup),
    Light_Flat_Intellij(FlatLightFlatIJTheme::setup),
    Cyan_Light_Intellij(FlatCyanLightIJTheme::setup),
    High_Contrast_Intellij(FlatHighContrastIJTheme::setup),
    Dracula_Intellij(FlatDraculaIJTheme::setup),
    Dark_Flat_Intellij(FlatDarkFlatIJTheme::setup),
    Spacegray_Intellij(FlatSpacegrayIJTheme::setup),
    Material_Dark_Intellij(FlatMaterialDesignDarkIJTheme::setup);

    ThemeInstaller installRunn;

    Theme(ThemeInstaller installRunn) {
        this.installRunn = installRunn;
    }

    public void install() throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        installRunn.install();
    }
}

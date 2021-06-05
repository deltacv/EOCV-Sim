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

package com.github.serivesmejia.eocvsim.gui.theme;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.intellijthemes.*;

import javax.swing.*;

public enum Theme {

    Default(() -> {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    }),
    Light(FlatLightLaf::install),
    Dark(FlatDarkLaf::install),
    Darcula(FlatDarculaLaf::install),
    Light_Intellij(FlatIntelliJLaf::install),
    Light_Flat_Intellij(FlatLightFlatIJTheme::install),
    Cyan_Light_Intellij(FlatCyanLightIJTheme::install),
    High_Contrast_Intellij(FlatHighContrastIJTheme::install),
    Dracula_Intellij(FlatDraculaIJTheme::install),
    Dark_Flat_Intellij(FlatDarkFlatIJTheme::install),
    Spacegray_Intellij(FlatSpacegrayIJTheme::install),
    Material_Dark_Intellij(FlatMaterialDesignDarkIJTheme::install);

    ThemeInstaller installRunn;

    Theme(ThemeInstaller installRunn) {
        this.installRunn = installRunn;
    }

    public void install() throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException {
        installRunn.install();
    }
}
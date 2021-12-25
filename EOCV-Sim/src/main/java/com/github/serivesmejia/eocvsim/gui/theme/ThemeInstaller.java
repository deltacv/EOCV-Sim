package com.github.serivesmejia.eocvsim.gui.theme;

import javax.swing.*;

public interface ThemeInstaller {
    void install() throws ClassNotFoundException, UnsupportedLookAndFeelException, InstantiationException, IllegalAccessException;
}

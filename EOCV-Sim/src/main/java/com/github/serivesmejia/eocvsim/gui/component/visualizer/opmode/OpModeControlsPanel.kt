package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import javax.swing.JPanel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout

class OpModeControlsPanel : JPanel() {

    init {
        val c = GridBagConstraints()
        layout = GridBagLayout()

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
    }

}
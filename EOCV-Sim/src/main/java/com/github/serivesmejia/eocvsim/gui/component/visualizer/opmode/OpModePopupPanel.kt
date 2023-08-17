package com.github.serivesmejia.eocvsim.gui.component.visualizer.opmode

import javax.swing.JButton
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane

class OpModePopupPanel(autonomousSelector: JList<*>) : JPanel() {

    init {
        val scroll = JScrollPane()

        scroll.setViewportView(autonomousSelector)
        scroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        scroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

        add(scroll)

        autonomousSelector.selectionModel.clearSelection()
    }

}
package com.github.serivesmejia.eocvsim.gui.component.visualizer

import org.firstinspires.ftc.robotcore.external.Telemetry
import java.awt.FlowLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import javax.swing.*

class TelemetryPanel : JPanel() {

    val telemetryScroll = JScrollPane()
    val telemetryList  = JList<String>()

    val telemetryLabel = JLabel("Telemetry")

    init {
        layout = GridBagLayout()

        /*
         * TELEMETRY
         */

        telemetryLabel.font = telemetryLabel.font.deriveFont(20.0f)
        telemetryLabel.horizontalAlignment = JLabel.CENTER

        add(telemetryLabel, GridBagConstraints().apply {
            gridy = 0
            ipady = 20
        })

        telemetryScroll.setViewportView(telemetryList)
        telemetryScroll.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        telemetryScroll.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS

        //tooltips for the telemetry list items (thnx stackoverflow)
        telemetryList.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(e: MouseEvent) {}
            override fun mouseMoved(e: MouseEvent) {
                val l = e.source as JList<*>
                val m = l.model
                val index = l.locationToIndex(e.point)
                if (index > -1) {
                    l.toolTipText = m.getElementAt(index).toString()
                }
            }
        })

        telemetryList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION

        add(telemetryScroll, GridBagConstraints().apply {
            gridy = 1

            weightx = 0.5
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            ipadx = 120
            ipady = 20
        })
    }

    fun revalAndRepaint() {
        telemetryList.revalidate()
        telemetryList.repaint()
        telemetryScroll.revalidate()
        telemetryScroll.repaint()
    }

    fun updateTelemetry(telemetry: Telemetry?) {
        val cacheTelemetryText = telemetry.toString()

        var telemetryText: String? = null

        if (telemetry != null && telemetry.hasChanged()) {
            telemetryText = cacheTelemetryText

            val listModel = DefaultListModel<String>()
            for (line in telemetryText.split("\n").toTypedArray()) {
                listModel.addElement(line)
            }

            telemetryList.model = listModel
        }

        if (telemetryList.model.size <= 0 || (telemetryText != null && telemetryText.trim { it <= ' ' } == "")) {
            val listModel = DefaultListModel<String>()

            listModel.addElement("<html></html>")
            telemetryList.model = listModel
        }

        telemetryList.fixedCellWidth = 240

        revalAndRepaint()
    }

}

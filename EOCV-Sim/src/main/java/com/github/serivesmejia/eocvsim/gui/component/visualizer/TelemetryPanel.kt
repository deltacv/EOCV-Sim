/*
 * Copyright (c) 2024 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.component.visualizer

import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.robotcore.internal.opmode.EOCVSimTelemetryImpl
import org.firstinspires.ftc.robotcore.internal.opmode.TelemetryTransmissionReceiver
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.border.TitledBorder

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TelemetryPanel : JPanel(), TelemetryTransmissionReceiver, KoinComponent {

    private val pipelineManager: PipelineManager by inject()


    val telemetryScroll = JScrollPane()
    val telemetryList  = JList<String>()

    val telemetryLabel = JLabel("Telemetry")

    init {
        border = TitledBorder("Telemetry").apply {
            titleFont = titleFont.deriveFont(Font.BOLD)
            border = EmptyBorder(0, 0, 0, 0)
        }

        layout = GridBagLayout()

        /*
         * TELEMETRY
         */

        telemetryLabel.font = telemetryLabel.font.deriveFont(20.0f)
        telemetryLabel.horizontalAlignment = JLabel.CENTER

        // add(telemetryLabel, GridBagConstraints().apply {
        //    gridy = 0
        //    ipady = 20
        //})

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
            gridy = 0

            weightx = 0.5
            weighty = 1.0
            fill = GridBagConstraints.BOTH

            ipadx = 120
            ipady = 20
        })

        pipelineManager.onPipelineChange { // update telemetry receiver on pipeline change
            val telemetry = pipelineManager.currentTelemetry
            if (telemetry is EOCVSimTelemetryImpl) {
                telemetry.addTransmissionReceiver(this@TelemetryPanel)
            }
        }

    }

    fun revalAndRepaint() {
        telemetryList.revalidate()
        telemetryList.repaint()
        telemetryScroll.revalidate()
        telemetryScroll.repaint()
    }

    fun updateTelemetry(telemetryText: String?, captionSeparator: String, itemSeparator: String) {

        if (telemetryText != null) {
            val listModel = DefaultListModel<String>()
            for (line in telemetryText.split("\n").toTypedArray()) {
                if(line != captionSeparator && line != itemSeparator) {
                    listModel.addElement(line)
                }
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

    private var lastTelemetry = "";

    override fun onTelemetryTransmission(text: String, srcTelemetry: Telemetry) {
        SwingUtilities.invokeLater {
            if(lastTelemetry != text) {
                updateTelemetry(text, srcTelemetry.captionValueSeparator, srcTelemetry.itemSeparator)
            }
            lastTelemetry = text
        }
    }

}


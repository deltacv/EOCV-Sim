/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.gui.dialog

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.EOCVSimIconLibrary
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.gui.util.GuiUtil
import com.github.serivesmejia.eocvsim.util.StrUtil
import io.github.deltacv.vision.external.gui.component.ImageX
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.awt.*
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.swing.*
import javax.swing.border.EmptyBorder

class About : KoinComponent {

    private val visualizer: Visualizer by inject()

    val about = JDialog(visualizer.frame)

    companion object {
        var CONTRIBS_LIST_MODEL: ListModel<String>? = null
        var OSL_LIST_MODEL: ListModel<String>? = null

        init {
            try {
                CONTRIBS_LIST_MODEL = GuiUtil.isToListModel(About::class.java.getResourceAsStream("/contributors.txt"), StandardCharsets.UTF_8)
                OSL_LIST_MODEL = GuiUtil.isToListModel(About::class.java.getResourceAsStream("/opensourcelibs.txt"), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        initAbout()
    }

    private fun initAbout() {
        about.isModal = true
        about.title = "About"

        val contents = JPanel(GridLayout(2, 1))
        contents.alignmentX = Component.CENTER_ALIGNMENT

        val icon = ImageX(EOCVSimIconLibrary.icoEOCVSim64)
        icon.setSize(50, 50)
        icon.alignmentX = Component.CENTER_ALIGNMENT
        icon.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

        val appInfo = JLabel("EasyOpenCV Simulator v" + EOCVSim.VERSION)
        appInfo.font = appInfo.font.deriveFont(appInfo.font.style or Font.BOLD) // set font to bold

        val appInfoLogo = JPanel(FlowLayout())
        appInfoLogo.add(icon)
        appInfoLogo.add(appInfo)
        appInfoLogo.border = BorderFactory.createEmptyBorder(10, 10, -30, 10)

        contents.add(appInfoLogo)

        val tabbedPane = JTabbedPane()

        val contributors = JPanel(FlowLayout(FlowLayout.CENTER))
        val contribsList = JList<String>()
        contribsList.model = CONTRIBS_LIST_MODEL
        contribsList.selectionModel = GuiUtil.NoSelectionModel()
        contribsList.layout = FlowLayout(FlowLayout.CENTER)
        contribsList.alignmentY = Component.TOP_ALIGNMENT
        contribsList.visibleRowCount = 4

        val contributorsList = JPanel(FlowLayout(FlowLayout.CENTER))
        contributorsList.alignmentY = Component.TOP_ALIGNMENT

        val contribsListScroll = JScrollPane()
        contribsListScroll.border = EmptyBorder(0, 0, 20, 10)
        contribsListScroll.alignmentX = Component.CENTER_ALIGNMENT
        contribsListScroll.alignmentY = Component.TOP_ALIGNMENT
        contribsListScroll.setViewportView(contribsList)

        contributors.alignmentY = Component.TOP_ALIGNMENT
        contents.alignmentY = Component.TOP_ALIGNMENT

        contributorsList.add(contribsListScroll)
        contributors.add(contributorsList)

        tabbedPane.addTab("Contributors", contributors)

        val osLibs = JPanel(FlowLayout(FlowLayout.CENTER))
        val osLibsList = JList<String>()
        osLibsList.model = OSL_LIST_MODEL
        osLibsList.layout = FlowLayout(FlowLayout.CENTER)
        osLibsList.alignmentY = Component.TOP_ALIGNMENT
        osLibsList.visibleRowCount = 4

        osLibsList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val text = osLibsList.selectedValue
                if (text != null) {
                    val urls = StrUtil.findUrlsInString(text)
                    if (urls.isNotEmpty()) {
                        try {
                            Desktop.getDesktop().browse(URI(urls[0]))
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
                osLibsList.clearSelection()
            }
        }

        val osLibsListPane = JPanel(FlowLayout(FlowLayout.CENTER))
        osLibsList.alignmentY = Component.TOP_ALIGNMENT

        val osLibsListScroll = JScrollPane()
        osLibsListScroll.border = EmptyBorder(0, 0, 20, 10)
        osLibsListScroll.alignmentX = Component.CENTER_ALIGNMENT
        osLibsListScroll.alignmentY = Component.TOP_ALIGNMENT
        osLibsListScroll.setViewportView(osLibsList)

        osLibs.alignmentY = Component.TOP_ALIGNMENT
        osLibsListPane.add(osLibsListScroll)
        osLibs.add(osLibsListPane)

        tabbedPane.addTab("Open Source Libraries", osLibs)

        contents.add(tabbedPane)
        contents.border = EmptyBorder(10, 10, 10, 10)

        about.add(contents)
        about.pack()
        about.setLocationRelativeTo(null)
        about.isResizable = false
        about.isVisible = true
    }
}


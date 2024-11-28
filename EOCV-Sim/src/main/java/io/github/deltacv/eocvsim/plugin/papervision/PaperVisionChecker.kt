package io.github.deltacv.eocvsim.plugin.papervision

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.gui.Visualizer
import com.github.serivesmejia.eocvsim.util.extension.hashString
import com.github.serivesmejia.eocvsim.util.loggerForThis
import io.github.deltacv.common.util.ParsedVersion
import io.github.deltacv.eocvsim.plugin.loader.PluginManager
import io.github.deltacv.eocvsim.plugin.loader.PluginSource
import io.github.deltacv.eocvsim.plugin.repository.PluginRepositoryManager
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.math.log

object PaperVisionChecker {

    val LATEST_PAPERVISION = ParsedVersion(1, 0, 3)

    const val RESET_QUESTION = "o you wish to fix this by resetting back to the default settings? Please note this will wipe your plugins folder!"

    val logger by loggerForThis()

    fun check(
        eocvSim: EOCVSim
    ) {
        fun startFresh() {
            eocvSim.onMainUpdate.doOnce {
                eocvSim.config.flags["startFresh"] = true
                PluginRepositoryManager.REPOSITORY_FILE.delete()
                PluginRepositoryManager.CACHE_FILE.delete()
            }

            val result = JOptionPane.showOptionDialog(
                eocvSim.visualizer.frame,
                "You need to restart to apply the latest changes, Restart now?",
                "Restart Now",
                JOptionPane.WARNING_MESSAGE,
                JOptionPane.YES_NO_OPTION,
                null, arrayOf("Restart", "Ignore"), null
            )

            if(result == JOptionPane.YES_OPTION) {
                eocvSim.onMainUpdate.doOnce {
                    eocvSim.restart()
                }
            }
        }

        val paperVisionPlugin = eocvSim.pluginManager.loaders.values.find { it.pluginName == "PaperVision" && it.pluginAuthor == "deltacv" }
        val hash = paperVisionPlugin?.pluginFile?.absolutePath?.hashString

        logger.info("hash_check = ${eocvSim.config.flags["${hash}_check"]}")
        logger.info("null_check = ${eocvSim.config.flags["null_check"]}")

        if(eocvSim.config.flags["${hash}_check"] == true) {
            return
        } else {
            eocvSim.config.flags["${hash}_check"] = true
        }

        val parsedVersion = try {
            ParsedVersion(paperVisionPlugin!!.pluginVersion).apply {
                logger.info("Parsed PaperVision version: $this")
            }
        } catch(e: Exception) {
            logger.warn("Failed to parse PaperVision version", e)
            null
        }

        if(paperVisionPlugin == null) {
            SwingUtilities.invokeLater {
                val result = JOptionPane.showOptionDialog(
                    eocvSim.visualizer.frame,
                    "The PaperVision plugin is not present.\nD$RESET_QUESTION",
                    "PaperVision Missing",
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION,
                    null, arrayOf("Reset and Fix", "Ignore and Continue"), null
                )

                if(result == JOptionPane.YES_OPTION) {
                    startFresh()
                }
            }

            logger.warn("PaperVision plugin not present")
        } else if(paperVisionPlugin.pluginSource == PluginSource.FILE) {
            SwingUtilities.invokeLater {
                val result = JOptionPane.showOptionDialog(
                    eocvSim.visualizer.frame,
                    "PaperVision was loaded from a file. You can ignore this message ONLY IF you did this intentionally and intend to test development versions.\nIf that's not the case, d$RESET_QUESTION",
                    "PaperVision Source",
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION,
                    null, arrayOf("Reset and Fix", "Ignore and Continue"), null
                )

                if(result == JOptionPane.YES_OPTION) {
                    startFresh()
                }
            }

            eocvSim.config.flags["null_check"] = false
            logger.warn("PaperVision plugin loaded from file")
        } else if(parsedVersion == null || parsedVersion < LATEST_PAPERVISION) {
            SwingUtilities.invokeLater {
                val result = JOptionPane.showOptionDialog(
                    eocvSim.visualizer.frame,
                    "The PaperVision plugin is outdated.\nD$RESET_QUESTION",
                    "PaperVision Outdated",
                    JOptionPane.WARNING_MESSAGE,
                    JOptionPane.YES_NO_OPTION,
                    null, arrayOf("Reset and Fix", "Ignore and Continue"), null
                )

                if(result == JOptionPane.YES_OPTION) {
                    startFresh()
                }
            }

            eocvSim.config.flags["null_check"] = false
            logger.warn("PaperVision plugin outdated")
        }
    }

}
/*
 * Copyright (c) 2026 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.workspace.config

import com.github.serivesmejia.eocvsim.BuildInfo
import com.github.serivesmejia.eocvsim.util.SysUtil
import com.github.serivesmejia.eocvsim.util.serialization.JacksonJsonSupport
import org.deltacv.common.util.loggerForThis
import java.io.File

/**
 * Class to load and save workspace configurations
 * @param workspaceFile the workspace directory
 * @see WorkspaceConfig
 */
class WorkspaceConfigLoader(var workspaceFile: File) {

    /**
     * The workspace configuration file
     */
    val workspaceConfigFile get() = File(workspaceFile, File.separator + "eocvsim_workspace.json")

    private val logger by loggerForThis()

    /**
     * Load the workspace configuration
     * @return the workspace configuration if it exists, null otherwise
     */
    fun loadWorkspaceConfig(): WorkspaceConfig? {
        if(!workspaceConfigFile.exists()) return null

        val configStr = SysUtil.loadFileStr(workspaceConfigFile)

        return try {
            JacksonJsonSupport.persistenceMapper.readValue(configStr, WorkspaceConfig::class.java)
        } catch(e: Exception) {
            logger.error("Failed to load workspace config", e)
            null
        }
    }

    /**
     * Save the workspace configuration to the workspace directory
     * @param config the workspace configuration to save
     */
    fun saveWorkspaceConfig(config: WorkspaceConfig) {
        config.eocvSimVersion = BuildInfo.STANDARD_VERSION_STRING
        val configStr = JacksonJsonSupport.persistenceMapper.writeValueAsString(config)
        SysUtil.saveFileStr(workspaceConfigFile, configStr)
    }

}

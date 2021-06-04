package com.github.serivesmejia.eocvsim.workspace.config

import com.github.serivesmejia.eocvsim.util.SysUtil
import com.google.gson.GsonBuilder
import java.io.File

class WorkspaceConfigLoader(var workspaceFile: File) {

    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
    }

    val workspaceConfigFile get() = File(workspaceFile, File.separator + "eocvsim_workspace.json")

    fun loadWorkspaceConfig(): WorkspaceConfig? {
        if(!workspaceConfigFile.exists()) return null

        val configStr = SysUtil.loadFileStr(workspaceConfigFile)

        return try {
            gson.fromJson(configStr, WorkspaceConfig::class.java)
        } catch(e: Exception) {
            null
        }
    }

    fun saveWorkspaceConfig(config: WorkspaceConfig) {
        val configStr = gson.toJson(config)
        SysUtil.saveFileStr(workspaceConfigFile, configStr)
    }

}
/*
 * Copyright (c) 2021 Sebastian Erives
 * Licensed under the MIT License.
 */

package com.github.serivesmejia.eocvsim.workspace.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import com.github.serivesmejia.eocvsim.util.SysUtil
import io.github.deltacv.common.util.loggerForThis
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.File

/**
 * Utility class to launch Visual Studio Code
 * in the workspace directory
 * @see WorkspaceManager
 * @see SysUtil.runShellCommand
 */
object VSCodeLauncher {

    val logger by loggerForThis()

    /**
     * Launches Visual Studio Code in the workspace directory
     * @param workspace the workspace directory to open
     */
    fun launch(workspace: File) {
        logger.info("Opening VS Code...")

        val result = SysUtil.runShellCommand("code \"${workspace.absolutePath}\"")

        if(result.output.isNotEmpty()) logger.info(result.output)

        if(result.exitCode == 0)
            logger.info("VS Code opened")
        else
            logger.info("VS Code failed to open")
    }

    /**
     * Launches Visual Studio Code in the workspace directory
     * Runs in a coroutine in the IO dispatcher context
     */
    @OptIn(DelicateCoroutinesApi::class)
    fun asyncLaunch(workspace: File, scope: CoroutineScope) = scope.launch(Dispatchers.IO) { launch(workspace) }

}

/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package com.github.serivesmejia.eocvsim.pipeline.util;

import com.github.serivesmejia.eocvsim.pipeline.PipelineData
import com.github.serivesmejia.eocvsim.pipeline.PipelineManager
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.StrUtil
import com.github.serivesmejia.eocvsim.util.loggerForThis

class PipelineExceptionTracker(private val pipelineManager: PipelineManager) {

    companion object {
        const val millisExceptionExpire = 25000L
        const val cutStacktraceLines = 9
    }

    val logger by loggerForThis()

    var currentPipeline: PipelineData? = null
        private set

    val exceptionsThrown = mutableMapOf<Throwable, PipelineException>()
    val messages = mutableMapOf<String, Long>()

    val onPipelineException      = EventHandler("OnPipelineException")
    val onNewPipelineException   = EventHandler("OnNewPipelineException")
    val onPipelineExceptionClear = EventHandler("OnPipelineExceptionClear")

    val onUpdate = EventHandler("OnPipelineExceptionTrackerUpdate")

    fun update(data: PipelineData, ex: Throwable?) {
        if(currentPipeline != data) {
            exceptionsThrown.clear()
            currentPipeline = data
        }

        val exStr = if(ex != null) StrUtil.fromException(ex) else ""

        if(ex != null) {
            onPipelineException.run()

            val exception = exceptionsThrown.values.stream().filter {
                it.stacktrace == exStr
            }.findFirst()

            if(!exception.isPresent) {
                logger.error(
                    "Uncaught exception thrown while processing pipeline ${data.clazz.simpleName}",
                    ex
                )

                logger.warn("Note that to avoid spam, continuously equal thrown exceptions are only logged once.")
                logger.warn("It will be reported once the pipeline stops throwing the exception after $millisExceptionExpire ms")

                exceptionsThrown[ex] = PipelineException(
                    0, exStr, System.currentTimeMillis()
                )

                onNewPipelineException.run()
            }
        }

        for((e, d) in exceptionsThrown.entries.toTypedArray()) {
            if(ex != null && d.stacktrace == exStr) {
                d.count++
                d.millisThrown = System.currentTimeMillis()
            }

            val timeElapsed = System.currentTimeMillis() - d.millisThrown
            if(timeElapsed >= millisExceptionExpire) {
                exceptionsThrown.remove(e)
                logger.info(
                    "Pipeline ${currentPipeline!!.clazz.simpleName} stopped throwing $e"
                )

                if(exceptionsThrown.isEmpty())
                    onPipelineExceptionClear.run()
            }
        }

        for((message, millisAdded) in messages.entries.toTypedArray()) {
            val timeElapsed = System.currentTimeMillis() - millisAdded
            if(timeElapsed >= millisExceptionExpire) {
                messages.remove(message)
            }
        }

        onUpdate.run()
    }

    val message: String get() {
        if(currentPipeline == null)
            return "**No pipeline selected**"

        val messageBuilder = StringBuilder()
        val pipelineName = currentPipeline!!.clazz.simpleName

        if(exceptionsThrown.isNotEmpty()) {
            messageBuilder
                .append("**Pipeline $pipelineName is throwing ${exceptionsThrown.size} exception(s)**")
                .appendLine("\n")
        } else {
            messageBuilder.append("**Pipeline $pipelineName ")

            if(pipelineManager.paused) {
                messageBuilder.append("is paused (last time was running at ${pipelineManager.pipelineFpsCounter.fps} FPS)")
            } else {
                messageBuilder.append("running OK at ${pipelineManager.pipelineFpsCounter.fps} FPS")
            }

            messageBuilder.append("**").appendLine("\n")
        }

        for((_, data) in exceptionsThrown) {
            val expiresIn = millisExceptionExpire - (System.currentTimeMillis() - data.millisThrown)
            val expiresInSecs = String.format("%.1f", expiresIn.toDouble() / 1000.0)

            val shortStacktrace = StrUtil.cutStringBy(
                data.stacktrace, "\n", cutStacktraceLines
            ).trim()

            messageBuilder
                .appendLine("> $shortStacktrace")
                .appendLine()
                .appendLine("! It has been thrown ${data.count} times, and will expire in $expiresInSecs seconds !")
                .appendLine()
        }

        for((message, _) in messages) {
            messageBuilder.appendLine(message)
        }

        return messageBuilder.toString().trim()
    }

    fun clear() = exceptionsThrown.clear()

    fun addMessage(s: String) {
        messages[s] = System.currentTimeMillis()
        onNewPipelineException.run()
    }

    data class PipelineException(var count: Int,
                                 val stacktrace: String,
                                 var millisThrown: Long)

}

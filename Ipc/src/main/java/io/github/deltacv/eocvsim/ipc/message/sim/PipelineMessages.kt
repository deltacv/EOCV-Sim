package io.github.deltacv.eocvsim.ipc.message.sim

import io.github.deltacv.eocvsim.ipc.message.IpcMessageBase
import io.github.deltacv.eocvsim.pipeline.PipelineSource

class PythonPipelineSourceMessage(
    var pipelineName: String,
    var pythonSource: String
) : IpcMessageBase()

class ChangePipelineMessage(
    var pipelineName: String,
    var pipelineSource: PipelineSource,
    var force: Boolean = false
) : IpcMessageBase()
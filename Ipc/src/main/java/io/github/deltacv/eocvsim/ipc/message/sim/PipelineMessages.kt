package io.github.deltacv.eocvsim.ipc.message.sim

import io.github.deltacv.eocvsim.ipc.message.IpcMessageBase

class PythonPipelineSourceMessage(
    var pipelineName: String,
    var pythonSource: String
) : IpcMessageBase()

class ChangePipelineMessage(
    var pipelineName: String,
    var pipelineSource: PipelineSource,
    var force: Boolean = false
) : IpcMessageBase()

enum class PipelineSource { CLASSPATH, COMPILED_ON_RUNTIME, PYTHON_RUNTIME }
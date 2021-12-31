package io.github.deltacv.eocvsim.ipc.message.sim

import io.github.deltacv.eocvsim.ipc.message.IpcMessageBase

class PythonPipelineSourceMessage(
    var pipelineName: String,
    var pythonSource: String
) : IpcMessageBase()
package io.github.deltacv.eocvsim.ipc.message.sim

import io.github.deltacv.eocvsim.ipc.message.IpcMessageBase
import io.github.deltacv.eocvsim.ipc.message.response.IpcMessageResponse

class PrintMessage(var printMessage: String) : IpcMessageBase()
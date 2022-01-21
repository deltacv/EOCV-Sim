package io.github.deltacv.eocvsim.ipc.message.sim

import io.github.deltacv.eocvsim.ipc.message.IpcMessageBase

class StartStreamingMessage(
    var width: Int,
    var height: Int,
    var opcode: Byte
) : IpcMessageBase()

class StopStreamingMessage : IpcMessageBase()
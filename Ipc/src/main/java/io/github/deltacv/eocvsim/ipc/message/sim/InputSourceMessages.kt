package io.github.deltacv.eocvsim.ipc.message.sim

import io.github.deltacv.eocvsim.input.InputSourceType
import io.github.deltacv.eocvsim.ipc.message.IpcMessageBase

class InputSourcesListMessage : IpcMessageBase()

class SetInputSourceMessage(
    var name: String
) : IpcMessageBase()
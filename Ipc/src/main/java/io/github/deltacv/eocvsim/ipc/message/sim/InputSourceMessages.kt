package io.github.deltacv.eocvsim.ipc.message.sim

import io.github.deltacv.eocvsim.ipc.message.IpcMessageBase

class InputSourcesListMessage : IpcMessageBase()

class GetCurrentInputSourceMessage : IpcMessageBase()

class SetInputSourceMessage(
    var name: String
) : IpcMessageBase()
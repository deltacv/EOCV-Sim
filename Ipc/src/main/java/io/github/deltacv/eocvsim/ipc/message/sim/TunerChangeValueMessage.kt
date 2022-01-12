package io.github.deltacv.eocvsim.ipc.message.sim

import io.github.deltacv.eocvsim.ipc.message.IpcMessageBase

class TunerChangeValueMessage(
    var label: String,
    var index: Int,
    var value: Any
) : IpcMessageBase()

class TunerChangeValuesMessage(
    var label: String,
    var values: Array<Any>
) : IpcMessageBase()
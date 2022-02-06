package io.github.deltacv.eocvsim.ipc.message.response.sim

import io.github.deltacv.eocvsim.input.InputSourceData
import io.github.deltacv.eocvsim.ipc.message.response.IpcOkResponse

class InputSourcesListResponse(
    var sources: Array<InputSourceData>
) : IpcOkResponse()
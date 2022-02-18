package io.github.deltacv.eocvsim.ipc.message.response

class IpcBooleanResponse(
    var value: Boolean
) : IpcOkResponse()


class IpcStringResponse(
    var value: String
) : IpcOkResponse()
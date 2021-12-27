package io.github.deltacv.eocvsim.ipc.message

import io.github.deltacv.eocvsim.ipc.security.PassToken

class AuthMessage(var passToken: PassToken) : IpcMessageBase()
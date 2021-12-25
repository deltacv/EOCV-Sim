package com.github.serivesmejia.eocvsim.ipc.message

abstract class IpcMessageBase : IpcMessage {

    companion object {
        private var idCount = -1

        fun nextId(): Int {
            idCount++
            return idCount
        }
    }

    override val id = nextId()

}
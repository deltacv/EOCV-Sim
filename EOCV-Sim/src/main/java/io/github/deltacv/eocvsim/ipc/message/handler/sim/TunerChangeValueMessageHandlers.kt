package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.handler.dsl.IpcMessageHandlerDsl
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValueMessage
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValuesMessage

@IpcMessageHandler.Register(TunerChangeValueMessage::class)
class TunerChangeValueMessageHandler : IpcMessageHandlerDsl<TunerChangeValueMessage>({

    handle {
        eocvSim.pipelineManager.onUpdate.doOnce {
            val field = eocvSim.tunerManager.getTunableFieldWithLabel(message.label)
                ?: return@doOnce

            field.setFieldValue(message.index, message.value)
        }
    }

})

@IpcMessageHandler.Register(TunerChangeValuesMessage::class)
class TunerChangeValuesMessageHandler : IpcMessageHandlerDsl<TunerChangeValuesMessage>({

    handle {
        eocvSim.pipelineManager.onUpdate.doOnce {
            val field = eocvSim.tunerManager.getTunableFieldWithLabel(message.label)
                ?: return@doOnce

            for((i, value) in message.values.withIndex()) {
                field.setFieldValue(i, value)
            }
        }
    }

})
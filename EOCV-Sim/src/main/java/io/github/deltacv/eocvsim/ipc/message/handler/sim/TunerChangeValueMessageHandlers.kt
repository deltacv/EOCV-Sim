package io.github.deltacv.eocvsim.ipc.message.handler.sim

import io.github.deltacv.eocvsim.ipc.IpcServer
import io.github.deltacv.eocvsim.ipc.message.handler.IpcMessageHandler
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValueMessage
import io.github.deltacv.eocvsim.ipc.message.sim.TunerChangeValuesMessage

@IpcMessageHandler.Register(
    TunerChangeValueMessage::class
)
class TunerChangeValueMessageHandler : IpcMessageHandler<TunerChangeValueMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<TunerChangeValueMessage>) {
        ctx.eocvSim.pipelineManager.onUpdate.doOnce {
            val field = ctx.eocvSim.tunerManager.getTunableFieldWithLabel(ctx.message.label)
                ?: return@doOnce
            field.setFieldValue(ctx.message.index, ctx.message.value)
        }
    }

}

@IpcMessageHandler.Register(
    TunerChangeValuesMessage::class
)
class TunerChangeValuesMessageHandler : IpcMessageHandler<TunerChangeValuesMessage>() {

    override fun handle(ctx: IpcServer.IpcTransactionContext<TunerChangeValuesMessage>) {
        ctx.eocvSim.pipelineManager.onUpdate.doOnce {
            val field = ctx.eocvSim.tunerManager.getTunableFieldWithLabel(ctx.message.label)
                ?: return@doOnce

            for((i, value) in ctx.message.values.withIndex()) {
                field.setFieldValue(i, value)
            }
        }
    }

}
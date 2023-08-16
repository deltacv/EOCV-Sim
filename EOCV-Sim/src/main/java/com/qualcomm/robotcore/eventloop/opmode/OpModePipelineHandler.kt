package com.qualcomm.robotcore.eventloop.opmode

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.input.InputSource
import com.github.serivesmejia.eocvsim.input.InputSourceManager
import com.github.serivesmejia.eocvsim.pipeline.handler.SpecificPipelineHandler
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.qualcomm.robotcore.hardware.HardwareMap
import io.github.deltacv.eocvsim.input.VisionInputSourceHander
import io.github.deltacv.vision.external.source.ThreadSourceHander
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline
import org.openftc.easyopencv.OpenCvViewport

enum class OpModeState { SELECTED, INIT, START, STOP, STOPPED }

class OpModePipelineHandler(val inputSourceManager: InputSourceManager, private val viewport: OpenCvViewport) : SpecificPipelineHandler<OpMode>(
        { it is OpMode }
) {

    private val onStop = EventHandler("OpModePipelineHandler-onStop")

    override fun preInit() {
        inputSourceManager.setInputSource(inputSourceManager.defaultInputSource)

        ThreadSourceHander.register(VisionInputSourceHander(onStop, viewport))

        pipeline?.telemetry = telemetry
        pipeline?.hardwareMap = HardwareMap()
    }

    override fun init() { }


    override fun processFrame(currentInputSource: InputSource?) {
    }

    override fun onChange(pipeline: OpenCvPipeline, telemetry: Telemetry) {
        this.pipeline?.requestOpModeStop()
        onStop.run()

        super.onChange(pipeline, telemetry)
    }

}
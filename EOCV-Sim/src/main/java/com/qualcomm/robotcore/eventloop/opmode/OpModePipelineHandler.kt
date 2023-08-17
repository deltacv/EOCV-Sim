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

enum class OpModeType { AUTONOMOUS, TELEOP }

class OpModePipelineHandler(val inputSourceManager: InputSourceManager, private val viewport: OpenCvViewport) : SpecificPipelineHandler<OpMode>(
        { it is OpMode }
) {

    private val onStop = EventHandler("OpModePipelineHandler-onStop")

    override fun preInit() {
        if(pipeline == null) return

        inputSourceManager.setInputSource(inputSourceManager.defaultInputSource)
        ThreadSourceHander.register(VisionInputSourceHander(pipeline!!.notifier, viewport))

        pipeline?.telemetry = telemetry
        pipeline?.hardwareMap = HardwareMap()
    }

    override fun init() { }

    override fun processFrame(currentInputSource: InputSource?) {
    }

    override fun onChange(beforePipeline: OpenCvPipeline?, newPipeline: OpenCvPipeline, telemetry: Telemetry) {
        if(beforePipeline is OpMode && beforePipeline == pipeline) {
            beforePipeline.requestOpModeStop()
            onStop.run()
        }

        super.onChange(beforePipeline, newPipeline, telemetry)
    }

}

val Class<*>.opModeType get() = when {
    this.autonomousAnnotation != null -> OpModeType.AUTONOMOUS
    this.teleopAnnotation != null -> OpModeType.TELEOP
    else -> null
}

val OpMode.opModeType get() = this.javaClass.opModeType

val Class<*>.autonomousAnnotation get() = this.getAnnotation(Autonomous::class.java)
val Class<*>.teleopAnnotation get() = this.getAnnotation(TeleOp::class.java)

val OpMode.autonomousAnnotation get() = this.javaClass.autonomousAnnotation
val OpMode.teleopAnnotation get() = this.javaClass.teleopAnnotation
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

/**
 * Enum class to classify the type of an OpMode
 */
enum class OpModeType { AUTONOMOUS, TELEOP }

/**
 * Handler for OpModes in the pipeline manager
 * It helps to manage the lifecycle of OpModes
 * and to set the input source and viewport
 * within it in order to transform a pipeline
 * lifecycle into an OpMode lifecycle
 * @param inputSourceManager the input source manager to set the input source
 * @param viewport the viewport to set in the OpMode
 * @see SpecificPipelineHandler
 */
class OpModePipelineHandler(val inputSourceManager: InputSourceManager, private val viewport: OpenCvViewport) : SpecificPipelineHandler<OpMode>(
        { it is OpMode }
) {

    private val onStop = EventHandler("OpModePipelineHandler-onStop")

    /**
     * Set up for the OpMode lifecycle
     * It sets the input source and registers the VisionInputSourceHander
     * to let the OpMode request and find input sources from the simulator
     */
    override fun preInit() {
        if(pipeline == null) return

        inputSourceManager.setInputSource(null)
        ThreadSourceHander.register(VisionInputSourceHander(pipeline?.notifier ?: return, viewport))

        pipeline?.telemetry = telemetry
        pipeline?.hardwareMap = HardwareMap()    }

    override fun init() { }

    override fun processFrame(currentInputSource: InputSource?) {
    }

    /**
     * Stops the OpMode and the pipeline if an exception is thrown
     * It also calls the onStop event to notify any listeners
     * that the OpMode has stopped
     */
    override fun onException() {
        if(pipeline != null) {
            pipeline!!.forceStop()
            onStop.run()
        }
    }

    /**
     * Stops the OpMode and the pipeline when the pipeline is changed
     * It also calls the onStop event to notify any listeners
     * that the OpMode has stopped
     */
    override fun onChange(beforePipeline: OpenCvPipeline?, newPipeline: OpenCvPipeline, telemetry: Telemetry) {
        if(beforePipeline is OpMode) {
            beforePipeline.forceStop()
            onStop.run()
        }

        super.onChange(beforePipeline, newPipeline, telemetry)
    }

}

/**
 * Extension function to get the OpModeType of a class from its annotations
 * @return the OpModeType of the class
 */
val Class<*>.opModeType get() = when {
    this.autonomousAnnotation != null -> OpModeType.AUTONOMOUS
    this.teleopAnnotation != null -> OpModeType.TELEOP
    else -> null
}

/**
 * Extension function to get the OpModeType of an OpMode from its annotations
 * @return the OpModeType of the OpMode
 */
val OpMode.opModeType get() = this.javaClass.opModeType

/**
 * Extension function to get the Autonomous annotation of a class
 * @return the Autonomous annotation of the class
 */
val Class<*>.autonomousAnnotation get() = this.getAnnotation(Autonomous::class.java)

/**
 * Extension function to get the TeleOp annotation of a class
 * @return the TeleOp annotation of the class
 */
val Class<*>.teleopAnnotation get() = this.getAnnotation(TeleOp::class.java)

/**
 * Extension function to get the Autonomous annotation of an OpMode
 */
val OpMode.autonomousAnnotation get() = this.javaClass.autonomousAnnotation
/**
 * Extension function to get the TeleOp annotation of an OpMode
 */
val OpMode.teleopAnnotation get() = this.javaClass.teleopAnnotation
/* Copyright (c) 2014 Qualcomm Technologies Inc

All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted (subject to the limitations in the disclaimer below) provided that
the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of Qualcomm Technologies Inc nor the names of its contributors
may be used to endorse or promote products derived from this software without
specific prior written permission.

NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.hardware.HardwareMap;
import io.github.deltacv.vision.external.util.FrameQueue;
import org.openftc.easyopencv.TimestampedOpenCvPipeline;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.opencv.core.Mat;

public abstract class OpMode extends TimestampedOpenCvPipeline { // never in my life would i have imagined...

    public Telemetry telemetry;

    volatile boolean isStarted = false;
    volatile boolean stopRequested = false;

    protected FrameQueue inputQueue;

    public HardwareMap hardwareMap;

    public OpMode(int maxInputQueueCapacity) {
        inputQueue = new FrameQueue(maxInputQueueCapacity);
    }

    public OpMode() {
        this(10);
    }

    /* BEGIN OpMode abstract methods */

    /**
     * User defined init method
     * <p>
     * This method will be called once when the INIT button is pressed.
     */
    abstract public void init();

    /**
     * User defined init_loop method
     * <p>
     * This method will be called repeatedly when the INIT button is pressed.
     * This method is optional. By default this method takes no action.
     */
    public void init_loop() {};

    /**
     * User defined start method.
     * <p>
     * This method will be called once when the PLAY button is first pressed.
     * This method is optional. By default this method takes not action.
     * Example usage: Starting another thread.
     *
     */
    public void start() {};

    /**
     * User defined loop method
     * <p>
     * This method will be called repeatedly in a loop while this op mode is running
     */
    abstract public void loop();

    /**
     * User defined stop method
     * <p>
     * This method will be called when this op mode is first disabled.
     * <p>
     * The stop method is optional. By default this method takes no action.
     */
    public void stop() {};

    public void requestOpModeStop() {
        stop();
    }

    /* BEGIN OpenCvPipeline Impl */

    @Override
    public final void init(Mat mat) {
        init();
        telemetry.update();
    }

    private boolean startCalled = false;

    @Override
    public final Mat processFrame(Mat input, long captureTimeNanos) {
        if(!startCalled) {
            start();
            startCalled = true;
            telemetry.update();
        }

        loop();
        telemetry.update();

        return null; // OpModes don't actually show anything to the viewport, we'll delegate that
    }

    @Override
    public final void onViewportTapped() {
    }

}

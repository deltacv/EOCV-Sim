/*
 * Copyright (c) 2023 OpenFTC & EOCV-Sim implementation by Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.deltacv.vision.external;

import io.github.deltacv.vision.external.source.VisionSource;
import io.github.deltacv.vision.external.source.FrameReceiver;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraControls;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.*;
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibrationIdentity;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.openftc.easyopencv.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrameReceiverOpenCvCamera extends OpenCvCameraBase implements OpenCvWebcam, FrameReceiver {

    private final VisionSource source;
    OpenCvViewport handedViewport;

    boolean streaming = false;

    private Logger logger = LoggerFactory.getLogger(FrameReceiverOpenCvCamera.class);

    public FrameReceiverOpenCvCamera(VisionSource source, OpenCvViewport handedViewport, boolean viewportEnabled) {
        super(handedViewport, viewportEnabled);

        this.source = source;
        this.handedViewport = handedViewport;
    }

    @Override
    public int openCameraDevice() {
        prepareForOpenCameraDevice();

        return source.init();
    }

    @Override
    public void openCameraDeviceAsync(AsyncCameraOpenListener cameraOpenListener) {
        new Thread(() -> {
            int code = openCameraDevice();

            try {
                if (code == 0) {
                    cameraOpenListener.onOpened();
                } else {
                    cameraOpenListener.onError(code);
                }
            } catch (Exception e) {
                logger.error("Error in camera open listener", e);
            }
        }).start();
    }

    @Override
    public void closeCameraDevice() {
        synchronized (source) {
            source.close();
        }
    }

    @Override
    public void closeCameraDeviceAsync(AsyncCameraCloseListener cameraCloseListener) {
        new Thread(() -> {
            closeCameraDevice();
            cameraCloseListener.onClose();
        }).start();
    }

    @Override
    public void startStreaming(int width, int height) {
        startStreaming(width, height, getDefaultRotation());
    }

    @Override
    public void startStreaming(int width, int height, OpenCvCameraRotation rotation) {
        prepareForStartStreaming(width, height, rotation);

        synchronized (source) {
            source.start(new Size(width, height));
            source.attach(this);
        }

        streaming = true;
    }

    @Override
    public void stopStreaming() {
        source.stop();
        cleanupForEndStreaming();
    }


    @Override
    protected OpenCvCameraRotation getDefaultRotation() {
        return OpenCvCameraRotation.UPRIGHT;
    }

    @Override
    protected int mapRotationEnumToOpenCvRotateCode(OpenCvCameraRotation rotation)
    {
        /*
         * The camera sensor in a webcam is mounted in the logical manner, such
         * that the raw image is upright when the webcam is used in its "normal"
         * orientation. However, if the user is using it in any other orientation,
         * we need to manually rotate the image.
         */

        if(rotation == OpenCvCameraRotation.SENSOR_NATIVE)
        {
            return -1;
        }
        else if(rotation == OpenCvCameraRotation.SIDEWAYS_LEFT)
        {
            return Core.ROTATE_90_COUNTERCLOCKWISE;
        }
        else if(rotation == OpenCvCameraRotation.SIDEWAYS_RIGHT)
        {
            return Core.ROTATE_90_CLOCKWISE;
        }
        else if(rotation == OpenCvCameraRotation.UPSIDE_DOWN)
        {
            return Core.ROTATE_180;
        }
        else
        {
            return -1;
        }
    }

    @Override
    protected boolean cameraOrientationIsTiedToDeviceOrientation() {
        return false;
    }

    @Override
    protected boolean isStreaming() {
        return streaming;
    }

    @Override
    public void onFrameStart() {
        if(!isStreaming()) return;

        notifyStartOfFrameProcessing();
    }

    @Override
    public void setMillisecondsPermissionTimeout(int ms) { }

    @Override
    public void startStreaming(int width, int height, OpenCvCameraRotation rotation, StreamFormat streamFormat) {
        startStreaming(width, height, rotation);
    }

    @Override
    public ExposureControl getExposureControl() {
        return getControl(ExposureControl.class);
    }

    @Override
    public FocusControl getFocusControl() {
        return getControl(FocusControl.class);
    }

    @Override
    public PtzControl getPtzControl() {
        return getControl(PtzControl.class);
    }

    @Override
    public GainControl getGainControl() {
        return getControl(GainControl.class);
    }

    @Override
    public WhiteBalanceControl getWhiteBalanceControl() {
        return getControl(WhiteBalanceControl.class);
    }

    @Override
    public <T extends CameraControl> T getControl(Class<T> controlType) {
        return source.getControlMap().get(controlType);
    }

    @Override
    public CameraCalibrationIdentity getCalibrationIdentity() {
        return null; // TODO
    }

    //
    // Inheritance from Sourced
    //
    @Override
    public void onNewFrame(Mat frame, long timestamp) {
        if(!isStreaming()) return;
        if(frame == null || frame.empty()) return;

        handleFrameUserCrashable(frame, timestamp);
    }

    @Override
    public void stop() {
        closeCameraDevice();
    }
}
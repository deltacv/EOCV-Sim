package io.github.deltacv.vision.external;

import io.github.deltacv.vision.external.source.VisionSource;
import io.github.deltacv.vision.external.source.VisionSourced;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.openftc.easyopencv.*;

public class SourcedOpenCvCamera extends OpenCvCameraBase implements OpenCvWebcam, VisionSourced {

    private final VisionSource source;
    OpenCvViewport handedViewport;

    boolean streaming = false;

    public SourcedOpenCvCamera(VisionSource source, OpenCvViewport handedViewport, boolean viewportEnabled) {
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

            if(code == 0) {
                cameraOpenListener.onOpened();
            } else {
                cameraOpenListener.onError(code);
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

    //
    // Inheritance from Sourced
    //

    @Override
    public void onNewFrame(Mat frame, long timestamp) {
        if(!isStreaming()) return;
        if(frame.empty() || frame == null) return;

        handleFrameUserCrashable(frame, timestamp);
    }

    @Override
    public void stop() {
        closeCameraDevice();
    }
}